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

package com.bytechef.platform.connection.config;

import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Help;
import com.bytechef.component.definition.Resources;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.connection.service.TestExternalConnectionCredentialStore;
import com.bytechef.platform.credential.store.service.DatabaseCredentialStore;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.encryption", "com.bytechef.platform.connection"
    })
@EnableAutoConfiguration
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class ConnectionIntTestConfiguration {

    @Bean
    ComponentHandler componentHandler() {
        return () -> new ComponentDefinition() {
            @Override
            public List<ActionDefinition> getActions() {
                return List.of();
            }

            @Override
            public List<ComponentCategory> getComponentCategories() {
                return List.of();
            }

            @Override
            public List<ClusterElementDefinition<?>> getClusterElements() {
                return List.of();
            }

            @Override
            public Optional<ConnectionDefinition> getConnection() {
                return Optional.empty();
            }

            @Override
            public boolean getCustomAction() {
                return false;
            }

            @Override
            public Optional<Help> getCustomActionHelp() {
                return Optional.empty();
            }

            @Override
            public List<? extends com.bytechef.component.definition.PropertyGroup> getInputs() {
                return List.of();
            }

            @Override
            public Optional<String> getDescription() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getIcon() {
                return Optional.empty();
            }

            @Override
            public Map<String, Object> getMetadata() {
                return Map.of();
            }

            @Override
            public String getName() {
                return "componentName";
            }

            @Override
            public Optional<Resources> getResources() {
                return Optional.empty();
            }

            @Override
            public List<String> getTags() {
                return List.of();
            }

            @Override
            public Optional<String> getTitle() {
                return Optional.empty();
            }

            @Override
            public List<TriggerDefinition> getTriggers() {
                return List.of();
            }

            @Override
            public Optional<UnifiedApiDefinition> getUnifiedApi() {
                return Optional.empty();
            }
        };
    }

    @Bean
    DatabaseCredentialStore databaseCredentialStore() {
        return new DatabaseCredentialStore();
    }

    @Bean
    TestExternalConnectionCredentialStore testExternalConnectionCredentialStore() {
        return new TestExternalConnectionCredentialStore();
    }

    @Bean
    JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry(
        List<JobPrincipalAccessor> jobPrincipalAccessors) {

        return new JobPrincipalAccessorRegistry(jobPrincipalAccessors);
    }

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class ConnectionIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public ConnectionIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected @NonNull List<?> userConverters() {
            return Arrays.asList(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper));
        }
    }
}
