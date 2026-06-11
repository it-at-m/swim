package de.muenchen.oss.swim.libs.handlercore.domain.model;

import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import jakarta.validation.constraints.NotBlank;

import java.net.URI;
import java.net.URISyntaxException;

public record FileReference(
        @NotBlank String bucket,
        @NotBlank String path) {
    public String getFileName() {
        final int lastSlash = path.lastIndexOf('/');
        return lastSlash == -1 ? path : path.substring(lastSlash + 1);
    }

    /**
     * Get filename without file extension.
     *
     * @return The filename without extension.
     * @throws IllegalArgumentException If filename has no extension.
     */
    public String getFileNameWithoutExtension() {
        final String fileName = this.getFileName();
        final int lastPointIndex = fileName.lastIndexOf('.');
        if (lastPointIndex == -1) {
            throw new IllegalArgumentException("Filename has no extension");
        }
        return fileName.substring(0, lastPointIndex);
    }

    /**
     * Get file extension.
     *
     * @return The file extension.
     * @throws IllegalArgumentException If filename has no extension.
     */
    public String getFileExtension() {
        final String fileName = this.getFileName();
        final int lastPointIndex = fileName.lastIndexOf('.');
        if (lastPointIndex == -1) {
            throw new IllegalArgumentException("Filename has no extension");
        }
        return fileName.substring(lastPointIndex + 1);
    }

    /**
     * Build {@link FileReference} from presigned URL.
     *
     * @param presignedUrlString The presigned URL of a file.
     * @return The resolved FileReference.
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public static FileReference fromPresignedUrl(final String presignedUrlString) throws PresignedUrlException {
        final URI presignedUrl;
        try {
            presignedUrl = new URI(presignedUrlString);
        } catch (final URISyntaxException e) {
            throw new PresignedUrlException("Presigned URL couldn't be parsed", e);
        }
        final String path = presignedUrl.getPath().replaceFirst("^/", "");
        if (path.isEmpty()) {
            throw new PresignedUrlException("Empty path in presigned URL");
        }
        final int firstSlash = path.indexOf('/');
        if (firstSlash == -1) {
            throw new PresignedUrlException("Invalid path format: missing bucket/file structure");
        }
        final String filePath = path.substring(firstSlash + 1);
        final String bucket = path.substring(0, firstSlash);
        if (filePath.isBlank() || bucket.isBlank()) {
            throw new PresignedUrlException("Invalid path format: missing bucket or file path");
        }
        return new FileReference(bucket, filePath);
    }
}
