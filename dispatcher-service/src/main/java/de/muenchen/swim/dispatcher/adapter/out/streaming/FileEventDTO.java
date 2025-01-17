package de.muenchen.swim.dispatcher.adapter.out.streaming;

record FileEventDTO(
        String useCase,
        String presignedUrl,
        String metadataPresignedUrl) {
}
