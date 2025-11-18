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

package com.bytechef.platform.component.config;

import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.petstore.PetstoreComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.oas.handler.loader.OpenApiComponentHandlerLoader;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.file.storage.TempFileStorageImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@ComponentScan(
    basePackages = {
        "com.bytechef.encryption", "com.bytechef.platform.component", "com.bytechef.platform.connection"
    })
@EnableAutoConfiguration
@EnableCaching
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
@ComponentRegistryConfigurationSharedMocks
@Configuration
public class ComponentRegistryConfiguration {

    public static final TempFileStorage FILES_FILE_STORAGE = new TempFileStorageImpl(new Base64FileStorageService());
    public static final PetstoreComponentHandler PETSTORE_COMPONENT_HANDLER = new PetstoreComponentHandler() {

        @Override
        public ComponentDsl.ModifiableConnectionDefinition modifyConnection(
            ComponentDsl.ModifiableConnectionDefinition modifiableConnectionDefinition) {

            modifiableConnectionDefinition.baseUri((connectionParameters, context) -> "http://localhost:9999");

            return modifiableConnectionDefinition;
        }

        @Override
        public ModifiableActionDefinition modifyAction(ModifiableActionDefinition modifiableActionDefinition) {
            return modifiableActionDefinition.perform(
                OpenApiComponentHandlerLoader.PERFORM_FUNCTION_FUNCTION.apply(modifiableActionDefinition));
        }
    };

    @Bean
    ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Component component = new ApplicationProperties.Component();

        component.setRegistry(new ApplicationProperties.Component.Registry());

        applicationProperties.setComponent(component);

        return applicationProperties;
    }

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    TempFileStorage filesFileStorage() {
        return FILES_FILE_STORAGE;
    }

    @Bean
    JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry() {
        return new JobPrincipalAccessorRegistry(List.of());
    }

    @Bean
    TaskFileStorage workflowFileStorage() {
        return new TaskFileStorageImpl(new Base64FileStorageService());
    }

    @EnableJdbcRepositories(basePackages = "com.bytechef.platform.connection.repository")
    public static class ConnectionIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public ConnectionIntTestJdbcConfiguration(
            Encryption encryption, @Qualifier("objectMapper") ObjectMapper objectMapper) {

            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper));
        }
    }
}
