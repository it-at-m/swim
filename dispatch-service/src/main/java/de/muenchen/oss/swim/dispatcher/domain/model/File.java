package de.muenchen.oss.swim.dispatcher.domain.model;

import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.logging.log4j.util.Strings;

public record File(@NotBlank String bucket, @NotBlank String path, Long size) {
    public String getFileName() {
        return path.substring(path.lastIndexOf('/') + 1);
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

    public static File fromPresignedUrl(final String presignedUrl) throws PresignedUrlException {
        // check input has content
        if (Strings.isBlank(presignedUrl)) {
            throw new PresignedUrlException("Empty presigned url can't be parsed");
        }
        // parse presigned url
        final URI uri;
        try {
            uri = new URI(presignedUrl);
        } catch (final URISyntaxException e) {
            throw new PresignedUrlException("Presigned url could not be parsed", e);
        }
        // create File object from presigned url
        final String uriPath = uri.getPath().replaceFirst("^/", "");
        final int slashIndex = uriPath.indexOf('/');
        final String bucket = uriPath.substring(0, slashIndex);
        final String filePath = uriPath.substring(slashIndex + 1);
        return new File(bucket, filePath, null);
    }
}
