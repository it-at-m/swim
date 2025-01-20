package de.muenchen.oss.swim.dms.adapter.out.dms;

import jakarta.validation.constraints.NotBlank;
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
}
