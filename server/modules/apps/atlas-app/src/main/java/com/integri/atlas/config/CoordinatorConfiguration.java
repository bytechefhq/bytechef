/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.config;

import com.integri.atlas.engine.coordinator.Coordinator;
import com.integri.atlas.engine.coordinator.CoordinatorImpl;
import com.integri.atlas.engine.coordinator.annotation.ConditionalOnCoordinator;
import com.integri.atlas.engine.coordinator.error.ErrorHandlerChain;
import com.integri.atlas.engine.coordinator.error.TaskExecutionErrorHandler;
import com.integri.atlas.engine.coordinator.event.ContextJobStatusEventListener;
import com.integri.atlas.engine.coordinator.event.ContextService;
import com.integri.atlas.engine.coordinator.event.JobStatusWebhookEventListener;
import com.integri.atlas.engine.coordinator.event.TaskProgressedEventListener;
import com.integri.atlas.engine.coordinator.event.TaskStartedEventListener;
import com.integri.atlas.engine.coordinator.event.TaskStartedWebhookEventListener;
import com.integri.atlas.engine.coordinator.job.executor.DefaultJobExecutor;
import com.integri.atlas.engine.coordinator.job.repository.JobRepository;
import com.integri.atlas.engine.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandlerChain;
import com.integri.atlas.engine.coordinator.task.dispatcher.ControlTaskDispatcher;
import com.integri.atlas.engine.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.integri.atlas.engine.coordinator.task.dispatcher.TaskDispatcherChain;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowRepository;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.counter.repository.CounterRepository;
import com.integri.atlas.engine.core.error.ErrorHandler;
import com.integri.atlas.engine.core.event.EventPublisher;
import com.integri.atlas.engine.core.message.broker.MessageBroker;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.core.task.evaluator.TaskEvaluator;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.task.dispatcher.each.EachTaskDispatcher;
import com.integri.atlas.task.dispatcher.each.completion.EachTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.fork.ForkTaskDispatcher;
import com.integri.atlas.task.dispatcher.fork.completion.ForkTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.if_.IfTaskDeclaration;
import com.integri.atlas.task.dispatcher.if_.IfTaskDispatcher;
import com.integri.atlas.task.dispatcher.if_.completion.IfTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.loop.LoopTaskDispatcher;
import com.integri.atlas.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.map.MapTaskDispatcher;
import com.integri.atlas.task.dispatcher.map.completion.MapTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.parallel.ParallelTaskDispatcher;
import com.integri.atlas.task.dispatcher.parallel.completion.ParallelTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.sequence.SequenceTaskDeclaration;
import com.integri.atlas.task.dispatcher.sequence.SequenceTaskDispatcher;
import com.integri.atlas.task.dispatcher.sequence.completion.SequenceTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.subflow.SubflowTaskDispatcher;
import com.integri.atlas.task.dispatcher.subflow.event.SubflowJobStatusEventListener;
import com.integri.atlas.task.dispatcher.switch_.SwitchTaskDispatcher;
import com.integri.atlas.task.dispatcher.switch_.completion.SwitchTaskCompletionHandler;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCoordinator
public class CoordinatorConfiguration {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private ContextRepository contextRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private MessageBroker messageBroker;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private TaskEvaluator taskEvaluator;

    @Bean
    Coordinator coordinator() {
        CoordinatorImpl coordinator = new CoordinatorImpl();

        coordinator.setContextRepository(contextRepository);
        coordinator.setEventPublisher(eventPublisher);
        coordinator.setJobRepository(jobRepository);
        coordinator.setJobTaskRepository(taskExecutionRepository);
        coordinator.setWorkflowRepository(workflowRepository);
        coordinator.setJobExecutor(jobExecutor());
        coordinator.setTaskDispatcher(taskDispatcher());
        coordinator.setErrorHandler(errorHandler());
        coordinator.setTaskCompletionHandler(taskCompletionHandler());
        coordinator.setMessageBroker(messageBroker);

        return coordinator;
    }

    @Bean
    ErrorHandler errorHandler() {
        return new ErrorHandlerChain(Arrays.asList(jobTaskErrorHandler()));
    }

    @Bean
    JobStatusWebhookEventListener jobStatusWebhookEventHandler() {
        return new JobStatusWebhookEventListener(jobRepository);
    }

    @Bean
    TaskExecutionErrorHandler jobTaskErrorHandler() {
        TaskExecutionErrorHandler jobTaskErrorHandler = new TaskExecutionErrorHandler();
        jobTaskErrorHandler.setJobRepository(jobRepository);
        jobTaskErrorHandler.setJobTaskRepository(taskExecutionRepository);
        jobTaskErrorHandler.setTaskDispatcher(taskDispatcher());
        jobTaskErrorHandler.setEventPublisher(eventPublisher);
        return jobTaskErrorHandler;
    }

