/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.encryption.EncryptionKey;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfiguration.PlatformConfigurationIntTestJdbcConfiguration;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.repository.PropertyRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PropertyServiceIntTest.PropertyServiceIntTestConfiguration.class)
class PropertyServiceIntTest {

    private static final long ENVIRONMENT = 0;
    private static final String KEY = "ai.provider.ollama";

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyService propertyService;

    @AfterEach
    void afterEach() {
        propertyRepository.deleteAll();
    }

    @Test
    void testSaveEmptyValueCreatesEnabledProperty() {
        propertyService.save(KEY, Map.of(), Scope.PLATFORM, null, ENVIRONMENT);

        Optional<Property> property = propertyService.fetchProperty(KEY, Scope.PLATFORM, null, ENVIRONMENT);

        assertThat(property).hasValueSatisfying(curProperty -> {
            assertThat(curProperty.isEnabled()).isTrue();
            assertThat(curProperty.getValue()).isEmpty();
        });
    }

    @Test
    void testUpdateEnabledOnPropertyCreatedWithEmptyValue() {
        propertyService.save(KEY, Map.of(), Scope.PLATFORM, null, ENVIRONMENT);

        propertyService.update(KEY, false, Scope.PLATFORM, null, ENVIRONMENT);

        Optional<Property> property = propertyService.fetchProperty(KEY, Scope.PLATFORM, null, ENVIRONMENT);

        assertThat(property).hasValueSatisfying(curProperty -> assertThat(curProperty.isEnabled()).isFalse());
    }

    @Test
    void testUpdateEnabledCreatesMissingProperty() {
        propertyService.update(KEY, true, Scope.PLATFORM, null, ENVIRONMENT);

        Optional<Property> property = propertyService.fetchProperty(KEY, Scope.PLATFORM, null, ENVIRONMENT);

        assertThat(property).hasValueSatisfying(curProperty -> {
            assertThat(curProperty.isEnabled()).isTrue();
            assertThat(curProperty.getValue()).isEmpty();
        });
    }

    @Test
    void testSaveEmptyValueKeepsExistingValues() {
        propertyService.save(KEY, Map.of("url", "http://ollama:11434"), Scope.PLATFORM, null, ENVIRONMENT);

        propertyService.save(KEY, Map.of(), Scope.PLATFORM, null, ENVIRONMENT);

        Optional<Property> property = propertyService.fetchProperty(KEY, Scope.PLATFORM, null, ENVIRONMENT);

        assertThat(property).hasValueSatisfying(
            curProperty -> assertThat(curProperty.get("url")).isEqualTo("http://ollama:11434"));
    }

    @ComponentScan(basePackages = "com.bytechef.encryption")
    @Configuration
    @EnableAutoConfiguration
    @Import({
        JacksonConfiguration.class, LiquibaseConfiguration.class, PlatformConfigurationIntTestJdbcConfiguration.class,
        PostgreSQLContainerConfiguration.class, PropertyServiceImpl.class
    })
    static class PropertyServiceIntTestConfiguration {

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }
    }
}
