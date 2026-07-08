package de.muenchen.oss.swim.libs.handlercore.domain.model;

public sealed interface FileEvent
        permits SingleFileEvent, MultiFileEvent {
}
