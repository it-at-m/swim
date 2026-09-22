package de.muenchen.oss.swim.dispatcher.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import de.muenchen.oss.swim.dispatcher.domain.model.streaming.SingleFileEvent;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MarkFileFinishedUseCaseE2ETest extends DispatchServiceE2ETestBase {
    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void giveFinishedEvent_thenFileAndMetadataAreMovedAndTagged() throws Exception {
        final String filePath = "test/inProcess/e2e-finished.pdf";
        final String metadataPath = "test/inProcess/e2e-finished.json";
        putObject(filePath, "test".getBytes(StandardCharsets.UTF_8));
        putObject(metadataPath, "{}".getBytes(StandardCharsets.UTF_8));
        awaitConsumerGroup("dispatch-finished-e2e");

        sendEvent(FINISHED_TOPIC, new SingleFileEvent(USE_CASE, new de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile(
                presignedUrl(filePath), presignedUrl(metadataPath))), Map.of());

        awaitObjectState(filePath, false);
        awaitObjectState(metadataPath, false);
        awaitObjectState("test/finished/e2e-finished.pdf", true);
        awaitObjectState("test/finished/e2e-finished.json", true);
        assertThat(tags("test/finished/e2e-finished.pdf").get().get("SWIM_State")).isEqualTo("finished");
        assertThat(tags("test/finished/e2e-finished.json").get().get("SWIM_State")).isEqualTo("finished");
        assertThat(meterRegistry.counter("swim_dispatch_finished_count", "use-case", USE_CASE).count()).isEqualTo(2.0);
    }
}
