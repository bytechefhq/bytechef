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

import com.bytechef.atlas.coordinator.Coordinator;
import com.bytechef.atlas.coordinator.CoordinatorImpl;
import com.bytechef.atlas.coordinator.error.ErrorHandlerChain;
import com.bytechef.atlas.coordinator.error.TaskExecutionErrorHandler;
import com.bytechef.atlas.coordinator.event.DeleteContextEventListener;
import com.bytechef.atlas.coordinator.event.JobStatusWebhookEventListener;
import com.bytechef.atlas.coordinator.event.TaskProgressedEventListener;
import com.bytechef.atlas.coordinator.event.TaskStartedEventListener;
import com.bytechef.atlas.coordinator.event.TaskStartedWebhookEventListener;
import com.bytechef.atlas.coordinator.job.executor.DefaultJobExecutor;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.ControlTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.error.ErrorHandler;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.service.context.ContextService;
import com.bytechef.atlas.service.job.JobService;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.service.workflow.WorkflowService;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
public class CoordinatorConfiguration {

    @Autowired
    private ContextService contextService;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private Environment environment;

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
    private TaskEvaluator taskEvaluator;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private WorkflowService workflowService;

    @Bean
    @ConditionalOnProperty(name = "atlas.context.delete-listener.enabled", havingValue = "true")
    DeleteContextEventListener deleteContextEventListener(ContextService contextService) {
        return new DeleteContextEventListener(contextService, jobService);
    }

    @Bean
    ControlTaskDispatcher controlTaskDispatcher() {
        return new ControlTaskDispatcher(messageBroker);
    }

    @Bean
    Coordinator coordinator() {
        CoordinatorImpl coordinator = new CoordinatorImpl();

        coordinator.setContextService(contextService);
        coordinator.setErrorHandler(errorHandler());
        coordinator.setEventPublisher(eventPublisher);
        coordinator.setJobService(jobService);
        coordinator.setJobExecutor(jobExecutor());
        coordinator.setMessageBroker(messageBroker);
        coordinator.setTaskDispatcher(taskDispatcher());
        coordinator.setTaskExecutionService(taskExecutionService);
        coordinator.setTaskCompletionHandler(taskCompletionHandler());

        return coordinator;
    }

    @Bean
    DefaultTaskCompletionHandler defaultTaskCompletionHandler() {
        DefaultTaskCompletionHandler taskCompletionHandler = new DefaultTaskCompletionHandler();

        taskCompletionHandler.setContextService(contextService);
        taskCompletionHandler.setJobExecutor(jobExecutor());
        taskCompletionHandler.setJobService(jobService);
        taskCompletionHandler.setTaskExecutionService(taskExecutionService);
        taskCompletionHandler.setWorkflowService(workflowService);
        taskCompletionHandler.setEventPublisher(eventPublisher);
        taskCompletionHandler.setTaskEvaluator(taskEvaluator);

        return taskCompletionHandler;
    }

    @Bean
    DefaultTaskDispatcher defaultTaskDispatcher() {
        return new DefaultTaskDispatcher(messageBroker, taskDispatcherPreSendProcessors);
    }

    @Bean
    @Primary
    ErrorHandler<?> errorHandler() {
        return new ErrorHandlerChain(List.of(jobTaskErrorHandler()));
    }

    @Bean
    DefaultJobExecutor jobExecutor() {
        DefaultJobExecutor jobExecutor = new DefaultJobExecutor();

        jobExecutor.setContextService(contextService);
        jobExecutor.setTaskExecutionService(taskExecutionService);
        jobExecutor.setWorkflowService(workflowService);
        jobExecutor.setTaskDispatcher(taskDispatcher());
        jobExecutor.setTaskEvaluator(taskEvaluator);

        return jobExecutor;
    }

    @Bean
    JobStatusWebhookEventListener jobStatusWebhookEventHandler() {
        return new JobStatusWebhookEventListener(jobService);
    }

    @Bean
    TaskExecutionErrorHandler jobTaskErrorHandler() {
        TaskExecutionErrorHandler jobTaskErrorHandler = new TaskExecutionErrorHandler();

        jobTaskErrorHandler.setJobService(jobService);
        jobTaskErrorHandler.setTaskExecutionService(taskExecutionService);
        jobTaskErrorHandler.setTaskDispatcher(taskDispatcher());
        jobTaskErrorHandler.setEventPublisher(eventPublisher);

        return jobTaskErrorHandler;
    }

    @Bean
    @Primary
    TaskCompletionHandler taskCompletionHandler() {
        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(Stream.concat(
                        taskCompletionHandlerFactories.stream()
                                .map(taskCompletionHandlerFactory ->
                                        taskCompletionHandlerFactory.createTaskCompletionHandler(
                                                taskCompletionHandlerChain, taskDispatcher())),
                        Stream.of(defaultTaskCompletionHandler()))
                .toList());

        return taskCompletionHandlerChain;
    }

    @Bean
    @Primary
    TaskDispatcher<?> taskDispatcher() {
        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        List<TaskDispatcherResolver> resolvers = Stream.concat(
                        taskDispatcherResolverFactories.stream()
                                .map(taskDispatcherFactory ->
                                        taskDispatcherFactory.createTaskDispatcherResolver(taskDispatcherChain)),
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
