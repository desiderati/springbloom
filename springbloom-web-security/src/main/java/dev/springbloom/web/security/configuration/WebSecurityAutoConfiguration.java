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
import dev.springbloom.web.UrlUtils;
import dev.springbloom.web.configuration.WebAutoConfiguration;
import dev.springbloom.web.security.*;
import dev.springbloom.web.security.auth.jwt.JwtAuthenticationFilter;
import dev.springbloom.web.security.auth.jwt.multitenant.MultiTenantJwtAuthenticationConverter;
import dev.springbloom.web.security.auth.jwt.multitenant.MultiTenantJwtContextHolderFilter;
import dev.springbloom.web.security.auth.sign.SignRequestAuthorizationFilter;
import dev.springbloom.web.security.graphql.UserDataGraphQLArgumentResolver;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authorization.method.AuthorizationManagerAfterMethodInterceptor;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.web.method.annotation.CurrentSecurityContextArgumentResolver;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static dev.springbloom.data.multitenant.context.MultiTenantContextHolder.MODE_INHERITABLE_THREAD_LOCAL;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.core.context.SecurityContextHolder.MODE_INHERITABLETHREADLOCAL;

/**
 * This class provides comprehensive security configuration for web applications,
 * supporting multiple authentication mechanisms and security features.
 * <p>
 * It configures authentication methods (form-based, HTTP basic, JWT), authorization,
 * CSRF protection, CORS, session management, and more.
 * <p>
 * The configuration is highly customizable through application properties and
 * conditional beans.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@AutoConfigureBefore(GraphQlAutoConfiguration.class)
@PropertySource("classpath:application-springbloom-web-security.properties")
@ComponentScan(basePackages = "dev.springbloom.web.security",
    // Do not add the auto-configured classes, otherwise the auto-configuration will not work as expected.
    excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
)
@Import({
    WebAutoConfiguration.class,
    JwtAuthenticationConfiguration.class,
    OAuth2AuthenticationConfiguration.class,
    SignRequestAuthorizationFilter.class,
}) // To be used with @WebMvcTest
public class WebSecurityAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @AutoConfigureBefore(GraphQlAutoConfiguration.class)
    public static class CustomArgumentResolversConfiguration {

        /**
         * Creates a service for retrieving the authenticated username.
         * This bean provides a standardized way to access the current user's username
         * across different parts of the application.
         */
        @Bean
        @ConditionalOnMissingBean(AuthenticatedUsernameGetter.class)
        public AuthenticatedUsernameGetter authenticatedUsernameGetter() {
            return new AuthenticatedUsernameGetterImpl();
        }

        /**
         * Creates a REST controller argument resolver for user data objects.
         * <p>
         * This resolver enables REST controllers to automatically inject complete user data
         * objects into method parameters annotated with the appropriate annotation.
         * Similar to the GraphQL version, but for standard Spring MVC controllers.
         * <p>
         * Only created when a UserDataRetriever implementation is available.
         *
         * @param authenticatedUsernameGetter The service for retrieving authenticated usernames
         * @param userDataRetriever           The service for retrieving user data by username
         */
        @Bean
        @ConditionalOnBean(UserDataRetriever.class)
        public UserDataArgumentResolver userDataArgumentResolver(
            AuthenticatedUsernameGetter authenticatedUsernameGetter,
            UserDataRetriever<? extends UserData> userDataRetriever
        ) {
            return new UserDataArgumentResolver(authenticatedUsernameGetter, userDataRetriever);
        }

        /**
         * Creates a GraphQL argument resolver for user data objects.
         * <p>
         * This resolver enables GraphQL operations to automatically inject complete user data
         * objects into method parameters annotated with the appropriate annotation.
         * It uses the authenticated username to retrieve the corresponding user data.
         * <p>
         * Only created when a UserDataRetriever implementation is available.
         *
         * @param authenticatedUsernameGetter The service for retrieving authenticated usernames
         * @param userDataRetriever           The service for retrieving user data by username
         */
        @Bean
        @ConditionalOnBean(UserDataRetriever.class)
        public UserDataGraphQLArgumentResolver userDataGraphQLArgumentResolver(
            AuthenticatedUsernameGetter authenticatedUsernameGetter,
            UserDataRetriever<? extends UserData> userDataRetriever
        ) {
            return new UserDataGraphQLArgumentResolver(authenticatedUsernameGetter, userDataRetriever);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(JwtAuthenticationConverter.class)
    @ConditionalOnExpression(
        """
        !'${spring.datasource.multitenant.type}'.equalsIgnoreCase('NONE') and (
            ${spring.web.security.jwt.authentication.enabled:false}
                or ${spring.web.security.jwt.authorization.enabled:false}
        )
        """
    )
    public static class MultiTenantJwtConfiguration {

        @Bean
        public MultiTenantJwtAuthenticationConverter multiTenantJwtAuthenticationConverter(
            JwtAuthenticationConverter jwtAuthenticationConverter
        ) {
            return new MultiTenantJwtAuthenticationConverter(jwtAuthenticationConverter);
        }
    }

    @Value("${spring.web.security.form-based.authentication.enabled:false}")
    private boolean formBasedAuthenticationEnabled;

    @Value("${spring.web.security.http-basic.authorization.enabled:false}")
    private boolean httpBasicAuthorizationEnabled;

    @Value("${spring.web.security.jwt.authentication.enabled:false}")
    private boolean jwtAuthenticationEnabled;

    @Value("${spring.web.security.jwt.authentication.base-path-login:/authenticate}")
    private String jwtAuthenticationBasePathLogin;

    @Value("${spring.web.security.jwt.authorization.enabled:false}")
    private boolean jwtAuthorizationEnabled;

    @Value("${spring.web.security.sign-request.authorization.enabled:false}")
    private boolean signRequestAuthorizationEnabled;

    @Value("${spring.graphql.security.enabled:true}")
    private boolean springGraphqlSecurityEnabled;

    @Value("${spring.graphql.path:/graphql}")
    private String springGraphqlPath;

    @Value("${spring.graphql.graphiql.enabled:false}")
    private boolean graphqlGraphiqlEnabled;

    @Value("${spring.graphql.altair.enabled:false}")
    private boolean graphqlAltairEnabled;

    @Value("${spring.graphql.voyager.enabled:false}")
    private boolean graphqlVoyagerEnabled;

    @Value("${spring.web.atmosphere.security.enabled:true}")
    private boolean atmosphereSecurityEnabled;

    @Value("${spring.web.atmosphere.url.mapping:/atm}")
    private String atmosphereUrlMapping;

    @Value("${springdoc.api-docs.path:/api-docs}")
    private String springDocOpenApiPath;

    /**
     * This constructor initializes the security configuration and sets up the security context
     * holder strategy based on the application properties.
     * <p>
     * If async delegate security context is enabled, it configures the security context to be
     * inherited by child threads and reconfigures the method security interceptors.
     *
     * @param springMvcAsyncDelegateSecurityContext Whether to delegate security context to async tasks
     * @param preAuthorizeInterceptors              Collection of pre-authorize interceptors
     * @param postAuthorizeInterceptors             Collection of post-authorize interceptors
     */
    public WebSecurityAutoConfiguration(
        @Value("${spring.mvc.async.delegate-security-context:true}")
        boolean springMvcAsyncDelegateSecurityContext,

        RequestMappingHandlerAdapter requestMappingHandlerAdapter,
        Collection<AuthorizationManagerBeforeMethodInterceptor> preAuthorizeInterceptors,
        Collection<AuthorizationManagerAfterMethodInterceptor> postAuthorizeInterceptors
    ) {
        if (springMvcAsyncDelegateSecurityContext) {
            SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL);
            MultiTenantContextHolder.setStrategyName(MODE_INHERITABLE_THREAD_LOCAL);

            // As we have defined a new SecurityContextHolderStrategy, we must reconfigure the argument resolvers.
            if (requestMappingHandlerAdapter.getArgumentResolvers() != null) {
                requestMappingHandlerAdapter.getArgumentResolvers().forEach(argumentResolver -> {
                    if (argumentResolver instanceof AuthenticationPrincipalArgumentResolver authenticationPrincipalArgumentResolver) {
                        authenticationPrincipalArgumentResolver.setSecurityContextHolderStrategy(
                            SecurityContextHolder.getContextHolderStrategy()
                        );
                    }

                    if (argumentResolver instanceof CurrentSecurityContextArgumentResolver currentSecurityContextArgumentResolver) {
                        currentSecurityContextArgumentResolver.setSecurityContextHolderStrategy(
                            SecurityContextHolder.getContextHolderStrategy()
                        );
                    }
                });
            }

            // As we have defined a new SecurityContextHolderStrategy, we must reconfigure
            // the method security interceptors and the argument resolvers.
            preAuthorizeInterceptors.forEach(it ->
                it.setSecurityContextHolderStrategy(SecurityContextHolder.getContextHolderStrategy())
            );
            postAuthorizeInterceptors.forEach(it ->
                it.setSecurityContextHolderStrategy(SecurityContextHolder.getContextHolderStrategy()
                )
            );
        }
    }

    private String defaultApiBasePath;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private SignRequestAuthorizationFilter signRequestAuthorizationFilter;
    private CorsFilter graphQLCorsFilter;

    @Autowired
    public void setDefaultApiBasePath(String defaultApiBasePath) {
        this.defaultApiBasePath = defaultApiBasePath;
    }

    @Autowired(required = false)
    public void setJwtAuthenticationFilter(@Lazy JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Autowired(required = false)
    public void setSignRequestAuthorizationFilter(
        @Lazy SignRequestAuthorizationFilter signRequestAuthorizationFilter
    ) {
        this.signRequestAuthorizationFilter = signRequestAuthorizationFilter;
    }

    @Autowired(required = false)
    public void setGraphQLCorsFilter(@Qualifier("corsConfigurer") CorsFilter graphQLCorsFilter) {
        this.graphQLCorsFilter = graphQLCorsFilter;
    }

    /**
     * Configures security-aware argument resolvers for controller methods.
     * When async security context delegation is enabled, this method ensures that
     * security-related argument resolvers use the correct security context strategy.
     * This is crucial for methods using @AuthenticationPrincipal or @CurrentSecurityContext
     * annotations in async execution environments.
     *
     * @param springMvcAsyncDelegateSecurityContext Whether to delegate security context to async tasks
     * @param argumentResolvers                     The list of method argument resolvers to configure
     */
    @Autowired
    public void configureArgumentResolvers(
        @Value("${spring.mvc.async.delegate-security-context:true}")
        boolean springMvcAsyncDelegateSecurityContext,

        List<HandlerMethodArgumentResolver> argumentResolvers
    ) {
        if (springMvcAsyncDelegateSecurityContext) {
            argumentResolvers.forEach(
                it -> {
                    if (it instanceof AuthenticationPrincipalArgumentResolver) {
                        ((AuthenticationPrincipalArgumentResolver) it).setSecurityContextHolderStrategy(
                            SecurityContextHolder.getContextHolderStrategy()
                        );
                    } else if (it instanceof CurrentSecurityContextArgumentResolver) {
                        ((CurrentSecurityContextArgumentResolver) it).setSecurityContextHolderStrategy(
                            SecurityContextHolder.getContextHolderStrategy()
                        );
                    }
                }
            );
        }
    }

    /**
     * Configures the application's security filter chain.
     * <p>
     * This method establishes a comprehensive set of security rules, including:
     * <ul>
     *   <li>CSRF protection (enabled or disabled based on the selected authentication method)</li>
     *   <li>CORS support</li>
     *   <li>Session management (stateless or conditional based on authentication strategy)</li>
     *   <li>Support for multiple authentication methods (JWT, form-based login, HTTP Basic, OAuth2)</li>
     *   <li>Authorization rules for protected resources and endpoints</li>
     *   <li>Configuration of public endpoints (e.g., Actuator, Swagger UI, GraphQL tools)</li>
     * </ul>
     * <p>
     * The entire configuration is designed to be flexible and customizable via application properties.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity httpSecurity,
        ObjectProvider<OAuth2ClientProperties> oAuth2ClientProperties,
        ObjectProvider<MultiTenantJwtAuthenticationConverter> multiTenantJwtAuthenticationConverterProvider
    ) throws Exception {
        // We don't need to enable CSRF support when using JWT Tokens.
        // And also because with it enabled, we will not be able to call our back-end from the front-end.
        final boolean oauth2AuthenticationEnabled = oAuth2ClientProperties.getIfAvailable() != null &&
            !oAuth2ClientProperties.getIfAvailable().getRegistration().isEmpty();

        if (!isValidConfiguration(oauth2AuthenticationEnabled)) {
            throw new IllegalSecurityConfigurationException(
                """
                    Error with your security configuration!
                    Possible configurations for an application are:
                        * Form Based Authentication
                        * Form Based Authentication + HTTP Basic Authorization
                        * Form Based Authentication + OAuth2 Login Authentication
                        * HTTP Basic Authorization
                        * OAuth2 Login Authentication
                        * Jwt (Self Contained) Authentication
                        * Jwt (Self Contained) Authentication + Jwt Authorization (Using OAuth2 Resource Server)
                        * Jwt Authorization (Using OAuth2 Resource Server)
                        * Sign Request Authorization
                    """
            );
        }

        if (jwtAuthorizationEnabled || (!formBasedAuthenticationEnabled && !oauth2AuthenticationEnabled)) {
            httpSecurity.csrf(AbstractHttpConfigurer::disable);
        } else {
            httpSecurity.csrf(csrf -> {
                if (jwtAuthenticationEnabled) {
                    csrf.ignoringRequestMatchers(jwtAuthenticationBasePathLogin);
                }

                // Since Atmosphere endpoints are typically accessed via WebSockets or long polling,
                // they should be protected using token-based or header-based authentication.
                // CSRF protection is unnecessary in this case, as it's mainly designed for form-based interactions.
                csrf.ignoringRequestMatchers(UrlUtils.appendDoubleAsterisk(atmosphereUrlMapping));

                // I couldn't make it work with XORed CSRF Tokens.
                CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
                csrf.csrfTokenRequestHandler(csrfHandler);

                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
            });
        }

        // If the GraphQL CORS filter is enabled, it must be registered before the authentication filters!
        if (graphQLCorsFilter != null) {
            httpSecurity.addFilter(graphQLCorsFilter);
        }

        // We have to enable Cross-Origin Resource Sharing.
        httpSecurity.cors(withDefaults());

        if (formBasedAuthenticationEnabled) {
            // For some reason, when we enable both FormLogin and HttpBasic authentication,
            // the DefaultAuthenticationEntryPoint configurations are not set correctly.
            // As a result, the API requests that are not authenticated are being redirected to the login page
            // instead of returning a 401 Unauthorized response.
            //
            // API requests should be handled by HttpStatusEntryPoint, and by default,
            // they're being handled by LoginUrlAuthenticationEntryPoint.
            httpSecurity.exceptionHandling(
                exceptionHandling -> {
                    var contentNegotiationStrategy = new HeaderContentNegotiationStrategy();
                    MediaTypeRequestMatcher restMatcher = getRestMatcher(contentNegotiationStrategy);
                    RequestMatcher notHtmlMatcher = getNotHtmlMatcher(contentNegotiationStrategy);
                    RequestMatcher restNotHtmlMatcher =
                        new AndRequestMatcher(Arrays.asList(notHtmlMatcher, restMatcher));

                    exceptionHandling.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        restNotHtmlMatcher
                    );
                }
            );
        }

        // We do not wish to enable session. Only if default or oauth authentication is enabled.
        httpSecurity.sessionManagement(sessionManagement ->
            sessionManagement.sessionCreationPolicy(
                formBasedAuthenticationEnabled || oauth2AuthenticationEnabled ?
                    SessionCreationPolicy.IF_REQUIRED :
                    SessionCreationPolicy.STATELESS
            )
        );

        // Disables page caching.
        httpSecurity.headers(headers -> headers.cacheControl(withDefaults()));

        // Configures the endpoints.
        httpSecurity.authorizeHttpRequests(authorizeHttpRequests -> {
            if (!formBasedAuthenticationEnabled
                && !httpBasicAuthorizationEnabled
                && !jwtAuthenticationEnabled
                && !jwtAuthorizationEnabled
                && !signRequestAuthorizationEnabled
                && !oauth2AuthenticationEnabled
            ) {
                // If none configured, it uses the default behavior.
                authorizeHttpRequests.anyRequest().permitAll();

            } else {
                // Default public endpoints. Security should not be enabled for these!

                // Default error page.
                authorizeHttpRequests
                    .requestMatchers(HttpMethod.GET, "/error")
                    .permitAll();

                // We enable all Actuator RESTs.
                authorizeHttpRequests
                    .requestMatchers(HttpMethod.GET, "/actuator/**")
                    .permitAll();

                // We enable all Open API (formally Swagger API) RESTs.
                authorizeHttpRequests.requestMatchers(HttpMethod.GET, "/swagger-resources/**").permitAll();
                authorizeHttpRequests.requestMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll();
                authorizeHttpRequests.requestMatchers(HttpMethod.GET, "/swagger-ui.html").permitAll();
                authorizeHttpRequests.requestMatchers(HttpMethod.GET, "/webjars/**").permitAll();
                authorizeHttpRequests.requestMatchers(HttpMethod.GET, springDocOpenApiPath).permitAll();
                authorizeHttpRequests.requestMatchers(
                    HttpMethod.GET, UrlUtils.appendDoubleAsterisk(springDocOpenApiPath)
                ).permitAll();

                // It enables all calls to the public API.
                authorizeHttpRequests.requestMatchers(defaultApiBasePath + "/public/**").permitAll();

                // Notification (Atmosphere) Support.
                if (!atmosphereSecurityEnabled) {
                    // It should be used only on development environments.
                    authorizeHttpRequests.requestMatchers(UrlUtils.sanitize(atmosphereUrlMapping)).permitAll();
                    authorizeHttpRequests.requestMatchers(
                        UrlUtils.appendDoubleAsterisk(atmosphereUrlMapping)
                    ).permitAll();
                }

                // GraphQL Support.
                if (!springGraphqlSecurityEnabled) {
                    // It should be used only on development environments.
                    authorizeHttpRequests.requestMatchers(UrlUtils.sanitize(springGraphqlPath)).permitAll();
                    authorizeHttpRequests.requestMatchers(
                        UrlUtils.appendDoubleAsterisk(springGraphqlPath)
                    ).permitAll();
                }

                if (graphqlGraphiqlEnabled) {
                    // It should be used only on development environments.
                    // The URL mapping is not customized!
                    authorizeHttpRequests.requestMatchers("/graphiql").permitAll();
                    authorizeHttpRequests.requestMatchers("/graphiql/**").permitAll();
                }

                if (graphqlVoyagerEnabled) {
                    // It should be used only on development environments.
                    // The URL mapping is not customized!
                    authorizeHttpRequests.requestMatchers("/voyager").permitAll();
                    authorizeHttpRequests.requestMatchers("/voyager/**").permitAll();
                    authorizeHttpRequests.requestMatchers("/vendor/voyager").permitAll();
                    authorizeHttpRequests.requestMatchers("/vendor/voyager/**").permitAll();
                }

                if (graphqlAltairEnabled) {
                    // It should be used only on development environments.
                    // The URL mapping is not customized!
                    authorizeHttpRequests.requestMatchers("/altair").permitAll();
                    authorizeHttpRequests.requestMatchers("/altair/**").permitAll();
                    authorizeHttpRequests.requestMatchers("/vendor/altair").permitAll();
                    authorizeHttpRequests.requestMatchers("/vendor/altair/**").permitAll();
                }

                if (oauth2AuthenticationEnabled) {
                    // We enable default OAuth2 paths.
                    authorizeHttpRequests
                        // TODO Felipe Desiderati: Permitir que seja customizado!
                        .requestMatchers(HttpMethod.GET, defaultApiBasePath + "/oauth2/authorization/**")
                        .permitAll();
                }

                if (jwtAuthenticationEnabled) {
                    authorizeHttpRequests.requestMatchers("/.well-known/jwks.json").permitAll();

                    // Login URL for JWT Authentication.
                    // When using form-based authentication, Spring Security defines a default login URL
                    // unless explicitly overridden.
                    authorizeHttpRequests =
                        authorizeHttpRequests.requestMatchers(HttpMethod.POST, jwtAuthenticationBasePathLogin).permitAll();
                    httpSecurity.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                }

                if (signRequestAuthorizationEnabled) {
                    httpSecurity.addFilterBefore(
                        signRequestAuthorizationFilter, UsernamePasswordAuthenticationFilter.class
                    );
                }

                authorizeHttpRequests.anyRequest().authenticated();
            }
        });

        if (oauth2AuthenticationEnabled) {
            boolean isAuthorizationCode = false;
            var clientRegistrations = oAuth2ClientProperties.getIfAvailable().getRegistration().entrySet();
            for (var entry : clientRegistrations) {
                if ("authorization_code".equalsIgnoreCase(entry.getValue().getAuthorizationGrantType())) {
                    isAuthorizationCode = true;
                    break;
                }
            }

            // We enable default OAuth2 paths.
            // TODO Felipe Desiderati: Validar se realmente precisamos colocar isso e permitir que seja customizado.
            if (isAuthorizationCode) {
                httpSecurity.oauth2Login(it ->
                    it.loginProcessingUrl("/login/oauth2/code/*").permitAll()
                );
            }
            httpSecurity.oauth2Client(withDefaults());
        }

        if (jwtAuthorizationEnabled) {
            MultiTenantJwtAuthenticationConverter multiTenantJwtAuthenticationConverter =
                multiTenantJwtAuthenticationConverterProvider.getIfAvailable();

            if (multiTenantJwtAuthenticationConverter != null) {
                httpSecurity.oauth2ResourceServer(it -> it.jwt(
                    jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(
                        multiTenantJwtAuthenticationConverter
                    )
                ));

                httpSecurity.addFilterAfter(
                    new MultiTenantJwtContextHolderFilter(), BearerTokenAuthenticationFilter.class
                );
            } else {
                httpSecurity.oauth2ResourceServer(it -> it.jwt(withDefaults()));
            }
        }

        if (formBasedAuthenticationEnabled) {
            // It will enable: UsernamePasswordAuthenticationFilter
            httpSecurity.formLogin(withDefaults());
        }

        if (httpBasicAuthorizationEnabled) {
            httpSecurity.httpBasic(httpBasic ->
                httpBasic.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );
        }

        return httpSecurity.build();
    }

    private boolean isValidConfiguration(boolean oauth2AuthenticationEnabled) {
        // Count how many authentication/authorization mechanisms are enabled.
        int enabledCount = 0;
        if (oauth2AuthenticationEnabled) enabledCount++;
        if (formBasedAuthenticationEnabled) enabledCount++;
        if (httpBasicAuthorizationEnabled) enabledCount++;
        if (jwtAuthenticationEnabled) enabledCount++;
        if (jwtAuthorizationEnabled) enabledCount++;
        if (signRequestAuthorizationEnabled) enabledCount++;

        if (enabledCount == 0) {
            // If no mechanisms are enabled, it's a valid configuration (no authentication configured).
            return true;

        } else if (enabledCount == 1) {
            // Check for valid combinations (exactly 1 or 2 mechanisms enabled in specific combinations)
            // Single mechanism configurations.
            return true;

        } else if (enabledCount == 2) {
            // Valid two-mechanism combinations.
            return (formBasedAuthenticationEnabled && httpBasicAuthorizationEnabled)
                || (formBasedAuthenticationEnabled && oauth2AuthenticationEnabled)
                || (jwtAuthenticationEnabled && jwtAuthorizationEnabled);

        } else {
            // More than 2 mechanisms enabled is always invalid!
            return true;
        }
    }

    /**
     * Creates a request matcher that matches requests with non-HTML content types.
     * This matcher is used to differentiate between browser requests (HTML) and API requests.
     * Unlike Spring's default configuration, this uses exact media type matching for more precise control.
     */
    private @NotNull RequestMatcher getNotHtmlMatcher(HeaderContentNegotiationStrategy contentNegotiationStrategy) {
        MediaTypeRequestMatcher htmlMatcher =
            new MediaTypeRequestMatcher(contentNegotiationStrategy, MediaType.TEXT_HTML);
        htmlMatcher.setUseEquals(true); // Here is the difference with Spring's default config.
        return new NegatedRequestMatcher(htmlMatcher);
    }

    /**
     * Creates a request matcher for REST API requests.
     * This matcher identifies requests with common API content types like JSON, XML, etc.
     * It's used to apply the appropriate security handling for API requests vs. browser requests.
     * The matcher ignores the wildcard media type to prevent false positives.
     */
    private @NotNull MediaTypeRequestMatcher getRestMatcher(
        HeaderContentNegotiationStrategy contentNegotiationStrategy
    ) {
        MediaTypeRequestMatcher restMatcher =
            new MediaTypeRequestMatcher(
                contentNegotiationStrategy,
                MediaType.APPLICATION_ATOM_XML,
                MediaType.APPLICATION_FORM_URLENCODED,
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_OCTET_STREAM,
                MediaType.APPLICATION_XML,
                MediaType.MULTIPART_FORM_DATA,
                MediaType.TEXT_XML
            );
        restMatcher.setIgnoredMediaTypes(Collections.singleton(MediaType.ALL));
        return restMatcher;
    }

    /**
     * Configures global web security settings.
     * <p>
     * This method is intended for settings that affect the overall web security configuration, such as:
     * <ul>
     *   <li>Excluding specific resources from security filters</li>
     *   <li>Enabling or disabling debug mode</li>
     *   <li>Registering custom HTTP firewall definitions</li>
     * </ul>
     * <p>
     * In this implementation, it sets up a custom HTTP firewall that allows URL-encoded slashes,
     * which can be required by applications that handle path parameters with encoded separators.
     *
     * @return the configured {@code WebSecurityCustomizer} bean
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
    }

    private HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    /**
     * Creates a security-aware async task executor.
     * This method creates an executor that propagates the security context to async tasks.
     * It wraps the application's task executor in a {@link DelegatingSecurityContextAsyncTaskExecutor},
     * which ensures that the security context is available in async tasks.
     * <p>
     * This is particularly useful for async operations that need access to the current user's
     * authentication information.
     *
     * @param taskExecutor The application's task executor
     * @return A security-aware async task executor
     */
    @Bean("delegatingSecurityContextAsyncTaskExecutor")
    @ConditionalOnProperty(name = "spring.mvc.async.delegate-security-context", havingValue = "true")
    @ConditionalOnClass({DispatcherServlet.class, DefaultAuthenticationEventPublisher.class})
    public Executor delegatingSecurityContextAsyncTaskExecutor(
        @Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME) AsyncTaskExecutor taskExecutor
    ) {
        return new DelegatingSecurityContextAsyncTaskExecutor(taskExecutor);
    }
}
