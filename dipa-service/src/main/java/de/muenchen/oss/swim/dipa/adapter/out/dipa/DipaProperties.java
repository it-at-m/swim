package de.muenchen.oss.swim.dipa.adapter.out.dipa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties("swim.dipa")
@Data
@ToString(exclude = "password")
@Validated
class DipaProperties {
    @NotBlank
    private String endpointUrl;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotNull
    private Duration sendTimeout;
}
