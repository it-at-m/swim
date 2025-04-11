package de.muenchen.oss.swim.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application class for starting the microservice.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableScheduling
@SuppressWarnings("PMD.UseUtilityClass")
public class SwimMatchingServiceApplication {
    public static void main(final String[] args) {
        SpringApplication.run(SwimMatchingServiceApplication.class, args);
    }
}
