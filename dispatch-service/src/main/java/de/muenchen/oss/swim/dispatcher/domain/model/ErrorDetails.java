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
        final int index = this.message.indexOf("; ");
        return index != -1 ? this.message.substring(index + 1).trim() : this.message;
    }
}
