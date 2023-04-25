
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.coordinator.config;

import com.bytechef.atlas.coordinator.TaskCoordinator;
import com.bytechef.atlas.coordinator.error.TaskExecutionErrorHandler;
import com.bytechef.atlas.coordinator.event.JobStatusWebhookEventListener;
import com.bytechef.atlas.coordinator.event.TaskProgressedEventListener;
import com.bytechef.atlas.coordinator.event.TaskStartedEventListener;
import com.bytechef.atlas.coordinator.event.TaskStartedWebhookEventListener;
import com.bytechef.atlas.coordinator.job.executor.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.ControlTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.event.EventPublisher;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.autoconfigure.annotation.ConditionalOnCoordinator;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCoordinator
@EnableConfigurationProperties(TaskCoordinatorProperties.class)
public class TaskCoordinatorConfiguration {

    @Autowired
    private ContextService contextService;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private JobService jobService;

    @Autowired
    private MessageBroker messageBroker;

    @Autowired(required = false)
    private List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories = Collections.emptyList();

    @Autowired(required = false)
    private List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories = Collections.emptyList();

    @Autowired(required = false)
    private List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors = Collections.emptyList();

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private WorkflowService workflowService;

    @Bean
    ControlTaskDispatcher controlTaskDispatcher() {
        return new ControlTaskDispatcher(messageBroker);
    }

    @Bean
    DefaultTaskCompletionHandler defaultTaskCompletionHandler() {
        return new DefaultTaskCompletionHandler(
            contextService, eventPublisher, jobExecutor(), jobService, taskExecutionService,
            workflowService);
    }

    @Bean
    DefaultTaskDispatcher defaultTaskDispatcher() {
        return new DefaultTaskDispatcher(messageBroker, taskDispatcherPreSendProcessors);
    }

    @Bean
    JobExecutor jobExecutor() {
        return new JobExecutor(contextService, taskDispatcher(), taskExecutionService, workflowService);
    }

    @Bean
    JobStatusWebhookEventListener jobStatusWebhookEventHandler() {
        return new JobStatusWebhookEventListener(jobService);
    }

    @Bean
    TaskExecutionErrorHandler taskExecutionErrorHandler() {
        return new TaskExecutionErrorHandler(
            eventPublisher, jobService, taskDispatcher(), taskExecutionService);
    }

    @Bean
    @Primary
    TaskCompletionHandler taskCompletionHandler() {
        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        Stream<TaskCompletionHandler> taskCompletionHandlerStream = Stream.concat(
            taskCompletionHandlerFactories.stream()
                .map(taskCompletionHandlerFactory -> taskCompletionHandlerFactory.createTaskCompletionHandler(
                    taskCompletionHandlerChain, taskDispatcher())),
            Stream.of(defaultTaskCompletionHandler()));

        taskCompletionHandlerChain.setTaskCompletionHandlers(taskCompletionHandlerStream.toList());

        return taskCompletionHandlerChain;
    }

    @Bean
    TaskCoordinator taskCoordinator() {
        return TaskCoordinator.builder()
            .eventPublisher(eventPublisher)
            .jobExecutor(jobExecutor())
            .jobService(jobService)
            .messageBroker(messageBroker)
            .taskCompletionHandler(taskCompletionHandler())
            .taskDispatcher(taskDispatcher())
            .taskExecutionService(taskExecutionService)
            .build();
    }

    @Bean
    @Primary
    TaskDispatcher<? super Task> taskDispatcher() {
        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        List<TaskDispatcherResolver> resolvers = Stream.concat(
            taskDispatcherResolverFactories.stream()
                .map(taskDispatcherFactory -> taskDispatcherFactory.createTaskDispatcherResolver(taskDispatcherChain)),
            Stream.of(controlTaskDispatcher(), defaultTaskDispatcher()))
            .toList();

        taskDispatcherChain.setTaskDispatcherResolvers(resolvers);

        return taskDispatcherChain;
    }

    @Bean
    TaskProgressedEventListener taskProgressedEventListener() {
        return new TaskProgressedEventListener(taskExecutionService);
    }

    @Bean
    TaskStartedEventListener taskStartedEventListener() {
        return new TaskStartedEventListener(taskExecutionService, taskDispatcher(), jobService);
    }

    @Bean
    TaskStartedWebhookEventListener taskStartedWebhookEventListener() {
        return new TaskStartedWebhookEventListener(jobService);
    }
}
