package de.muenchen.oss.swim.dms.adapter.out.dms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "swim.dms")
@Data
@Validated
@ToString(exclude = "password")
public class DmsProperties {
    @NotBlank
    private String baseUrl;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotNull
    private Duration connectionTimeout = Duration.ofSeconds(30);
    @NotNull
    private Duration readTimeout = Duration.ofSeconds(180);
}
