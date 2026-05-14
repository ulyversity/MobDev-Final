package ph.edu.usc24100050.ItirenaryPlannerCore;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Groq implements LLMAPI {
    // this class solely interacts with the AI
    private final String API_KEY = "";
    private final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final String MODEL = "openai/gpt-oss-20b";

    @Override
    public CompletableFuture<String> ask(String text, String role, String responseFormat)  {

        CompletableFuture<String> future = new CompletableFuture<>();

        String escapedText = text.replace("\"", "\\\"");

        String jsonPayload = "";
        if (responseFormat.equals(""))
        {

            jsonPayload = String.format(
                    "{\"model\": \"%s\", \"messages\": [{\"role\": \"system\", \"content\": \"%s\"},{\"role\": \"user\", \"content\": \"%s\"}]}",
                    MODEL, role, escapedText
            );
        }
        else {
            jsonPayload = String.format(
                    "{\"model\": \"%s\", \"messages\": [{\"role\": \"system\", \"content\": \"%s\"},{\"role\": \"user\", \"content\": \"%s\"}], %s}",
                    MODEL, role, escapedText, responseFormat
            );
        }

        Log.d("HELLOWORLDJSONPAYLOAD", jsonPayload);




        OkHttpClient client = new OkHttpClient();

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
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    future.completeExceptionally(
                            new IOException("HTTP error: " + response.code())
                    );
                    return;
                }

                future.complete(response.body().string());
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<String> ask(String text, String role) {
        return ask(text, role, "");
    }


}
