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

package com.bytechef.automation.knowledgebase.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToPGObjectConverter;
import com.bytechef.commons.data.jdbc.converter.PGobjectToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseVectorStoreMetadataService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Integration-test configuration for the automation-knowledge-base service slice. Brings up the platform-KB service
 * beans (so the workspace-aware service can delegate to {@code KnowledgeBaseSourceService}) plus the automation-KB
 * service beans, while mocking out the AI/vector-store/file-storage moving parts that the unit slice doesn't need.
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.automation.knowledgebase",
        "com.bytechef.platform.knowledgebase",
        "com.bytechef.jackson.config",
        "com.bytechef.platform.tag"
    },
    excludeFilters = @ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.REGEX,
        pattern = {
            "com\\.bytechef\\.platform\\.knowledgebase\\.config\\.KnowledgeBasePgVectorConfiguration",
            "com\\.bytechef\\.automation\\.knowledgebase\\.facade\\..*"
        }))
@EnableAutoConfiguration(excludeName = {
    "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration",
    "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration"
})
@EnableConfigurationProperties(ApplicationProperties.class)
@Import(LiquibaseConfiguration.class)
@Configuration
public class AutomationKnowledgeBaseIntTestConfiguration {

    @Bean
    ComponentDefinitionService componentDefinitionService() {
        // The IntTest slice exercises the workspace facade end-to-end; the workflow generator path requires the
        // facade to resolve a source cluster element name. Tests that drive create() either supply an explicit name
        // (so the resolver short-circuits before touching this mock) or operate on pre-persisted source rows that
        // bypass create() entirely.
        return mock(ComponentDefinitionService.class);
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return mock(EmbeddingModel.class);
    }

    @Bean
    VectorStore knowledgeBasePgVectorStore() {
        return mock(VectorStore.class);
    }

    @Bean
    FileStorageService fileStorageService() {
        return mock(FileStorageService.class);
    }

    @Bean
    KnowledgeBaseFileStorage knowledgeBaseFileStorage() {
        return mock(KnowledgeBaseFileStorage.class);
    }

    @Bean
    FileStorageServiceRegistry fileStorageServiceRegistry(FileStorageService fileStorageService) {
        FileStorageServiceRegistry registry = mock(FileStorageServiceRegistry.class);

        when(registry.getFileStorageService(anyString())).thenReturn(fileStorageService);

        return registry;
    }

    @Bean
    KnowledgeBaseVectorStoreMetadataService knowledgeBaseVectorStoreMetadataUpdater() {
        return mock(KnowledgeBaseVectorStoreMetadataService.class);
    }

    @Bean
    MessageBroker messageBroker() {
        return mock(MessageBroker.class);
    }

    @Bean
    ObjectMapper jacksonObjectMapper() {
        return JsonMapper.builder()
            .build();
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AutomationKnowledgeBaseIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public AutomationKnowledgeBaseIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new FileEntryToStringConverter(objectMapper),
                new MapWrapperToPGObjectConverter(objectMapper),
                new PGobjectToMapWrapperConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper));
        }
    }
}
