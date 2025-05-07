package de.muenchen.oss.swim.dipa.domain.model;

import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Defines how the target PersNr and Category are gathered.
 */
@Data
public class UseCaseSource {
    /**
     * Defines how the target PersNr and Category are gathered.
     */
    @NotNull
    private Type type;
    /**
     * Regex pattern for extracting target PersNr from filename.
     * {@link Type#FILENAME}
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String filenamePersNrPattern;
    /**
     * Regex pattern for extracting target Category from filename.
     * {@link Type#FILENAME}
     */
    @Pattern(regexp = PatternHelper.RAW_PATTERN)
    private String filenameCategoryPattern;
    /**
     * Static target PersNr.
     * See {@link Type#STATIC}
     */
    private String staticPersNr;
    /**
     * Static target Category.
     * See {@link Type#STATIC}
     */
    private String staticCategory;

    public enum Type {
        /**
         * Target PersNr and Category are extracted from filename with regex.
         */
        FILENAME,
        /**
         * Target PersNr and Category are statically configured.
         */
        STATIC
    }
}
