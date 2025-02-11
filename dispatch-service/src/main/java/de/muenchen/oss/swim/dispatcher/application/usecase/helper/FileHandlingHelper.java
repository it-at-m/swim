package de.muenchen.oss.swim.dispatcher.application.usecase.helper;

import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileHandlingHelper {
    public static final String FILE_EXTENSION_PDF = "pdf";
    public static final String FILE_EXTENSION_CSV = "csv";

    private final FileSystemOutPort fileSystemOutPort;
    private final SwimDispatcherProperties swimDispatcherProperties;

    /**
     * Mark a file that has led to an exception while processing.
     *
     * @param file File that throw an error.
     * @param stateTagKey The key of the state tag.
     * @param e The exception that was thrown.
     */
    public void markFileError(final File file, final String stateTagKey, final Exception e) {
        // escape illegal chars from message
        final String escapedMessage = e.getMessage().replaceAll("[^\\w .-]", " ");
        // shorten exception message for tag value max 256 chars
        final String shortenedExceptionMessage = escapedMessage.length() > 256 ? escapedMessage.substring(0, 256) : escapedMessage;
        fileSystemOutPort.tagFile(file.bucket(), file.path(), Map.of(
                stateTagKey, swimDispatcherProperties.getErrorStateValue(),
                swimDispatcherProperties.getErrorClassTagKey(), e.getClass().getName(),
                swimDispatcherProperties.getErrorMessageTagKey(), shortenedExceptionMessage));
    }
}
