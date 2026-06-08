package de.muenchen.oss.swim.dispatcher.domain.model.streaming;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;

public record FileEventDTO(
        String useCase,
        String presignedUrl,
        String metadataPresignedUrl) implements FileEvent {

    public final static String TYPE_NAME = "single";

    public static FileEventDTO fromPresignedFile(final String useCase, final PresignedFile file) {
        return new FileEventDTO(useCase, file.presignedUrl(), file.metadataPresignedUrl());
    }
}
