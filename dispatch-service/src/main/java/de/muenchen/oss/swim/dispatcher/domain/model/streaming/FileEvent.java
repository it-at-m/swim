package de.muenchen.oss.swim.dispatcher.domain.model.streaming;

public sealed interface FileEvent
        permits FileEventDTO, MultiFileEventDTO {
}
