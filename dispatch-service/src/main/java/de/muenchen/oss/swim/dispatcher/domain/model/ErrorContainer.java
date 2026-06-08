package de.muenchen.oss.swim.dispatcher.domain.model;

import java.util.HashMap;
import java.util.Map;

public record ErrorContainer<T>(Map<FileReference, Throwable> errors, T value) {
    public ErrorContainer(final T value) {
        this(new HashMap<>(), value);
    }
}
