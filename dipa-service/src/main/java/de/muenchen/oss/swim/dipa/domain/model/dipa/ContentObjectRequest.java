package de.muenchen.oss.swim.dipa.domain.model.dipa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;

/**
 * Request for creating a ContentObject.
 *
 * @param name The name of the new ContentObject.
 * @param extension The extension of the ContentObject.
 * @param content The content of the new ContentObject.
 */
public record ContentObjectRequest(
        @NotBlank String name,
        @NotBlank String extension,
        @NotNull InputStream content) {
}
