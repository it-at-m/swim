package de.muenchen.oss.swim.libs.handlercore.adapter.in;

import de.muenchen.oss.swim.libs.handlercore.application.port.in.ProcessFileInPort;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.File;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEvent;
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
            final File file;
            try {
                file = File.fromPresignedUrl(fileEvent.presignedUrl());
            } catch (final PresignedUrlException e) {
                log.warn("Error while parsing presinged url {} in use case {}", fileEvent.presignedUrl(), fileEvent.useCase(), e);
                throw new FileProcessingException(e);
            }
            try {
                processFileInPort.processFile(fileEvent, file);
            } catch (final PresignedUrlException | UnknownUseCaseException | MetadataException e) {
                log.warn("Error while processing file {} in use case {}", file.path(), fileEvent.useCase(), e);
                throw new FileProcessingException(e);
            }
        };
    }
}
