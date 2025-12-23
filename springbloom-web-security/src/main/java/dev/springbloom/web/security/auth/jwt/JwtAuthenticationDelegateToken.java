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

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * This class can be used when we define a {@link AuthenticationProvider} which is responsible for calling
 * another system responsible for the authentication.
 *
 * @see for details: {@link JwtAuthenticationDelegateProvider}
 */
public class JwtAuthenticationDelegateToken extends UsernamePasswordAuthenticationToken {

    /**
     * We need to store the authorization header, which was sent by the delegated authentication provider.
     */
    public String authorizationHeader;

    public JwtAuthenticationDelegateToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public JwtAuthenticationDelegateToken(
        Object principal,
        Object credentials,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(principal, credentials, authorities);
    }
}
