package de.muenchen.oss.swim.dipa.domain.model;

import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Properties for creating a ContentObject.
 */
@Data
public class UseCaseContentObject {
    /**
     * Regex pattern for overwriting filename in DiPa by providing regex pattern.
     * Pattern is applied to S3 filename.
     * The filename is used as ContentObject name.
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String filenameOverwritePattern;
}
