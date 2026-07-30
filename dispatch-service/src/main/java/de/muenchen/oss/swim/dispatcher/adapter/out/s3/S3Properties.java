package de.muenchen.oss.swim.dispatcher.adapter.out.s3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.Data;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("swim.s3")
@Validated
class S3Properties {
    @NotBlank
    private String url;
    @NotBlank
    private String accessKey;
    @NotBlank
    private String secretKey;
    /**
     * Time after which the created presigned urls expire.
     * Numeric configuration values are interpreted as seconds.
     * Default: 7d
     */
    @NotNull
    @DurationUnit(ChronoUnit.SECONDS)
    @DurationMin(hours = 1)
    private Duration presignedUrlExpiry = Duration.ofDays(7);
    /**
     * Timeout for connecting to S3.
     * Default: 30s
     */
    @NotNull
    @DurationMin(seconds = 1)
    private Duration connectionTimeout = Duration.ofSeconds(30);
    /**
     * Timeout for reading from S3.
     * Default: 60s
     */
    @NotNull
    @DurationMin(seconds = 1)
    private Duration readTimeout = Duration.ofSeconds(60);
    /**
     * Timeout for writing to S3.
     * Default: 60s
     */
    @NotNull
    @DurationMin(seconds = 1)
    private Duration writeTimeout = Duration.ofSeconds(60);
}
