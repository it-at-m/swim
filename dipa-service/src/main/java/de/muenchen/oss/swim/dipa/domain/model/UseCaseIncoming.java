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
     * Regex pattern for defining an Incoming subject.
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String incomingSubjPattern;
}
