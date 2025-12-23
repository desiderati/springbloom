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
package dev.springbloom.data.multitenant.schema;

import dev.springbloom.data.configuration.MultiTenantProperties;
import dev.springbloom.data.multitenant.HikariDatasourceConnectionProvider;
import dev.springbloom.data.multitenant.MultiTenantException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class responsible for applying the rules of multi-tenant whenever a connection is created by the Hibernate.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.datasource.multitenant.type", havingValue = "schema")
public class MultiTenantSchemaConnectionProvider extends AbstractMultiTenantConnectionProvider<String> {

    private final HikariDatasourceConnectionProvider connectionProvider;
    private final MultiTenantSchemaLiquibaseUpdater liquibaseUpdater;
    private final MultiTenantProperties multiTenantProperties;

    /**
     * Local cache to ensure that the rules are applied only once per schema (Tenant).
     */
    private final Set<String> processedSchemas;
    private final ReentrantLock lock = new ReentrantLock();

    public MultiTenantSchemaConnectionProvider(
        HikariDatasourceConnectionProvider connectionProvider,
        ObjectProvider<MultiTenantSchemaLiquibaseUpdater> liquibaseUpdater,
        MultiTenantProperties multiTenantProperties
    ) {
        this.connectionProvider = connectionProvider;
        this.liquibaseUpdater = liquibaseUpdater.getIfAvailable();
        this.multiTenantProperties = multiTenantProperties;
        this.processedSchemas = new HashSet<>();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        String schema = StringUtils.trim(tenantIdentifier);
        Connection connection = super.getConnection(schema);

        if (!processedSchemas.contains(schema)) {
            lock.lock();
            try (Statement statement = connection.createStatement()) {
                //noinspection SqlSourceToSinkFlow
                statement.execute(
                    getStringSubstitutor(schema).replace(multiTenantProperties.getSchema().getDdlCreate())
                );

                if (liquibaseUpdater != null) {
                    // Ensure that Liquibase rules are being applied to the new clients.
                    liquibaseUpdater.update(schema);
                }

                processedSchemas.add(schema);
            } catch (Exception e) {
                throw new MultiTenantException("Failed to create schema: " + schema, e);
            } finally {
                lock.unlock();
            }
        }

        connection.setSchema(schema);
        return connection;
    }

    @NotNull
    private StringSubstitutor getStringSubstitutor(String tenantIdentifier) {
        Map<String, String> props = new HashMap<>();
        props.put("schemaName", tenantIdentifier);
        return new StringSubstitutor(props);
    }

    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        return connectionProvider;
    }

    @Override
    protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
        return connectionProvider;
    }
}
