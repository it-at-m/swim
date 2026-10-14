package de.muenchen.oss.swim.dispatcher.application.usecase.helper;

import static de.muenchen.oss.swim.dispatcher.application.usecase.helper.GroupingHelper.CHUNKED_FILE_COUNT_GROUP;
import static de.muenchen.oss.swim.dispatcher.application.usecase.helper.GroupingHelper.CHUNKED_FILE_INDEX_GROUP;
import static de.muenchen.oss.swim.dispatcher.application.usecase.helper.GroupingHelper.CHUNKED_FILE_PATTERN;

import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileChunkException;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileSizeException;
import de.muenchen.oss.swim.dispatcher.domain.model.FileGroup;
import de.muenchen.oss.swim.dispatcher.domain.model.FileReference;
import de.muenchen.oss.swim.dispatcher.domain.model.FileWithMetadata;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationHelper {
    private final SwimDispatcherProperties swimDispatcherProperties;

    /**
     * Validate and filter a FileGroup.
     * Validates group with all files and each file separately.
     *
     * @param useCase The use case the files were found for.
     * @param fileGroup The FileGroup to validate.
     */
    public boolean validateFileGroup(final UseCase useCase, final String baseFileName, final FileGroup fileGroup) throws FileSizeException, FileChunkException {
        final List<FileWithMetadata> files = fileGroup.getFiles();
        // if multiple files
        if (fileGroup.isMulti() && !this.validateGroup(baseFileName, files)) {
            return false;
        }
        // validate each file
        for (final FileWithMetadata file : files) {
            if (!this.validateFile(useCase, file)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates a group of files that represent one logical chunked reference.
     * <p>
     * The validation ensures:
     * <ul>
     * <li>All files in the group share identical {@link FileWithMetadata#tags()}.</li>
     * <li>All chunk indices from 1 to the declared total are present exactly once.</li>
     * </ul>
     *
     * @param baseFileName the base filename of the group (without extension)
     * @param files the files that belong to the base filename
     * @throws FileChunkException if tags differ or any chunk is missing
     */
    protected boolean validateGroup(final String baseFileName, final List<FileWithMetadata> files) throws FileChunkException {
        // check all files same tags
        if (files.stream().map(FileWithMetadata::tags).distinct().limit(2).count() > 1) {
            throw new FileChunkException("The tags of the %d files %s are different.".formatted(files.size(), baseFileName));
        }
        // check all file chunks are present and throw error if over age limit
        final List<Integer> missingChunks = this.getMissingChunks(files);
        final boolean allChunksPresent = missingChunks.isEmpty();
        final Duration newestFileAge = files.stream()
                .map(FileWithMetadata::lastModified)
                .max(Comparator.naturalOrder())
                .map(i -> Duration.between(i, ZonedDateTime.now()))
                .orElseThrow();
        if (!allChunksPresent && newestFileAge.compareTo(swimDispatcherProperties.getMaxFileChunkAge()) > 0) {
            throw new FileChunkException("The chunks %s are missing for file %s and all files are older than %s"
                    .formatted(missingChunks, baseFileName, swimDispatcherProperties.getMaxFileChunkAge()));
        }
        // skip processing for group if chunks missing but below threshold
        if (!allChunksPresent) {
            log.info("The chunks {} are missing for file {}, but below age threshold of {}", missingChunks,
                    baseFileName, swimDispatcherProperties.getMaxFileChunkAge());
            return false;
        }
        return true;
    }

    /**
     * Returns the chunk indices that are missing from the given list of files.
     * The expected number of parts is derived from the first file name via CHUNKED_FILE_PATTERN.
     *
     * @param files list of chunk files that belong together
     * @return a sorted list of missing chunk indices; empty if none are missing
     */
    protected List<Integer> getMissingChunks(final List<FileWithMetadata> files) {
        final Matcher countMatcher = CHUNKED_FILE_PATTERN.matcher(files.getFirst().reference().getFileNameWithoutExtension());
        if (!countMatcher.matches()) {
            throw new IllegalArgumentException("File needs to match chunk pattern (counter)");
        }
        final int parts = Integer.parseInt(countMatcher.group(CHUNKED_FILE_COUNT_GROUP));
        final Set<Integer> present = files.stream()
                .map(FileWithMetadata::reference)
                .map(FileReference::getFileNameWithoutExtension)
                .map(i -> {
                    final Matcher indexMatcher = CHUNKED_FILE_PATTERN.matcher(i);
                    if (!indexMatcher.matches()) {
                        throw new IllegalArgumentException("Files need to match chunk pattern (index)");
                    }
                    return indexMatcher.group(CHUNKED_FILE_INDEX_GROUP);
                })
                .map(Integer::parseInt)
                .collect(Collectors.toSet());

        return IntStream.rangeClosed(1, parts)
                .filter(i -> !present.contains(i))
                .boxed()
                .toList();
    }

    /**
     * Validates a single file.
     * Validates if the file is below the file size limit.
     *
     * @param useCase The use case of the file.
     * @param file The file to validate
     * @throws FileSizeException When the file is above the file size limit.
     */
    protected boolean validateFile(final UseCase useCase, final FileWithMetadata file) throws FileSizeException {
        // validate file size
        if (file.size() > useCase.getMaxFileSize().toBytes()) {
            final String message = String.format("FileReference %s too large. %d > %d", file.reference().path(), file.size(),
                    useCase.getMaxFileSize().toBytes());
            throw new FileSizeException(message);
        }
        return true;
    }
}
