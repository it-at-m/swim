package de.muenchen.oss.swim.dispatcher.adapter.out.s3;

import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import io.minio.MinioClient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@ConfigurationProperties("swim.s3")
class S3Properties {
    /**
     * Internal mapping of tenant names to already created Clients.
     * Ensures only one MinoClient per tenant is created and reused.
     */
    private static final Map<String, MinioClient> clients = new HashMap<>();
    /**
     * List of S3 tenants which can be used as {@link UseCase#getTenant()}.
     */
    @NestedConfigurationProperty
    @NotNull
    private Map<String, ConnectionOptions> tenants;
    /**
     * Time in seconds after which the created presigned urls expire.
     * Default: 7d
     */
    @NotNull
    private int presignedUrlExpiry = 7 * 24 * 60 * 60;

    /**
     * Configuration for a single S3 tenant.
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    @Getter
    @ToString(exclude = { "secretKey", "accessKey" })
    static class ConnectionOptions {
        @NotBlank
        private String url;
        @NotBlank
        private String accessKey;
        @NotBlank
        private String secretKey;
    }

    /**
     * Get a MinioClient for a specific tenant name.
     * See {@link #tenants}.
     *
     * @param tenant The name of the tenant to get the client for.
     * @return A MinioClient for the specified tenant.
     */
    protected MinioClient getClient(final String tenant) {
        return clients.computeIfAbsent(tenant, key -> {
            if (this.tenants.containsKey(key)) {
                throw new IllegalArgumentException("Tenant doesn't exist: " + tenant);
            }
            final ConnectionOptions options = this.tenants.get(key);
            return MinioClient.builder()
                    .endpoint(options.url)
                    .credentials(options.accessKey, options.secretKey)
                    .build();
        });
    }

    /**
     * Find a tenant name by url.
     * See {@link #tenants} and {@link ConnectionOptions#url}.
     *
     * @param url The url to search for.
     * @return The resolved tenant name.
     */
    protected String findTenantByUrl(final String url) {
        return this.tenants.entrySet().stream()
                .filter(entry -> entry.getValue().getUrl().equals(url))
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow(
                        () -> new IllegalArgumentException(String.format("Tenant with URL %s doesn't exist", url))
                );
    }
}
