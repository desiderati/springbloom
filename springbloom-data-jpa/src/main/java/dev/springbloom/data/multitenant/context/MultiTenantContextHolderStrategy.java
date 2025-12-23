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

import java.util.function.Supplier;

/**
 * Strategy interface for storing and retrieving the {@link MultiTenantContext} in various scopes.
 * <p>
 * This interface defines the contract for different strategies that can be used to store and
 * retrieve tenant context information in a multi-tenant application. The strategy pattern allows
 * for different implementations based on the specific requirements of the application.
 * <p>
 * There are three standard implementations provided:
 * <ul>
 *     <li>{@link ThreadLocalMultiTenantContextHolderStrategy} - Stores the context in a ThreadLocal variable,
 *     making it accessible only to the current thread.
 *     </li>
 *     <li>{@link InheritableThreadLocalMultiTenantContextHolderStrategy} - Stores the context in an
 *     {@link InheritableThreadLocal} variable, allowing child threads to inherit the context from their parent thread.
 *     </li>
 *     <li>{@link GlobalMultiTenantContextHolderStrategy} - Stores the context in a static field, making it
 *     accessible to all threads in the JVM.
 *     </li>
 * </ul>
 * <p>
 * The strategy to be used can be configured through the {@link MultiTenantContextHolder} class.
 *
 * @see MultiTenantContext
 * @see MultiTenantContextHolder
 */
public interface MultiTenantContextHolderStrategy {

    /**
     * Clears the current context.
     */
    void clearContext();

    /**
     * Gets the current context.
     *
     * @return a context (never <code>null</code> - create a default implementation if
     * necessary)
     */
    MultiTenantContext getContext();

    /**
     * Obtains a {@link Supplier} that returns the current context.
     *
     * @return a {@link Supplier} that returns the current context (never
     * <code>null</code> - create a default implementation if necessary)
     */
    default Supplier<MultiTenantContext> getDeferredContext() {
        return this::getContext;
    }

    /**
     * Sets the current multiTenantContext.
     *
     * @param multiTenantContext to the new argument (should never be <code>null</code>, although
     *                           implementations must check if <code>null</code> has been passed and throw an
     *                           <code>IllegalArgumentException</code> in such cases)
     */
    void setContext(MultiTenantContext multiTenantContext);

    /**
     * Sets a {@link Supplier} that will return the current context. Implementations can
     * override the default to avoid invoking {@link Supplier#get()}.
     *
     * @param deferredContext a {@link Supplier} that returns the {@link MultiTenantContext}
     */
    default void setDeferredContext(Supplier<MultiTenantContext> deferredContext) {
        setContext(deferredContext.get());
    }

    /**
     * Creates a new, empty context implementation.
     *
     * @return the empty context.
     */
    MultiTenantContext createEmptyContext();

}
