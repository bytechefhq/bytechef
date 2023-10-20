
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
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.event.listener.ErrorEventListener;
import com.bytechef.atlas.coordinator.event.listener.LogTaskApplicationEventListener;
import com.bytechef.atlas.coordinator.event.listener.TaskExecutionErrorEventListener;
import com.bytechef.atlas.coordinator.event.listener.TaskProgressedApplicationEventListener;
import com.bytechef.atlas.coordinator.event.listener.TaskStartedApplicationEventListener;
import com.bytechef.atlas.coordinator.event.listener.WebhookJobStatusApplicationEventListener;
import com.bytechef.atlas.coordinator.event.listener.WebhookTaskStartedApplicationEventListener;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.ControlTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.execution.service.RemoteContextService;

import com.bytechef.atlas.execution.service.RemoteJobService;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.configuration.service.RemoteWorkflowService;
import com.bytechef.atlas.configuration.task.Task;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "coordinator.enabled", matchIfMissing = true)
@EnableConfigurationProperties(TaskCoordinatorProperties.class)
public class TaskCoordinatorConfiguration {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RemoteContextService contextService;

    @Autowired
    private RemoteJobService jobService;

    @Autowired(required = false)
    private List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories = Collections.emptyList();

    @Autowired(required = false)
    private List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories = Collections.emptyList();

    @Autowired(required = false)
    private List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors = Collections.emptyList();

    @Autowired
    private RemoteTaskExecutionService taskExecutionService;

    @Autowired
    @Qualifier("workflowAsyncFileStorageFacade")
    private WorkflowFileStorageFacade workflowFileStorageFacade;

    @Autowired
    private RemoteWorkflowService workflowService;

    @Bean
    ControlTaskDispatcher controlTaskDispatcher() {
        return new ControlTaskDispatcher(eventPublisher);
    }

    @Bean
    DefaultTaskCompletionHandler defaultTaskCompletionHandler() {
        return new DefaultTaskCompletionHandler(
            contextService, eventPublisher, jobExecutor(), jobService, taskExecutionService,
            workflowFileStorageFacade, workflowService);
    }

    @Bean
    DefaultTaskDispatcher defaultTaskDispatcher() {
        return new DefaultTaskDispatcher(eventPublisher, taskDispatcherPreSendProcessors);
    }

    @Bean
    JobExecutor jobExecutor() {
        return new JobExecutor(
            contextService, taskDispatcher(), taskExecutionService, workflowFileStorageFacade, workflowService);
    }

    @Bean
    LogTaskApplicationEventListener logTaskApplicationEventListener() {
        return new LogTaskApplicationEventListener();
    }

    @Bean
    WebhookJobStatusApplicationEventListener jobStatusWebhookEventHandler() {
        return new WebhookJobStatusApplicationEventListener(jobService);
    }

    @Bean
    TaskExecutionErrorEventListener taskExecutionErrorHandler() {
        return new TaskExecutionErrorEventListener(eventPublisher, jobService, taskDispatcher(), taskExecutionService);
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
    TaskCoordinator taskCoordinator(
        List<ApplicationEventListener> applicationEventListeners, List<ErrorEventListener> errorEventListeners) {

        return new TaskCoordinator(
            applicationEventListeners, errorEventListeners, eventPublisher, jobExecutor(), jobService,
            taskCompletionHandler(), taskDispatcher(), taskExecutionService);
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
    TaskProgressedApplicationEventListener taskProgressedEventListener() {
        return new TaskProgressedApplicationEventListener(taskExecutionService);
    }

    @Bean
    TaskStartedApplicationEventListener taskStartedEventListener() {
        return new TaskStartedApplicationEventListener(taskExecutionService, taskDispatcher(), jobService);
    }

    @Bean
    WebhookTaskStartedApplicationEventListener taskStartedWebhookEventListener() {
        return new WebhookTaskStartedApplicationEventListener(jobService);
    }
}
