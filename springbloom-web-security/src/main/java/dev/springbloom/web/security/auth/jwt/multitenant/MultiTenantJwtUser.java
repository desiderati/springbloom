/*
 * Copyright (c) 2024 - Felipe Desiderati
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
package dev.springbloom.web.security.auth.jwt.multitenant;

import dev.springbloom.data.multitenant.MultiTenantAware;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@Setter
@SuppressWarnings("squid:S2160") // Override equals(..)
public class MultiTenantJwtUser extends User implements MultiTenantAware {

    private String tenant;

    public MultiTenantJwtUser(
        String username,
        String password,
        Collection<? extends GrantedAuthority> authorities,
        String tenant
    ) {
        super(username, password, authorities);
        this.tenant = tenant;
    }

    public static MultiTenantJwtUser from(UserDetails userDetails, String tenant) {
        return new MultiTenantJwtUser(
            userDetails.getUsername(),
            userDetails.getPassword(),
            userDetails.getAuthorities(),
            tenant
        );
    }
}
