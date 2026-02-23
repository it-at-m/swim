package de.muenchen.oss.swim.dispatcher.domain.model;

import java.util.List;

public record ErrorDetails(
        String source,
        String className,
        String message,
        String stacktrace) {

    public String getTrimmedMessage() {
        return List.of(this.message.split(";")).getLast().trim();
    }
}
