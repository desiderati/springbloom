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
package dev.springbloom.data.configuration;

import dev.springbloom.data.multitenant.context.MultiTenantContextHolder;
import dev.springbloom.data.multitenant.schema.MultiTenantSchemaConnectionProvider;
import dev.springbloom.data.multitenant.MultiTenantIdentifierResolver;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    LiquibaseAutoConfiguration.class
})
@EnableConfigurationProperties(MultiTenantProperties.class)
@ConditionalOnExpression("!'${spring.datasource.multitenant.type:NONE}'.equalsIgnoreCase('NONE')")
@Import(MultiTenantComponentConfiguration.class)
public class MultiTenantAutoConfiguration {

    @Autowired
    public MultiTenantAutoConfiguration(MultiTenantProperties multiTenantProperties) {
        MultiTenantContextHolder.setStrategyName(multiTenantProperties.getContextHolderStrategyName());

        // This will be valid only during application bootstrap.
        MultiTenantContextHolder.getContext().setTenantId(multiTenantProperties.getDefaultTenantId());
    }

    /**
     * Customize the Hibernate properties before it is used by an auto-configured class.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.multitenant.type", havingValue = "schema")
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
        MultiTenantSchemaConnectionProvider multiTenantSchemaConnectionProvider,
        MultiTenantIdentifierResolver multiTenantSchemaIdentifierResolver
    ) {
        return hibernateProperties -> {
            hibernateProperties.put(
                AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantSchemaConnectionProvider
            );
            hibernateProperties.put(
                AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, multiTenantSchemaIdentifierResolver
            );
        };
    }
}
