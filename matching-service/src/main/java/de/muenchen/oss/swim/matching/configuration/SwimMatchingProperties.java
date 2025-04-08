package de.muenchen.oss.swim.matching.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("swim")
@Data
@Validated
public class SwimMatchingProperties {
    /**
     * Cron for triggering import via export in DMS.
     */
    @NotBlank
    final String scheduleCron;
}
