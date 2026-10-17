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

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacadeImpl;
import com.bytechef.platform.workflow.execution.repository.PrincipalJobRepository;
import com.bytechef.platform.workflow.execution.service.PrincipalJobServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
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
    excludeName = "com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionJdbcRepositoryConfiguration")
@EnableJdbcRepositories(
    basePackageClasses = PrincipalJobRepository.class,
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PrincipalJobRepository.class))
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PrincipalJobFacadeImpl.class,
    PrincipalJobServiceImpl.class
})
public class PrincipalJobFacadeIntTestConfiguration {

    @Bean
    JobFacade jobFacade(
        ApplicationEventPublisher eventPublisher, ContextService contextService, JobService jobService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        return new JobFacadeImpl(
            eventPublisher, contextService, jobService, taskExecutionService, taskFileStorage, workflowService);
    }

    @Bean
    JobService jobService(JobRepository jobRepository) {
        return new JobServiceImpl(jobRepository);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class PrincipalJobFacadeIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI")
        public PrincipalJobFacadeIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
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
