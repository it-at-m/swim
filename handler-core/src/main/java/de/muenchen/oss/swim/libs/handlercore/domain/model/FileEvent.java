package de.muenchen.oss.swim.libs.handlercore.domain.model;

import java.util.List;

public sealed interface FileEvent
        permits SingleFileEvent, MultiFileEvent {
    String useCase();

    List<PresignedFile> files();
}
