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
package dev.springbloom.web.configuration;

import dev.springbloom.data.configuration.JpaAutoConfiguration;
import dev.springbloom.web.UrlUtils;
import dev.springbloom.web.configuration.async.AsyncWebConfiguration;
import dev.springbloom.web.graphql.NameSchemaDirectiveWiring;
import dev.springbloom.web.rest.exception.ResponseExceptionDTOHttpMessageConverter;
import graphql.schema.GraphQLScalarType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Type;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.webmvc.api.MultipleOpenApiActuatorResource;
import org.springdoc.webmvc.api.MultipleOpenApiWebMvcResource;
import org.springdoc.webmvc.api.OpenApiActuatorResource;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springdoc.webmvc.ui.SwaggerConfigResource;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.*;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.graphql.data.method.HandlerMethodArgumentResolver;
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executor;

import static dev.springbloom.web.UrlUtils.URL_PATH_SEPARATOR;

/**
 * Auto-configuration class for web applications that provides common web functionality.
 * <p>
 * This class configures various aspects of a Spring Web MVC application:
 * <ul>
 *   <li>Sets up a common API base path for all REST controllers and repositories</li>
 *   <li>Configures Cross-Origin Resource Sharing (CORS) for API endpoints</li>
 *   <li>Customizes Spring Data REST repository configuration</li>
 *   <li>Configures GraphQL support with custom scalar types and directives</li>
 *   <li>Sets up exception handling for REST responses</li>
 * </ul>
 * <p>
 * The configuration is automatically applied to servlet-based web applications and
 * imports several other configurations for async processing, JPA integration,
 * OpenAPI documentation, and HTTP client support.
 *
 * @see AsyncWebConfiguration
 * @see JpaAutoConfiguration
 * @see JpaWebConfiguration
 * @see OpenApiConfiguration
 * @see HttpClientsConfiguration
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@AutoConfigureBefore(GraphQlAutoConfiguration.class)
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)
@EnableWebMvc
@PropertySource("classpath:application-springbloom-web.properties")
@ComponentScan({
    "dev.springbloom.web.graphql",
    "dev.springbloom.web.rest"
})
@Import({
    // Need to be auto-loaded too.
    AsyncWebConfiguration.class,
    JpaAutoConfiguration.class,
    JpaWebConfiguration.class,
    OpenApiConfiguration.class,
    HttpClientsConfiguration.class
})
public class WebAutoConfiguration implements WebMvcRegistrations, WebMvcConfigurer, RepositoryRestConfigurer {

    private final String apiBasePath;
    private final Validator validator;
    private final EntityManager entityManager;
    private final ListableBeanFactory beanFactory;
    private CorsProperties webCorsProperties;

    /**
     * @param apiBasePath   The base path for all REST API endpoints.
     *                      Defaults to "/api" if not specified.
     *                      This path will be sanitized to ensure proper URL formatting.
     * @param validator     A custom validator for validating request payloads.
     * @param entityManager The JPA EntityManager for accessing entity metadata, used for configuring
     *                      Spring Data REST repositories.
     */
    @Autowired
    public WebAutoConfiguration(
        @Value("${app.api-base-path:/api}") String apiBasePath,
        @Qualifier("customValidator") Validator validator,
        ObjectProvider<EntityManager> entityManager,
        ListableBeanFactory beanFactory
    ) {
        this.apiBasePath = UrlUtils.sanitize(apiBasePath);
        log.info("Configuring RESTful API base path as: {}", this.apiBasePath);

        this.validator = validator;
        this.entityManager = entityManager.getIfAvailable();
        this.beanFactory = beanFactory;
    }

    /**
     * Sets the CORS properties for web endpoints.
     * This method is autowired and uses lazy initialization to avoid circular dependencies.
     *
     * @param webCorsProperties The CORS configuration properties to be used for configuring
     *                          Cross-Origin Resource Sharing for web endpoints.
     */
    @Autowired
    public void setWebCorsProperties(
        @Lazy @Qualifier("webCorsProperties") CorsProperties webCorsProperties
    ) {
        this.webCorsProperties = webCorsProperties;
    }

    /**
     * Creates a bean for CORS configuration properties.
     * <p>
     * This bean is bound to the "spring.web.cors" configuration properties prefix
     * and is validated against any constraints defined in the CorsProperties class.
     *
     * @return A new instance of CorsProperties configured from application properties.
     */
    @Validated
    @Bean("webCorsProperties")
    @ConfigurationProperties("spring.web.cors")
    public CorsProperties webCorsProperties() {
        return new CorsProperties();
    }

    /**
     * Provides the configured API base path as a bean that can be injected into other components.
     * <p>
     * This base path is used to prefix all REST API endpoints in the application,
     * ensuring consistent URL structure across controllers and repositories.
     * The value is initialized from the "app.api-base-path" property with a default of "/api".
     *
     * @return The sanitized API base path string.
     */
    @Bean("defaultApiBasePath")
    public String defaultApiBasePath() {
        return apiBasePath;
    }

    /**
     * Provides a custom validator for Spring MVC to use when validating request payloads.
     * <p>
     * This method overrides the default validator provided by Spring MVC and uses a custom
     * validator injected into this configuration.
     * The custom validator can use a specific MessageSource for resolving validation messages
     * instead of relying on JSR-303's default "ValidationMessages.properties" bundle in the classpath.
     *
     * @return The custom validator instance.
     */
    @Override
    public Validator getValidator() {
        return validator;
    }

    /**
     * Configures Cross-Origin Resource Sharing (CORS) for the application.
     * <p>
     * This method applies CORS configuration to all API endpoints by using the
     * webCorsProperties bean, which is configured from application properties.
     * The configuration is applied to the API base path with a wildcard pattern
     * to match all endpoints under that path.
     * <p>
     * For more information on Spring's global CORS configuration, see:
     * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-cors-global">
     * Global CORS configuration
     * </a>
     *
     * @param registry The CorsRegistry to which CORS configuration is added.
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        webCorsProperties.addCorsMappings(registry, UrlUtils.appendDoubleAsterisk(apiBasePath));
    }

    /**
     * Provides a custom RequestMappingHandlerMapping that automatically prefixes all REST controller
     * endpoints with the configured API base path.
     * <p>
     * This method overrides the default Spring MVC RequestMappingHandlerMapping to ensure that
     * all methods in classes annotated with {@link RestController} have their request mappings
     * prefixed with the API base path.
     * <p>
     * This creates a consistent URL structure for all REST APIs in the application
     * without requiring developers to manually add the prefix to each mapping.
     * <p>
     * OpenAPI/Swagger controllers are excluded from this prefixing to ensure they remain accessible
     * at their standard paths.
     *
     * @return A custom RequestMappingHandlerMapping that prefixes REST controller paths.
     */
    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {

            /**
             * Overrides the standard handler method registration to add the API base path prefix
             * to all REST controller mappings.
             *
             * @param handler The handler object containing the method.
             * @param method The handler method to register.
             * @param mapping The request mapping information for the method.
             */
            @Override
            protected void registerHandlerMethod(
                @NonNull Object handler,
                @NonNull Method method,
                @NonNull RequestMappingInfo mapping
            ) {
                Class<?> beanType = method.getDeclaringClass();
                RestController restApiController = beanType.getAnnotation(RestController.class);
                if (restApiController != null && isNotOpenApiController(beanType)) {

                    String[] pathPatterns;
                    if (mapping.getPathPatternsCondition() != null) {
                        pathPatterns =
                            new PathPatternsRequestCondition(
                                PathPatternParser.defaultInstance,
                                apiBasePath
                            ).combine(mapping.getPathPatternsCondition()).getPatterns().stream()
                                .map(PathPattern::getPatternString)
                                .toArray(String[]::new);

                    } else if (mapping.getPatternsCondition() != null) {
                        pathPatterns =
                            new PatternsRequestCondition(apiBasePath)
                                .combine(mapping.getPatternsCondition())
                                .getPatterns()
                                .toArray(String[]::new);
                    } else {
                        throw new IllegalStateException(); // It should never happen!
                    }

                    mapping = mapping.mutate().paths(pathPatterns).build();
                }
                super.registerHandlerMethod(handler, method, mapping);
            }
        };
    }

    /**
     * Determines if a controller class is not an OpenAPI/Swagger controller.
     * <p>
     * This helper method is used to exclude OpenAPI and Swagger UI controllers from
     * having the API base path prefix applied to their request mappings, ensuring
     * that the OpenAPI documentation remains accessible at its standard paths.
     *
     * @param beanType The controller class to check.
     * @return {@code true} if the class is not an OpenAPI/Swagger controller, {@code false} otherwise.
     */
    private boolean isNotOpenApiController(Class<?> beanType) {
        return !SwaggerConfigResource.class.isAssignableFrom(beanType)
            && !OpenApiWebMvcResource.class.isAssignableFrom(beanType)
            && !OpenApiActuatorResource.class.isAssignableFrom(beanType)
            && !MultipleOpenApiWebMvcResource.class.isAssignableFrom(beanType)
            && !MultipleOpenApiActuatorResource.class.isAssignableFrom(beanType);
    }

    /**
     * Configures Spring Data REST repositories with consistent settings and ensures they are
     * prefixed with the API base path.
     * <p>
     * This method performs three main configurations:
     * <ol>
     *   <li>Exposes entity IDs in REST responses for all JPA entities</li>
     *   <li>Sets the repository detection strategy to only expose repositories explicitly
     *       annotated with @RepositoryRestResource</li>
     *   <li>Ensures all repository endpoints are prefixed with the API base path</li>
     * </ol>
     *
     * @param repositoryRestConfiguration The Spring Data REST configuration to modify.
     * @param corsRegistry                The CORS registry (not used in this implementation).
     */
    @Override
    public void configureRepositoryRestConfiguration(
        RepositoryRestConfiguration repositoryRestConfiguration,
        CorsRegistry corsRegistry
    ) {
        // Forces the Spring Data Rest to return the ID of the object being handled.
        if (entityManager != null) {
            repositoryRestConfiguration.exposeIdsFor(
                entityManager.getMetamodel().getEntities().stream()
                    .map(Type::getJavaType)
                    .toArray(Class[]::new));
        }

        // Only repositories annotated with @RepositoryRestResource are exposed, unless their exported flag
        // is set to false.
        repositoryRestConfiguration.setRepositoryDetectionStrategy(
            RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED
        );

        // Felipe Desiderati: Spring Data Rest should allow defining the complete path
        // on annotation @RepositoryRestResource.
        // See: https://stackoverflow.com/questions/30396953/how-to-customize-spring-data-rest-to-use-a-multi-segment-path-for-a-repository-r
        // Configures the Base Path. It can be redefined using property: spring.data.rest.base-path
        if (StringUtils.isBlank(repositoryRestConfiguration.getBasePath().getPath())) {
            repositoryRestConfiguration.setBasePath(apiBasePath + URL_PATH_SEPARATOR);
        }
    }

    /**
     * Creates a bean for converting exception responses to HTTP messages.
     * <p>
     * This converter is responsible for properly formatting exception responses
     * as JSON in the HTTP response body.
     * It uses the Jackson HTTP message converter to serialize exception DTOs to JSON.
     *
     * @param mappingJackson2HttpMessageConverter The Jackson converter used for JSON serialization.
     * @return A new ResponseExceptionDTOHttpMessageConverter instance.
     */
    @Bean
    public ResponseExceptionDTOHttpMessageConverter responseExceptionDTOMessageConverter(
        @Qualifier("jacksonHttpMessageConverter")
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter
    ) {
        return new ResponseExceptionDTOHttpMessageConverter(mappingJackson2HttpMessageConverter);
    }

    /**
     * Creates a bean for configuring GraphQL schema wiring.
     * <p>
     * This configurer is responsible for registering custom GraphQL scalar types and
     * schema directives with the GraphQL runtime.
     * <p>
     * It collects all GraphQLScalarType and NameSchemaDirectiveWiring beans from the application context
     * and registers them with the GraphQL schema.
     *
     * @param graphQLScalarTypes Provider for custom GraphQL scalar types.
     * @param customDirectives   Provider for custom GraphQL schema directives.
     * @return A RuntimeWiringConfigurer that registers scalars and directives.
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer(
        ObjectProvider<GraphQLScalarType> graphQLScalarTypes,
        ObjectProvider<NameSchemaDirectiveWiring> customDirectives
    ) {
        return (wiringBuilder) -> {
            graphQLScalarTypes.orderedStream().forEach(wiringBuilder::scalar);
            customDirectives.orderedStream().forEach(
                directive -> wiringBuilder.directive(directive.getDirectiveName(), directive)
            );
        };
    }

    /**
     * Configures custom argument resolvers for GraphQL controllers.
     * <p>
     * This method adds a custom PageableGraphQLArgumentResolver to the GraphQL controller
     * configuration, allowing GraphQL queries to use Spring Data's Pageable parameter for
     * pagination.
     * <p>
     * If other modules define a {@link HandlerMethodArgumentResolver}, it will be registered here.
     */
    @Bean
    public AnnotatedControllerConfigurer annotatedControllerConfigurer(
        @Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
        ObjectProvider<Executor> executorProvider,
        List<HandlerMethodArgumentResolver> graphqlHandlerMethodArgumentResolvers
    ) {
        AnnotatedControllerConfigurer annotatedControllerConfigurer = new AnnotatedControllerConfigurer();
        annotatedControllerConfigurer.addFormatterRegistrar(
            (registry) -> ApplicationConversionService.addBeans(registry, this.beanFactory)
        );
        executorProvider.ifAvailable(annotatedControllerConfigurer::setExecutor);
        graphqlHandlerMethodArgumentResolvers.forEach(it -> {
            log.info("Registering graphqlHandlerMethodArgumentResolver: {}", it);
            annotatedControllerConfigurer.addCustomArgumentResolver(it);
        });
        return annotatedControllerConfigurer;
    }
}
