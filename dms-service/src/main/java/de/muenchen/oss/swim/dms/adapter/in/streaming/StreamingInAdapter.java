package de.muenchen.oss.swim.dms.adapter.in.streaming;

import de.muenchen.oss.swim.dms.application.port.in.ProcessFileInPort;
import de.muenchen.oss.swim.dms.domain.model.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StreamingInAdapter {
    private final ProcessFileInPort processFileInPort;

    @Bean
    public Consumer<Message<DmsEventDTO>> dms() {
        return message -> {
            final DmsEventDTO dmsEventDTO = message.getPayload();
            final File file = this.presignedUrlToFile(dmsEventDTO.presignedUrl());
            processFileInPort.processFile(dmsEventDTO.useCase(), file, dmsEventDTO.presignedUrl(), dmsEventDTO.metadataPresignedUrl());
        };
    }

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
