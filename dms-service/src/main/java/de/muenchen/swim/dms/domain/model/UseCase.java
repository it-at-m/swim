package de.muenchen.swim.dms.domain.model;

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

    public enum Type {
        /**
         * Create an Object inside an Inbox.
         */
        INBOX,
        PROCEDURE
    }

    public enum CooSource {
        /**
         * Target coo is extracted from separate metadata file.
         */
        METADATA_FILE,
        FILENAME
    }
}
