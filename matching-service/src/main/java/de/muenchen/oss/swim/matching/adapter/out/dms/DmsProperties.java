package de.muenchen.oss.swim.matching.adapter.out.dms;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "swim.dms")
@Data
@Validated
public class DmsProperties {
    @NotBlank
    private String importCoo;
    @NotBlank
    private String importUsername;
}
