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
    private static final String KEY_NAME = "gemini_api_key";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

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
            try {
                // Step 1: Check API key
                String apiKey = getApiKeyInternal();
                Log.d(TAG, "chat() called with message: " + userMessage);
                Log.d(TAG, "API key length: " + apiKey.length());

                if (apiKey.isEmpty()) {
                    Log.e(TAG, "API key is empty!");
                    postError(callback, "API key not set. Please go back and save your Gemini API key.");
                    return;
                }

                // Step 2: Build contents array
                JSONArray contents = new JSONArray();

                // Add conversation history
                for (Message msg : history) {
                    JSONObject part = new JSONObject();
                    part.put("text", msg.getContent());

                    JSONArray parts = new JSONArray();
                    parts.put(part);

                    JSONObject content = new JSONObject();
                    // Gemini uses "user" and "model" (not "assistant")
                    String role = msg.getRole().equals("assistant") ? "model" : "user";
                    content.put("role", role);
                    content.put("parts", parts);
                    contents.put(content);
                }

                JSONObject userPart = new JSONObject();
                userPart.put("text", userMessage);

                JSONArray userParts = new JSONArray();
                userParts.put(userPart);

                JSONObject userContent = new JSONObject();
                userContent.put("role", "user");
                userContent.put("parts", userParts);
                contents.put(userContent);

                // Step 3: Build system instruction (Gemini format)
                JSONObject systemPart = new JSONObject();
                systemPart.put("text", SYSTEM_PROMPT);

                JSONArray systemParts = new JSONArray();
                systemParts.put(systemPart);

                JSONObject systemInstruction = new JSONObject();
                systemInstruction.put("parts", systemParts);

                // Step 4: Build full request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("contents", contents);
                requestBody.put("systemInstruction", systemInstruction);

                Log.d(TAG, "Sending request to Gemini API...");

                // Step 5: Make HTTP request
                String urlWithKey = API_URL + "?key=" + apiKey;

                RequestBody body = RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(urlWithKey)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                // Step 6: Execute and read response
                Response response = client.newCall(request).execute();
                int statusCode = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";

                Log.d(TAG, "Response status code: " + statusCode);
                Log.d(TAG, "Response body: " + responseBody);

                // Step 7: Handle HTTP errors
                if (!response.isSuccessful()) {
                    String errorMsg;
                    switch (statusCode) {
                        case 400:
                            errorMsg = "Bad request. Check your API key format.";
                            break;
                        case 403:
                            errorMsg = "API key invalid or not authorized. Check your Gemini key.";
                            break;
                        case 429:
                            errorMsg = "Too many requests. Please wait a moment and try again.";
                            break;
                        case 500:
                            errorMsg = "Gemini server error. Please try again in a moment.";
                            break;
                        default:
                            errorMsg = "API error " + statusCode + ": " + responseBody;
                    }
                    Log.e(TAG, "HTTP error: " + errorMsg);
                    postError(callback, errorMsg);
                    return;
                }

                JSONObject json = new JSONObject(responseBody);

                if (!json.has("candidates")) {
                    Log.e(TAG, "Response missing 'candidates' field: " + responseBody);
                    postError(callback, "Unexpected response from Gemini. Try again.");
                    return;
                }

                String reply = json
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                Log.d(TAG, "Success! Reply length: " + reply.length());
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(reply));

            } catch (Exception e) {
                Log.e(TAG, "Exception in chat(): " + e.getMessage(), e);
                postError(callback, "Error: " + e.getMessage());
            }
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

    public static void storeApiKey(Context context, String apiKey) {
        try {
            Log.d(TAG, "Storing Gemini API key, length: " + apiKey.trim().length());
            getEncryptedPrefs(context).edit()
                    .putString(KEY_NAME, apiKey.trim())
                    .apply();
            Log.d(TAG, "Gemini API key stored successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to store API key: " + e.getMessage(), e);
        }
    }

    public static String getApiKey(Context context) {
        try {
            String key = getEncryptedPrefs(context).getString(KEY_NAME, "");
            Log.d(TAG, "getApiKey(context) called, key length: " + key.length());
            return key;
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve API key: " + e.getMessage(), e);
            return "";
        }
    }

    private String getApiKeyInternal() {
        return getApiKey(context);
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
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    private void postError(AICallback callback, String message) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onError(message));
    }
}