    @Bean
    TaskCompletionHandlerChain taskCompletionHandler() {
        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();
        taskCompletionHandlerChain.setTaskCompletionHandlers(
            Arrays.asList(
                eachTaskCompletionHandler(taskCompletionHandlerChain),
                forkTaskCompletionHandler(taskCompletionHandlerChain),
                ifTaskCompletionHandler(taskCompletionHandlerChain),
                loopTaskCompletionHandler(taskCompletionHandlerChain),
                mapTaskCompletionHandler(taskCompletionHandlerChain),
                parallelTaskCompletionHandler(taskCompletionHandlerChain),
                sequenceTaskCompletionHandler(taskCompletionHandlerChain),
                switchTaskCompletionHandler(taskCompletionHandlerChain),
                defaultTaskCompletionHandler()
            )
        );
        return taskCompletionHandlerChain;
    }

    @Bean
    DefaultTaskCompletionHandler defaultTaskCompletionHandler() {
        DefaultTaskCompletionHandler taskCompletionHandler = new DefaultTaskCompletionHandler();
        taskCompletionHandler.setContextRepository(contextRepository);
        taskCompletionHandler.setJobExecutor(jobExecutor());
        taskCompletionHandler.setJobRepository(jobRepository);
        taskCompletionHandler.setJobTaskRepository(taskExecutionRepository);
        taskCompletionHandler.setWorkflowRepository(workflowRepository);
        taskCompletionHandler.setEventPublisher(eventPublisher);
        taskCompletionHandler.setTaskEvaluator(taskEvaluator);
        return taskCompletionHandler;
    }

