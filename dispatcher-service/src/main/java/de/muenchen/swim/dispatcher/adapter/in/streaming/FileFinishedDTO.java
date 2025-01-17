package de.muenchen.swim.dispatcher.adapter.in.streaming;

public record FileFinishedDTO(
        String useCase,
        String presignedUrl,
        String metadataPresignedUrl) {
}
