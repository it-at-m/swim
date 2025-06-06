package de.muenchen.oss.swim.dispatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application class for starting the microservice.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableJpaAuditing
@SuppressWarnings("PMD.UseUtilityClass")
public class SwimDispatcherServiceApplication {
    public static void main(final String[] args) {
        SpringApplication.run(SwimDispatcherServiceApplication.class, args);
    }
}
