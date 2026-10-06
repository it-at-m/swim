package de.muenchen.oss.swim.dispatcher.adapter.out.s3;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.Data;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("swim.s3")
@Validated
class S3Properties {
    /**
     * Time after which the created presigned urls expire.
     * Numeric configuration values are interpreted as seconds.
     * Default: 7d
     */
    @NotNull
    @DurationUnit(ChronoUnit.SECONDS)
    @DurationMin(hours = 1)
    @DurationMax(days = 7)
    private Duration presignedUrlExpiry = Duration.ofDays(7);
}
