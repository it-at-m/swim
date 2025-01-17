package de.muenchen.swim.dispatcher.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class UseCase {
    /**
     * Name of the use case.
     */
    @NotBlank
    private String name;
    /**
     * The s3 bucket to look for files in.
     */
    @NotBlank
    private String bucket;
    /**
     * The folder prefix to look for files in.
     */
    @NotBlank
    private String path;
    /**
     * If to look recursive for files.
     */
    private boolean recursive = false;
    /**
     * If filename contains sensitive data and should not be logged.
     */
    private boolean sensitiveFilename = false;
    /**
     * If metadata file is required by use case.
     */
    private boolean requiresMetadata = false;
    /**
     * Tags required to start processing file.
     */
    private Map<String, String> requiredTags = Map.of();
    /**
     * Destination to send notification about file to.
     */
    @NotBlank
    private String destinationBinding;
    /**
     * Mail addresses to notify about use case errors or specific events.
     */
    @NotNull
    private List<String> mailAddresses = List.of();
}
