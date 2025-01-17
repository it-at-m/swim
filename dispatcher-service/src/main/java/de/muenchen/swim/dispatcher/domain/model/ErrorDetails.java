package de.muenchen.swim.dispatcher.domain.model;

public record ErrorDetails(
        String source,
        String className,
        String message,
        String stacktrace) {
}
