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

package com.bytechef.config;

import com.bytechef.atlas.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.context.repository.ContextRepository;
import com.bytechef.atlas.context.service.ContextService;
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
import com.bytechef.atlas.coordinator.task.dispatcher.ControlTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.counter.repository.CounterRepository;
import com.bytechef.atlas.counter.service.CounterService;
import com.bytechef.atlas.error.ErrorHandler;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.job.repository.JobRepository;
import com.bytechef.atlas.job.service.JobService;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.task.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.workflow.repository.WorkflowRepository;
import com.bytechef.atlas.workflow.service.WorkflowService;
import com.bytechef.task.dispatcher.each.EachTaskDispatcher;
import com.bytechef.task.dispatcher.each.completion.EachTaskCompletionHandler;
import com.bytechef.task.dispatcher.fork.ForkTaskDispatcher;
import com.bytechef.task.dispatcher.fork.completion.ForkTaskCompletionHandler;
import com.bytechef.task.dispatcher.if_.IfTaskDescriptorHandler;
import com.bytechef.task.dispatcher.if_.IfTaskDispatcher;
import com.bytechef.task.dispatcher.if_.completion.IfTaskCompletionHandler;
import com.bytechef.task.dispatcher.loop.LoopBreakTaskDescriptorHandler;
import com.bytechef.task.dispatcher.loop.LoopBreakTaskDispatcher;
import com.bytechef.task.dispatcher.loop.LoopTaskDescriptorHandler;
import com.bytechef.task.dispatcher.loop.LoopTaskDispatcher;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import com.bytechef.task.dispatcher.map.MapTaskDispatcher;
import com.bytechef.task.dispatcher.map.completion.MapTaskCompletionHandler;
import com.bytechef.task.dispatcher.parallel.ParallelTaskDispatcher;
import com.bytechef.task.dispatcher.parallel.completion.ParallelTaskCompletionHandler;
import com.bytechef.task.dispatcher.sequence.SequenceTaskDescriptorHandler;
import com.bytechef.task.dispatcher.sequence.SequenceTaskDispatcher;
import com.bytechef.task.dispatcher.sequence.completion.SequenceTaskCompletionHandler;
import com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcher;
import com.bytechef.task.dispatcher.subflow.event.SubflowJobStatusEventListener;
import com.bytechef.task.dispatcher.switch_.SwitchTaskDispatcher;
import com.bytechef.task.dispatcher.switch_.completion.SwitchTaskCompletionHandler;
import com.bytechef.task.execution.service.TaskExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCoordinator
public class CoordinatorConfiguration {

    @Autowired
    private ContextService contextService;

    @Autowired
    private CounterService counterService;

    @Autowired
    private Environment environment;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private JobService jobService;

    @Autowired
    private MessageBroker messageBroker;

    @Autowired(required = false)
    private List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors;

    @Autowired
    private TaskExecutionService taskExecutionService;

    private TaskEvaluator taskEvaluator;

    @Autowired
    private WorkflowService workflowService;

