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
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = {
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
            public Optional<List<ActionDefinition>> getActions() {
                return Optional.empty();
            }

            @Override
            public Optional<List<ComponentCategory>> getComponentCategories() {
                return Optional.empty();
            }

            @Override
            public Optional<List<ClusterElementDefinition<?>>> getClusterElements() {
                return Optional.empty();
            }

            @Override
            public Optional<ConnectionDefinition> getConnection() {
                return Optional.empty();
            }

            @Override
            public Optional<Boolean> getCustomAction() {
                return Optional.empty();
            }

            @Override
            public Optional<Help> getCustomActionHelp() {
                return Optional.empty();
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
            public Optional<Map<String, Object>> getMetadata() {
                return Optional.empty();
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
            public Optional<List<String>> getTags() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getTitle() {
                return Optional.empty();
            }

            @Override
            public Optional<List<TriggerDefinition>> getTriggers() {
                return Optional.empty();
            }

            @Override
            public Optional<UnifiedApiDefinition> getUnifiedApi() {
                return Optional.empty();
            }

            @Override
            public int getVersion() {
                return 1;
            }
        };
    }

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @EnableJdbcRepositories(basePackages = {
        "com.bytechef.platform.connection.repository", "com.bytechef.platform.tag.repository"
    })
    public static class ConnectionIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public ConnectionIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        @NonNull
        protected List<?> userConverters() {
            return Arrays.asList(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper));
        }
    }
}
