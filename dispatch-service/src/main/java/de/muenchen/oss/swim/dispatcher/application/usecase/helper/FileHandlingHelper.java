package de.muenchen.oss.swim.dispatcher.application.usecase.helper;

import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.configuration.DispatchMeter;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileHandlingHelper {
    public static final String FILE_EXTENSION_PDF = "pdf";
    public static final String FILE_EXTENSION_CSV = "csv";

    private static final String ILLEGAL_CHARS_PATTERN = "[^\\w .-]";
    private static final int TAG_MAX_VALUE_LENGTH = 256;

    private final FileSystemOutPort fileSystemOutPort;
    private final SwimDispatcherProperties swimDispatcherProperties;
    private final DispatchMeter dispatchMeter;

    /**
     * Mark a file that has led to an exception while processing.
     *
     * @param file File that throw an error.
     * @param stateTagKey The key of the state tag.
     * @param e The exception that was thrown.
     */
    public void markFileError(final File file, final String stateTagKey, final Exception e) {
        // escape illegal chars from message
        final String escapedMessage = e.getMessage().replaceAll(ILLEGAL_CHARS_PATTERN, " ");
        // shorten exception message for tag value max 256 chars
        final String shortenedExceptionMessage = escapedMessage.length() > TAG_MAX_VALUE_LENGTH ? escapedMessage.substring(0, TAG_MAX_VALUE_LENGTH)
                : escapedMessage;
        fileSystemOutPort.tagFile(file.tenant(), file.bucket(), file.path(), Map.of(
                stateTagKey, swimDispatcherProperties.getErrorStateValue(),
                swimDispatcherProperties.getErrorClassTagKey(), e.getClass().getName(),
                swimDispatcherProperties.getErrorMessageTagKey(), shortenedExceptionMessage));
    }

    /**
     * Tag file as finished and move to finished dir.
     *
     * @param tenant Tenant of the file.
     * @param useCase Use case of the file.
     * @param bucket Bucket the file is in.
     * @param path Path of the file.
     */
    public void finishFile(final UseCase useCase, final String tenant, final String bucket, final String path) {
        // tag file as finished
        fileSystemOutPort.tagFile(tenant, bucket, path, Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getDispatchFileFinishedTagValue()));
        // move file
        final String destPath = useCase.getFinishedPath(swimDispatcherProperties, path);
        fileSystemOutPort.moveFile(tenant, bucket, path, destPath);
        log.info("Finished file {} in use case {}", path, useCase.getName());
        // update metric
        dispatchMeter.incrementFinished(useCase.getName());
    }
}
