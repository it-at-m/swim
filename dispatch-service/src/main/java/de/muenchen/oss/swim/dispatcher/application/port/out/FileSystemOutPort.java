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
     * Get files matching required and exclude tags from specified bucket and path.
     *
     * @param bucket The bucket to look in.
     * @param pathPrefix The path to look under.
     * @param recursive If the lookup should be recursive.
     * @param extension The extension the files should have.
     * @param requiredTags Tag entries which are required to be on each file.
     * @param excludeTags Tag entries where none should be on the file.
     * @return List of files having required and not having any exclude tags.
     */
    List<File> getMatchingFiles(
            @NotBlank String bucket,
            @NotNull String pathPrefix,
            @NotNull boolean recursive,
            @NotBlank String extension,
            @NotNull Map<String, String> requiredTags,
            @NotNull Map<String, List<String>> excludeTags);

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
}
