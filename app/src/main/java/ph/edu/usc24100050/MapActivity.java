package ph.edu.usc24100050;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private Button setDestinationButton;
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
        setDestinationButton = findViewById(R.id.setDestinationButton);

        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        findViewById(R.id.zoomInButton).setOnClickListener(v -> mapController.zoomIn());
        findViewById(R.id.zoomOutButton).setOnClickListener(v -> mapController.zoomOut());

        mapController = map.getController();
        mapController.setZoom(15.0);
        mapController.setCenter(new GeoPoint(10.3157, 123.8854));

        adapter = new SuggestionAdapter(this, R.layout.item_search_suggestion, suggestions);
        searchEditText.setAdapter(adapter);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) updateSuggestions(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnItemClickListener((parent, view, position, id) -> {
            Address addr = addressResults.get(position);
            lastSearchedPoint = new GeoPoint(addr.getLatitude(), addr.getLongitude());
            mapController.animateTo(lastSearchedPoint);
            placeName.setText(addr.getFeatureName() != null ? addr.getFeatureName() : addr.getAddressLine(0));
            locationInfoCard.setVisibility(View.VISIBLE);
        });

        setDestinationButton.setOnClickListener(v -> {
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

        searchButton.setOnClickListener(v -> searchLocation());

        actionButton.setOnClickListener(v -> confirmSelection());
        retryButton.setOnClickListener(v -> handleRetry());
        setupTabs();
    }

    private void handleMapClick(GeoPoint point) {
        if (isSelectingPickup) {
            pickupPoint = point;
            if (pickupMarker != null) map.getOverlays().remove(pickupMarker);
            pickupMarker = createMarker(point, "Pickup", R.drawable.ic_location_pin);
            retryButton.setVisibility(View.VISIBLE);
            actionButton.setText(R.string.confirm_pickup);
            actionButton.setVisibility(View.VISIBLE);
        } else {
            destinationPoint = point;
            if (destinationMarker != null) map.getOverlays().remove(destinationMarker);
            destinationMarker = createMarker(point, "Destination", R.drawable.ic_location_pin);
            retryButton.setVisibility(View.VISIBLE);
            actionButton.setText(R.string.confirm_destination);
            actionButton.setVisibility(View.VISIBLE);
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
            // Case: Destination pinned but not confirmed, clear only destination
            if (destinationMarker != null) map.getOverlays().remove(destinationMarker);
            destinationPoint = null;
            actionButton.setText(R.string.confirm_destination);
            actionButton.setVisibility(View.GONE); // Hide until pinned again
            if (routeLine != null) map.getOverlays().remove(routeLine);
            routeDistance.setText("Pin destination to calculate");
            hideTransit();
        } else {
            // Case: Pickup pinned, clear pickup
            if (pickupMarker != null) map.getOverlays().remove(pickupMarker);
            pickupPoint = null;
            actionButton.setVisibility(View.GONE);
            retryButton.setVisibility(View.GONE);
        }
        map.invalidate();
    }

    private void resetApp() {
        isSelectingPickup = true;
        pickupPoint = null;
        destinationPoint = null;
        distanceKm = 0;
        if (pickupMarker != null) map.getOverlays().remove(pickupMarker);
        if (destinationMarker != null) map.getOverlays().remove(destinationMarker);
        if (routeLine != null) map.getOverlays().remove(routeLine);
        actionButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        fareDetailsContainer.setVisibility(View.INVISIBLE);
        routeDistance.setText(R.string.search_hint);
        hideTransit();
        map.invalidate();
    }

    private void confirmSelection() {
        if (isSelectingPickup && pickupPoint != null) {
            isSelectingPickup = false;
            actionButton.setVisibility(View.GONE);
            Toast.makeText(this, "Pickup point confirmed! Now pin your destination.", Toast.LENGTH_SHORT).show();
        } else if (destinationPoint != null) {
            actionButton.setVisibility(View.GONE);
            fareDetailsContainer.setVisibility(View.VISIBLE);
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
                    for (Address addr : addresses) {
                        suggestions.add(addr.getAddressLine(0));
                        addressResults.add(addr);
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
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
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
                                if (selectedType == TravelType.JEEPNEY) fetchJeepneyRoutes();
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
            jeepneyRoutesText.setVisibility(View.VISIBLE);
            jeepneyRoutesText.setText("Querying transit data for Cebu...");
        });

        // Intersection query: Find routes near pickup that ALSO pass near destination
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
                runOnUiThread(() -> jeepneyRoutesText.setText("Unable to connect to transit server."));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String body = response.body().string();
                        JSONArray elements = new JSONObject(body).getJSONArray("elements");
                        Set<String> codes = new HashSet<>();
                        for (int i = 0; i < elements.length(); i++) {
                            JSONObject tags = elements.getJSONObject(i).getJSONObject("tags");
                            String ref = tags.optString("ref", "");
                            if (!ref.isEmpty()) codes.add(ref);
                        }
                        runOnUiThread(() -> {
                            if (codes.isEmpty()) {
                                jeepneyRoutesText.setText("No direct jeepneys found connecting these points. Transfer may be required.");
                            } else {
                                StringBuilder sb = new StringBuilder("Direct Cebu Lines:\n");
                                int count = 0;
                                for (String code : codes) {
                                    sb.append("• ").append(code).append("\n");
                                    if (++count >= 10) break;
                                }
                                jeepneyRoutesText.setText(sb.toString().trim());
                            }
                        });
                    } catch (Exception ignored) {
                        runOnUiThread(() -> jeepneyRoutesText.setText("Error reading transit data."));
                    }
                } else {
                    runOnUiThread(() -> jeepneyRoutesText.setText("Transit data currently unavailable."));
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
            if (distanceKm > 0) fetchJeepneyRoutes();
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
        double base = 13, rate = 2;
        if (selectedType == TravelType.TAXI) { base = 45; rate = 13.5; }
        else if (selectedType == TravelType.MOTORCYCLE) { base = 20; rate = 10; }
        double total = base + (Math.max(0, distanceKm - 2) * rate);
        fareCalculation.setText(getString(R.string.fare_base_rate, base, rate));
        totalFareText.setText(getString(R.string.total_fare, total));
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

    @Override public void onResume() { super.onResume(); map.onResume(); }
    @Override public void onPause() { super.onPause(); map.onPause(); }
}
