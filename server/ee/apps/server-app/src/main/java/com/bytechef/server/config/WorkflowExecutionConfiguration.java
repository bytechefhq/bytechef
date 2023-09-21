
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

package com.bytechef.server.config;

import com.bytechef.atlas.configuration.service.RemoteWorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.facade.RemoteJobFacade;
import com.bytechef.atlas.execution.repository.ContextRepository;
import com.bytechef.atlas.execution.repository.CounterRepository;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.CounterServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.RemoteContextService;
import com.bytechef.atlas.execution.service.RemoteCounterService;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.event.EventPublisher;
import com.bytechef.message.broker.MessageBroker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkflowExecutionConfiguration {

    @Bean
    ContextService contextService(ContextRepository contextRepository) {
        return new ContextServiceImpl(contextRepository);
    }

    @Bean
    JobFacade jobFacade(
        ContextService contextService, EventPublisher eventPublisher, JobService jobService,
        MessageBroker messageBroker,
        @Qualifier("workflowAsyncFileStorageFacade") WorkflowFileStorageFacade workflowFileStorageFacade,
        RemoteWorkflowService workflowService) {

        return new JobFacadeImpl(
            contextService, eventPublisher, jobService, messageBroker, workflowFileStorageFacade, workflowService);
    }

    @Bean
    JobService jobService(JobRepository jobRepository) {
        return new JobServiceImpl(jobRepository);
    }

    @Bean
    RemoteContextService remoteContextService(ContextRepository contextRepository) {
        return new ContextServiceImpl(contextRepository);
    }

    @Bean
    RemoteCounterService remoteCounterService(CounterRepository counterRepository) {
        return new CounterServiceImpl(counterRepository);
    }

    @Bean
    RemoteJobFacade remoteJobFacade(
        ContextService contextService, EventPublisher eventPublisher, JobService jobService,
        MessageBroker messageBroker,
        @Qualifier("workflowAsyncFileStorageFacade") WorkflowFileStorageFacade workflowFileStorageFacade,
        RemoteWorkflowService workflowService) {

        return new JobFacadeImpl(
            contextService, eventPublisher, jobService, messageBroker, workflowFileStorageFacade, workflowService);
    }

    @Bean
    RemoteTaskExecutionService remoteTaskExecutionService(TaskExecutionRepository taskExecutionRepository) {
        return new TaskExecutionServiceImpl(taskExecutionRepository);
    }

    @Bean
    TaskExecutionService taskExecutionService(TaskExecutionRepository taskExecutionRepository) {
        return new TaskExecutionServiceImpl(taskExecutionRepository);
    }
}
