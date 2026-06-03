package de.muenchen.oss.swim.dispatcher.domain.model;

import lombok.Getter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class ProcessingResult {
    @Getter
    private final MultiValueMap<String, Throwable> errors = new LinkedMultiValueMap<>();

    public void add(final FileReference file, final Throwable error) {
        errors.add(file.path(), error);
    }
}
