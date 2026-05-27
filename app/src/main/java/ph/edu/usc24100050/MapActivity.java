package ph.edu.usc24100050;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private IMapController mapController;
    private final OkHttpClient client = new OkHttpClient();

    private Marker pickupMarker;
    private Marker destinationMarker;
    private Polyline routeLine;
    private MyLocationNewOverlay mLocationOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float MAX_DISTANCE_KM = 12.0f; // Limit for fixed fare estimates

    private Button actionButton;
    private Button retryButton;
    private AutoCompleteTextView searchEditText;
    private TextView routeDistance;

    private View fareDetailsContainer;
    private TextView fareCalculation;
    private TextView totalFareText;
    private TextView tabJeepney, tabTaxi, tabMotorcycle;

    private View divider;
    private TextView jeepneyRoutesTitle;
    private TextView jeepneyRoutesText;

    private CardView locationInfoCard;
    private TextView placeName;
    private GeoPoint lastSearchedPoint;

    private boolean isSelectingPickup = true;
    private GeoPoint pickupPoint;
    private GeoPoint destinationPoint;
    private float distanceKm = 0;

    private enum TravelType { JEEPNEY, TAXI, MOTORCYCLE }
    private TravelType selectedType = TravelType.JEEPNEY;

    private SuggestionAdapter adapter;
    private final List<String> suggestions = new ArrayList<>();
    private final List<Address> addressResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        searchEditText = findViewById(R.id.searchEditText);
        ImageButton searchButton = findViewById(R.id.searchButton);
        actionButton = findViewById(R.id.actionButton);
        retryButton = findViewById(R.id.retryButton);
        routeDistance = findViewById(R.id.routeDistance);

        fareDetailsContainer = findViewById(R.id.fareDetailsContainer);
        fareCalculation = findViewById(R.id.fareCalculation);
        totalFareText = findViewById(R.id.totalFareText);

        tabJeepney = findViewById(R.id.tabJeepney);
        tabTaxi = findViewById(R.id.tabTaxi);
        tabMotorcycle = findViewById(R.id.tabMotorcycle);

        divider = findViewById(R.id.divider);
        jeepneyRoutesTitle = findViewById(R.id.jeepneyRoutesTitle);
        jeepneyRoutesText = findViewById(R.id.jeepneyRoutesText);

        locationInfoCard = findViewById(R.id.locationInfoCard);
        placeName = findViewById(R.id.placeName);
        Button setDestinationButton = findViewById(R.id.setDestinationButton);

        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        findViewById(R.id.zoomInButton).setOnClickListener(v -> mapController.zoomIn());
        findViewById(R.id.zoomOutButton).setOnClickListener(v -> mapController.zoomOut());

        mapController = map.getController();
        mapController.setZoom(19.0);

        adapter = new SuggestionAdapter(this, R.layout.item_search_suggestion, suggestions);
        searchEditText.setAdapter(adapter);

        if(getIntent().hasExtra("location"))
        {
            searchEditText.setText(getIntent().getStringExtra("location"));
        }

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) updateSuggestions(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnItemClickListener((parent, view, position, id) -> {
            if (fareDetailsContainer.getVisibility() == View.VISIBLE) {
                Toast.makeText(this, R.string.destination_confirmed, Toast.LENGTH_SHORT).show();
                return;
            }
            Address addr = addressResults.get(position);
            lastSearchedPoint = new GeoPoint(addr.getLatitude(), addr.getLongitude());
            mapController.animateTo(lastSearchedPoint);
            placeName.setText(addr.getFeatureName() != null ? addr.getFeatureName() : addr.getAddressLine(0));
            locationInfoCard.setVisibility(View.VISIBLE);
        });

        setDestinationButton.setOnClickListener(v -> {
            if (fareDetailsContainer.getVisibility() == View.VISIBLE) {
                Toast.makeText(this, R.string.destination_locked, Toast.LENGTH_SHORT).show();
                return;
            }
            if (lastSearchedPoint != null) {
                isSelectingPickup = false;
                handleMapClick(lastSearchedPoint);
                locationInfoCard.setVisibility(View.GONE);
            }
        });

        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
                locationInfoCard.setVisibility(View.GONE);
                handleMapClick(p);
                return true;
            }
            @Override public boolean longPressHelper(GeoPoint p) { return false; }
        }));

        searchButton.setOnClickListener(v -> {
            if (fareDetailsContainer.getVisibility() == View.VISIBLE) {
                Toast.makeText(this, R.string.route_confirmed_reset, Toast.LENGTH_SHORT).show();
                return;
            }
            searchLocation();
        });

        actionButton.setOnClickListener(v -> confirmSelection());
        retryButton.setOnClickListener(v -> handleRetry());
        setupTabs();

        requestLocationPermissions();

        actionButton.setText(R.string.confirm_pickup);
        actionButton.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
    }

    private Bitmap getArrowBitmap() {
        float d = getResources().getDisplayMetrics().density;
        int size = (int) (60 * d);
        Bitmap arrow = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas arrowCanvas = new Canvas(arrow);
        Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.parseColor("#404285F4"));
        arrowCanvas.drawCircle(size / 2f, size / 2f, 28 * d, arrowPaint);
        arrowPaint.setColor(Color.parseColor("#4285F4"));
        Path path = new Path();
        path.moveTo(size / 2f, 8 * d);
        path.lineTo(size / 2f - 14 * d, size - 14 * d);
        path.lineTo(size / 2f, size - 22 * d);
        path.lineTo(size / 2f + 14 * d, size - 14 * d);
        path.close();
        arrowCanvas.drawPath(path, arrowPaint);
        return arrow;
    }

    private void updatePickupMarker(GeoPoint point) {
        if (pickupMarker != null) map.getOverlays().remove(pickupMarker);
        pickupMarker = new Marker(map);
        pickupMarker.setPosition(point);
        pickupMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        pickupMarker.setIcon(new BitmapDrawable(getResources(), getArrowBitmap()));
        pickupMarker.setTitle("Pickup Point");
        map.getOverlays().add(pickupMarker);
        map.invalidate();
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (mLocationOverlay == null) {
            mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            mLocationOverlay.setPersonIcon(getArrowBitmap());
            mLocationOverlay.setPersonAnchor(0.5f, 0.5f);
            mLocationOverlay.setDirectionIcon(getArrowBitmap());
            mLocationOverlay.setDirectionAnchor(0.5f, 0.5f);
            mLocationOverlay.setDrawAccuracyEnabled(false);

            mLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
                GeoPoint myLoc = mLocationOverlay.getMyLocation();
                if (myLoc != null && isSelectingPickup) {
                    pickupPoint = myLoc;
                    updatePickupMarker(myLoc);
                    mapController.animateTo(myLoc);
                }
            }));

            map.getOverlays().add(mLocationOverlay);
        }

        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableFollowLocation();
        map.invalidate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission denied. Map might not track your position.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleMapClick(GeoPoint point) {
        if (fareDetailsContainer.getVisibility() == View.VISIBLE) {
            return;
        }

        boolean isMyLocation = false;
        GeoPoint myLoc = (mLocationOverlay != null) ? mLocationOverlay.getMyLocation() : null;

        if (myLoc != null && point.distanceToAsDouble(myLoc) < 50.0) {
            isMyLocation = true;
            point = myLoc;
        }

        if (isSelectingPickup) {
            if (isMyLocation) {
                pickupPoint = point;
                updatePickupMarker(point);
                Toast.makeText(this, "Pickup set to your current location.", Toast.LENGTH_SHORT).show();
                retryButton.setVisibility(View.VISIBLE);
                actionButton.setText(R.string.confirm_pickup);
                actionButton.setVisibility(View.VISIBLE);
            }
            return;
        }

        destinationPoint = point;
        if (destinationMarker != null) map.getOverlays().remove(destinationMarker);
        destinationMarker = createMarker(point, "Destination", R.drawable.ic_location_pin);
        retryButton.setVisibility(View.VISIBLE);
        actionButton.setText(R.string.confirm_destination);
        actionButton.setVisibility(View.VISIBLE);
        if (pickupPoint != null) {
            fetchRoadRoute(pickupPoint, destinationPoint);
        }
        map.invalidate();
    }

    private Marker createMarker(GeoPoint point, String title, int iconRes) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(this, iconRes));
        marker.setTitle(title);
        map.getOverlays().add(marker);
        return marker;
    }

    private void handleRetry() {
        if (fareDetailsContainer.getVisibility() == View.VISIBLE) {
            resetApp();
        } else if (!isSelectingPickup) {
            if (destinationMarker != null) map.getOverlays().remove(destinationMarker);
            destinationPoint = null;
            actionButton.setText(R.string.confirm_destination);
            actionButton.setVisibility(View.GONE);
            if (routeLine != null) map.getOverlays().remove(routeLine);
            routeDistance.setText(R.string.pin_dest_hint);
            hideTransit();
            searchEditText.setEnabled(true);
        } else {
            resetApp();
        }
        map.invalidate();
    }

    private void resetApp() {
        isSelectingPickup = true;
        GeoPoint myLoc = (mLocationOverlay != null) ? mLocationOverlay.getMyLocation() : null;
        pickupPoint = myLoc;
        destinationPoint = null;
        distanceKm = 0;
        if (pickupMarker != null) map.getOverlays().remove(pickupMarker);
        if (destinationMarker != null) map.getOverlays().remove(destinationMarker);
        if (routeLine != null) map.getOverlays().remove(routeLine);

        if (myLoc != null) {
            mapController.animateTo(myLoc);
            updatePickupMarker(myLoc);
            mLocationOverlay.enableFollowLocation();
        }

        actionButton.setText(R.string.confirm_pickup);
        actionButton.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);

        fareDetailsContainer.setVisibility(View.INVISIBLE);
        routeDistance.setText(R.string.search_hint);
        hideTransit();

        searchEditText.setEnabled(true);
        searchEditText.setText("");

        map.invalidate();
    }

    private void confirmSelection() {
        if (isSelectingPickup && pickupPoint != null) {
            isSelectingPickup = false;
            actionButton.setVisibility(View.GONE);
            Toast.makeText(this, R.string.pickup_confirmed, Toast.LENGTH_SHORT).show();
            if (mLocationOverlay != null) mLocationOverlay.disableFollowLocation();
        } else if (destinationPoint != null) {
            actionButton.setVisibility(View.GONE);
            fareDetailsContainer.setVisibility(View.VISIBLE);
            searchEditText.setEnabled(false);
        }
    }

    private void updateSuggestions(String query) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(query + ", Cebu", 5);
                runOnUiThread(() -> {
                    suggestions.clear();
                    addressResults.clear();
                    if (addresses != null) {
                        for (Address addr : addresses) {
                            suggestions.add(addr.getAddressLine(0));
                            addressResults.add(addr);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
            } catch (IOException ignored) {}
        }).start();
    }

    private void fetchRoadRoute(GeoPoint start, GeoPoint end) {
        String url = String.format(Locale.US, "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                start.getLongitude(), start.getLatitude(), end.getLongitude(), end.getLatitude());

        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray routes = json.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            distanceKm = (float) route.getDouble("distance") / 1000f;
                            JSONArray coords = route.getJSONObject("geometry").getJSONArray("coordinates");
                            List<GeoPoint> roadPoints = new ArrayList<>();
                            for (int i = 0; i < coords.length(); i++) {
                                JSONArray p = coords.getJSONArray(i);
                                roadPoints.add(new GeoPoint(p.getDouble(1), p.getDouble(0)));
                            }
                            runOnUiThread(() -> {
                                drawRouteLine(roadPoints);
                                updateFareDisplay();
                                routeDistance.setText(getString(R.string.distance_label, distanceKm));
                                if (selectedType == TravelType.JEEPNEY && distanceKm <= MAX_DISTANCE_KM) fetchJeepneyRoutes();
                            });
                        }
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void drawRouteLine(List<GeoPoint> points) {
        if (routeLine != null) map.getOverlays().remove(routeLine);
        routeLine = new Polyline(map);
        routeLine.setPoints(points);
        routeLine.getOutlinePaint().setColor(Color.parseColor("#008DFF"));
        routeLine.getOutlinePaint().setStrokeWidth(14f);
        map.getOverlays().add(routeLine);
        map.invalidate();
    }

    private void fetchJeepneyRoutes() {
        if (pickupPoint == null || destinationPoint == null) return;

        runOnUiThread(() -> {
            divider.setVisibility(View.VISIBLE);
            jeepneyRoutesTitle.setVisibility(View.VISIBLE);
            jeepneyRoutesTitle.setText(R.string.jeepney_lines_title);
            jeepneyRoutesTitle.setTextSize(18);
            jeepneyRoutesText.setVisibility(View.VISIBLE);
            jeepneyRoutesText.setText(R.string.querying_transit);
        });

        String query = "[out:json][timeout:25];" +
                "relation[\"route\"~\"share_taxi|bus\"](around:1200," + pickupPoint.getLatitude() + "," + pickupPoint.getLongitude() + ")->.r1;" +
                "relation.r1(around:1200," + destinationPoint.getLatitude() + "," + destinationPoint.getLongitude() + ");" +
                "out tags;";

        String url = "https://overpass-api.de/api/interpreter?data=" + Uri.encode(query);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "CebuTravelApp/1.0")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> jeepneyRoutesText.setText(R.string.connection_error));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String body = response.body().string();
                        JSONArray elements = new JSONObject(body).getJSONArray("elements");

                        Map<String, String> routeMap = new TreeMap<>();
                        for (int i = 0; i < elements.length(); i++) {
                            JSONObject tags = elements.getJSONObject(i).getJSONObject("tags");
                            String ref = tags.optString("ref", "");
                            if (ref.isEmpty()) continue;

                            String from = tags.optString("from", "");
                            String to = tags.optString("to", "");
                            String via = tags.optString("via", "");
                            String name = tags.optString("name", "");

                            String routeDesc = "";
                            if (!from.isEmpty() && !to.isEmpty()) {
                                routeDesc = from + (via.isEmpty() ? "" : " - " + via) + " - " + to;
                            } else if (!name.isEmpty()) {
                                routeDesc = name.replace("Jeepney " + ref, "").replace(ref, "").trim();
                                if (routeDesc.startsWith(":") || routeDesc.startsWith("-")) routeDesc = routeDesc.substring(1).trim();
                            }

                            if (!routeMap.containsKey(ref) || (routeMap.get(ref) != null && routeMap.get(ref).length() < routeDesc.length())) {
                                routeMap.put(ref, routeDesc);
                            }
                        }

                        runOnUiThread(() -> {
                            if (routeMap.isEmpty()) {
                                jeepneyRoutesText.setText(R.string.no_jeepneys);
                            } else {
                                SpannableStringBuilder ssb = new SpannableStringBuilder();
                                int count = 0;
                                for (Map.Entry<String, String> entry : routeMap.entrySet()) {
                                    String code = entry.getKey();
                                    String desc = entry.getValue();

                                    int start = ssb.length();
                                    ssb.append("• ").append(code);
                                    int end = ssb.length();

                                    ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    ssb.setSpan(new RelativeSizeSpan(1.4f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#008DFF")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                                    if (desc != null && !desc.isEmpty()) {
                                        int descStart = ssb.length();
                                        ssb.append("\n    ").append(desc);
                                        ssb.setSpan(new RelativeSizeSpan(0.95f), descStart, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                        ssb.setSpan(new ForegroundColorSpan(Color.DKGRAY), descStart, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    }

                                    ssb.append("\n\n");
                                    if (++count >= 15) break;
                                }
                                jeepneyRoutesText.setText(ssb);
                            }
                        });
                    } catch (Exception ignored) {
                        runOnUiThread(() -> jeepneyRoutesText.setText(R.string.transit_error));
                    }
                } else {
                    runOnUiThread(() -> jeepneyRoutesText.setText(R.string.transit_unavailable));
                }
            }
        });
    }

    private void setupTabs() {
        tabJeepney.setOnClickListener(v -> selectTab(TravelType.JEEPNEY));
        tabTaxi.setOnClickListener(v -> selectTab(TravelType.TAXI));
        tabMotorcycle.setOnClickListener(v -> selectTab(TravelType.MOTORCYCLE));
    }

    private void selectTab(TravelType type) {
        selectedType = type;
        Drawable sel = ContextCompat.getDrawable(this, R.drawable.tab_selected_bg);
        tabJeepney.setBackground(null); tabTaxi.setBackground(null); tabMotorcycle.setBackground(null);
        tabJeepney.setTextColor(Color.GRAY); tabTaxi.setTextColor(Color.GRAY); tabMotorcycle.setTextColor(Color.GRAY);
        if (type == TravelType.JEEPNEY) {
            tabJeepney.setBackground(sel); tabJeepney.setTextColor(Color.WHITE);
            if (distanceKm > 0 && distanceKm <= MAX_DISTANCE_KM) fetchJeepneyRoutes();
            else hideTransit();
        } else if (type == TravelType.TAXI) {
            tabTaxi.setBackground(sel); tabTaxi.setTextColor(Color.WHITE);
            hideTransit();
        } else {
            tabMotorcycle.setBackground(sel); tabMotorcycle.setTextColor(Color.WHITE);
            hideTransit();
        }
        if (distanceKm > 0) updateFareDisplay();
    }

    private void hideTransit() { divider.setVisibility(View.GONE); jeepneyRoutesTitle.setVisibility(View.GONE); jeepneyRoutesText.setVisibility(View.GONE); }

    private void updateFareDisplay() {
        if (distanceKm == 0) return;

        if (distanceKm > MAX_DISTANCE_KM) {
            fareCalculation.setText(R.string.destination_too_far);
            fareCalculation.setTextColor(Color.RED);
            fareCalculation.setTextSize(20);
            fareCalculation.setTypeface(null, Typeface.BOLD);
            totalFareText.setText(R.string.price_depends_on_driver);
            totalFareText.setTextColor(Color.BLACK);
            totalFareText.setTextSize(18);
            totalFareText.setTypeface(null, Typeface.BOLD);
            hideTransit();
            return;
        }

        fareCalculation.setTextColor(Color.GRAY);
        fareCalculation.setTextSize(13);
        fareCalculation.setTypeface(null, Typeface.NORMAL);
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        if (selectedType == TravelType.JEEPNEY) {
            double base = 13, rate = 1.0;
            double total = base + (Math.max(0, distanceKm - 2) * rate);
            fareCalculation.setText(getString(R.string.fare_base_rate, base, rate));
            totalFareText.setText(getString(R.string.total_fare, total));
            totalFareText.setTextColor(Color.parseColor("#008DFF"));
            totalFareText.setTextSize(22);
            totalFareText.setTypeface(null, Typeface.BOLD);
        } else if (selectedType == TravelType.TAXI) {
            fareCalculation.setText(R.string.taxi_estimates);
            double normal = 45 + (Math.max(0, distanceKm - 2) * 13.5);
            double greenGsm = 40 + (Math.max(0, distanceKm - 2) * 13.5);
            double grab = 45 + (Math.max(0, distanceKm - 2) * 13.5) + 45;

            appendFareRow(ssb, "Normal Taxi", normal, 45, 13.5, null);
            appendFareRow(ssb, "Green GSM", greenGsm, 40, 13.5, null);
            appendFareRow(ssb, "Grab Taxi", grab, 45, 13.5, "₱45.00 booking fee");

            totalFareText.setText(ssb);
            totalFareText.setTextColor(Color.parseColor("#333333"));
            totalFareText.setTextSize(16);
            totalFareText.setTypeface(null, Typeface.NORMAL);
        } else if (selectedType == TravelType.MOTORCYCLE) {
            fareCalculation.setText(R.string.motor_estimates);
            double moveit = 50 + (Math.max(0, distanceKm - 2) * 10);
            double maxim = 30 + (Math.max(0, distanceKm - 2) * 8);
            double angkas = 50 + (Math.max(0, distanceKm - 2) * 10);

            appendFareRow(ssb, "Moveit", moveit, 50, 10, null);
            appendFareRow(ssb, "Maxim", maxim, 30, 8, null);
            appendFareRow(ssb, "Angkas", angkas, 50, 10, null);

            totalFareText.setText(ssb);
            totalFareText.setTextColor(Color.parseColor("#333333"));
            totalFareText.setTextSize(16);
            totalFareText.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void appendFareRow(SpannableStringBuilder ssb, String name, double price, double base, double rate, String extra) {
        int start = ssb.length();
        ssb.append(name).append(": ");
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(String.format(Locale.US, "₱%.2f\n", price));

        int formulaStart = ssb.length();
        String formula = String.format(Locale.US, "    Base: ₱%.2f | Rate: ₱%.2f/km%s\n\n", base, rate, extra != null ? " + " + extra : "");
        ssb.append(formula);
        ssb.setSpan(new RelativeSizeSpan(0.85f), formulaStart, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new ForegroundColorSpan(Color.GRAY), formulaStart, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void searchLocation() {
        String loc = searchEditText.getText().toString();
        if (loc.isEmpty()) return;
        new Thread(() -> {
            try {
                List<Address> list = new Geocoder(this).getFromLocationName(loc + ", Cebu", 1);
                if (list != null && !list.isEmpty()) {
                    runOnUiThread(() -> {
                        Address a = list.get(0);
                        lastSearchedPoint = new GeoPoint(a.getLatitude(), a.getLongitude());
                        mapController.animateTo(lastSearchedPoint);
                        placeName.setText(a.getFeatureName());
                        locationInfoCard.setVisibility(View.VISIBLE);
                    });
                }
            } catch (IOException ignored) {}
        }).start();
    }

    @Override public void onResume() {
        super.onResume();
        map.onResume();
        if (mLocationOverlay != null) mLocationOverlay.enableMyLocation();
    }

    @Override public void onPause() {
        super.onPause();
        map.onPause();
        if (mLocationOverlay != null) mLocationOverlay.disableMyLocation();
    }
}
