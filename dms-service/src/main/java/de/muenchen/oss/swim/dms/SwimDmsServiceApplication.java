package de.muenchen.oss.swim.dms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application class for starting the microservice.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@SuppressWarnings("PMD.UseUtilityClass")
public class SwimDmsServiceApplication {
    public static void main(final String[] args) {
        SpringApplication.run(SwimDmsServiceApplication.class, args);
    }
}
