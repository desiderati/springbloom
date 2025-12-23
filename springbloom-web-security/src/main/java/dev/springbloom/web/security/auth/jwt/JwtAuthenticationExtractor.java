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
package dev.springbloom.web.security.auth.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * Class used for retrieving the user/password from the request. This information will be
 * used by the {@link AuthenticationManager} to authenticate the user.
 */
public class JwtAuthenticationExtractor implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        try {
            String requestBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode jsonNode = new ObjectMapper().readTree(requestBody);
            JsonNode usernameNode =
                jsonNode.get(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY);
            if (usernameNode == null) {
                throw new AuthenticationServiceException("Invalid username key!");
            }
            String username = usernameNode.asText();

            JsonNode passwordNode =
                jsonNode.get(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_PASSWORD_KEY);
            if (passwordNode == null) {
                throw new AuthenticationServiceException("Invalid password key!");
            }
            String password = passwordNode.asText();

            // Never get the roles from the request. The roles must be informed by the Authentication Manager.
            return new JwtAuthenticationDelegateToken(username, password);
        } catch (Exception e) {
            throw new AuthenticationServiceException("Authentication Failed: " + e.getMessage(), e);
        }
    }
}
