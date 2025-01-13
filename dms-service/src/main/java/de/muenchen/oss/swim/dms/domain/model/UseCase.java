package de.muenchen.oss.swim.dms.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
     * Static target coo.
     * See {@link UseCase.CooSource#STATIC}
     */
    private String targetCoo;
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

    public enum Type {
        /**
         * Create an Object inside an Inbox.
         */
        INBOX,
        /**
         * Create an Incoming inside a Procedure.
         */
        INCOMING_OBJECT
    }

    public enum CooSource {
        /**
         * Target coo is extracted from separate metadata file.
         */
        METADATA_FILE,
        FILENAME,
        /**
         * Target coo is statically configured.
         * {@link UseCase#targetCoo}
         */
        STATIC
    }
}