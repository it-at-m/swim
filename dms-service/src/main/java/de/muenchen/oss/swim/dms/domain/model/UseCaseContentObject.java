package de.muenchen.oss.swim.dms.domain.model;

import de.muenchen.oss.swim.dms.domain.helper.PatternHelper;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Properties for creating a ContentObject.
 */
@Data
public class UseCaseContentObject {
    /**
     * Regex pattern for overwriting filename in dms by providing regex pattern.
     * Pattern is applied to S3 filename.
     * The filename is used as ContentObject name.
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String filenameOverwritePattern;
    /**
     * Regex pattern for setting subject in dms by providing regex pattern.
     * Pattern is applied to S3 filename.
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String subjectPattern;
}
