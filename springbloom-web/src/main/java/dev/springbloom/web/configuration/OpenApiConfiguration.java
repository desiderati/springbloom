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

import dev.springbloom.web.UrlUtils;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.configuration.SpringDocDataRestConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

/**
 * Configuration class for OpenAPI (formerly Swagger API) documentation.
 * <p>
 * This configuration automatically sets up OpenAPI documentation for Spring REST controllers
 * and Spring Data REST repositories.
 * It provides customization options through properties defined in {@link SpringDocProperties}.
 * <p>
 * Use this configuration whenever you need to expose application controllers ({@link RestController}) via OpenAPI.
 * The configuration is conditionally enabled based on the property {@code springdoc.enabled=true}
 * and only applies to servlet-based web applications.
 * <p>
 * The configuration supports:
 * <ul>
 *   <li>Custom API information (title, description, version, license)</li>
 *   <li>Server URL configuration</li>
 *   <li>External documentation links</li>
 *   <li>Package scanning for API controllers</li>
 *   <li>Path matching for API endpoints</li>
 *   <li>Integration with Spring Data REST</li>
 * </ul>
 */
@Slf4j
@Configuration // We want singleton beans here!
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "springdoc.enabled", havingValue = "true")
@EnableConfigurationProperties(SpringDocProperties.class)
@Import({
    // Support for Spring Data Rest with Open API
    RepositoryRestMvcAutoConfiguration.class,
    SpringDocDataRestConfiguration.class
})
public class OpenApiConfiguration {

    /**
     * Comma-separated list of packages to scan for API controllers.
     * Configured via the {@code springdoc.package-to-scan} property.
     * If empty, all packages will be scanned.
     */
    @Value("${springdoc.package-to-scan:}")
    private String packagesToScan;

    /**
     * Comma-separated list of path patterns to expose in the OpenAPI documentation.
     * Configured via the {@code springdoc.paths-to-expose} property.
     * Defaults to "/v.*,/public/v.*" which exposes paths starting with "/v" or "/public/v".
     */
    @Value("${springdoc.paths-to-expose:/v.*,/public/v.*}")
    private String pathsToExpose;

    /**
     * The servlet context path of the application.
     * Configured via the {@code server.servlet.context-path} property.
     * Defaults to "/" if not specified.
     */
    @Value("${server.servlet.context-path:/}")
    private String servletContextPath;

    /**
     * The default API base path used for constructing API endpoint URLs.
     * This value is injected from the property {@code app.api-base-path} with a default of "/api".
     */
    private final String defaultApiBasePath;

    /**
     * Properties for configuring OpenAPI documentation.
     * These properties are defined in {@link SpringDocProperties} and bound from configuration.
     */
    private final SpringDocProperties springDocProperties;

    /**
     * Constructs a new OpenApiConfiguration with the specified parameters.
     *
     * @param defaultApiBasePath  The default API base path used for constructing API endpoint URLs.
     *                            This is injected from the property {@code app.api-base-path} with a default of "/api".
     * @param springDocProperties Properties for configuring OpenAPI documentation.
     */
    @Autowired
    public OpenApiConfiguration(
        @Value("${app.api-base-path:/api}") String defaultApiBasePath,
        SpringDocProperties springDocProperties
    ) {
        this.defaultApiBasePath = defaultApiBasePath;
        this.springDocProperties = springDocProperties;
    }

    /**
     * Creates and configures the OpenAPI specification bean.
     * <p>
     * This method creates an OpenAPI specification with information from the {@link SpringDocProperties}
     * including title, description, version, license information, server URL, and external documentation links.
     * <p>
     * The server URL is computed by combining the local server URL from properties with the servlet context path.
     *
     * @return A configured {@link OpenAPI} instance that defines the API documentation
     */
    @Bean
    @Scope(SCOPE_SINGLETON)
    public OpenAPI openAPI() {
        Server server = new Server().url(computeServerUrlToExpose(springDocProperties.getLocalServerUrl()))
            .description(springDocProperties.getLocalServerDescription());
        return new OpenAPI()
            .info(
                new Info().title(springDocProperties.getTitle())
                    .description(springDocProperties.getDescription())
                    .version(springDocProperties.getVersion())
                    .license(
                        new License()
                            .name(springDocProperties.getLicenseName())
                            .url(springDocProperties.getLicenseUrl())
                    )
            )
            .servers(List.of(server))
            .externalDocs(
                new ExternalDocumentation()
                    .description(springDocProperties.getWikiDescription())
                    .url(springDocProperties.getWikiUrl())
            );
    }

    /**
     * Creates and configures a GroupedOpenApi bean for organizing API endpoints.
     * <p>
     * This method creates a GroupedOpenApi configuration with the fixed group name "swagger-config".
     * It configures which packages to scan for API controllers and which URL paths to include in the
     * documentation based on the configured properties.
     * <p>
     * If {@code packagesToScan} is specified, only controllers in those packages will be included.
     * If {@code pathsToExpose} is specified, only endpoints matching those path patterns will be included.
     * Each path pattern is processed through {@link #computePathToExpose(String)} to add the necessary
     * context path and API base path prefixes.
     *
     * @return A configured {@link GroupedOpenApi} instance that defines which endpoints are included in the documentation
     */
    @Bean
    @Scope(SCOPE_SINGLETON)
    public GroupedOpenApi groupedOpenApi() {
        // The group name should not be changed!
        GroupedOpenApi.Builder builder = GroupedOpenApi.builder().group("swagger-config");

        if (StringUtils.isNotBlank(packagesToScan)) {
            builder.packagesToScan(packagesToScan.split(","));
        }

        if (StringUtils.isNotBlank(pathsToExpose)) {
            builder.pathsToMatch(
                Arrays.stream(pathsToExpose.split(",")).map(this::computePathToExpose).toArray(String[]::new)
            );
        }

        return builder.build();
    }

    /**
     * Computes the complete server URL to expose in the OpenAPI documentation.
     * <p>
     * This method combines the base URL with the servlet context path to create
     * the complete URL where the API is accessible.
     * The URL is sanitized to ensure proper formatting.
     *
     * @param urlToExpose The base URL to expose (typically from properties)
     * @return The complete URL with the servlet context path appended
     */
    private String computeServerUrlToExpose(String urlToExpose) {
        var completeUrlPathToExpose = UrlUtils.sanitizeUrl(urlToExpose)
            + UrlUtils.sanitize(servletContextPath);
        log.info("Application is now accessible at: {}", completeUrlPathToExpose);
        return completeUrlPathToExpose;
    }

    /**
     * Computes the complete path to expose for an API endpoint in the OpenAPI documentation.
     * <p>
     * This method combines the servlet context path, the API base path, and the specific
     * endpoint path to create the complete path where the endpoint is accessible.
     * All path components are sanitized to ensure proper URL formatting.
     *
     * @param pathToExpose The specific endpoint path to expose
     * @return The complete path with the servlet context path and API base path prefixed
     */
    private String computePathToExpose(String pathToExpose) {
        var completePathToExpose = UrlUtils.sanitize(servletContextPath)
            + UrlUtils.sanitize(defaultApiBasePath)
            + UrlUtils.sanitize(pathToExpose);
        log.info("Controller endpoints registered under path: {}", completePathToExpose);
        return completePathToExpose;
    }
}
