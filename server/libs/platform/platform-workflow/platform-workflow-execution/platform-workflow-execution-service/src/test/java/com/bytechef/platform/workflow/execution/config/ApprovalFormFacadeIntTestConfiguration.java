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

import static org.mockito.Mockito.mock;

import com.bytechef.commons.data.jdbc.converter.StringToTypedValueConverter;
import com.bytechef.commons.data.jdbc.converter.TypedValueToStringConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.facade.StateValueDataStorage;
import com.bytechef.platform.workflow.execution.facade.TaskStateFacadeImpl;
import com.bytechef.platform.workflow.execution.repository.TaskStateRepository;
import com.bytechef.platform.workflow.execution.repository.converter.JobResumeIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToJobResumeIdConverter;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TaskStateServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableAutoConfiguration(
    excludeName = {
        "com.bytechef.atlas.execution.repository.jdbc.config.AtlasExecutionJdbcRepositoryConfiguration",
        "com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionJdbcRepositoryConfiguration"
    })
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableJdbcRepositories(
    basePackageClasses = TaskStateRepository.class,
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TaskStateRepository.class))
@Import({
    JacksonConfiguration.class, JobPrincipalAccessorRegistry.class, LiquibaseConfiguration.class,
    StateValueDataStorage.class, TaskStateFacadeImpl.class, TaskStateServiceImpl.class
})
public class ApprovalFormFacadeIntTestConfiguration {

    @Bean
    PrincipalJobService principalJobService() {
        return mock(PrincipalJobService.class);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class ApprovalFormFacadeIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public ApprovalFormFacadeIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                new JobResumeIdToStringConverter(), new StringToJobResumeIdConverter(),
                new StringToTypedValueConverter(objectMapper), new TypedValueToStringConverter(objectMapper));
        }
    }
}
