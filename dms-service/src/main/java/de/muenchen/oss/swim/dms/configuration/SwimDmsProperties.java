package de.muenchen.oss.swim.dms.configuration;

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
     * Var name in metadata file to get target user inbox coo from.
     */
    @NotBlank
    private String metadataUserInboxCooKey;
    /**
     * Var name in metadata file to get target user inbox owner from.
     */
    @NotBlank
    private String metadataUserInboxUserKey;
    /**
     * Var name in metadata file to get target group inbox coo from.
     */
    @NotBlank
    private String metadataGroupInboxCooKey;
    /**
     * Var name in metadata file to get target user inbox owner from.
     */
    @NotBlank
    private String metadataGroupInboxUserKey;
    /**
     * Var name in metadata file to get target incoming coo from.
     */
    @NotBlank
    private String metadataIncomingCooKey;
    /**
     * Var name in metadata file to get target incoming owner from.
     */
    @NotBlank
    private String metadataIncomingUserKey;
    /**
     * Var name in metadata file to get target incoming joboe from.
     */
    @NotBlank
    private String metadataIncomingJoboeKey;
    /**
     * Var name in metadata file to get target incoming jobposition from.
     */
    @NotBlank
    private String metadataIncomingJobpositionKey;
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
     * Prefix for {@link UseCase#isDecodeGermanChars()}.
     */
    @NotBlank
    private String decodeGermanCharsPrefix;

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
}
