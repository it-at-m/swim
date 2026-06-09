package de.muenchen.oss.swim.dispatcher.domain.model.streaming;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import java.util.List;

public record MultiFileEvent(
        String useCase,
        List<PresignedFile> files) implements FileEvent {
    public static final String TYPE_NAME = "multi";

    public MultiFileEvent {
        if (useCase == null || useCase.isBlank()) {
            throw new IllegalArgumentException("useCase must not be null or blank");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files must not be null or empty");
        }
    }
}
