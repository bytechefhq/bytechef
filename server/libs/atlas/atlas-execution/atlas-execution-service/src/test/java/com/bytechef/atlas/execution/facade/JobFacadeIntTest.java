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

package com.bytechef.atlas.execution.facade;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Ivica Cardic
 */
@SpringBootTest(
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
public class JobFacadeIntTest {

    @Autowired
    private JobFacade jobFacade;

    @Test
    public void testRequiredParameters() {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> jobFacade.createJob(new JobParametersDTO("aGVsbG8x", Collections.emptyMap())));
    }

    @ComponentScan(
        basePackages = {
            "com.bytechef.atlas.execution.facade", "com.bytechef.atlas.execution.repository.jdbc"
        })
    @EnableAutoConfiguration
    @Configuration
    public static class WorkflowExecutionIntTestConfiguration {

        @MockitoBean
        private ContextService contextService;

        @MockitoBean
        private WorkflowService workflowService;

        @MockitoBean
        private TaskExecutionService taskExecutionService;

        @MockitoBean
        private TaskFileStorage taskFileStorage;

        @Bean
        JobFacade jobFacade(ApplicationEventPublisher eventPublisher, JobService jobService) {
            return new JobFacadeImpl(
                eventPublisher, contextService, jobService, taskExecutionService, taskFileStorage, workflowService);
        }

        @Bean
        JobService jobService(JobRepository jobRepository) {
            return new JobServiceImpl(jobRepository);
        }

        @EnableJdbcRepositories(basePackages = "com.bytechef.atlas.execution.repository.jdbc")
        public static class WorkflowIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
