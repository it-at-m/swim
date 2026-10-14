package de.muenchen.oss.swim.dispatcher.domain.model.streaming;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import java.util.List;

public sealed interface FileEvent
        permits SingleFileEvent, MultiFileEvent {
    String useCase();

    List<PresignedFile> files();
}
