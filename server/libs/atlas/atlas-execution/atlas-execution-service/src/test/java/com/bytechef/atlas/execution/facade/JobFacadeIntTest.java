
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.config.WorkflowExecutionIntTestConfiguration;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Collections;

/**
 * Ivica Cardic
 */
@SpringBootTest(
    classes = {
        WorkflowExecutionIntTestConfiguration.class
    },
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
public class JobFacadeIntTest {

    @Autowired
    private JobFacade jobFacade;

    @Test
    public void testRequiredParameters() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> jobFacade.createJob(new JobParameters("aGVsbG8x", Collections.emptyMap())));
    }

    @TestConfiguration
    public static class JobFactoryIntTestConfiguration {

        @MockBean
        private ContextService contextService;

        @MockBean
        private WorkflowService workflowService;

        @MockBean
        private WorkflowFileStorageFacade workflowFileStorageFacade;

        @Bean
        JobFacade jobFacade(JobService jobService, MessageBroker messageBroker) {
            return new JobFacadeImpl(
                contextService, e -> {}, jobService, messageBroker, workflowFileStorageFacade, workflowService);
        }

        @Bean
        JobService jobService(JobRepository jobRepository) {
            return new JobServiceImpl(jobRepository);
        }
    }
}
