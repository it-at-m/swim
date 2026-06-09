package de.muenchen.oss.swim.dispatcher.domain.model.streaming;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;

public record SingleFileEvent(
        String useCase,
        String presignedUrl,
        String metadataPresignedUrl) implements FileEvent {

    public static final String TYPE_NAME = "single";

    public SingleFileEvent {
        if (useCase == null || useCase.isBlank()) {
            throw new IllegalArgumentException("useCase must not be null or blank");
        }
        if (presignedUrl == null || presignedUrl.isBlank()) {
            throw new IllegalArgumentException("presignedUrl must not be null or blank");
        }
    }

    public static SingleFileEvent fromPresignedFile(final String useCase, final PresignedFile file) {
        return new SingleFileEvent(useCase, file.presignedUrl(), file.metadataPresignedUrl());
    }
}
