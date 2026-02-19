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

package com.bytechef.automation.configuration.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacadeImpl;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.atlas.configuration.repository.jdbc", "com.bytechef.commons.util", "com.bytechef.jackson.config",
        "com.bytechef.platform.category", "com.bytechef.automation.configuration", "com.bytechef.platform.connection",
        "com.bytechef.platform.tag"
    })
@EnableAutoConfiguration
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@Import(LiquibaseConfiguration.class)
@Configuration
public class ProjectIntTestConfiguration {

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Bean
    Evaluator evaluator() {
        return SpelEvaluator.create();
    }

    @Bean
    SharedTemplateFileStorage sharedFileStorage() {
        SharedTemplateFileStorage sharedTemplateFileStorage = mock(SharedTemplateFileStorage.class);

        Mockito.when(
            sharedTemplateFileStorage.storeFileContent(anyString(), any(InputStream.class)))
            .thenAnswer(invocation -> {
                String name = invocation.getArgument(0);

                FileEntry fileEntry = mock(FileEntry.class);

                Mockito.when(fileEntry.getName())
                    .thenReturn(name);
                Mockito.when(fileEntry.toId())
                    .thenReturn(String.valueOf(UUID.randomUUID()));

                return fileEntry;
            });

        return sharedTemplateFileStorage;
    }

    @Bean
    WorkflowFacade workflowFacade(WorkflowService workflowService) {
        return new WorkflowFacadeImpl(componentConnectionFacade, componentDefinitionService, workflowService);
    }

    @Bean
    WorkflowService workflowService(
        CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
        List<WorkflowRepository> workflowRepositories) {

        return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
    }

    @EnableJdbcRepositories(
        basePackages = {
            "com.bytechef.atlas.configuration.repository.jdbc", "com.bytechef.platform.category.repository",
            "com.bytechef.automation.configuration.repository", "com.bytechef.platform.tag.repository",
            "com.bytechef.platform.configuration.repository"
        })
    public static class ProjectIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public ProjectIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new FileEntryToStringConverter(objectMapper),
                new MapWrapperToStringConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper));
        }
    }
}
