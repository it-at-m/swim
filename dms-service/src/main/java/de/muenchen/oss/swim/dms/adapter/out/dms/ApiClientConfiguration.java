package de.muenchen.oss.swim.dms.adapter.out.dms;

import de.muenchen.refarch.integration.dms.ApiClient;
import de.muenchen.refarch.integration.dms.api.ContentObjectsApi;
import de.muenchen.refarch.integration.dms.api.IncomingFromInboxApi;
import de.muenchen.refarch.integration.dms.api.IncomingsApi;
import de.muenchen.refarch.integration.dms.api.ObjectAndImportToInboxApi;
import de.muenchen.refarch.integration.dms.api.ProcedureObjectsApi;
import de.muenchen.refarch.integration.dms.api.ProceduresApi;
import de.muenchen.refarch.integration.dms.api.SearchObjNamesApi;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
class ApiClientConfiguration {
    // FIXME remove workaround after API response was changed
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
    protected ObjectAndImportToInboxApi objectAndImportToInboxApi(final ApiClient apiClient) {
        return new ObjectAndImportToInboxApi(apiClient);
    }

    @Bean
    protected IncomingsApi incomingsApi(final ApiClient apiClient) {
        return new IncomingsApi(apiClient);
    }

    @Bean
    protected ProceduresApi proceduresApi(final ApiClient apiClient) {
        return new ProceduresApi(apiClient);
    }

    @Bean
    protected ProcedureObjectsApi procedureObjectsApi(final ApiClient apiClient) {
        return new ProcedureObjectsApi(apiClient);
    }

    @Bean
    protected ContentObjectsApi contentObjectsApi(final ApiClient apiClient) {
        return new ContentObjectsApi(apiClient);
    }

    @Bean
    protected SearchObjNamesApi searchObjNamesApi(final ApiClient apiClient) {
        return new SearchObjNamesApi(apiClient);
    }

    @Bean
    protected IncomingFromInboxApi incomingFromInboxApi(final ApiClient apiClient) {
        return new IncomingFromInboxApi(apiClient);
    }
}
