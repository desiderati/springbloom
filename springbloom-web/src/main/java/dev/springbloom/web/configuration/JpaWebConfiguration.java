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
package dev.springbloom.web.configuration;

import dev.springbloom.data.configuration.ConditionalOnJpa;
import jakarta.servlet.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;

@Slf4j
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)

// Tentei de todas as formas possíveis usar a anotação comentada abaixo, mas por algum motivo
// que não sei dizer qual, e, mesmo colocando a anotação @Order, a verificação desta anotação
// sempre ocorria antes do registro do 'AbstractEntityManagerFactoryBean'. Desta forma, como
// tal Bean ainda não havia sido registrado no contexto, o filtro abaixo não era criado.
// @ConditionalOnBean(AbstractEntityManagerFactoryBean.class)

// FIXME Felipe Desiderati: Entender porque não consigo validar a criação do Bean,
//  apenas após a criação do 'AbstractEntityManagerFactoryBean'.
@ConditionalOnJpa
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "spring.jpa.open-in-view", havingValue = "true")
public class JpaWebConfiguration {

    @Bean
    public Filter openEntityManagerInViewFilter() {
        return new OpenEntityManagerInViewFilter();
    }
}
