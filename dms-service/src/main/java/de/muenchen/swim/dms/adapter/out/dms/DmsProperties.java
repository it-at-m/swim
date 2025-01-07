package de.muenchen.swim.dms.adapter.out.dms;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "swim.dms")
@Data
@Validated
public class DmsProperties {
    @NotBlank
    private String baseUrl;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
