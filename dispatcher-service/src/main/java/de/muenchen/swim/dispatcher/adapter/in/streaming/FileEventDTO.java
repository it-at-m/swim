package de.muenchen.swim.dispatcher.adapter.in.streaming;

public record FileEventDTO(
        String useCase,
        String presignedUrl,
        String metadataPresignedUrl) {
}
