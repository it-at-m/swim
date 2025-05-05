package de.muenchen.oss.swim.dipa.adapter.out.dipa;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("swim.dipa")
@Data
@ToString(exclude = "password")
@Validated
class DipaProperties {
    @NotBlank
    final String endpointUrl;
    @NotBlank
    final String username;
    @NotBlank
    final String password;
}
