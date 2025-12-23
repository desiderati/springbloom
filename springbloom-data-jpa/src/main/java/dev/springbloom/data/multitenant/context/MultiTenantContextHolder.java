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

import lombok.Getter;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

/**
 * Associates a given {@link MultiTenantContext} with the current execution thread.
 * <p>
 * This class provides a series of static methods that delegate to an instance of
 * {@link MultiTenantContextHolderStrategy}. The purpose of the class is to provide
 * a convenient way to specify the strategy that should be used for a given JVM.
 * This is a JVM-wide setting, since everything in this class is <code>static</code>
 * to facilitate ease of use in calling code.
 * <p>
 * To specify which strategy should be used, you must provide a mode setting. A mode
 * setting is one of the three valid <code>MODE_</code> settings defined as
 * <code>static final</code> fields, or a fully qualified classname to a concrete
 * implementation of {@link MultiTenantContextHolderStrategy} that provides a public
 * no-argument constructor.
 * <p>
 * There are two ways to specify the desired strategy mode <code>String</code>. The first
 * is to specify it via the system property keyed on {@link #SYSTEM_PROPERTY}. The second
 * is to call {@link #setStrategyName(String)} before using the class. If neither approach
 * is used, the class will default to using {@link #MODE_THREAD_LOCAL}, which is backwards
 * compatible, has fewer JVM incompatibilities and is appropriate on servers (whereas
 * {@link #MODE_GLOBAL} is definitely inappropriate for server use).
 */
@SuppressWarnings("unused")
public class MultiTenantContextHolder {

    public static final String MODE_THREAD_LOCAL = "MODE_THREAD_LOCAL";

    public static final String MODE_INHERITABLE_THREAD_LOCAL = "MODE_INHERITABLE_THREAD_LOCAL";

    public static final String MODE_GLOBAL = "MODE_GLOBAL";

    private static final String MODE_PRE_INITIALIZED = "MODE_PRE_INITIALIZED";

    public static final String SYSTEM_PROPERTY = "spring.datasource.multitenant.context-holder-strategy-name";

    private static String strategyName = System.getProperty(SYSTEM_PROPERTY);

    private static MultiTenantContextHolderStrategy strategy;

    /**
     * Primarily for troubleshooting purposes, this property shows how many times the class
     * has re-initialized its <code>MultiTenantContextHolderStrategy</code>.
     * <p>
     * It contains the count (should be one unless you've called {@link #setStrategyName(String)} or
     * {@link #setContextHolderStrategy(MultiTenantContextHolderStrategy)} to switch to an
     * alternate strategy).
     */
    @Getter
    private static int initializeCount = 0;

    static {
        initialize();
    }

    private static void initialize() {
        initializeStrategy();
        initializeCount++;
    }

    private static void initializeStrategy() {
        if (!StringUtils.hasText(strategyName)) {
            // Set the default mode.
            strategyName = MODE_THREAD_LOCAL;
        }

        switch (strategyName) {
            case MODE_PRE_INITIALIZED -> {
                Assert.state(strategy != null, "When using " + MODE_PRE_INITIALIZED +
                    ", setContextHolderStrategy(..) must be called with the fully constructed strategy.");
                return;
            }
            case MODE_THREAD_LOCAL -> {
                strategy = new ThreadLocalMultiTenantContextHolderStrategy();
                return;
            }
            case MODE_INHERITABLE_THREAD_LOCAL -> {
                strategy = new InheritableThreadLocalMultiTenantContextHolderStrategy();
                return;
            }
            case MODE_GLOBAL -> {
                strategy = new GlobalMultiTenantContextHolderStrategy();
                return;
            }
        }

        // Try to load a custom strategy.
        try {
            Class<?> clazz = Class.forName(strategyName);
            Constructor<?> customStrategy = clazz.getConstructor();
            strategy = (MultiTenantContextHolderStrategy) customStrategy.newInstance();
        } catch (Exception ex) {
            ReflectionUtils.handleReflectionException(ex);
        }
    }

    /**
     * Explicitly clears the context value from the current thread.
     */
    public static void clearContext() {
        strategy.clearContext();
    }

    /**
     * @return the current <code>MultiTenantContext</code> (never <code>null</code>)
     */
    public static MultiTenantContext getContext() {
        return strategy.getContext();
    }

    /**
     * @return a {@link Supplier} that returns the current context (never
     * <code>null</code> - create a default implementation if necessary)
     */
    public static Supplier<MultiTenantContext> getDeferredContext() {
        return strategy.getDeferredContext();
    }

    /**
     * Associates a new <code>MultiTenantContext</code> with the current thread of execution.
     *
     * @param context the new <code>MultiTenantContext</code> (may not be <code>null</code>)
     */
    public static void setContext(MultiTenantContext context) {
        strategy.setContext(context);
    }

    /**
     * Sets a {@link Supplier} that will return the current context. Implementations can
     * override the default to avoid invoking {@link Supplier#get()}.
     *
     * @param deferredContext a {@link Supplier} that returns the {@link MultiTenantContext}
     */
    public static void setDeferredContext(Supplier<MultiTenantContext> deferredContext) {
        strategy.setDeferredContext(deferredContext);
    }

    /**
     * Changes the preferred strategy. Do <em>NOT</em> call this method more than once for
     * a given JVM, as it will re-initialize the strategy and adversely affect any
     * existing threads using the old strategy.
     *
     * @param strategyName the fully qualified class name of the strategy that should be used.
     */
    public static void setStrategyName(String strategyName) {
        MultiTenantContextHolder.strategyName = strategyName;
        initialize();
    }

    /**
     * Call either {@link #setStrategyName(String)} or this method, but not both.
     * <p>
     * This method is not thread safe. Changing the strategy while requests are in-flight
     * may cause race conditions.
     * <p>
     * {@link MultiTenantContextHolder} maintains a static reference to the provided
     * {@link MultiTenantContextHolderStrategy}. This means that the strategy and its members
     * will not be garbage collected until you remove your strategy.
     * <p>
     * To ensure garbage collection, remember the original strategy like so:
     * <pre>
     *     MultiTenantContextHolderStrategy original = MultiTenantContextHolder.getContextHolderStrategy();
     *     MultiTenantContextHolder.setContextHolderStrategy(myStrategy);
     * </pre>
     * <p>
     * And then when you are ready for {@code myStrategy} to be garbage collected you can do:
     * <pre>
     *     MultiTenantContextHolder.setContextHolderStrategy(original);
     * </pre>
     *
     * @param strategy the {@link MultiTenantContextHolderStrategy} to use.
     */
    public static void setContextHolderStrategy(MultiTenantContextHolderStrategy strategy) {
        Assert.notNull(strategy, "MultiTenantContextHolderStrategy cannot be null");
        MultiTenantContextHolder.strategyName = MODE_PRE_INITIALIZED;
        MultiTenantContextHolder.strategy = strategy;
        initialize();
    }

    /**
     * Allows retrieval of the context strategy.
     *
     * @return the configured strategy for storing the multitenant context.
     */
    public static MultiTenantContextHolderStrategy getContextHolderStrategy() {
        return strategy;
    }

    /**
     * Delegates the creation of a new, empty context to the configured strategy.
     */
    public static MultiTenantContext createEmptyContext() {
        return strategy.createEmptyContext();
    }

    @Override
    public String toString() {
        return "MultiTenantContextHolder[strategy='" + strategy.getClass().getSimpleName() + "'; initializeCount="
            + initializeCount + "]";
    }
}
