/*
 * Copyright (c) 2025 - Felipe Desiderati
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package dev.springbloom.web.security.configuration;

import dev.springbloom.web.security.auth.sign.SignRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for enabling Sign Request-based authentication and HTTP client security features.
 * This configuration ensures that request signing headers are automatically added to outgoing HTTP requests.
 * It is conditionally loaded based on the presence of specific properties and classes in the application context.
 * <p>
 * This configuration primarily serves as middleware to secure HTTP communication by signing requests
 * using credentials defined in the application properties.
 * <p>
 * The configuration consists of a nested class {@code HttpClientsSecurityConfiguration}, which is responsible
 * for decorating HTTP clients (e.g., RestTemplate and RestClient) with appropriate security headers.
 * <p>
 * Required properties for enabling this configuration:
 * <ul>
 *     <li>{@code spring.web.security.sign-request.client.id}: The client identifier for Sign Request authorization.</li>
 *     <li>{@code spring.web.security.sign-request.client.secret-key}: The secret key for signing and validating requests.</li>
 * </ul>
 * <p>
 * Optional properties for enabling additional HTTP client decoration:
 * <ul>
 *     <li>{@code spring.web.http.clients.enabled}</li>
 *     <li>{@code spring.web.http.clients.decorate-with-sign-request-token}</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty({
    "spring.web.security.sign-request.client.id",
    "spring.web.security.sign-request.client.secret-key"
})
public class SignRequestClientConfiguration {

    /**
     * Configuration class for HTTP clients security.
     * <p>
     * This class decorates HTTP clients (RestTemplate and RestClient) with Sign Request headers.
     * It automatically adds Sign Request headers to outgoing requests.
     * <p>
     * This configuration is conditionally enabled when Sign Request authorization is enabled, and
     * HTTP clients decoration is enabled in the application properties.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(
        name = {
            "spring.web.http.clients.enabled",
            "spring.web.http.clients.decorate-with-sign-request-token",
        },
        havingValue = "true"
    )
    public static class HttpClientsSecurityConfiguration {

        /**
         * This constructor initializes the configuration and decorates the default HTTP clients
         * with Sign Request headers.
         * <p>
         * It adds an interceptor to the HTTP clients that automatically adds Sign Request headers
         * to outgoing requests.
         *
         * @param defaultRestTemplate             The default RestTemplate bean
         * @param defaultRestClient               The default RestClient bean
         * @param beanFactory                     The bean factory for managing beans
         * @param signRequestAuthorizationService The class responsible for signing the request
         */
        @Autowired
        public HttpClientsSecurityConfiguration(
            @Qualifier("defaultRestTemplate") RestTemplate defaultRestTemplate,
            @Qualifier("defaultRestClient") RestClient defaultRestClient,
            ConfigurableListableBeanFactory beanFactory,
            SignRequestService signRequestAuthorizationService
        ) {
            ClientHttpRequestInterceptor signHttpRequestInterceptor =
                (request, body, execution) ->
                    execution.execute(signRequestAuthorizationService.sign(request, body), body);

            defaultRestTemplate.getInterceptors().add(signHttpRequestInterceptor);
            if (defaultRestClient != null) {
                var decoratedDefaultRestClient = defaultRestClient.mutate().requestInterceptor(
                    signHttpRequestInterceptor
                ).build();

                ((DefaultListableBeanFactory) beanFactory).destroySingleton("defaultRestClient");
                beanFactory.autowireBean(decoratedDefaultRestClient);
            }
        }
    }
}
