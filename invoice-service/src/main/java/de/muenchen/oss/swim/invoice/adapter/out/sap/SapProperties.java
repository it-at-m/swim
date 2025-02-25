package de.muenchen.oss.swim.invoice.adapter.out.sap;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties("swim.sap")
class SapProperties {
    private String endpoint;
    private String username;
    private String password;
}
