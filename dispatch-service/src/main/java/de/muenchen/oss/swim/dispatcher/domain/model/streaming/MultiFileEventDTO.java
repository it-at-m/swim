package de.muenchen.oss.swim.dispatcher.domain.model.streaming;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import java.util.List;

public record MultiFileEventDTO(
        String useCase,
        List<PresignedFile> files) implements FileEvent {
    public final static String TYPE_NAME = "multi";

    public MultiFileEventDTO {
        if (useCase == null || useCase.isBlank()) {
            throw new IllegalArgumentException("useCase must not be null or blank");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files must not be null or empty");
        }
    }
}
