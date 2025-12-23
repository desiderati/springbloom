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
package dev.springbloom.jms.configuration;

import dev.springbloom.jms.JmsErrorHandler;
import jakarta.jms.Queue;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Slf4j
@EnableJms
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(ArtemisAutoConfiguration.class)
@Import({ArtemisAutoConfiguration.class, org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration.class})
@PropertySource({"classpath:application-springbloom-jms.properties", "classpath:jms.properties"})
@ComponentScan(basePackages = "dev.springbloom.jms",
    // Do not add the auto-configured classes, otherwise the auto-configuration will not work as expected.
    excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
)
public class JmsAutoConfiguration {

    public static final String TYPE_ID_PROPERTY_NAME = "_type";

    public JmsAutoConfiguration(@Value("${jms.dlq.enabled:false}") Boolean jmsDefaultQueueDlqEnabled) {
        log.info("JMS default queue DLQ enabled? {}", jmsDefaultQueueDlqEnabled);
    }

    @Bean
    public Queue queue(@Value("${jms.default-queue.name}") String queueName) {
        return new ActiveMQQueue(queueName);
    }

    @Bean
    @ConditionalOnExpression("${jms.dlq.enabled:false}")
    public Queue dlqQueue(
        @Value("${jms.dlq.prefix}") String queueDlqPrefix,
        @Value("${jms.default-queue.name}") String queueName
    ) {
        return new ActiveMQQueue(queueDlqPrefix + queueName);
    }

    @Bean
    @ConditionalOnProperty("jms.default-response-queue.name")
    public Queue responseQueue(@Value("${jms.default-response-queue.name}") String responseQueueName) {
        return new ActiveMQQueue(responseQueueName);
    }

    @Bean
    @ConditionalOnExpression("${jms.dlq.enabled:false} and !'${jms.default-response-queue.name:}'.isEmpty()")
    public Queue responseDlqQueue(
        @Value("${jms.dlq.prefix}") String queueDlqPrefix,
        @Value("${jms.default-response-queue.name}") String responseQueueName
    ) {
        return new ActiveMQQueue(queueDlqPrefix + responseQueueName);
    }

    /**
     * We registered a converter for JSON.
     */
    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);

        // TypeIdPropertyName is a name of a property that identifies the entity.
        // Jackson mapper should know what entity to use when deserializing incoming JSON.
        converter.setTypeIdPropertyName(TYPE_ID_PROPERTY_NAME);
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean(JmsErrorHandler.class)
    public JmsErrorHandler jmsErrorHandler(
        @Value("${jms.default-queue.max-delivery-attempts:10}") Integer maxDeliveryAttempts
    ) {
        return new JmsErrorHandler(maxDeliveryAttempts);
    }
}
