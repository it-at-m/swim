package de.muenchen.oss.swim.dispatcher.domain.model;

import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
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

    /**
     * Get {@link UseCase#path} without slash at end.
     *
     * @return Configured path without slash at end.
     */
    public String getPathWithoutSlash() {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Get full path of folder for files to dispatch.
     *
     * @param properties Used for getting dispatch folder name.
     * @return Full path of dispatch folder.
     */
    public String getDispatchPath(final SwimDispatcherProperties properties) {
        return String.format("%s/%s", this.getPathWithoutSlash(), properties.getDispatchFolder());
    }

    /**
     * Get full path of folder for finished files.
     *
     * @param properties Used for getting finished folder name.
     * @return Full path of finished folder.
     */
    public String getFinishedPath(final SwimDispatcherProperties properties) {
        return String.format("%s/%s", this.getPathWithoutSlash(), properties.getFinishedFolder());
    }

    /**
     * Get finished path for a given file path.
     * Transforms the file path from the process folder to the finished folder.
     *
     * @param properties Used for getting dispatch and finished folder name.
     * @param originalPath Path of the file in the process folder.
     * @return Path of the file in the finished folder.
     */
    public String getFinishedPath(final SwimDispatcherProperties properties, final String originalPath) {
        return originalPath.replaceFirst("^" + this.getDispatchPath(properties), this.getFinishedPath(properties));
    }
}
