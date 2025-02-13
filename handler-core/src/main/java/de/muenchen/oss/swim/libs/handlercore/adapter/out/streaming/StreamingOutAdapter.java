package de.muenchen.oss.swim.libs.handlercore.adapter.out.streaming;

import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingOutAdapter implements FileEventOutPort {
    private final StreamBridge streamBridge;

    @Override
    public void fileFinished(final String useCase, final String presignedUrl, final String metadataPresignedUrl) {
        final FileEventDTO event = new FileEventDTO(useCase, presignedUrl, metadataPresignedUrl);
        final boolean sent = streamBridge.send("finished-out", event);
        if (!sent) {
            throw new MessagingException("Failed to send file finished event");
        }
        log.info("File finished event sent for use case {}", useCase);
    }
}
