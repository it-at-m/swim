package de.muenchen.oss.swim.dipa.domain.model;

import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Properties for creating an Incoming.
 */
@Data
public class UseCaseIncoming {
    /**
     * Regex pattern for defining a custom Incoming subject.
     * If not defined overwritten filename is used.
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String incomingSubjPattern;
}
