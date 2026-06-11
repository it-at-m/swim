package de.muenchen.oss.swim.dispatcher.domain.model;

import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.net.URISyntaxException;

public record FileReference(@NotBlank String bucket, @NotBlank String path) {
    public String getFileName() {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public String getFileNameWithoutExtension() {
        final String fileName = this.getFileName();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    public String getParentPath() {
        return path.substring(0, path.lastIndexOf('/'));
    }

    public String getParentName() {
        final String parentPath = this.getParentPath();
        return parentPath.substring(parentPath.lastIndexOf('/') + 1);
    }

    public String getMetadataFilePath() {
        return String.format("%s/%s.json", this.getParentPath(), this.getFileNameWithoutExtension());
    }

    public FileReference getMetadataFile() {
        return new FileReference(this.bucket, this.getMetadataFilePath());
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
