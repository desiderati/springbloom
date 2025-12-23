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
package dev.springbloom.data.multitenant;

import dev.springbloom.data.configuration.MultiTenantProperties;
import dev.springbloom.data.multitenant.context.MultiTenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression("!'${spring.datasource.multitenant.type:NONE}'.equalsIgnoreCase('NONE')")
public class MultiTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {


    private final MultiTenantProperties multiTenantProperties;

    public MultiTenantIdentifierResolver(MultiTenantProperties multiTenantProperties) {
        this.multiTenantProperties = multiTenantProperties;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = MultiTenantContextHolder.getContext().getTenantId();
        if (tenantId == null) {
            tenantId = multiTenantProperties.getDefaultTenantId();
            log.warn("Tenant identifier was not configured! Falling back to default tenant identifier: {}", tenantId);
        }
        return tenantId;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
