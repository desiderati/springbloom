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

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.function.Function;

/**
 * Allows getting and setting the Spring {@link MultiTenantContext} into a {@link Context}.
 */
@SuppressWarnings("unused")
public final class ReactiveMultiTenantContextHolder {

    private static final Class<?> MULTI_TENANT_CONTEXT_CLASS = MultiTenantContext.class;

    private ReactiveMultiTenantContextHolder() {
    }

    /**
     * Gets the {@code Mono<MultiTenantContext>} from Reactor {@link Context}
     *
     * @return the {@code Mono<MultiTenantContext>}
     */
    public static Mono<MultiTenantContext> getContext() {
        return Mono.deferContextual(Mono::just)
            .cast(Context.class)
            .filter(ReactiveMultiTenantContextHolder::hasMultiTenantContext)
            .flatMap(ReactiveMultiTenantContextHolder::getMultiTenantContext);
    }

    private static boolean hasMultiTenantContext(Context context) {
        return context.hasKey(MULTI_TENANT_CONTEXT_CLASS);
    }

    private static Mono<MultiTenantContext> getMultiTenantContext(Context context) {
        return context.<Mono<MultiTenantContext>>get(MULTI_TENANT_CONTEXT_CLASS);
    }

    /**
     * Clears the {@code Mono<MultiTenantContext>} from Reactor {@link Context}
     *
     * @return Return a {@code Mono<Void>} which only replays complete and error signals
     * from clearing the context.
     */
    public static Function<Context, Context> clearContext() {
        return (context) -> context.delete(MULTI_TENANT_CONTEXT_CLASS);
    }

    /**
     * Creates a Reactor {@link Context} that contains the {@code Mono<MultiTenantContext>}
     * that can be merged into another {@link Context}
     *
     * @param MultiTenantContext the {@code Mono<MultiTenantContext>} to set in the returned
     *                           Reactor {@link Context}
     * @return a Reactor {@link Context} that contains the {@code Mono<MultiTenantContext>}
     */
    public static Context withMultiTenantContext(Mono<? extends MultiTenantContext> MultiTenantContext) {
        return Context.of(MULTI_TENANT_CONTEXT_CLASS, MultiTenantContext);
    }

    /**
     * A shortcut for {@link #withMultiTenantContext(Mono)}
     *
     * @param tenantId the tenant ID to be used.
     * @return a Reactor {@link Context} that contains the {@code Mono<MultiTenantContext>}
     */
    public static Context withAuthentication(String tenantId) {
        return withMultiTenantContext(Mono.just(new MultiTenantContext(tenantId)));
    }
}
