package de.muenchen.swim.dms.adapter.in.streaming;

public record DmsEventDTO(
        String useCase,
        String presignedUrl,
        String metadataPresignedUrl) {
}
