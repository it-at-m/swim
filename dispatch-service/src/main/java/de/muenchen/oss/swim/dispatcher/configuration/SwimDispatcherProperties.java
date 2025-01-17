package de.muenchen.oss.swim.dispatcher.configuration;

import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = "swim")
@Validated
public class SwimDispatcherProperties {
    // tag keys
    /**
     * Tag key used for dispatching file state.
     */
    @NotBlank
    private String dispatchStateTagKey;
    /**
     * Tag key used for protocol state.
     */
    @NotBlank
    private String protocolStateTagKey;
    /**
     * Tag key used for error class name.
     */
    @NotBlank
    private String errorClassTagKey;
    /**
     * Tag key used for error messages.
     */
    @NotBlank
    private String errorMessageTagKey;
    // tag values
    /**
     * Tag value for successfully dispatched files.
     */
    @NotBlank
    private String dispatchedStateTagValue;
    /**
     * Tag value for files finished processing.
     */
    @NotBlank
    private String dispatchFileFinishedTagValue;
    /**
     * Tag value for protocol finished processing.
     */
    @NotBlank
    private String protocolProcessedStateTageValue;
    /**
     * Tag value for error occurred.
     */
    @NotBlank
    private String errorStateValue = "error";
    // use cases
    @NotEmpty
    private List<UseCase> useCases = List.of();
    // mail
    @NotEmpty
    private String fallbackMail;
    // max file size
    /**
     * Max size files can have that they are dispatched.
     * Default: 100MiB
     */
    private Long maxFileSize = 100 * 1024 * 1024L;

    /**
     * Default tags which are excluded when looking up files for dispatching.
     *
     * @return Tags to exclude
     */
    public Map<String, List<String>> getDispatchExcludeTags() {
        return Map.of(
                dispatchStateTagKey, List.of(dispatchedStateTagValue, dispatchFileFinishedTagValue, errorStateValue));
    }

    /**
     * Default tags which are excluded when looking up protocol files.
     *
     * @return Tags to exclude
     */
    public Map<String, List<String>> getProtocolExcludeTags() {
        return Map.of(
                protocolStateTagKey, List.of(protocolProcessedStateTageValue, errorStateValue));
    }

    /**
     * Finde UseCase via name.
     *
     * @param useCaseName Name of the UseCase to find.
     * @return The first UseCase with the given name.
     * @throws UseCaseException If no UseCase was found.
     */
    public UseCase findUseCase(final String useCaseName) throws UseCaseException {
        return this.getUseCases().stream()
                .filter(i -> i.getName().equals(useCaseName))
                .findFirst().orElseThrow(() -> new UseCaseException("Unknown use case " + useCaseName));
    }
}
