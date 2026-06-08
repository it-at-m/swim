package de.muenchen.oss.swim.dispatcher.domain.model.streaming;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;

public record FileEventDTO(
        String useCase,
        String presignedUrl,
        String metadataPresignedUrl) implements FileEvent {

    public final static String TYPE_NAME = "single";

    public FileEventDTO {
        if (useCase == null || useCase.isBlank()) {
            throw new IllegalArgumentException("useCase must not be null or blank");
        }
        if (presignedUrl == null || presignedUrl.isBlank()) {
            throw new IllegalArgumentException("presignedUrl must not be null or blank");
        }
    }

    public static FileEventDTO fromPresignedFile(final String useCase, final PresignedFile file) {
        return new FileEventDTO(useCase, file.presignedUrl(), file.metadataPresignedUrl());
    }
}
