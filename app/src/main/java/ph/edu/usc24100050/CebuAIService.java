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

    // Groq API keys (gsk_...)
    private static final String PRIMARY_KEY = "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";
    private static final String BACKUP_KEY  = ""; // add a second Groq key here if you have one

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile"; // fast & free on Groq

    private final Context context;
    private final OkHttpClient client;
    private final Executor executor;

    private static final String SYSTEM_PROMPT =
            "You are a friendly local tourism assistant for Cebu, Philippines.\n" +
                    "You specialize in:\n" +
                    "- Tourist spots in Cebu (Kawasan Falls, Oslob, Magellan's Cross,\n" +
                    "  Basilica del Santo Niño, Tops Lookout, Temple of Leah, etc.)\n" +
                    "- Cebuano food and restaurants (lechon, sutukil, puso, etc.)\n" +
                    "- Local food spots in Cebu City, Mactan, and nearby towns\n" +
                    "- Practical tips: transport, best time to visit, entrance fees\n\n" +
                    "Always respond in a warm, helpful tone. Keep answers concise\n" +
                    "for mobile. If asked about non-Cebu topics, politely redirect\n" +
                    "the user back to Cebu travel and food.";

    public interface AICallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }
    // ─── Structured JSON itinerary for PlannerActivity ────────────────────────
    private static final String ITINERARY_JSON_PROMPT =
            "You are a Cebu travel planner. Respond ONLY with a valid JSON array. " +
                    "No explanation, no markdown, no code fences. Just the raw JSON array.\n\n" +
                    "Each item must follow this exact structure:\n" +
                    "[{\"day\":1,\"time\":\"09:00 AM\",\"place_name\":\"Magellan's Cross\"," +
                    "\"place_type\":\"HISTORICAL\",\"duration_minutes\":45," +
                    "\"notes\":\"Short tip or cost info\",\"latitude\":10.2929,\"longitude\":123.9018}]\n\n" +
                    "place_type must be one of: HISTORICAL, BEACH, FOOD, NATURE, SHOPPING, RELIGIOUS\n" +
                    "Include 4-6 items per day. Only real places in Cebu with accurate coordinates.";

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
                    JSONArray messages = new JSONArray();
                    messages.put(new JSONObject()
                            .put("role", "system")
                            .put("content", ITINERARY_JSON_PROMPT));
                    messages.put(new JSONObject()
                            .put("role", "user")
                            .put("content", userMessage));

                    JSONObject requestBody = new JSONObject()
                            .put("model", MODEL)
                            .put("messages", messages)
                            .put("max_tokens", 2048);

                    Request request = new Request.Builder()
                            .url(API_URL)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .post(RequestBody.create(
                                    requestBody.toString(),
                                    MediaType.parse("application/json")))
                            .build();

                    Response response = client.newCall(request).execute();
                    int statusCode = response.code();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (statusCode == 401 || statusCode == 403 || statusCode == 429) {
                        lastError = "Key failed (" + statusCode + ")";
                        continue;
                    }
                    if (!response.isSuccessful()) {
                        postError(callback, "API error " + statusCode + ". Please try again.");
                        return;
                    }

                    JSONObject json = new JSONObject(responseBody);
                    String reply = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(reply));
                    return;

                } catch (Exception e) {
                    lastError = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    if (lastError.contains("Unable to resolve host")) {
                        postError(callback, "No internet connection. Please check your network.");
                        return;
                    }
                }
            }
            postError(callback, "All API keys failed. Last error: " + lastError);
        });
    }
    public CebuAIService(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void chat(String userMessage, List<Message> history, AICallback callback) {
        executor.execute(() -> {
            // Build key list: saved key first, then PRIMARY, then BACKUP
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
                    Log.d(TAG, "Trying key ending in: ..." + apiKey.substring(apiKey.length() - 4));

                    JSONObject requestBody = buildRequestBody(userMessage, history);

                    Request request = new Request.Builder()
                            .url(API_URL)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .post(RequestBody.create(
                                    requestBody.toString(),
                                    MediaType.parse("application/json")))
                            .build();

                    Response response = client.newCall(request).execute();
                    int statusCode = response.code();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    Log.d(TAG, "Status: " + statusCode);

                    // Key-related errors — try next key
                    if (statusCode == 401 || statusCode == 403 || statusCode == 429) {
                        lastError = "Key failed (" + statusCode + "), trying next key...";
                        Log.w(TAG, lastError);
                        continue;
                    }

                    // Other HTTP errors — no point retrying
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "API error: " + statusCode + " - " + responseBody);
                        postError(callback, "API error " + statusCode + ". Please try again.");
                        return;
                    }

                    // Parse Groq/OpenAI-style response
                    JSONObject json = new JSONObject(responseBody);
                    String reply = json
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    Log.d(TAG, "Success! Reply length: " + reply.length());
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(reply));
                    return; // Done — stop trying more keys

                } catch (Exception e) {
                    lastError = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    Log.e(TAG, "Key attempt failed: " + lastError);

                    if (lastError.contains("Unable to resolve host")) {
                        postError(callback, "No internet connection. Please check your network.");
                        return;
                    }
                    // Otherwise try the next key
                }
            }

            // All keys exhausted
            Log.e(TAG, "All keys failed. Last error: " + lastError);
            postError(callback, "All API keys failed. Last error: " + lastError);
        });
    }

    public void generateItinerary(int days, List<String> interests, AICallback callback) {
        String prompt = "Create a " + days + "-day Cebu itinerary focused on: "
                + String.join(", ", interests) + ".\n"
                + "Format it as Day 1, Day 2, etc. Include:\n"
                + "- Morning, afternoon, and evening activities\n"
                + "- Recommended local food spots for each day\n"
                + "- Practical tips (transport, estimated costs in PHP)\n"
                + "- Mix of popular and hidden gem spots";
        chat(prompt, new ArrayList<>(), callback);
    }

    public void recommendSpots(String category, AICallback callback) {
        String prompt;
        switch (category) {
            case "food":
                prompt = "Give me the top 5 must-try Cebuano food spots right now, "
                        + "with what to order and rough prices in PHP.";
                break;
            case "beaches":
                prompt = "List the best beaches near Cebu City, "
                        + "with how to get there and entrance fees.";
                break;
            case "history":
                prompt = "What are the top historical and cultural spots in Cebu City "
                        + "for first-time visitors?";
                break;
            case "waterfalls":
                prompt = "Which waterfalls near Cebu are worth visiting? "
                        + "Include difficulty level and travel tips.";
                break;
            default:
                prompt = "Recommend the top tourist spots in Cebu "
                        + "for someone interested in " + category + ".";
        }
        chat(prompt, new ArrayList<>(), callback);
    }

    private JSONObject buildRequestBody(String userMessage, List<Message> history) throws Exception {
        JSONArray messages = new JSONArray();

        // System prompt as first message
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", SYSTEM_PROMPT));

        // Conversation history
        for (Message msg : history) {
            messages.put(new JSONObject()
                    .put("role", msg.getRole()) // "user" or "assistant" — no conversion needed
                    .put("content", msg.getContent()));
        }

        // Current user message
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userMessage));

        return new JSONObject()
                .put("model", MODEL)
                .put("messages", messages)
                .put("max_tokens", 1024);
    }

    public static void storeApiKey(Context context, String apiKey) {
        try {
            getEncryptedPrefs(context).edit()
                    .putString(KEY_NAME, apiKey.trim())
                    .apply();
            Log.d(TAG, "API key stored successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to store API key: " + e.getMessage(), e);
        }
    }

    public static String getApiKey(Context context) {
        try {
            return getEncryptedPrefs(context).getString(KEY_NAME, "");
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve API key: " + e.getMessage(), e);
            return "";
        }
    }

    private static SharedPreferences getEncryptedPrefs(Context context) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                context.getApplicationContext(),
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    private void postError(AICallback callback, String message) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onError(message));
    }
}