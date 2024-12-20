package de.muenchen.oss.swim.matching.adapter.out.ldap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "swim.ldap")
@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public class LdapProperties {
    /**
     * List of ous from which users are loaded for inbox enrichment.
     */
    private List<String> searchOus = List.of();
    /**
     * Base ou to search for users in.
     * Is used with {@link String#format} to inject search ou.
     * Example: "ou=%s,o=example,c=com".
     */
    @NotBlank
    private String userBaseOu;
}
