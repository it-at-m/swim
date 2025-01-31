package de.muenchen.oss.swim.libs.handlercore.domain.model;

import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.net.URISyntaxException;

public record File(
        @NotBlank String bucket,
        @NotBlank String path) {
    public String getFileName() {
        return path.substring(path.lastIndexOf('/') + 1);
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
            final String path = presignedUrl.getPath().replaceAll("^/", "");
            final int firstSlash = path.indexOf('/');
            final String bucket = path.substring(0, firstSlash);
            final String filePath = path.substring(firstSlash + 1);
            return new File(bucket, filePath);
        } catch (final URISyntaxException e) {
            throw new PresignedUrlException("Presigned URL couldn't be parsed", e);
        }
    }
}
