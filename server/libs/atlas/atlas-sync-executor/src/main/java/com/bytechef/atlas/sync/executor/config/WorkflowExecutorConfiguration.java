
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

package com.bytechef.atlas.sync.executor.config;

import com.bytechef.atlas.coordinator.event.EventListener;
import com.bytechef.atlas.coordinator.event.EventListenerChain;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.repository.WorkflowRepository;
import com.bytechef.atlas.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.service.impl.ContextServiceImpl;
import com.bytechef.atlas.service.impl.CounterServiceImpl;
import com.bytechef.atlas.service.impl.JobServiceImpl;
import com.bytechef.atlas.service.impl.TaskExecutionServiceImpl;
import com.bytechef.atlas.service.impl.WorkflowServiceImpl;
import com.bytechef.atlas.sync.executor.WorkflowExecutor;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistrar;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WorkflowExecutorConfiguration implements InitializingBean {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired(required = false)
    private List<TaskHandlerRegistrar> taskHandlerRegistrars = List.of();

    public void afterPropertiesSet() {
        for (TaskHandlerRegistrar taskHandlerRegistrar : taskHandlerRegistrars) {
            taskHandlerRegistrar.registerTaskHandlers(beanFactory);
        }
    }

    @Bean
    ContextService contextService() {
        return new ContextServiceImpl(new InMemoryContextRepository());
    }

    @Bean
    CounterService counterService() {
        return new CounterServiceImpl(new InMemoryCounterRepository());
    }

    @Bean
    JobService jobService(WorkflowRepository workflowRepository, ObjectMapper objectMapper) {
        return new JobServiceImpl(
            new InMemoryJobRepository(taskExecutionRepository(), objectMapper), workflowRepository);
    }

    @Bean
    TaskExecutionService taskExecutionService() {
        return new TaskExecutionServiceImpl(taskExecutionRepository());
    }

    @Bean
    InMemoryTaskExecutionRepository taskExecutionRepository() {
        return new InMemoryTaskExecutionRepository();
    }

    @Bean
    WorkflowService workflowService(WorkflowRepository workflowRepository) {
        return new WorkflowServiceImpl(workflowRepository);
    }

    @Bean
    WorkflowExecutor workflowExecutor(
        ContextService contextService,
        CounterService counterService,
        JobService jobService,
        EventPublisher eventPublisher,
        TaskExecutionService taskExecutionService,
        @Autowired(required = false) Map<String, TaskHandler<?>> taskHandlerMap,
        WorkflowService workflowService) {
        return new WorkflowExecutor(
            contextService,
            counterService,
            jobService,
            eventPublisher,
            taskExecutionService,
            taskHandlerMap == null ? Map.of() : taskHandlerMap,
            workflowService);
    }

    @Configuration
    public static class EventConfiguration {

        @Bean
        @Primary
        EventListenerChain eventListener(List<EventListener> eventListeners) {
            return new EventListenerChain(eventListeners);
        }

        @Bean
        EventPublisher eventPublisher(EventListener eventListener) {
            return eventListener::onApplicationEvent;
        }
    }

    @Configuration
    public static class MessageBrokerConfiguration {

        @Bean
        MessageBroker messageBroker() {
            return new SyncMessageBroker();
        }
    }
}
