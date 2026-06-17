package de.muenchen.oss.swim.dispatcher.adapter.in.streaming;

import de.muenchen.oss.swim.dispatcher.application.port.in.ErrorHandlerInPort;
import de.muenchen.oss.swim.dispatcher.application.port.in.MarkFileFinishedInPort;
import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.domain.model.ErrorDetails;
import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import de.muenchen.oss.swim.dispatcher.domain.model.streaming.FileEvent;
import de.muenchen.oss.swim.dispatcher.domain.model.streaming.MultiFileEvent;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StreamingInAdapter {
    private final MarkFileFinishedInPort markFileFinishedInPort;
    private final ErrorHandlerInPort errorHandlerInPort;

    @Bean
    protected Consumer<Message<FileEvent>> finished() {
        return fileEventMessage -> {
            final FileEvent event = fileEventMessage.getPayload();
            final MultiFileEvent multiFileEvent = MultiFileEvent.fromFileEvent(event);
            try {
                for (final PresignedFile file : multiFileEvent.files()) {
                    final String useCase = multiFileEvent.useCase();
                    markFileFinishedInPort.markFileFinished(useCase, file.presignedUrl());
                    if (StringUtils.isNotBlank(file.metadataPresignedUrl())) {
                        markFileFinishedInPort.markFileFinished(useCase, file.metadataPresignedUrl());
                    }
                }
            } catch (PresignedUrlException | UseCaseException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Bean
    protected Consumer<Message<FileEvent>> dlq() {
        return fileEventMessage -> {
            final FileEvent event = fileEventMessage.getPayload();
            final MultiFileEvent multiFileEvent = MultiFileEvent.fromFileEvent(event);
            final ErrorDetails error = this.errorDetailsFromHeaders(fileEventMessage.getHeaders());
            for (final PresignedFile file : multiFileEvent.files()) {
                errorHandlerInPort.handleError(multiFileEvent.useCase(), file, error);
            }
        };
    }

    protected ErrorDetails errorDetailsFromHeaders(final Map<String, Object> headers) {
        return new ErrorDetails(
                this.resolveByteHeaderToString(headers, "x-original-topic"),
                this.resolveByteHeaderToString(headers, "x-exception-fqcn"),
                this.resolveByteHeaderToString(headers, "x-exception-message"),
                this.resolveByteHeaderToString(headers, "x-exception-stacktrace"));
    }

    protected String resolveByteHeaderToString(final Map<String, Object> headers, final String key) {
        final byte[] val = (byte[]) headers.get(key);
        if (val == null) {
            return null;
        }
        return new String(val, StandardCharsets.UTF_8);
    }
}
