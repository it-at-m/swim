package de.muenchen.oss.swim.libs.handlercore.domain.model;

import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.net.URISyntaxException;

public record File(
        @NotBlank String bucket,
        @NotBlank String path) {
    public String getFileName() {
        final int lastSlash = path.lastIndexOf('/');
        return lastSlash == -1 ? path : path.substring(lastSlash + 1);
    }

    public String getFileNameWithoutExtension() {
        final String fileName = this.getFileName();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    public String getFileExtension() {
        return this.getFileName().substring(this.getFileName().lastIndexOf(".") + 1);
    }

    /**
     * Build {@link File} from presigned URL.
     *
     * @param presignedUrlString The presigned URL of a file.
     * @return The resolve File.
     */
    static public File fromPresignedUrl(final String presignedUrlString) throws PresignedUrlException {
        try {
            final URI presignedUrl = new URI(presignedUrlString);
            final String path = presignedUrl.getPath().replaceFirst("^/", "");
            if (path.isEmpty()) {
                throw new PresignedUrlException("Empty path in presigned URL");
            }
            final int firstSlash = path.indexOf('/');
            if (firstSlash == -1) {
                throw new PresignedUrlException("Invalid path format: missing bucket/file structure");
            }
            final String bucket = path.substring(0, firstSlash);
            final String filePath = path.substring(firstSlash + 1);
            return new File(bucket, filePath);
        } catch (final URISyntaxException e) {
            throw new PresignedUrlException("Presigned URL couldn't be parsed", e);
        }
    }
}
