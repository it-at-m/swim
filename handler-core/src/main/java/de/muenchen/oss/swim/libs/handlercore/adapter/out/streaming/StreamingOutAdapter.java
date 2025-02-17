package de.muenchen.oss.swim.libs.handlercore.adapter.out.streaming;

import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEvent;
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
    public void fileFinished(final FileEvent event) {
        final boolean sent = streamBridge.send("finished-out", event);
        if (!sent) {
            throw new MessagingException("Failed to send file finished event");
        }
        log.info("File finished event sent for use case {}", event.useCase());
    }
}