    @PostConstruct
    private void afterPropertiesSet() {
        taskEvaluator = SpelTaskEvaluator.builder().environment(environment).build();
    }

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
    EachTaskCompletionHandler eachTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new EachTaskCompletionHandler(taskExecutionService, taskCompletionHandler, counterService);
    }

    @Bean
    EachTaskDispatcher eachTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new EachTaskDispatcher(
                taskDispatcher, taskExecutionService, messageBroker, contextService, counterService, taskEvaluator);
    }

    @Bean
    @Primary
    ErrorHandler<?> errorHandler() {
        return new ErrorHandlerChain(List.of(jobTaskErrorHandler()));
    }

    @Bean
    ForkTaskCompletionHandler forkTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new ForkTaskCompletionHandler(
                taskExecutionService,
                taskCompletionHandler,
                counterService,
                taskDispatcher(),
                contextService,
                taskEvaluator);
    }

    @Bean
    ForkTaskDispatcher forkTaskDispatcher(TaskDispatcher taskDispatcher) {
        ForkTaskDispatcher forkTaskDispatcher = new ForkTaskDispatcher();

        forkTaskDispatcher.setTaskDispatcher(taskDispatcher);
        forkTaskDispatcher.setTaskEvaluator(taskEvaluator);
        forkTaskDispatcher.setTaskExecutionService(taskExecutionService);
        forkTaskDispatcher.setMessageBroker(messageBroker);
        forkTaskDispatcher.setContextService(contextService);
        forkTaskDispatcher.setCounterService(counterService);

        return forkTaskDispatcher;
    }

    @Bean
    IfTaskCompletionHandler ifTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new IfTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher(), taskEvaluator, taskExecutionService);
    }

    @Bean
    IfTaskDescriptorHandler ifTaskDescriptorHandler() {
        return new IfTaskDescriptorHandler();
    }

    @Bean
    IfTaskDispatcher ifTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new IfTaskDispatcher(contextService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService);
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
    LoopBreakTaskDescriptorHandler loopBreakTaskDescriptorHandler() {
        return new LoopBreakTaskDescriptorHandler();
    }

    @Bean
    LoopBreakTaskDispatcher loopBreakTaskDispatcher() {
        return new LoopBreakTaskDispatcher(messageBroker, taskExecutionService);
    }

    @Bean
    LoopTaskCompletionHandler loopTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new LoopTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher(), taskEvaluator, taskExecutionService);
    }

    @Bean
    LoopTaskDescriptorHandler loopTaskDescriptorHandler() {
        return new LoopTaskDescriptorHandler();
    }

    @Bean
    LoopTaskDispatcher loopTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new LoopTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService);
    }

    @Bean
    MapTaskCompletionHandler mapTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new MapTaskCompletionHandler(taskExecutionService, taskCompletionHandler, counterService);
    }

    @Bean
    MapTaskDispatcher mapTaskDispatcher(TaskDispatcher taskDispatcher) {
        return MapTaskDispatcher.builder()
                .taskDispatcher(taskDispatcher)
                .taskExecutionService(taskExecutionService)
                .messageBroker(messageBroker)
                .contextService(contextService)
                .counterService(counterService)
                .taskEvaluator(taskEvaluator)
                .build();
    }

    @Bean
    ParallelTaskCompletionHandler parallelTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        ParallelTaskCompletionHandler dispatcher = new ParallelTaskCompletionHandler();

        dispatcher.setCounterService(counterService);
        dispatcher.setTaskCompletionHandler(taskCompletionHandler);
        dispatcher.setTaskExecutionService(taskExecutionService);

        return dispatcher;
    }

    @Bean
    ParallelTaskDispatcher parallelTaskDispatcher(TaskDispatcher taskDispatcher) {
        ParallelTaskDispatcher dispatcher = new ParallelTaskDispatcher();

        dispatcher.setContextService(contextService);
        dispatcher.setCounterService(counterService);
        dispatcher.setMessageBroker(messageBroker);
        dispatcher.setTaskDispatcher(taskDispatcher);
        dispatcher.setTaskExecutionService(taskExecutionService);

        return dispatcher;
    }

    @Bean
    SequenceTaskCompletionHandler sequenceTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new SequenceTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher(), taskEvaluator, taskExecutionService);
    }

    @Bean
    SequenceTaskDescriptorHandler sequenceTaskDescriptorHandler() {
        return new SequenceTaskDescriptorHandler();
    }

    @Bean
    SequenceTaskDispatcher sequenceTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new SequenceTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService);
    }

    @Bean
    SubflowJobStatusEventListener subflowJobStatusEventListener() {
        return new SubflowJobStatusEventListener(jobService, taskExecutionService, coordinator(), taskEvaluator);
    }

    @Bean
    SubflowTaskDispatcher subflowTaskDispatcher() {
        return new SubflowTaskDispatcher(messageBroker);
    }

    @Bean
    SwitchTaskCompletionHandler switchTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new SwitchTaskCompletionHandler(
                contextService, taskExecutionService, taskCompletionHandler, taskDispatcher(), taskEvaluator);
    }

    @Bean
    SwitchTaskDispatcher switchTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new SwitchTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskExecutionService, taskEvaluator);
    }

    @Bean
    @Primary
    TaskCompletionHandler taskCompletionHandler() {
        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(Arrays.asList(
                eachTaskCompletionHandler(taskCompletionHandlerChain),
                forkTaskCompletionHandler(taskCompletionHandlerChain),
                ifTaskCompletionHandler(taskCompletionHandlerChain),
                loopTaskCompletionHandler(taskCompletionHandlerChain),
                mapTaskCompletionHandler(taskCompletionHandlerChain),
                parallelTaskCompletionHandler(taskCompletionHandlerChain),
                sequenceTaskCompletionHandler(taskCompletionHandlerChain),
                switchTaskCompletionHandler(taskCompletionHandlerChain),
                defaultTaskCompletionHandler()));

        return taskCompletionHandlerChain;
    }

    @Bean
    @Primary
    TaskDispatcher<?> taskDispatcher() {
        TaskDispatcherChain taskDispatcher = new TaskDispatcherChain();

        List<TaskDispatcherResolver> resolvers = Arrays.asList(
                eachTaskDispatcher(taskDispatcher),
                ifTaskDispatcher(taskDispatcher),
                loopTaskDispatcher(taskDispatcher),
                forkTaskDispatcher(taskDispatcher),
                mapTaskDispatcher(taskDispatcher),
                parallelTaskDispatcher(taskDispatcher),
                sequenceTaskDispatcher(taskDispatcher),
                switchTaskDispatcher(taskDispatcher),
                controlTaskDispatcher(),
                subflowTaskDispatcher(),
                defaultTaskDispatcher());

        taskDispatcher.setTaskDispatcherResolvers(resolvers);

        return taskDispatcher;
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

    @Configuration
    public static class ServiceCoordinatorConfiguration {

        @Bean
        public ContextService contextService(
                ContextRepository contextRepository, TaskExecutionRepository taskExecutionRepository) {
            return new ContextService(contextRepository, taskExecutionRepository);
        }

        @Bean
        public CounterService counterService(CounterRepository counterRepository) {
            return new CounterService(counterRepository);
        }

        @Bean
        public JobService jobService(
                JobRepository jobRepository,
                TaskExecutionRepository taskExecutionRepository,
                WorkflowRepository workflowRepository) {
            return new JobService(jobRepository, taskExecutionRepository, workflowRepository);
        }

        @Bean
        public TaskExecutionService taskExecutionService(TaskExecutionRepository taskExecutionRepository) {
            return new TaskExecutionService(taskExecutionRepository);
        }

        @Bean
        public WorkflowService workflowService(WorkflowRepository workflowRepository) {
            return new WorkflowService(workflowRepository);
        }
    }
}
