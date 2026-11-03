package de.muenchen.oss.swim.dispatcher.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import de.muenchen.oss.swim.dispatcher.application.port.in.DispatcherInPort;
import de.muenchen.oss.swim.dispatcher.domain.model.streaming.MultiFileEvent;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DispatcherUseCaseE2ETest extends DispatchServiceE2ETestBase {
    private static final String STATE_TAG = "SWIM_State";

    @Autowired
    private DispatcherInPort dispatcherInPort;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void giveTwoChunkedFilesWithMetadata_thenMultiFileEventIsPublishedAndFilesAreTagged() throws Exception {
        final String firstFilePath = "test/inProcess/e2e-dispatch-1v2.pdf";
        final String secondFilePath = "test/inProcess/e2e-dispatch-2v2.pdf";
        final String firstMetadataPath = "test/inProcess/e2e-dispatch-1v2.json";
        final String secondMetadataPath = "test/inProcess/e2e-dispatch-2v2.json";
        putObject(firstFilePath, "first".getBytes(StandardCharsets.UTF_8));
        putObject(secondFilePath, "second".getBytes(StandardCharsets.UTF_8));
        putObject(firstMetadataPath, "{}".getBytes(StandardCharsets.UTF_8));
        putObject(secondMetadataPath, "{}".getBytes(StandardCharsets.UTF_8));
        tagObject(firstFilePath, Map.of(STATE_TAG, "processed"));
        tagObject(secondFilePath, Map.of(STATE_TAG, "processed"));

        dispatcherInPort.triggerDispatching();

        final MultiFileEvent event = receiveMultiFileEvent("dispatch-output-assertion-e2e");

        assertThat(event.useCase()).isEqualTo(USE_CASE);
        assertThat(event.files()).hasSize(2);
        assertThat(event.files()).allSatisfy(file -> {
            assertThat(file.presignedUrl()).isNotBlank();
            assertThat(file.metadataPresignedUrl()).isNotBlank();
        });
        awaitTag(firstFilePath, STATE_TAG, "sentToKafka");
        awaitTag(secondFilePath, STATE_TAG, "sentToKafka");
        assertThat(meterRegistry.counter("swim_dispatch_dispatched_count", "use-case", USE_CASE, "destination", "dms-out").count())
                .isEqualTo(1.0);
    }
}
