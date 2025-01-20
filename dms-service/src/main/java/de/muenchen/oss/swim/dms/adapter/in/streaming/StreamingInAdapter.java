package de.muenchen.oss.swim.dms.adapter.in.streaming;

import de.muenchen.oss.swim.dms.application.port.in.ProcessFileInPort;
import de.muenchen.oss.swim.dms.domain.exception.MetadataException;
import de.muenchen.oss.swim.dms.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dms.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.dms.domain.model.File;
import java.net.URI;
import java.net.URISyntaxException;
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
    public Consumer<Message<DmsEventDTO>> dms() {
        return message -> {
            final DmsEventDTO dmsEventDTO = message.getPayload();
            final File file = this.presignedUrlToFile(dmsEventDTO.presignedUrl());
            try {
                processFileInPort.processFile(dmsEventDTO.useCase(), file, dmsEventDTO.presignedUrl(), dmsEventDTO.metadataPresignedUrl());
            } catch (final PresignedUrlException | UnknownUseCaseException | MetadataException e) {
                log.warn("Error while processing file {} in use case {}", file.path(), dmsEventDTO.useCase(), e);
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Extract file attributes from presigned url.
     *
     * @param presignedUrlString The presigned url of a file.
     * @return The resolve file attributes.
     */
    protected File presignedUrlToFile(final String presignedUrlString) {
        try {
            final URI presignedUrl = new URI(presignedUrlString);
            final String path = presignedUrl.getPath().replaceAll("^/", "");
            final int firstSlash = path.indexOf('/');
            final String bucket = path.substring(0, firstSlash);
            final String filePath = path.substring(firstSlash + 1);
            return new File(bucket, filePath);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
