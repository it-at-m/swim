package de.muenchen.oss.swim.dispatcher.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import de.muenchen.oss.swim.dispatcher.domain.model.ErrorDetails;
import de.muenchen.oss.swim.dispatcher.domain.model.streaming.SingleFileEvent;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ErrorHandlerUseCaseE2ETest extends DispatchServiceE2ETestBase {
    @Test
    void giveErrorEvent_thenFileIsTaggedAndNotificationIsSent() throws Exception {
        final String filePath = "test/inProcess/e2e-error.pdf";
        final ErrorDetails error = new ErrorDetails("handler-e2e", "com.example.TestException", "processing failed", "stacktrace");
        putObject(filePath, "test".getBytes(StandardCharsets.UTF_8));
        awaitConsumerGroup("dispatch-dlq-e2e");

        sendEvent(DLQ_TOPIC, new SingleFileEvent(USE_CASE, presignedFile(filePath)), Map.of(
                "x-original-topic", "handler-events",
                "x-exception-fqcn", error.className(),
                "x-exception-message", error.message(),
                "x-exception-stacktrace", error.stacktrace()));

        awaitTag(filePath, "SWIM_State", "error");
        assertThat(tags(filePath).get().get("SWIM_State")).isEqualTo("error");
        assertThat(tags(filePath).get().get("errorClass")).isEqualTo(error.className());
        assertThat(tags(filePath).get().get("errorMessage")).isEqualTo(error.message());
        awaitMail("test-meta@example.com", filePath);
    }
}
