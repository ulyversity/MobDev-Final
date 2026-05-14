package ph.edu.usc24100050.ItirenaryPlannerCore;

import java.util.concurrent.CompletableFuture;

public interface LLMAPI {
    CompletableFuture<String> ask(String text, String role, String responseFormat);
    CompletableFuture<String> ask(String text, String role);
}
