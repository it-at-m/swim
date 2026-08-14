package de.muenchen.oss.swim.dms.configuration;

import de.muenchen.oss.swim.dms.application.usecase.CleanupShadowFilesUseCase;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.dms.domain.model.UseCaseIncoming;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.UnknownUseCaseException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = "swim")
@Validated
public class SwimDmsProperties {
    @NotNull
    @Valid
    @NestedConfigurationProperty
    private List<UseCase> useCases = List.of();
    /**
     * Collection of var names in metadata file to get target user inbox.
     */
    @Valid
    @NotNull
    private MetadataRequestContextProperty metadataUserInbox;
    /**
     * Collection of var names in metadata file to get target group inbox.
     */
    @Valid
    @NotNull
    private MetadataRequestContextProperty metadataGroupInbox;
    /**
     * Collection of var names in metadata file to get target incoming.
     */
    @Valid
    @NotNull
    private MetadataRequestContextProperty metadataIncoming;
    /**
     * Collection of var names in metadata file to get target shadow file.
     */
    @Valid
    @NotNull
    private MetadataRequestContextProperty metadataShadowFile;
    /**
     * Var name in metadata file to get dms target resource type from.
     */
    @NotBlank
    private String metadataDmsTargetKey;
    /**
     * Prefix of metadata index fields which should be put into subject.
     * See {@link UseCaseIncoming#isMetadataSubject()}.
     */
    @NotBlank
    private String metadataSubjectPrefix;
    /**
     * Attributes used for the shadow file cleanup, see {@link CleanupShadowFilesUseCase}.
     */
    @NotNull
    @Valid
    private ShadowFileCleanup shadowFileCleanup = new ShadowFileCleanup();

    @Data
    public static class ShadowFileCleanup {
        /**
         * The cron definition to use for triggering the scheduled cleanup.
         */
        @NotBlank
        private String cron;
        /**
         * User to use for cleaning up shadow files.
         */
        @NotBlank
        private String username;
        /**
         * Job oe to use for cleaning up shadow files.
         */
        private String jobOe;
        /**
         * Job position to use for cleaning up shadow files.
         */
        private String jobPosition;
        /**
         * List of shadow file COOs to clean up via {@link CleanupShadowFilesUseCase}
         */
        @NotNull
        private List<@NotBlank String> coos = List.of();
    }

    /**
     * Resolve use case via name.
     *
     * @param useCase The name of the use case.
     * @return The use case with the name.
     * @throws UnknownUseCaseException If there is no use case with that name.
     */
    public UseCase findUseCase(@NotBlank final String useCase) throws UnknownUseCaseException {
        return this.getUseCases().stream()
                .filter(i -> i.getName().equals(useCase)).findFirst()
                .orElseThrow(() -> new UnknownUseCaseException(String.format("Unknown use case %s", useCase)));
    }

    public record MetadataRequestContextProperty(
            @NotBlank String userKey,
            @NotBlank String jobOeKey,
            @NotBlank String jobPositionKey,
            @NotBlank String cooKey) {
    }
}
