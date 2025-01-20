package de.muenchen.oss.swim.dispatcher.adapter.in.streaming;

public record FileEventDTO(
        String useCase,
        String presignedUrl,
        String metadataPresignedUrl) {
}
