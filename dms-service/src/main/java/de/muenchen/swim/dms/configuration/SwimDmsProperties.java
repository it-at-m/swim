package de.muenchen.swim.dms.configuration;

import de.muenchen.swim.dms.domain.model.UseCase;
import jakarta.validation.Valid;
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
}
