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

import dev.springbloom.data.multitenant.MultiTenantAware;
import dev.springbloom.web.UrlUtils;
import dev.springbloom.web.configuration.CorsProperties;
import dev.springbloom.web.security.auth.jwt.*;
import dev.springbloom.web.security.auth.jwt.multitenant.InMemoryMultiTenantJwtUserDetailsManager;
import dev.springbloom.web.security.auth.jwt.multitenant.MultiTenantJwtAuthenticationConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.lang.NonNull;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Configuration class for self-contained JWT authentication.
 * <p>
 * In this setup, the application is fully responsible for validating user credentials
 * and issuing JWT tokens as part of the authentication response—without relying on external identity providers.
 * <p>
 * This configuration enables JWT-based authentication and provides various customization options
 * for token generation, validation, and security context management.
 */
@Configuration
@ConditionalOnClass(Jwt.class)
@ConditionalOnProperty(name = "spring.web.security.jwt.authentication.enabled", havingValue = "true")
public class JwtAuthenticationConfiguration implements WebMvcConfigurer {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(value = {
        AuthenticationManager.class,
        AuthenticationProvider.class,
        UserDetailsService.class,
        AuthenticationManagerResolver.class
    })
    @ConditionalOnProperty(name = "spring.web.security.jwt.authentication.enabled", havingValue = "true")
    public static class InMemoryUserDetailsConfiguration {

        private static final String NOOP_PASSWORD_PREFIX = "{noop}";

        private static final Pattern PASSWORD_ALGORITHM_PATTERN = Pattern.compile("^\\{.+}.*$");

        private static final Log logger = LogFactory.getLog(InMemoryUserDetailsConfiguration.class);

        @Bean
        @ConditionalOnProperty("spring.security.user.default-tenant")
        public InMemoryMultiTenantJwtUserDetailsManager inMemoryMultiTenantJwtUserDetailsManager(
            @Value("${spring.security.user.default-tenant}") String defaultTenant,
            SecurityProperties properties,
            ObjectProvider<PasswordEncoder> passwordEncoder
        ) {
            SecurityProperties.User user = properties.getUser();
            List<String> roles = user.getRoles();

            UserDetails userDetails = User.withUsername(user.getName())
                .password(getOrDeducePassword(user, passwordEncoder.getIfAvailable()))
                .roles(org.springframework.util.StringUtils.toStringArray(roles))
                .build();

            return new InMemoryMultiTenantJwtUserDetailsManager(defaultTenant, userDetails);
        }

        /**
         * We need to redefine this {@link UserDetailsService} because if we enable authentication along
         * with authorization, the default object defined by Spring Boot will not be created, as there will
         * already be an object of type {@link NimbusJwtDecoder} in the classpath.
         *
         * @see org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
         */
        @Bean
        @ConditionalOnExpression("'${spring.security.user.default-tenant:}'.isEmpty()")
        public InMemoryUserDetailsManager inMemoryUserDetailsManager(
            SecurityProperties properties,
            ObjectProvider<PasswordEncoder> passwordEncoder
        ) {
            SecurityProperties.User user = properties.getUser();
            List<String> roles = user.getRoles();
            return new InMemoryUserDetailsManager(User.withUsername(user.getName())
                .password(getOrDeducePassword(user, passwordEncoder.getIfAvailable()))
                .roles(org.springframework.util.StringUtils.toStringArray(roles))
                .build());
        }

        private String getOrDeducePassword(SecurityProperties.User user, PasswordEncoder encoder) {
            String password = user.getPassword();
            if (user.isPasswordGenerated()) {
                logger.warn(String.format(
                    "%n%nUsing generated security password: %s%n%nThis generated password is for development use only. "
                        + "Your security configuration must be updated before running your application in "
                        + "production.%n",
                    user.getPassword()));
            }

            if (encoder != null || PASSWORD_ALGORITHM_PATTERN.matcher(password).matches()) {
                return password;
            }
            return NOOP_PASSWORD_PREFIX + password;
        }
    }

