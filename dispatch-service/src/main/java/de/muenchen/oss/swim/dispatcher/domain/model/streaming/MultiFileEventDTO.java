package de.muenchen.oss.swim.dispatcher.domain.model.streaming;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import java.util.List;

public record MultiFileEventDTO(
        String useCase,
        List<PresignedFile> files) implements FileEvent {
    public final static String TYPE_NAME = "multi";
}
