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
package dev.springbloom.data.jpa;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.Properties;

/**
 * You need to set the property:
 * <p>
 * spring.jpa.hibernate.naming.physical-strategy=dev.springbloom.data.jpa.DefaultPhysicalNamingStrategy
 * <p>
 * Within the <b>application.properties</b> file. This class can be extended if you want to customize
 * the prefix to be used.
 */
@Slf4j
public class DefaultPhysicalNamingStrategy extends CamelCaseToUnderscoresNamingStrategy {

    private final String prefix;

    public DefaultPhysicalNamingStrategy() {
        prefix = getTablePrefix();
        if (prefix == null) {
            log.info("Property 'spring.datasource.table-prefix' not configured! Ignoring table prefix...");
        }
    }

    private String getTablePrefix() {
        try {
            Properties prop = new Properties();
            PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver =
                new PathMatchingResourcePatternResolver();

            Resource[] resources =
                pathMatchingResourcePatternResolver.getResources("classpath*:application.properties");
            for (Resource resource : resources) {
                InputStream is = resource.getInputStream();
                prop.load(is);
                String prefixTmp = prop.getProperty("spring.datasource.table-prefix");
                if (StringUtils.isNotBlank(prefixTmp)) {
                    return prefixTmp;
                }
            }
            return null;
        } catch (Exception ex) {
            String errorMsg =
                "It wasn't possible to load property 'spring.datasource.table-prefix' from file: application.properties";
            log.info(errorMsg);
            log.debug(errorMsg, ex);
            return null;
        }
    }

    @Override
    public final Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        Identifier identifier = super.toPhysicalTableName(name, jdbcEnvironment);
        String tablePrefix = (StringUtils.isBlank(prefix)) ? "" : prefix + "_";
        return new Identifier(tablePrefix + identifier.getText(), identifier.isQuoted());
    }
}
