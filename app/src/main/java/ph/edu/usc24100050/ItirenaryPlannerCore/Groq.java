package ph.edu.usc24100050.ItirenaryPlannerCore;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Groq implements LLMAPI {
    private static final String TAG = "GroqAPI";
    
    private static final String API_KEY = "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";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public CompletableFuture<String> ask(String text, String role, String responseFormat) {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            JSONObject payload = new JSONObject();
            payload.put("model", MODEL);
            
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", role));
            messages.put(new JSONObject().put("role", "user").put("content", text));
            payload.put("messages", messages);

            String jsonPayload;
            if (responseFormat != null && !responseFormat.trim().isEmpty()) {
                String basePayload = payload.toString();
                jsonPayload = basePayload.substring(0, basePayload.length() - 1) + ", " + responseFormat + "}";
            } else {
                jsonPayload = payload.toString();
            }

            Log.d(TAG, "Request: " + jsonPayload);

            RequestBody requestBody = RequestBody.create(
                    jsonPayload, MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(GROQ_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Log.e(TAG, "Network error", e);
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful() || responseBody == null) {
                            String errorMsg = "HTTP error: " + response.code();
                            if (responseBody != null) {
                                errorMsg += " - " + responseBody.string();
                            }
                            future.completeExceptionally(new IOException(errorMsg));
                            return;
                        }
                        future.complete(responseBody.string());
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error building request", e);
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public CompletableFuture<String> ask(String text, String role) {
        return ask(text, role, "");
    }
}
