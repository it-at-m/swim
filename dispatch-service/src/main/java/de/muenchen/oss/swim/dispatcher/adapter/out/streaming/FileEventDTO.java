package de.muenchen.oss.swim.dispatcher.adapter.out.streaming;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;

record FileEventDTO(
        String useCase,
        String presignedUrl,
        String metadataPresignedUrl) {
    /* default */ static FileEventDTO fromPresignedFile(final String useCase, final PresignedFile presignedFile) {
        return new FileEventDTO(useCase, presignedFile.presignedUrl(), presignedFile.metadataPresignedUrl());
    }
}
