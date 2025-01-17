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
}
