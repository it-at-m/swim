package de.muenchen.oss.swim.dispatcher.dispatcher.application.usecase;

import de.muenchen.oss.swim.dispatcher.dispatcher.application.port.in.ErrorHandlerInPort;
import de.muenchen.oss.swim.dispatcher.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.oss.swim.dispatcher.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dispatcher.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.dispatcher.domain.model.ErrorDetails;
import de.muenchen.oss.swim.dispatcher.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.dispatcher.domain.model.UseCase;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorHandlerUseCase implements ErrorHandlerInPort {
    private final SwimDispatcherProperties swimDispatcherProperties;
    private final NotificationOutPort notificationOutPort;
    private final FileSystemOutPort fileSystemOutPort;

    @Override
    public void handleError(final String useCaseName, final String presignedUrl, final String metadataPresignedUrl, final ErrorDetails cause) {
        log.warn("Processing error for use case {} and presigned url {}: {}", useCaseName, presignedUrl, cause);
        try {
            final UseCase useCase = swimDispatcherProperties.findUseCase(useCaseName);
            final File file = File.fromPresignedUrl(presignedUrl);
            // tag file
            this.markFileError(file, cause);
            // send notification
            notificationOutPort.sendFileError(useCase.getMailAddresses(), useCaseName, file.path(), cause);
        } catch (final UseCaseException | PresignedUrlException e) {
            log.error("Error while handling error", e);
            notificationOutPort.sendFileError(List.of(swimDispatcherProperties.getFallbackMail()), useCaseName, presignedUrl, cause, e);
        }
    }

    /**
     * Mark a file that has led to an exception while processing.
     *
     * @param file File that throw an error.
     * @param e The error that was thrown-
     */
    protected void markFileError(final File file, final ErrorDetails e) {
        // escape illegal chars from message
        final String escapedMessage = e.message().replaceAll("[^\\w .-]", " ");
        // shorten exception message for tag value max 256 chars
        final String shortenedExceptionMessage = escapedMessage.length() > 256 ? escapedMessage.substring(0, 256) : escapedMessage;
        fileSystemOutPort.tagFile(file.bucket(), file.path(), Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getErrorStateValue(),
                swimDispatcherProperties.getErrorClassTagKey(), e.className(),
                swimDispatcherProperties.getErrorMessageTagKey(), shortenedExceptionMessage));
    }
}
