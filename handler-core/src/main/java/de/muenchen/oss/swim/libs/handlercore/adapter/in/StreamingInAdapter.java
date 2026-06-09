package de.muenchen.oss.swim.libs.handlercore.adapter.in;

import de.muenchen.oss.swim.libs.handlercore.application.port.in.ProcessFileInPort;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEvent;
import de.muenchen.oss.swim.libs.handlercore.domain.model.MultiFileEvent;
import de.muenchen.oss.swim.libs.handlercore.domain.model.SingleFileEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingInAdapter {
    private final ProcessFileInPort processFileInPort;

    /**
     * Consumer for dispatch events sent via Kafka from the dispatch-service.
     *
     * @return The consumer.
     */
    @Bean
    public Consumer<Message<FileEvent>> event() {
        return message -> {
            final FileEvent fileEvent = message.getPayload();
            try {
                if (fileEvent instanceof SingleFileEvent single) {
                    processFileInPort.processEvent(single);
                } else if (fileEvent instanceof MultiFileEvent multi) {
                    processFileInPort.processEvent(multi);
                }
            } catch (final PresignedUrlException | UnknownUseCaseException | MetadataException e) {
                log.warn("Error while processing event in use case {}: {}", fileEvent.useCase(), fileEvent, e);
                throw new FileProcessingException(e);
            }
        };
    }
}