    /**
     * Configuration class for HTTP clients security.
     * <p>
     * This class decorates HTTP clients (RestTemplate and RestClient) with authentication headers.
     * It automatically adds JWT authentication headers to outgoing requests when the current
     * security context contains an authenticated user with a JWT token.
     * <p>
     * This configuration is conditionally enabled when JWT authentication is enabled, and
     * HTTP clients decoration is enabled in the application properties.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(
        name = {
            "spring.web.security.jwt.authentication.enabled",
            "spring.web.http.clients.enabled",
            "spring.web.http.clients.decorate-with-jwt-token",
        },
        havingValue = "true"
    )
    public static class HttpClientsSecurityConfiguration {

        /**
         * This constructor initializes the configuration and decorates the default HTTP clients
         * with authentication headers.
         * <p>
         * It adds an interceptor to the HTTP clients that automatically adds JWT authentication
         * headers to outgoing requests.
         *
         * @param defaultRestTemplate               The default RestTemplate bean
         * @param defaultRestClient                 The default RestClient bean
         * @param beanFactory                       The bean factory for managing beans
         * @param jwtAuthenticationHeaderConfigurer The configurer for JWT authentication headers
         */
        @Autowired
        public HttpClientsSecurityConfiguration(
            @Qualifier("defaultRestTemplate") RestTemplate defaultRestTemplate,
            @Qualifier("defaultRestClient") RestClient defaultRestClient,
            ConfigurableListableBeanFactory beanFactory,
            JwtAuthenticationHeaderConfigurer jwtAuthenticationHeaderConfigurer
        ) {
            ClientHttpRequestInterceptor authHeaderClientHttpRequestInterceptor =
                (request, body, execution) -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication == null) {
                        return execution.execute(request, body);
                    }

                    if (!(authentication.getCredentials() instanceof AbstractOAuth2Token token)) {
                        return execution.execute(request, body);
                    }

