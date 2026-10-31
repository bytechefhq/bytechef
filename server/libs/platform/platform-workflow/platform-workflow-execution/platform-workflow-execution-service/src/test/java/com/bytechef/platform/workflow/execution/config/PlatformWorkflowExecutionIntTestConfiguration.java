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

package com.bytechef.platform.workflow.execution.config;

import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.workflow.execution.repository.converter.JobResumeIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToJobResumeIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToTaskStateValueConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToTriggerStateValueConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowExecutionIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowTriggerConverter;
import com.bytechef.platform.workflow.execution.repository.converter.TaskStateValueToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.TriggerStateValueToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowExecutionIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowTriggerToStringConverter;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.platform.workflow.execution.repository", "com.bytechef.platform.workflow.execution.service"
    },
    excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
@EnableAutoConfiguration
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
@Configuration
public class PlatformWorkflowExecutionIntTestConfiguration {

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class PlatformWorkflowExecutionIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public PlatformWorkflowExecutionIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new ExecutionErrorToStringConverter(objectMapper),
                new FileEntryToStringConverter(objectMapper),
                new JobResumeIdToStringConverter(),
                new StringToExecutionErrorConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper),
                new StringToJobResumeIdConverter(),
                new StringToTaskStateValueConverter(objectMapper),
                new StringToTriggerStateValueConverter(objectMapper),
                new StringToWorkflowExecutionIdConverter(),
                new StringToWorkflowTriggerConverter(objectMapper),
                new TaskStateValueToStringConverter(objectMapper),
                new TriggerStateValueToStringConverter(objectMapper),
                new WorkflowExecutionIdToStringConverter(),
                new WorkflowTriggerToStringConverter(objectMapper));
        }
    }
}
