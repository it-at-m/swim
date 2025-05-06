package de.muenchen.oss.swim.matching.adapter.out.dms;

import de.muenchen.refarch.integration.dms.ApiClient;
import de.muenchen.refarch.integration.dms.api.ContentObjectsApi;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
class ApiClientConfiguration {
    // FIXME remove workaround after response is streamed
    private static final int MAX_RESPONSE_BODY_SIZE = 100 * 1024 * 1024;

    @Bean
    protected ApiClient apiClient(final DmsProperties dmsProperties) {
        final WebClient webClient = WebClient.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_RESPONSE_BODY_SIZE))
                .build();
        final ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(dmsProperties.getBaseUrl());
        apiClient.setUsername(dmsProperties.getUsername());
        apiClient.setPassword(dmsProperties.getPassword());
        return apiClient;
    }

    @Bean
    protected ContentObjectsApi contentObjectsApi(final ApiClient apiClient) {
        return new ContentObjectsApi(apiClient);
    }

}