                    jwtAuthenticationHeaderConfigurer.configureAuthorizationHeader(
                        request,
                        jwtAuthenticationHeaderConfigurer.configureBearerToken(token.getTokenValue())
                    );
                    return execution.execute(request, body);
                };

            defaultRestTemplate.getInterceptors().add(authHeaderClientHttpRequestInterceptor);
            if (defaultRestClient != null) {
                var decoratedDefaultRestClient = defaultRestClient.mutate().requestInterceptor(
                    authHeaderClientHttpRequestInterceptor
                ).build();

                ((DefaultListableBeanFactory) beanFactory).destroySingleton("defaultRestClient");
                beanFactory.autowireBean(decoratedDefaultRestClient);
            }
        }
    }

    @Value("${spring.web.security.jwt.authentication.base-path-login:/authenticate}")
    private String jwtAuthenticationBasePathLogin;

    @Value("${spring.web.security.jwt.authentication.authorities.parameter:authorities}")
    private String jwtAuthenticationAuthoritiesParameter;

    @Value("${spring.web.security.jwt.authentication.delegation.base-path-url:}")
    private String jwtAuthenticationDelegateBasePathUrl;

    @Value("${spring.web.security.jwt.authentication.delegation.base-path-login:/authenticate}")
    private String jwtAuthenticationDelegateBasePathLogin;

    private CorsProperties webSecurityCorsProperties;

    @Autowired(required = false)
    public void setWebSecurityCorsProperties(
        @Lazy @Qualifier("webSecurityCorsProperties") CorsProperties webSecurityCorsProperties
    ) {
        this.webSecurityCorsProperties = webSecurityCorsProperties;
    }

    @Validated
    @Bean("webSecurityCorsProperties")
    @ConfigurationProperties("spring.web.security.jwt.authentication.cors")
    public CorsProperties webSecurityCorsProperties() {
        return new CorsProperties();
    }

    @Bean
    @ConfigurationProperties("spring.web.security.jwt.authentication.keys")
    public JwtKeys jwtKeys() {
        return new JwtKeys();
    }

    /**
     * Creates and configures the JWT service for handling token operations.
     * This service is responsible for generating, validating, and processing JWT tokens.
     * It supports different encryption methods and configurable expiration periods.
     *
     * @param jwtKeys                            The JWT keys for signing and verification
     * @param jwtAuthenticationConverter         The converter for JWT authentication
     * @param jwtEncryptionMethod                The encryption method to use (default: asymmetric)
     * @param jwtExpirationPeriod                The token expiration period in days (default: 1)
     * @param jwtAuthenticationDelegationEnabled Whether JWT authentication delegation is enabled
     */
    @Bean
    @ConditionalOnMissingBean(JwtService.class)
    public JwtService jwtService(
        @Value("${spring.web.security.jwt.authentication.issuer:https://springbloom.dev}")
        String jwtIssuer,

        @Value("${spring.web.security.jwt.authentication.audience:}")
        String jwtAudience,

        @Autowired
        JwtKeys jwtKeys,

        @Autowired(required = false)
        JwtAuthenticationConverter jwtAuthenticationConverter,

        @Autowired(required = false)
        MultiTenantJwtAuthenticationConverter multiTenantJwtAuthenticationConverter,

        @Value("${spring.web.security.jwt.authentication.encryption-method:asymmetric}")
        JwtEncryptionMethod jwtEncryptionMethod,

        @Value("${spring.web.security.jwt.authentication.expiration-period:1}")
        int jwtExpirationPeriod,

        @Value("${spring.web.security.jwt.authentication.delegation.enabled:false}")
        boolean jwtAuthenticationDelegationEnabled
    ) {
        return new JwtService(
            jwtIssuer,
            jwtAudience,
            jwtKeys,
            jwtAuthenticationConverter,
            multiTenantJwtAuthenticationConverter,
            jwtEncryptionMethod,
            jwtExpirationPeriod,
            jwtAuthenticationDelegationEnabled
        );
    }

    /**
     * Creates a configurer for JWT authentication claims.
     * <p>
     * This configurer is responsible for setting up the claims in the JWT token,
     * including the subject (username), authorities, and other information if available.
     */
    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationClaimsConfigurer.class)
    public JwtAuthenticationClaimsConfigurer jwtAuthenticationClaimsConfigurer() {
        return (request, authentication) -> jwtClaimsSetBuilder -> {
            jwtClaimsSetBuilder.subject(((UserDetails) authentication.getPrincipal()).getUsername())
                .claim(jwtAuthenticationAuthoritiesParameter,
                    authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).collect(Collectors.toList())
                );

            // User (Principal) must have support to Multi-Tenant.
            if (authentication.getPrincipal() instanceof MultiTenantAware multiTenantAware) {
                jwtClaimsSetBuilder.claim(MultiTenantAware.TENANT, multiTenantAware.getTenant());
            }

            return jwtClaimsSetBuilder.build();
        };
    }

    /**
     * This converter is responsible for converting the JWT token into an Authentication object.
     */
    @Bean
    @ConditionalOnMissingBean(AuthenticationConverter.class)
    public JwtAuthenticationExtractor jwtAuthenticationExtractor() {
        return new JwtAuthenticationExtractor();
    }

    /**
     * This configurer is responsible for setting up the headers in the JWT token.
     */
    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationHeaderConfigurer.class)
    public JwtAuthenticationHeaderConfigurer jwtAuthenticationHeaderConfigurer() {
        return new JwtAuthenticationHeaderBearerTokenConfigurer();
    }

    /**
     * This provider is responsible for delegating authentication to another service.
     */
    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationDelegateProvider.class)
    @ConditionalOnProperty(name = "spring.web.security.jwt.authentication.delegation.enabled", havingValue = "true")
    public JwtAuthenticationDelegateProvider jwtAuthenticationDelegateProvider() {
        if (StringUtils.isBlank(jwtAuthenticationDelegateBasePathUrl)) {
            throw new IllegalStateException("Authentication delegate base path should be defined!");
        }

        return new JwtAuthenticationDelegateProvider(
            RestClient.builder().baseUrl(jwtAuthenticationDelegateBasePathUrl).build(),
            jwtAuthenticationDelegateBasePathLogin
        );
    }

    /**
     * Creates a custom method security expression handler.
     * This handler is used for evaluating security expressions in method annotations.
     * It supports custom expressions for checking administrator privileges.
     */
    @Bean
    static public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
        @Value("${spring.web.security.jwt.authentication.authorities.parameter-administrator:administrator}")
        String administratorAuthority
    ) {
        return new CustomMethodSecurityExpressionHandler(administratorAuthority);
    }

    /**
     * Set up Cross-Origin Resource Sharing (CORS) for JWT authentication endpoints.
     * <p>
     * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-cors-global">
     * Global CORS configuration
     * </a>
     */
    @Override
    public void addCorsMappings(final @NonNull CorsRegistry registry) {
        webSecurityCorsProperties.addCorsMappings(
            registry,
            UrlUtils.appendDoubleAsterisk(jwtAuthenticationBasePathLogin)
        );
    }
}
