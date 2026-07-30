package de.muenchen.oss.swim.dispatcher.adapter.out.s3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("swim.s3")
class S3Properties {
    @NotBlank
    private String url;
    @NotBlank
    private String accessKey;
    @NotBlank
    private String secretKey;
    /**
     * Time in seconds after which the created presigned urls expire.
     * Default: 7d
     */
    @NotNull
    private int presignedUrlExpiry = 7 * 24 * 60 * 60;
    /**
     * Timeout for connecting to S3.
     * Default: 30s
     */
    @NotNull
    private Duration connectionTimeout = Duration.ofSeconds(30);
    /**
     * Timeout for reading from S3.
     * Default: 60s
     */
    @NotNull
    private Duration readTimeout = Duration.ofSeconds(60);
    /**
     * Timeout for writing to S3.
     * Default: 60s
     */
    @NotNull
    private Duration writeTimeout = Duration.ofSeconds(60);
}
