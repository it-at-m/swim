package de.muenchen.oss.swim.dms.adapter.out.dms;

import de.muenchen.oss.refarch.integration.dms.ApiClient;
import de.muenchen.oss.refarch.integration.dms.api.ContentObjectsApi;
import de.muenchen.oss.refarch.integration.dms.api.DepositObjectsApi;
import de.muenchen.oss.refarch.integration.dms.api.IncomingFromInboxApi;
import de.muenchen.oss.refarch.integration.dms.api.IncomingsApi;
import de.muenchen.oss.refarch.integration.dms.api.ObjectAndImportToInboxApi;
import de.muenchen.oss.refarch.integration.dms.api.ProcedureObjectsApi;
import de.muenchen.oss.refarch.integration.dms.api.ProceduresApi;
import de.muenchen.oss.refarch.integration.dms.api.SearchObjNamesApi;
import de.muenchen.oss.refarch.integration.dms.api.SubjectAreasApi;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
class ApiClientConfiguration {
    @Bean
    protected ApiClient dmsApiClient(final DmsProperties dmsProperties) {
        final HttpClient httpClient = HttpClient.create()
                .responseTimeout(dmsProperties.getReadTimeout())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.toIntExact(dmsProperties.getConnectionTimeout().toMillis()));
        final WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        final ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(dmsProperties.getBaseUrl());
        apiClient.setUsername(dmsProperties.getUsername());
        apiClient.setPassword(dmsProperties.getPassword());
        return apiClient;
    }

    @Bean
    protected ObjectAndImportToInboxApi objectAndImportToInboxApi(final ApiClient dmsApiClient) {
        return new ObjectAndImportToInboxApi(dmsApiClient);
    }

    @Bean
    protected IncomingsApi incomingsApi(final ApiClient dmsApiClient) {
        return new IncomingsApi(dmsApiClient);
    }

    @Bean
    protected ProceduresApi proceduresApi(final ApiClient dmsApiClient) {
        return new ProceduresApi(dmsApiClient);
    }

    @Bean
    protected ProcedureObjectsApi procedureObjectsApi(final ApiClient dmsApiClient) {
        return new ProcedureObjectsApi(dmsApiClient);
    }

    @Bean
    protected ContentObjectsApi contentObjectsApi(final ApiClient dmsApiClient) {
        return new ContentObjectsApi(dmsApiClient);
    }

    @Bean
    protected SearchObjNamesApi searchObjNamesApi(final ApiClient dmsApiClient) {
        return new SearchObjNamesApi(dmsApiClient);
    }

    @Bean
    protected IncomingFromInboxApi incomingFromInboxApi(final ApiClient dmsApiClient) {
        return new IncomingFromInboxApi(dmsApiClient);
    }

    @Bean
    protected DepositObjectsApi depositObjectsApi(final ApiClient dmsApiClient) {
        return new DepositObjectsApi(dmsApiClient);
    }

    @Bean
    protected SubjectAreasApi subjectAreasApi(final ApiClient dmsApiClient) {
        return new SubjectAreasApi(dmsApiClient);
    }
}
