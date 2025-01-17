package de.muenchen.oss.swim.dispatcher.dispatcher.domain.model;

public record ErrorDetails(
        String source,
        String className,
        String message,
        String stacktrace) {
}
