package de.muenchen.oss.swim.dispatcher.adapter.out.streaming;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import java.util.List;

public record MultiFileEventDTO(
        String useCase,
        List<PresignedFile> files) {
}
