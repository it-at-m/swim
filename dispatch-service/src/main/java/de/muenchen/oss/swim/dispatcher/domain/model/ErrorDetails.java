package de.muenchen.oss.swim.dispatcher.domain.model;

public record ErrorDetails(
        String source,
        String className,
        String message,
        String stacktrace) {

    public String getTrimmedMessage() {
        if (this.message == null || this.message.isBlank()) {
            return "";
        }
        final String[] parts = this.message.split(";");
        return parts.length > 0 ? parts[parts.length - 1].trim() : "";
    }
}
