package de.muenchen.oss.swim.dms.domain.model;

import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

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
    /**
     * Fill subject with metadata. See {@link de.muenchen.oss.swim.dms.configuration.SwimDmsProperties}.
     * Currently only works for {@link UseCaseType#PROCEDURE_INCOMING}.
     */
    private boolean metadataSubject = false;
}
