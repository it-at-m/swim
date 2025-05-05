package de.muenchen.oss.swim.dipa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Application class for starting the microservice.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@SuppressWarnings("PMD.UseUtilityClass")
public class SwimDipaServiceApplication {
    public static void main(final String[] args) {
        SpringApplication.run(SwimDipaServiceApplication.class, args);
    }
}
