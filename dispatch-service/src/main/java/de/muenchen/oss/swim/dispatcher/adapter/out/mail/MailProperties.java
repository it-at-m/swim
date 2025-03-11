package de.muenchen.oss.swim.dispatcher.adapter.out.mail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Locale;
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
    /**
     * Localization of mails.
     * Available options are de and en.
     * Default is en.
     */
    @NotBlank
    @Pattern(regexp = "de|en")
    private Locale locale = Locale.ENGLISH;
}
