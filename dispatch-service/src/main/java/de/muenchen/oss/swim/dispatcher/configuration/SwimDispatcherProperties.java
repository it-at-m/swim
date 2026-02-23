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
@SuppressWarnings("PMD.TooManyFields")
public class SwimDispatcherProperties {
    // ########### tag keys ###########
    /**
     * Tag key used for dispatching file state.
     */
    @NotBlank
    private String dispatchStateTagKey;
    /**
     * Tag key used for custom dispatching actions.
     */
    @NotBlank
    private String dispatchActionTagKey;
    /**
     * Tag key used for resolving rerouting destination.
     * See {@link de.muenchen.oss.swim.dispatcher.domain.model.DispatchAction}.
     */
    @NotBlank
    private String dispatchActionDestinationTagKey;
    /**
     * Tag key used for protocol state.
     */
    @NotBlank
    private String protocolStateTagKey;
    /**
     * Tag key used for protocol match state (e.g. match, missingInProtocolAndFiles).
     */
    @NotBlank
    private String protocolMatchTagKey;
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
    // ########### tag values ###########
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
    private String protocolProcessedStateTagValue;
    /**
     * Tag value for state when file parsed protocol processing.
     * See {@link UseCase#isTagProtocolProcessed()}.
     */
    @NotBlank
    private String protocolProcessedFilesStateTagValue;
    /**
     * Tag value for error occurred.
     */
    @NotBlank
    private String errorStateValue;
    // ########### use cases ###########
    /**
     * Use cases which are processed.
     */
    @NotEmpty
    private List<UseCase> useCases = List.of();
    // ########### metadata ###########
    /**
     * Metadata IndexFields key to use for destination binding.
     * {@link UseCase#isOverwriteDestinationViaMetadata()}
     */
    @NotBlank
    private String metadataDispatchBindingKey;
    // ########### additional attributes ###########
    /**
     * Fallback mail address for notifications.
     * Is used when no recipients can be resolved via use case.
     */
    @NotEmpty
    private String fallbackMail;
    /**
     * Folder name where finished files are moved to.
     */
    @NotBlank
    private String finishedFolder;
    /**
     * Folder name where finished protocols are moved to.
     */
    @NotBlank
    private String protocolFinishedFolder;
    /**
     * Folder name where to look in for files to dispatch.
     * Looks inside {@link UseCase#getPath()} for this folder and dispatches files inside if matching
     * requirements.
     */
    @NotBlank
    private String dispatchFolder;

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
                protocolStateTagKey, List.of(protocolProcessedStateTagValue, errorStateValue));
    }

    /**
     * Find UseCase via name.
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
