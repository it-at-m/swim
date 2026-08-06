package de.muenchen.oss.swim.matching.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.restclient.autoconfigure.RestTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * The central class for configuration of all security aspects.
 */
@Configuration
@Profile("!no-security")
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@Import(RestTemplateAutoConfiguration.class)
public class SecurityConfiguration {

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Value("${security.oauth2.resource.user-info-uri}")
    private String userInfoUri;
    @Value("${security.oauth2.open-id-connect-url}")
    private String openIdConnectUrl;

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) {
        http
                .authorizeHttpRequests((requests) -> requests.requestMatchers(
                        // allow access to /actuator/info
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/actuator/info"),
                        // allow access to /actuator/health for OpenShift Health Check
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/actuator/health"),
                        // allow access to /actuator/health/liveness for OpenShift Liveness Check
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/actuator/health/liveness"),
                        // allow access to /actuator/health/readiness for OpenShift Readiness Check
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/actuator/health/readiness"),
                        // allow access to /actuator/metrics for Prometheus monitoring in OpenShift
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/actuator/metrics"),
                        // allow access to swagger-ui
                        PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
                        // allow access to opean-api endpoints
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/v3/api-docs"),
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/v3/api-docs.yaml"),
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/v3/api-docs/**"))
                        .permitAll())
                .authorizeHttpRequests((requests) -> requests.requestMatchers("/**")
                        .authenticated())
                .oauth2ResourceServer(httpSecurityOAuth2ResourceServerConfigurer -> httpSecurityOAuth2ResourceServerConfigurer
                        .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(new JwtUserInfoAuthenticationConverter(
                                new UserInfoAuthoritiesService(userInfoUri, restTemplateBuilder)))));

        return http.build();
    }

    @Bean
    public OpenAPI openApiLogin() {
        return new OpenAPI()
                .info(new Info().title("SWIM Matching Service"))
                .components(new Components()
                        .addSecuritySchemes(
                                "swim-matching-scheme",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.OPENIDCONNECT)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .openIdConnectUrl(openIdConnectUrl)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")));
    }

}
