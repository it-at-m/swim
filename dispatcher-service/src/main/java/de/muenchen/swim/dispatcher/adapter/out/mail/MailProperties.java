package de.muenchen.swim.dispatcher.adapter.out.mail;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "swim.mail")
public class MailProperties {
    @NotBlank
    private String fromAddress;
    /**
     * Prefix which is added to mail subject.
     * E.g. for defining environment.
     */
    private String mailSubjectPrefix = "";
}
