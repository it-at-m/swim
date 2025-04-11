package de.muenchen.oss.swim.matching.configuration;

import de.muenchen.oss.swim.matching.domain.model.DmsInbox;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
    private String scheduleCron;
    /**
     * Whitelist of DMS tenant to import.
     * See {@link DmsInbox#getDmsTenant()}
     */
    @NotNull
    List<String> dmsTenants;
}
