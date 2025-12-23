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

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnBean(MultiTenantSchemaConnectionProvider.class)
@ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true")
public class MultiTenantSchemaLiquibaseUpdater {

    private final SpringLiquibase liquibase;

    @Autowired
    public MultiTenantSchemaLiquibaseUpdater(SpringLiquibase liquibase) {
        this.liquibase = liquibase;
    }

    @SneakyThrows
    public void update(@NonNull String schema) {
        schema = StringUtils.trim(schema);
        if (StringUtils.isBlank(schema)) {
            throw new IllegalArgumentException("Schema cannot be empty!");
        }

        log.info("Applying changes for schema: {}", schema);
        liquibase.setDefaultSchema(schema);

        try {
            liquibase.afterPropertiesSet();
        } catch (LiquibaseException e) {
            throw new LiquibaseException("Error while running Liquibase for schema: " + schema, e);
        }
    }
}
