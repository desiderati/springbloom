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

import java.util.function.Supplier;

/**
 * An <code>InheritableThreadLocal</code>-based implementation of {@link MultiTenantContextHolderStrategy}.
 *
 * @see java.lang.InheritableThreadLocal
 */
final class InheritableThreadLocalMultiTenantContextHolderStrategy implements MultiTenantContextHolderStrategy {

    private static final ThreadLocal<Supplier<MultiTenantContext>> multiTenantContext = new InheritableThreadLocal<>();

    @Override
    public void clearContext() {
        multiTenantContext.remove();
    }

    @Override
    public MultiTenantContext getContext() {
        return getDeferredContext().get();
    }

    @Override
    public Supplier<MultiTenantContext> getDeferredContext() {
        Supplier<MultiTenantContext> result = multiTenantContext.get();
        if (result == null) {
            MultiTenantContext context = createEmptyContext();
            result = () -> context;
            multiTenantContext.set(result);
        }
        return result;
    }

    @Override
    public void setContext(MultiTenantContext context) {
        Assert.notNull(context, "Only non-null MultiTenantContext instances are allowed!");
        multiTenantContext.set(() -> context);
    }

    @Override
    public void setDeferredContext(Supplier<MultiTenantContext> deferredContext) {
        Assert.notNull(deferredContext, "Only non-null Supplier instances are allowed!");
        Supplier<MultiTenantContext> notNullDeferredContext = () -> {
            MultiTenantContext result = deferredContext.get();
            Assert.notNull(result, "A Supplier<MultiTenantContext> returned null and is not allowed.");
            return result;
        };
        multiTenantContext.set(notNullDeferredContext);
    }

    @Override
    public MultiTenantContext createEmptyContext() {
        return new MultiTenantContext();
    }
}
