package de.muenchen.oss.swim.dms.domain.model;

import de.muenchen.oss.swim.dms.domain.helper.PatternHelper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import lombok.Data;

/**
 * Defines how the target coo is gathered.
 */
@Data
public class UseCaseSource {
    /**
     * Defines how the target coo is gathered.
     */
    @NotNull
    private Type type;
    /**
     * Regex pattern for extracting target coo from filename.
     * {@link Type#FILENAME}
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String filenameCooPattern;
    /**
     * Regex pattern for extracting target dms resource name from filename.
     * {@link Type#FILENAME_NAME}
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String filenameNamePattern;
    /**
     * Map for resolving target coo via filename.
     * Key: Regex which is matched against filename (case-insensitive).
     * Value: Target coo.
     * First match is used.
     * {@link Type#FILENAME_MAP}
     */
    private Map<String, String> filenameToCoo;
    /**
     * Static target coo.
     * See {@link Type#STATIC}
     */
    private String targetCoo;

    public enum Type {
        /**
         * Target coo is extracted from separate metadata file.
         */
        METADATA_FILE,
        /**
         * Target coo is extracted from filename with regex.
         * {@link UseCaseSource#filenameCooPattern}
         */
        FILENAME,
        /**
         * Target coo via static filename map.
         * Searches for key matching filename (case-insensitive) and uses value as target coo.
         * {@link UseCaseSource#filenameToCoo}
         */
        FILENAME_MAP,
        /**
         * Resolve target coo by search for name extracted from filename.
         * See {@link UseCaseSource#filenameNamePattern}.
         */
        FILENAME_NAME,
        /**
         * Target coo is statically configured.
         * {@link UseCaseSource#targetCoo}
         */
        STATIC,
        /**
         * Target is OU work queue of {@link DmsRequestContext#username}.
         * Can only be used with {@link UseCaseType#PROCEDURE_INCOMING}.
         */
        OU_WORK_QUEUE
    }
}
