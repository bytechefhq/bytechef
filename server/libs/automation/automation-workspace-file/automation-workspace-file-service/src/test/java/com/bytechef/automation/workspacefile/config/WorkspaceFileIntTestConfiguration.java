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

package com.bytechef.automation.workspacefile.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.workspacefile.file.storage.WorkspaceFileFileStorage;
import com.bytechef.automation.workspacefile.file.storage.WorkspaceFileFileStorageImpl;
import com.bytechef.automation.workspacefile.file.storage.config.WorkspaceFileFileStorageConfiguration;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.automation.workspacefile",
        "com.bytechef.jackson.config",
        "com.bytechef.platform.tag"
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = WorkspaceFileFileStorageConfiguration.class))
@EnableAutoConfiguration
@Import(LiquibaseConfiguration.class)
@Configuration
public class WorkspaceFileIntTestConfiguration {

    @Bean
    FileStorageService fileStorageService() {
        return new Base64FileStorageService();
    }

    @Bean
    FileStorageServiceRegistry fileStorageServiceRegistry(FileStorageService fileStorageService) {
        FileStorageServiceRegistry registry = mock(FileStorageServiceRegistry.class);

        when(registry.getFileStorageService(anyString())).thenReturn(fileStorageService);

        return registry;
    }

    @Bean
    WorkspaceFileFileStorage workspaceFileFileStorage(FileStorageService fileStorageService) {
        return new WorkspaceFileFileStorageImpl(fileStorageService);
    }

    @Bean
    ObjectMapper jacksonObjectMapper() {
        return JsonMapper.builder()
            .build();
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class WorkspaceFileIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public WorkspaceFileIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new FileEntryToStringConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper));
        }
    }
}
