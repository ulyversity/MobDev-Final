package ph.edu.usc24100050;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CebuAIService {

    private static final String TAG = "CEBU_DEBUG";
    private static final String PREFS_NAME = "cebu_app_prefs";
    private static final String KEY_NAME = "groq_api_key";

    private static final String PRIMARY_KEY = "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";
    private static final String BACKUP_KEY  = ""; 

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile"; 

    private final Context context;
    private final OkHttpClient client;
    private final Executor executor;

    public static final String UMIKO_INTRO =
            "Hi! Mabuhay! 🌺 I am Umiko, your Cebu AI Travel guide. " +
                    "Kanindot ba nga nianhi ka para malingaw og masinati ang kanindot sa Cebu. (How exciting that you came here to have fun and explore the beauty of Cebu.)" +
                    "So, what can I do for you?";

    private static final String SYSTEM_PROMPT =
            "You are Umiko, a warm and bubbly local best friend and travel guide for Cebu, Philippines.\n" +
                    "Your personality: friendly, enthusiastic, knowledgeable, and always supportive — like a bestfriend showing a visitor around.\n\n" +
                    "You specialize in:\n" +
                    "- Tourist spots in Cebu (Kawasan Falls, Oslob whale sharks, Magellan's Cross, etc.)\n" +
                    "- Cebuano food and restaurants (lechon, sutukil, puso, tuwa, danggit, etc.)\n" +
                    "- Local food spots in Cebu City, Mactan, Lapu-Lapu, and nearby towns\n" +
                    "- Practical tips: transport, best time to visit, entrance fees in PHP\n" +
                    "- Creating 1-day, 3-day, and 1-week (7-day) itinerary plans\n" +
                    "Always respond in a warm, bestfriend tone. Keep answers concise for mobile.";

    // ── Shared Itinerary Schema (Nested Format) ──
    public static final String ITINERARY_JSON_SCHEMA = 
        "\"response_format\": {\n" +
        "  \"type\": \"json_schema\",\n" +
        "  \"json_schema\": {\n" +
        "    \"name\": \"cebu_itinerary\",\n" +
        "    \"strict\": true,\n" +
        "    \"schema\": {\n" +
        "      \"type\": \"object\",\n" +
        "      \"properties\": {\n" +
        "        \"itinerary\": {\n" +
        "          \"type\": \"object\",\n" +
        "          \"properties\": {\n" +
        "            \"start_date\": { \"type\": \"string\" },\n" +
        "            \"stop_date\": { \"type\": \"string\" },\n" +
        "            \"total\": { \"type\": \"integer\" },\n" +
        "            \"days\": {\n" +
        "              \"type\": \"array\",\n" +
        "              \"items\": {\n" +
        "                \"type\": \"object\",\n" +
        "                \"properties\": {\n" +
        "                  \"date\": { \"type\": \"string\" },\n" +
        "                  \"day_number\": { \"type\": \"integer\" },\n" +
        "                  \"activities\": {\n" +
        "                    \"type\": \"array\",\n" +
        "                    \"items\": {\n" +
        "                      \"type\": \"object\",\n" +
        "                      \"properties\": {\n" +
        "                        \"venue\": { \"type\": \"string\" },\n" +
        "                        \"activity\": { \"type\": \"string\" },\n" +
        "                        \"start_time\": { \"type\": \"string\", \"description\": \"HHMM format, e.g. 0900\" },\n" +
        "                        \"stop_time\": { \"type\": \"string\", \"description\": \"HHMM format, e.g. 1030\" },\n" +
        "                        \"place_type\": { \"type\": \"string\", \"enum\": [\"HISTORICAL\", \"BEACH\", \"FOOD\", \"NATURE\", \"SHOPPING\", \"RELIGIOUS\"] },\n" +
        "                        \"duration_minutes\": { \"type\": \"integer\" },\n" +
        "                        \"notes\": { \"type\": \"string\" },\n" +
        "                        \"travel_from_previous\": { \"type\": \"string\" },\n" +
        "                        \"latitude\": { \"type\": \"number\" },\n" +
        "                        \"longitude\": { \"type\": \"number\" }\n" +
        "                      },\n" +
        "                      \"required\": [\"venue\", \"activity\", \"start_time\", \"stop_time\", \"place_type\", \"duration_minutes\", \"notes\", \"travel_from_previous\", \"latitude\", \"longitude\"],\n" +
        "                      \"additionalProperties\": false\n" +
        "                    }\n" +
        "                  }\n" +
        "                },\n" +
        "                \"required\": [\"date\", \"day_number\", \"activities\"],\n" +
        "                \"additionalProperties\": false\n" +
        "              }\n" +
        "            }\n" +
        "          },\n" +
        "          \"required\": [\"start_date\", \"stop_date\", \"total\", \"days\"],\n" +
        "          \"additionalProperties\": false\n" +
        "        }\n" +
        "      },\n" +
        "      \"required\": [\"itinerary\"],\n" +
        "      \"additionalProperties\": false\n" +
        "    }\n" +
        "  }\n" +
        "}";

    public interface AICallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public CebuAIService(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void generateItineraryJson(String userMessage, AICallback callback) {
        executor.execute(() -> {
            List<String> keysToTry = new ArrayList<>();
            try {
                String saved = getEncryptedPrefs(context).getString(KEY_NAME, "");
                if (!saved.isEmpty()) keysToTry.add(saved);
            } catch (Exception ignored) {}
            keysToTry.add(PRIMARY_KEY);
            if (!BACKUP_KEY.isEmpty()) keysToTry.add(BACKUP_KEY);

            String lastError = "No API keys available.";

            for (String apiKey : keysToTry) {
                if (apiKey == null || apiKey.isEmpty()) continue;
                try {
                    JSONObject systemMsg = new JSONObject()
                            .put("role", "system")
                            .put("content", "You are Umiko, a Cebu travel planner. " +
                                    "Provide high-quality, real-world itineraries in Cebu. " +
                                    "Default to 3 days if duration isn't specified.");
                    
                    JSONObject userMsg = new JSONObject()
                            .put("role", "user")
                            .put("content", userMessage);

                    JSONArray messages = new JSONArray().put(systemMsg).put(userMsg);

                    String bodyString = "{" +
                            "\"model\": \"" + MODEL + "\"," +
                            "\"messages\": " + messages.toString() + "," +
                            "\"max_tokens\": 4096," +
                            ITINERARY_JSON_SCHEMA +
                            "}";

                    Request request = new Request.Builder()
                            .url(API_URL)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .post(RequestBody.create(bodyString, MediaType.parse("application/json")))
                            .build();

                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        lastError = "API error " + response.code();
                        continue;
                    }

                    JSONObject json = new JSONObject(response.body().string());
                    String reply = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(reply));
                    return;

                } catch (Exception e) {
                    lastError = e.getMessage() != null ? e.getMessage() : "Unknown error";
                }
            }
            postError(callback, lastError);
        });
    }

    public void chat(String userMessage, List<Message> history, AICallback callback) {
        executor.execute(() -> {
            List<String> keysToTry = new ArrayList<>();
            try {
                String saved = getEncryptedPrefs(context).getString(KEY_NAME, "");
                if (!saved.isEmpty()) keysToTry.add(saved);
            } catch (Exception ignored) {}
            keysToTry.add(PRIMARY_KEY);
            if (!BACKUP_KEY.isEmpty()) keysToTry.add(BACKUP_KEY);

            for (String apiKey : keysToTry) {
                if (apiKey == null || apiKey.isEmpty()) continue;
                try {
                    JSONObject requestBody = buildRequestBody(userMessage, history);
                    Request request = new Request.Builder()
                            .url(API_URL)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                            .build();

                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) continue;

                    JSONObject json = new JSONObject(response.body().string());
                    String reply = json.getJSONArray("choices").getJSONObject(0)
                            .getJSONObject("message").getString("content");

                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(reply));
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Chat attempt failed", e);
                }
            }
            postError(callback, "Failed to connect to AI.");
        });
    }

    private JSONObject buildRequestBody(String userMessage, List<Message> history) throws Exception {
        JSONArray messagesArr = new JSONArray();
        messagesArr.put(new JSONObject().put("role", "system").put("content", SYSTEM_PROMPT));
        for (Message msg : history) {
            messagesArr.put(new JSONObject().put("role", msg.getRole()).put("content", msg.getContent()));
        }
        messagesArr.put(new JSONObject().put("role", "user").put("content", userMessage));
        return new JSONObject().put("model", MODEL).put("messages", messagesArr).put("max_tokens", 1024);
    }

    public static void storeApiKey(Context context, String apiKey) {
        try {
            getEncryptedPrefs(context).edit().putString(KEY_NAME, apiKey.trim()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to store API key", e);
        }
    }

    private static SharedPreferences getEncryptedPrefs(Context context) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
        return EncryptedSharedPreferences.create(context.getApplicationContext(), PREFS_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    private void postError(AICallback callback, String message) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onError(message));
    }
}
