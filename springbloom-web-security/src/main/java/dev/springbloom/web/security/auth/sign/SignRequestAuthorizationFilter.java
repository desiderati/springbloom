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
package dev.springbloom.web.security.auth.sign;

import dev.springbloom.data.multitenant.context.MultiTenantContextHolder;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * The SignRequestAuthorizationFilter is a custom filter extending {@code OncePerRequestFilter} that
 * performs the authorization of HTTP requests by validating their signatures.
 * It integrates with the Spring Security framework and is enabled conditionally based on the property
 * {@code spring.web.security.sign-request.authorization.enabled=true}.
 * <p>
 * This filter ensures that the request carries a valid digital signature to be processed,
 * enhancing the security of the system.
 * <p>
 * Responsibilities:
 * - Validates the signature of HTTP requests using the {@link SignRequestService}.
 * - Sets the authenticated principal into the {@link SecurityContextHolder} after successful validation.
 * - Ensures the filter is registered only through Spring Security, avoiding duplicate execution.
 * <p>
 * This filter employs a {@link SignRequestWrapper} to allow the request body to be read multiple
 * times, as required for signature validation.
 * If authentication fails, the signature verification errors are logged and the security context
 * remains unauthenticated.
 * <p>
 * The registration of this filter as a Spring bean is explicitly disabled to allow correct
 * registration via Spring Security configuration.
 * </p>
 * Configuration Dependency:
 * - To activate the filter, set the property {@code spring.web.security.sign-request.authorization.enabled=true}.
 * <p>
 * Thread Safety:
 * - Instances of this class are expected to be stateless and designed for concurrent request handling.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.web.security.sign-request.authorization.enabled", havingValue = "true")
public class SignRequestAuthorizationFilter extends OncePerRequestFilter {

    private final SignRequestService signRequestService;

    @Autowired
    public SignRequestAuthorizationFilter(SignRequestService signRequestService) {
        this.signRequestService = signRequestService;
    }

    /**
     * By default, Spring Boot creates all beans of type: {@link Filter}. We do not want this filter
     * to be registered automatically because it needs to be registered via Spring Security configuration.
     * So preventing it from being run twice!
     */
    @Bean
    public FilterRegistrationBean<SignRequestAuthorizationFilter> signRequestAuthorizationFilterRegistration(
        @Qualifier("signRequestAuthorizationFilter") SignRequestAuthorizationFilter filter
    ) {
        FilterRegistrationBean<SignRequestAuthorizationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Override
    protected void doFilterInternal(
        @NotNull HttpServletRequest servletRequest,
        @NotNull HttpServletResponse servletResponse,
        @NotNull FilterChain filterChain
    ) throws ServletException, IOException {

        HttpServletRequest signServletRequest = new SignRequestWrapper(servletRequest);
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Authentication authentication = null;
            try {
                authentication = signRequestService.verifySignature(signServletRequest);
            } catch (AuthenticationServiceException failed) {
                log.error("Authorization Failed: {}", failed.getMessage(), failed);
            }
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            log.debug("SecurityContextHolder already contains: '{}'",
                SecurityContextHolder.getContext().getAuthentication()
            );
        }

        try {
            filterChain.doFilter(signServletRequest, servletResponse);
        } finally {
            MultiTenantContextHolder.clearContext();
        }
    }
}
