package de.muenchen.oss.swim.dispatcher.domain.model;

public record ErrorDetails(
        String source,
        String className,
        String message,
        String stacktrace) {
}
