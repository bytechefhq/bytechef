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

package com.bytechef.atlas.execution.repository.jdbc.config;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
        "com.bytechef.atlas.configuration.converter", "com.bytechef.atlas.execution.repository.jdbc"
    })
@EnableAutoConfiguration
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class WorkflowExecutionRepositoryIntTestConfiguration {

    @Bean
    TaskFileStorage workflowFileStorage() {
        return new TaskFileStorageImpl(new Base64FileStorageService());
    }

    @EnableJdbcRepositories(basePackages = "com.bytechef.atlas.execution.repository.jdbc")
    public static class WorkflowExecutionIntJdbcTestConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public WorkflowExecutionIntJdbcTestConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new ExecutionErrorToStringConverter(objectMapper),
                new FileEntryToStringConverter(objectMapper),
                new MapWrapperToStringConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper),
                new StringToWebhooksConverter(objectMapper),
                new StringToWorkflowTaskConverter(objectMapper),
                new WebhooksToStringConverter(objectMapper),
                new WorkflowTaskToStringConverter(objectMapper));
        }
    }
}
