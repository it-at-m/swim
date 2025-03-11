package de.muenchen.oss.swim.dms.domain.model;

import de.muenchen.oss.swim.dms.domain.helper.PatternHelper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import lombok.Data;
import lombok.Getter;

@Data
public class UseCase {
    @NotBlank
    private String name;
    /**
     * Type of the use case.
     * Specifies where the file is put.
     */
    @NotNull
    private Type type;
    /**
     * Defines how the target coo is gathered.
     */
    @NotNull
    private CooSource cooSource;
    /**
     * Regex pattern for overwriting filename in dms by providing regex pattern.
     * Pattern is applied to S3 filename.
     * The filename is used as ContentObject name.
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String filenameOverwritePattern;
    /**
     * Regex pattern for defining a custom Incoming name.
     * If not defined overwritten filename is used.
     * Only applies to {@link Type#PROCEDURE_INCOMING}
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String incomingNamePattern;
    /**
     * Regex pattern for extracting target coo from filename.
     * {@link UseCase.CooSource#FILENAME}
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String filenameCooPattern;
    /**
     * Regex pattern for extracting target dms resource name from filename.
     * {@link UseCase.CooSource#FILENAME_NAME}
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String filenameNamePattern;
    /**
     * Map for resolving target coo via filename.
     * Key: Regex which is matched against filename (case-insensitive).
     * Value: Target coo.
     * First match is used.
     * {@link UseCase.CooSource#FILENAME_MAP}
     */
    private Map<String, String> filenameToCoo;
    /**
     * Static target coo.
     * See {@link UseCase.CooSource#STATIC}
     */
    private String targetCoo;
    /**
     * Verify name of resolved Procedure against pattern, if defined.
     * Only applies to {@link Type#PROCEDURE_INCOMING}
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String verifyProcedureNamePattern;
    /**
     * Reuse Incoming with same name if true.
     * Only applies to {@link Type#PROCEDURE_INCOMING}
     */
    private boolean reuseIncoming = false;
    /**
     * Fill subject with metadata. See {@link de.muenchen.oss.swim.dms.configuration.SwimDmsProperties}.
     * Currently only works for {@link Type#PROCEDURE_INCOMING}.
     */
    private boolean metadataSubject = false;
    /**
     * Username used for accessing dms.
     * Used except {@link UseCase.CooSource#METADATA_FILE}
     */
    private String username;
    /**
     * Joboe for dms requests.
     */
    private String joboe = null;
    /**
     * Jobposition for dms requests.
     */
    private String jobposition = null;

    @Getter
    public enum Type {
        /**
         * Create a Content Object inside an Inbox.
         */
        INBOX_CONTENT_OBJECT(DmsResourceType.INBOX, DmsResourceType.CONTENT_OBJECT),
        /**
         * Create an Incoming either inside given Procedure {@link DmsTarget#getCoo()} or OU work queue of
         * {@link DmsTarget#getUsername()}.
         */
        PROCEDURE_INCOMING(DmsResourceType.PROCEDURE, DmsResourceType.INCOMING),
        /**
         * Resolve target resource type from metadata file.
         */
        METADATA_FILE(null, null);

        /**
         * The type of the target to create the new resource under.
         */
        private final DmsResourceType target;
        /**
         * The type of resource to create.
         */
        private final DmsResourceType type;

        Type(final DmsResourceType target, final DmsResourceType type) {
            this.target = target;
            this.type = type;
        }
    }

    public enum CooSource {
        /**
         * Target coo is extracted from separate metadata file.
         */
        METADATA_FILE,
        /**
         * Target coo is extracted from filename with regex.
         * {@link UseCase#filenameCooPattern}
         */
        FILENAME,
        /**
         * Target coo via static filename map.
         * Searches for key matching filename (case-insensitive) and uses value as target coo.
         * {@link UseCase#filenameToCoo}
         */
        FILENAME_MAP,
        /**
         * Resolve target coo by search for name extracted from filename.
         * See {@link UseCase#filenameNamePattern}.
         */
        FILENAME_NAME,
        /**
         * Target coo is statically configured.
         * {@link UseCase#targetCoo}
         */
        STATIC,
        /**
         * Target is OU work queue of {@link UseCase#username}.
         * Can only be used with {@link Type#PROCEDURE_INCOMING}.
         */
        OU_WORK_QUEUE
    }
}
