package de.muenchen.oss.swim.dispatcher.application.port.out;

import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
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
     * @return Map of files (with all tags) having required and not having any exclude tags.
     */
    Map<File, Map<String, String>> getMatchingFilesWithTags(
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
     * @param bucket The bucket the file is in.
     * @param path The path to the file.
     * @param tags The tags to add.
     */
    void tagFile(@NotBlank String bucket, @NotBlank String path, @NotNull Map<String, String> tags);

    /**
     * Check if a file exists.
     *
     * @param bucket The bucket the file is in.
     * @param path The path to the file.
     * @return If the file exists.
     */
    boolean fileExists(@NotBlank String bucket, @NotBlank String path);

    /**
     * Get content of a file.
     *
     * @param bucket The bucket the file is in.
     * @param path The path of the file.
     * @return The content of the file.
     */
    InputStream readFile(@NotBlank String bucket, @NotBlank String path);

    /**
     * Get presigned url for downloading a file.
     *
     * @param bucket The bucket the file is in.
     * @param path The path of the file.
     * @return The presigned url for the file.
     */
    String getPresignedUrl(@NotBlank String bucket, @NotBlank String path);

    /**
     * Verify a presigned url for downloading a file.
     *
     * @param presignedUrl The presigned url.
     */
    boolean verifyPresignedUrl(@NotBlank String presignedUrl) throws PresignedUrlException;

    /**
     * Move a file from one place to another.
     *
     * @param bucket The bucket the file is in.
     * @param srcPath The source path of the file.
     * @param destPath The destination path of the file.
     */
    void moveFile(@NotBlank String bucket, @NotBlank String srcPath, @NotBlank String destPath);

    /**
     * Copy a file from one place to another.
     *
     * @param srcBucket The bucket the file is in.
     * @param srcPath The source path of the file.
     * @param destBucket The destination bucket of the file.
     * @param destPath The destination path of the file.
     * @param clearTags If the existing tags should be removed.
     */
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    void copyFile(@NotBlank String srcBucket, @NotBlank String srcPath, @NotBlank String destBucket, @NotBlank String destPath, boolean clearTags);
}
