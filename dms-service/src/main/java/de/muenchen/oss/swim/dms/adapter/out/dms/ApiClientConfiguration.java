package de.muenchen.oss.swim.dms.adapter.out.dms;

import de.muenchen.refarch.integration.dms.ApiClient;
import de.muenchen.refarch.integration.dms.api.IncomingsApi;
import de.muenchen.refarch.integration.dms.api.ObjectAndImportToInboxApi;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
class ApiClientConfiguration {
    @Bean
    protected ApiClient apiClient(final DmsProperties dmsProperties) {
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(dmsProperties.getBaseUrl());
        apiClient.setUsername(dmsProperties.getUsername());
        apiClient.setPassword(dmsProperties.getPassword());
        return apiClient;
    }

    @Bean
    protected ObjectAndImportToInboxApi objectAndImportToInboxApi(final ApiClient apiClient) {
        return new ObjectAndImportToInboxApi(apiClient);
    }

    @Bean
    protected IncomingsApi incomingsApi(final ApiClient apiClient) {
        return new IncomingsApi(apiClient);
    }
}
