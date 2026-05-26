package de.muenchen.oss.swim.dispatcher.application.port.out;

import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dispatcher.domain.model.FileReference;
import de.muenchen.oss.swim.dispatcher.domain.model.FileWithMetadata;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;

@Validated
public interface FileSystemOutPort {
    /**
     * Get files (with all tags) matching required and exclude tags from specified bucket and path.
     *
     * @param bucket The bucket to look in.
     * @param pathPrefix The path to look under.
     * @param recursive If the lookup should be recursive.
     * @param extension The extension the files should have.
     * @param requiredTags Tag entries which are required to be on each file.
     * @param excludeTags Tag entries where none should be on the file.
     * @return List of files (with all tags) having required and not having any exclude tags.
     */
    List<FileWithMetadata> getMatchingFilesWithTags(
            @NotBlank String bucket,
            @NotNull String pathPrefix,
            @NotNull boolean recursive,
            @NotBlank String extension,
            @NotNull Map<String, String> requiredTags,
            @NotNull Map<String, List<String>> excludeTags);

    /**
     * Get names of subdirectories.
     *
     * @param bucket The bucket to look in.
     * @param pathPrefix The path to look under.
     * @return The names of the subdirectories.
     */
    List<String> getSubDirectories(
            @NotBlank String bucket,
            @NotNull String pathPrefix);

    /**
     * Add tags to a file.
     *
     * @param fileReference The reference identifying the file.
     * @param tags The tags to add.
     */
    void tagFile(@Valid @NotNull FileReference fileReference, @NotNull Map<String, String> tags);

    /**
     * Check if a file exists.
     *
     * @param fileReference The reference identifying the file.
     * @return If the file exists.
     */
    boolean fileExists(@Valid @NotNull FileReference fileReference);

    /**
     * Get content of a file.
     *
     * @param fileReference The reference identifying the file.
     * @return The content of the file.
     */
    InputStream readFile(@Valid @NotNull FileReference fileReference);

    /**
     * Get presigned url for downloading a file.
     *
     * @param fileReference The reference identifying the file.
     * @return The presigned url for the file.
     */
    String getPresignedUrl(@Valid @NotNull FileReference fileReference);

    /**
     * Verify a presigned url for downloading a file.
     *
     * @param presignedUrl The presigned url.
     */
    boolean verifyPresignedUrl(@NotBlank String presignedUrl) throws PresignedUrlException;

    /**
     * Move a file from one place to another.
     *
     * @param srcFileReference The reference identifying the source file.
     * @param destPath The destination path of the file.
     */
    void moveFile(@Valid @NotNull FileReference srcFileReference, @NotBlank String destPath);

    /**
     * Copy a file from one place to another.
     *
     * @param srcFileReference The reference identifying the source file.
     * @param destFileReference The reference identifying the destination.
     * @param clearTags If the existing tags should be removed.
     */
    void copyFile(@Valid @NotNull FileReference srcFileReference, @Valid @NotNull FileReference destFileReference, boolean clearTags);
}
