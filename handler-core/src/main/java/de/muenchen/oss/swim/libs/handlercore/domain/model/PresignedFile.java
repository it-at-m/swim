package de.muenchen.oss.swim.libs.handlercore.domain.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Container for presigned URLs for a single file.
 * Optional with presigned URL for the according metadata file.
 * <p>
 * ATTENTION: This class need to match the one in the dispatch-service.
 *
 * @param presignedUrl The presigned URL of the file.
 * @param metadataPresignedUrl The presigned URL of the according metadata file.
 */
public record PresignedFile(@NotBlank String presignedUrl, String metadataPresignedUrl) {
}
