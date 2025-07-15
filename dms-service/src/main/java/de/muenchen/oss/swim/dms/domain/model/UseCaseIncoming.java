package de.muenchen.oss.swim.dms.domain.model;

import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * Properties for creating an Incoming.
 */
@Data
public class UseCaseIncoming {
    /**
     * Regex pattern for defining a custom Incoming name.
     * If not defined overwritten filename is used.
     * Only applies to {@link UseCaseType#PROCEDURE_INCOMING}
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String incomingNamePattern;
    /**
     * Regex pattern for defining a custom Incoming subject.
     * Either this or {@link #isMetadataSubject()} can be defined.
     * Only applies to {@link UseCaseType#PROCEDURE_INCOMING}.
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String incomingSubjectPattern;
    /**
     * Fill subject with metadata. See {@link de.muenchen.oss.swim.dms.configuration.SwimDmsProperties}.
     * Either this or {@link #getIncomingSubjectPattern()} can be defined.
     * Currently only works for {@link UseCaseType#PROCEDURE_INCOMING}.
     */
    private boolean metadataSubject = false;
    /**
     * Verify name of resolved Procedure against pattern, if defined.
     * Only applies to {@link UseCaseType#PROCEDURE_INCOMING}
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String verifyProcedureNamePattern;
    /**
     * Reuse Incoming with same name if true.
     * Only applies to {@link UseCaseType#PROCEDURE_INCOMING}
     */
    private boolean reuseIncoming = false;

    @AssertTrue(message = "Only either pattern or metadata can be used for Incoming subject")
    protected boolean isOnlyOneSubjectSource() {
        return !metadataSubject || StringUtils.isBlank(incomingSubjectPattern);
    }
}
