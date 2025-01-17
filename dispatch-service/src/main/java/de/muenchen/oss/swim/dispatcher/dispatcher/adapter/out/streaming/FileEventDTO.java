package de.muenchen.oss.swim.dispatcher.dispatcher.adapter.out.streaming;

record FileEventDTO(
        String useCase,
        String presignedUrl,
        String metadataPresignedUrl) {
}
