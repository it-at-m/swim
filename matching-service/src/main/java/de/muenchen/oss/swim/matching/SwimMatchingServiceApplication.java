package de.muenchen.oss.swim.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Application class for starting the microservice.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@SuppressWarnings("PMD.UseUtilityClass")
public class SwimMatchingServiceApplication {
    public static void main(final String[] args) {
        SpringApplication.run(SwimMatchingServiceApplication.class, args);
    }
}
