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
package dev.springbloom.core.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncExecutionAspectSupport;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(name = "spring.async.enabled", havingValue = "true")
public class AsyncConfiguration implements AsyncConfigurer {

    @Configuration(proxyBeanMethods = false)
    public static class AsyncAliasTaskExecutorConfiguration {

        /**
         * Creates an alias for the "applicationTaskExecutor" bean with the name "taskExecutor".
         * This is necessary because Spring's AnnotationAsyncExecutionInterceptor looks for a bean
         * named "taskExecutor" by default when processing @Async annotations.
         *
         * @param applicationTaskExecutor The application's task executor bean
         * @return The same task executor instance but with the name "taskExecutor"
         */
        @Bean(AsyncExecutionAspectSupport.DEFAULT_TASK_EXECUTOR_BEAN_NAME)
        public AsyncTaskExecutor taskExecutor(
            @Lazy @Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
            AsyncTaskExecutor applicationTaskExecutor
        ) {
            log.info(
                "Creating alias '{}' for the '{}' bean",
                AsyncExecutionAspectSupport.DEFAULT_TASK_EXECUTOR_BEAN_NAME,
                TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME
            );
            return applicationTaskExecutor;
        }
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
