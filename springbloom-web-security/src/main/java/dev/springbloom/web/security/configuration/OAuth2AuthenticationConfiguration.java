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

import dev.springbloom.data.multitenant.context.MultiTenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.ConditionalOnOAuth2ClientRegistrationProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor.ClientRegistrationIdResolver;
import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver;
import org.springframework.security.oauth2.client.web.method.annotation.OAuth2AuthorizedClientArgumentResolver;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static dev.springbloom.data.multitenant.context.MultiTenantContextHolder.MODE_INHERITABLE_THREAD_LOCAL;
import static org.springframework.security.core.context.SecurityContextHolder.MODE_INHERITABLETHREADLOCAL;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OAuth2AuthorizedClient.class)
@ConditionalOnOAuth2ClientRegistrationProperties
public class OAuth2AuthenticationConfiguration {

    public OAuth2AuthenticationConfiguration(
        @Value("${spring.mvc.async.delegate-security-context:true}")
        boolean springMvcAsyncDelegateSecurityContext,

        RequestMappingHandlerAdapter requestMappingHandlerAdapter
    ) {
        if (springMvcAsyncDelegateSecurityContext) {
            SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL);
            MultiTenantContextHolder.setStrategyName(MODE_INHERITABLE_THREAD_LOCAL);

            // As we have defined a new SecurityContextHolderStrategy, we must reconfigure the argument resolvers.
            if (requestMappingHandlerAdapter.getArgumentResolvers() != null) {
                requestMappingHandlerAdapter.getArgumentResolvers().forEach(argumentResolver -> {
                    if (argumentResolver instanceof OAuth2AuthorizedClientArgumentResolver oAuth2AuthorizedClientArgumentResolver) {
                        oAuth2AuthorizedClientArgumentResolver.setSecurityContextHolderStrategy(
                            SecurityContextHolder.getContextHolderStrategy()
                        );
                    }
                });
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnOAuth2ClientRegistrationProperties
    @ConditionalOnProperty(
        name = {
            "spring.web.http.clients.enabled",
            "spring.web.http.clients.decorate-with-oauth2-access-token",
        },
        havingValue = "true"
    )
    public static class HttpClientsSecurityConfiguration {

        @Autowired
        public HttpClientsSecurityConfiguration(
            @Qualifier("defaultRestTemplate") RestTemplate defaultRestTemplate,
            @Qualifier("defaultRestClient") RestClient defaultRestClient,
            ConfigurableListableBeanFactory beanFactory,
            OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
            OAuth2AuthorizedClientRepository authorizedClientRepository,
            OAuth2ClientProperties oAuth2ClientProperties
        ) {
            var interceptor = getOAuth2ClientHttpRequestInterceptor(
                oAuth2AuthorizedClientManager, authorizedClientRepository, oAuth2ClientProperties
            );

            defaultRestTemplate.getInterceptors().add(interceptor);
            if (defaultRestClient != null) {
                var decoratedDefaultRestClient = defaultRestClient.mutate().requestInterceptor(
                    interceptor
                ).build();

                ((DefaultListableBeanFactory) beanFactory).destroySingleton("defaultRestClient");
                beanFactory.autowireBean(decoratedDefaultRestClient);
            }
        }
    }

    @Bean
    @ConditionalOnOAuth2ClientRegistrationProperties
    @ConditionalOnMissingBean(RestClient.class)
    public RestClient defaultOAuth2RestClient(
        OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
        OAuth2AuthorizedClientRepository authorizedClientRepository,
        OAuth2ClientProperties oAuth2ClientProperties
    ) {
        var interceptor = getOAuth2ClientHttpRequestInterceptor(
            oAuth2AuthorizedClientManager, authorizedClientRepository, oAuth2ClientProperties
        );
        return RestClient.builder().requestInterceptor(interceptor).build();
    }

    private static @NotNull OAuth2ClientHttpRequestInterceptor getOAuth2ClientHttpRequestInterceptor(
        OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
        OAuth2AuthorizedClientRepository authorizedClientRepository,
        OAuth2ClientProperties oAuth2ClientProperties
    ) {
        var compositeClientRegistrationIdResolver = compositeClientRegistrationIdResolver(oAuth2ClientProperties);
        var interceptor = new OAuth2ClientHttpRequestInterceptor(
            oAuth2AuthorizedClientManager
        );
        interceptor.setClientRegistrationIdResolver(compositeClientRegistrationIdResolver);
        interceptor.setPrincipalResolver(
            (request) -> SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication()
        );

        OAuth2AuthorizationFailureHandler authorizationFailureHandler =
            OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(authorizedClientRepository);
        interceptor.setAuthorizationFailureHandler(authorizationFailureHandler);
        return interceptor;
    }

    private static ClientRegistrationIdResolver compositeClientRegistrationIdResolver(
        OAuth2ClientProperties oAuth2ClientProperties
    ) {
        ClientRegistrationIdResolver requestAttributes = new RequestAttributeClientRegistrationIdResolver();
        ClientRegistrationIdResolver currentUser = authenticationClientRegistrationIdResolver();
        return (request) -> {
            String clientRegistrationId = requestAttributes.resolve(request);
            if (clientRegistrationId == null) {
                clientRegistrationId = currentUser.resolve(request);
            }
            if (clientRegistrationId == null) {
                clientRegistrationId = getClientRegistrationId(oAuth2ClientProperties);
            }
            return clientRegistrationId;
        };
    }

    private static ClientRegistrationIdResolver authenticationClientRegistrationIdResolver() {
        SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
        return (request) -> {
            Authentication authentication = securityContextHolderStrategy.getContext().getAuthentication();
            return (authentication instanceof OAuth2AuthenticationToken principal)
                ? principal.getAuthorizedClientRegistrationId() : null;
        };
    }

    private static String getClientRegistrationId(OAuth2ClientProperties oAuth2ClientProperties) {
        String clientRegistrationId;
        if (oAuth2ClientProperties.getRegistration().size() > 1) {
            log.info("Multiple OAuth2 Client authentication configured! Using first one...");
        }
        clientRegistrationId = oAuth2ClientProperties.getRegistration().keySet().iterator().next();
        return clientRegistrationId;
    }
}