    @Bean
    EachTaskCompletionHandler eachTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
        return new EachTaskCompletionHandler(taskExecutionRepository, aTaskCompletionHandler, counterRepository);
    }

    @Bean
    ForkTaskCompletionHandler forkTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
        return new ForkTaskCompletionHandler(
            taskExecutionRepository,
            aTaskCompletionHandler,
            counterRepository,
            taskDispatcher(),
            contextRepository,
            taskEvaluator
        );
    }

    @Bean
    IfTaskCompletionHandler ifTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
        return new IfTaskCompletionHandler(
            taskExecutionRepository,
            aTaskCompletionHandler,
            taskDispatcher(),
            contextRepository,
            taskEvaluator
        );
    }

    @Bean
    LoopTaskCompletionHandler loopTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new LoopTaskCompletionHandler(taskExecutionRepository, taskCompletionHandler, counterRepository);
    }

    @Bean
    MapTaskCompletionHandler mapTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
        return new MapTaskCompletionHandler(taskExecutionRepository, aTaskCompletionHandler, counterRepository);
    }

    @Bean
    ParallelTaskCompletionHandler parallelTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
        ParallelTaskCompletionHandler dispatcher = new ParallelTaskCompletionHandler();
        dispatcher.setCounterRepository(counterRepository);
        dispatcher.setTaskCompletionHandler(aTaskCompletionHandler);
        dispatcher.setTaskExecutionRepository(taskExecutionRepository);
        return dispatcher;
    }

    @Bean
    SequenceTaskCompletionHandler sequenceTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
        return new SequenceTaskCompletionHandler(
            taskExecutionRepository,
            aTaskCompletionHandler,
            taskDispatcher(),
            contextRepository,
            taskEvaluator
        );
    }

    @Bean
    SwitchTaskCompletionHandler switchTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
        return new SwitchTaskCompletionHandler(
            taskExecutionRepository,
            aTaskCompletionHandler,
            taskDispatcher(),
            contextRepository,
            taskEvaluator
        );
    }

    @Bean
    DefaultJobExecutor jobExecutor() {
        DefaultJobExecutor jobExecutor = new DefaultJobExecutor();
        jobExecutor.setContextRepository(contextRepository);
        jobExecutor.setJobTaskRepository(taskExecutionRepository);
        jobExecutor.setWorkflowRepository(workflowRepository);
        jobExecutor.setTaskDispatcher(taskDispatcher());
        jobExecutor.setTaskEvaluator(taskEvaluator);
        return jobExecutor;
    }

    @Bean
    TaskDispatcherChain taskDispatcher() {
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
            defaultTaskDispatcher()
        );

        taskDispatcher.setResolvers(resolvers);

        return taskDispatcher;
    }

    @Bean
    ContextJobStatusEventListener contextJobStatusEventHandler(ContextService contextService) {
        return new ContextJobStatusEventListener(contextService, jobRepository);
    }

    @Bean
    ContextService contextService() {
        return new ContextService(contextRepository, taskExecutionRepository);
    }

    @Bean
    ControlTaskDispatcher controlTaskDispatcher() {
        return new ControlTaskDispatcher(messageBroker);
    }

    @Bean
    DefaultTaskDispatcher defaultTaskDispatcher() {
        return new DefaultTaskDispatcher(messageBroker);
    }

    @Bean
    IfTaskDeclaration ifTaskDeclaration() {
        return new IfTaskDeclaration();
    }

    @Bean
    IfTaskDispatcher ifTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new IfTaskDispatcher(
            contextRepository,
            messageBroker,
            taskDispatcher,
            taskExecutionRepository,
            taskEvaluator
        );
    }

    @Bean
    EachTaskDispatcher eachTaskDispatcher(TaskDispatcher aTaskDispatcher) {
        return new EachTaskDispatcher(
            aTaskDispatcher,
            taskExecutionRepository,
            messageBroker,
            contextRepository,
            counterRepository,
            taskEvaluator
        );
    }

    @Bean
    ForkTaskDispatcher forkTaskDispatcher(TaskDispatcher aTaskDispatcher) {
        ForkTaskDispatcher forkTaskDispatcher = new ForkTaskDispatcher();
        forkTaskDispatcher.setTaskDispatcher(aTaskDispatcher);
        forkTaskDispatcher.setTaskEvaluator(taskEvaluator);
        forkTaskDispatcher.setTaskExecutionRepo(taskExecutionRepository);
        forkTaskDispatcher.setMessageBroker(messageBroker);
        forkTaskDispatcher.setContextRepository(contextRepository);
        forkTaskDispatcher.setCounterRepository(counterRepository);
        return forkTaskDispatcher;
    }

    @Bean
    LoopTaskDispatcher loopTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new LoopTaskDispatcher(
            taskDispatcher,
            taskExecutionRepository,
            messageBroker,
            contextRepository,
            counterRepository,
            taskEvaluator
        );
    }

    @Bean
    MapTaskDispatcher mapTaskDispatcher(TaskDispatcher aTaskDispatcher) {
        return MapTaskDispatcher
            .builder()
            .taskDispatcher(aTaskDispatcher)
            .taskExecutionRepository(taskExecutionRepository)
            .messageBroker(messageBroker)
            .contextRepository(contextRepository)
            .counterRepository(counterRepository)
            .taskEvaluator(taskEvaluator)
            .build();
    }

    @Bean
    ParallelTaskDispatcher parallelTaskDispatcher(TaskDispatcher aTaskDispatcher) {
        ParallelTaskDispatcher dispatcher = new ParallelTaskDispatcher();
        dispatcher.setContextRepository(contextRepository);
        dispatcher.setCounterRepository(counterRepository);
        dispatcher.setMessageBroker(messageBroker);
        dispatcher.setTaskDispatcher(aTaskDispatcher);
        dispatcher.setTaskExecutionRepository(taskExecutionRepository);
        return dispatcher;
    }

    @Bean
    SequenceTaskDeclaration sequenceTaskDeclaration() {
        return new SequenceTaskDeclaration();
    }

    @Bean
    SequenceTaskDispatcher sequenceTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new SequenceTaskDispatcher(
            contextRepository,
            messageBroker,
            taskDispatcher,
            taskExecutionRepository,
            taskEvaluator
        );
    }

    @Bean
    SubflowJobStatusEventListener subflowJobStatusEventListener() {
        return new SubflowJobStatusEventListener(jobRepository, taskExecutionRepository, coordinator(), taskEvaluator);
    }

    @Bean
    SubflowTaskDispatcher subflowTaskDispatcher() {
        return new SubflowTaskDispatcher(messageBroker);
    }

    @Bean
    SwitchTaskDispatcher switchTaskDispatcher(TaskDispatcher aTaskDispatcher) {
        return new SwitchTaskDispatcher(
            aTaskDispatcher,
            taskExecutionRepository,
            messageBroker,
            contextRepository,
            taskEvaluator
        );
    }

    @Bean
    TaskStartedEventListener taskStartedEventListener() {
        return new TaskStartedEventListener(taskExecutionRepository, taskDispatcher(), jobRepository);
    }

    @Bean
    TaskStartedWebhookEventListener taskStartedWebhookEventListener() {
        return new TaskStartedWebhookEventListener(jobRepository);
    }

    @Bean
    TaskProgressedEventListener taskProgressedEventListener() {
        return new TaskProgressedEventListener(taskExecutionRepository);
    }
}
