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

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.hibernate.HikariConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Stoppable;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Although Hibernate has its own implementation, such implementation does not have
 * the option to initialize it with the {@link HikariDataSource} configured by Spring.
 */
@Component
@ConditionalOnClass(HikariDataSource.class)
@ConditionalOnExpression("!'${spring.datasource.multitenant.type:NONE}'.equalsIgnoreCase('NONE')")
public class HikariDatasourceConnectionProvider implements ConnectionProvider, Stoppable {

    private final transient HikariDataSource dataSource;

    @Autowired
    public HikariDatasourceConnectionProvider(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void closeConnection(Connection conn) throws SQLException {
        conn.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public void stop() {
        dataSource.close();
    }

    @Override
    public boolean isUnwrappableAs(@NotNull Class unwrapType) {
        return ConnectionProvider.class.equals(unwrapType)
            || HikariConnectionProvider.class.isAssignableFrom(unwrapType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(@NotNull Class<T> unwrapType) {
        if (ConnectionProvider.class.equals(unwrapType) ||
                HikariConnectionProvider.class.isAssignableFrom(unwrapType)) {
            return (T) this;

        } else if (DataSource.class.isAssignableFrom(unwrapType)) {
            return (T) this.dataSource;

        } else {
            throw new UnknownUnwrapTypeException(unwrapType);
        }
    }
}
