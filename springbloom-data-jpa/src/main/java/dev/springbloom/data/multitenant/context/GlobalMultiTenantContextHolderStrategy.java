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
package dev.springbloom.data.multitenant.context;

import org.springframework.util.Assert;

/**
 * A <code>static</code> field-based implementation of {@link MultiTenantContextHolderStrategy}.
 * <p>
 * This means that all instances in the JVM share the same <code>MultiTenantContext</code>.
 * This is generally useful with rich clients, such as Swing.
 */
final class GlobalMultiTenantContextHolderStrategy implements MultiTenantContextHolderStrategy {

    private static MultiTenantContext multiTenantContext;

    @Override
    public void clearContext() {
        multiTenantContext = null;
    }

    @Override
    public MultiTenantContext getContext() {
        if (multiTenantContext == null) {
            multiTenantContext = new MultiTenantContext();
        }
        return multiTenantContext;
    }

    @Override
    public void setContext(MultiTenantContext context) {
        Assert.notNull(context, "Only non-null MultiTenantContext instances are allowed!");
        multiTenantContext = context;
    }

    @Override
    public MultiTenantContext createEmptyContext() {
        return new MultiTenantContext();
    }
}
