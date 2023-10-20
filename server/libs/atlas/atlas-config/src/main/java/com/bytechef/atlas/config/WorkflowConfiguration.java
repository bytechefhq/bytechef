
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

package com.bytechef.atlas.config;

import com.bytechef.event.EventPublisher;
import com.bytechef.atlas.facade.TaskExecutionFacade;
import com.bytechef.atlas.facade.TaskExecutionFacadeImpl;
import com.bytechef.atlas.job.JobFactory;
import com.bytechef.atlas.job.JobFactoryImpl;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.repository.ContextRepository;
import com.bytechef.atlas.repository.CounterRepository;
import com.bytechef.atlas.repository.JobRepository;
import com.bytechef.atlas.repository.TaskExecutionRepository;
import com.bytechef.atlas.repository.WorkflowCrudRepository;
import com.bytechef.atlas.repository.WorkflowRepository;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.service.ContextServiceImpl;
import com.bytechef.atlas.service.CounterServiceImpl;
import com.bytechef.atlas.service.JobServiceImpl;
import com.bytechef.atlas.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.service.WorkflowServiceImpl;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkflowConfiguration {

    @Bean
    ContextService contextService(ContextRepository contextRepository) {
        return new ContextServiceImpl(contextRepository);
    }

    @Bean
    CounterService counterService(CounterRepository counterRepository) {
        return new CounterServiceImpl(counterRepository);
    }

    @Bean
    JobFactory jobFactory(
        ContextService contextService, EventPublisher eventPublisher, JobService jobService,
        MessageBroker messageBroker) {

        return new JobFactoryImpl(contextService, eventPublisher, jobService, messageBroker);
    }

    @Bean
    JobService jobService(JobRepository jobRepository, List<WorkflowRepository> workflowRepositories) {
        return new JobServiceImpl(jobRepository, workflowRepositories);
    }

    @Bean
    TaskExecutionFacade taskExecutionFacade(ContextService contextService, TaskExecutionService taskExecutionService) {
        return new TaskExecutionFacadeImpl(contextService, taskExecutionService);
    }

    @Bean
    TaskExecutionService taskExecutionService(TaskExecutionRepository taskExecutionRepository) {
        return new TaskExecutionServiceImpl(taskExecutionRepository);
    }

    @Bean
    WorkflowService workflowService(
        CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
        List<WorkflowRepository> workflowRepositories) {

        return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
    }
}
