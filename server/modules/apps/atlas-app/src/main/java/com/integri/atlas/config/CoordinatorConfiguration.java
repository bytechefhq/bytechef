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

package com.integri.atlas.config;

import com.integri.atlas.context.service.ContextService;
import com.integri.atlas.engine.annotation.ConditionalOnCoordinator;
import com.integri.atlas.engine.context.repository.ContextRepository;
import com.integri.atlas.engine.coordinator.Coordinator;
import com.integri.atlas.engine.coordinator.CoordinatorImpl;
import com.integri.atlas.engine.coordinator.error.ErrorHandlerChain;
import com.integri.atlas.engine.coordinator.error.TaskExecutionErrorHandler;
import com.integri.atlas.engine.coordinator.event.DeleteContextEventListener;
import com.integri.atlas.engine.coordinator.event.JobStatusWebhookEventListener;
import com.integri.atlas.engine.coordinator.event.TaskProgressedEventListener;
import com.integri.atlas.engine.coordinator.event.TaskStartedEventListener;
import com.integri.atlas.engine.coordinator.event.TaskStartedWebhookEventListener;
import com.integri.atlas.engine.coordinator.job.executor.DefaultJobExecutor;
import com.integri.atlas.engine.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandlerChain;
import com.integri.atlas.engine.coordinator.task.dispatcher.ControlTaskDispatcher;
import com.integri.atlas.engine.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.integri.atlas.engine.coordinator.task.dispatcher.TaskDispatcherChain;
import com.integri.atlas.engine.counter.repository.CounterRepository;
import com.integri.atlas.engine.error.ErrorHandler;
import com.integri.atlas.engine.event.EventPublisher;
import com.integri.atlas.engine.job.repository.JobRepository;
import com.integri.atlas.engine.job.service.JobService;
import com.integri.atlas.engine.message.broker.MessageBroker;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.task.execution.evaluator.TaskEvaluator;
import com.integri.atlas.engine.task.execution.evaluator.spel.SpelTaskEvaluator;
import com.integri.atlas.engine.task.execution.repository.TaskExecutionRepository;
import com.integri.atlas.engine.workflow.repository.WorkflowRepository;
import com.integri.atlas.task.dispatcher.each.EachTaskDispatcher;
import com.integri.atlas.task.dispatcher.each.completion.EachTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.fork.ForkTaskDispatcher;
import com.integri.atlas.task.dispatcher.fork.completion.ForkTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.if_.IfTaskDefinitionHandler;
import com.integri.atlas.task.dispatcher.if_.IfTaskDispatcher;
import com.integri.atlas.task.dispatcher.if_.completion.IfTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.loop.LoopBreakTaskDefinitionHandler;
import com.integri.atlas.task.dispatcher.loop.LoopBreakTaskDispatcher;
import com.integri.atlas.task.dispatcher.loop.LoopTaskDefinitionHandler;
import com.integri.atlas.task.dispatcher.loop.LoopTaskDispatcher;
import com.integri.atlas.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.map.MapTaskDispatcher;
import com.integri.atlas.task.dispatcher.map.completion.MapTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.parallel.ParallelTaskDispatcher;
import com.integri.atlas.task.dispatcher.parallel.completion.ParallelTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.sequence.SequenceTaskDefinitionHandler;
import com.integri.atlas.task.dispatcher.sequence.SequenceTaskDispatcher;
import com.integri.atlas.task.dispatcher.sequence.completion.SequenceTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.subflow.SubflowTaskDispatcher;
import com.integri.atlas.task.dispatcher.subflow.event.SubflowJobStatusEventListener;
import com.integri.atlas.task.dispatcher.switch_.SwitchTaskDispatcher;
import com.integri.atlas.task.dispatcher.switch_.completion.SwitchTaskCompletionHandler;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCoordinator
public class CoordinatorConfiguration {

    @Autowired
    private ContextRepository contextRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private Environment environment;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private MessageBroker messageBroker;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    private TaskEvaluator taskEvaluator;

    @Autowired
    private WorkflowRepository workflowRepository;

    @PostConstruct
    private void afterPropertiesSet() {
        taskEvaluator = SpelTaskEvaluator.builder().environment(environment).build();
    }

    @Bean
    @ConditionalOnProperty(name = "atlas.context.delete-listener.enabled", havingValue = "true")
    DeleteContextEventListener deleteContextEventListener(ContextService contextService) {
        return new DeleteContextEventListener(contextService, jobRepository);
    }

    @Bean
    ControlTaskDispatcher controlTaskDispatcher() {
        return new ControlTaskDispatcher(messageBroker);
    }

    @Bean
    Coordinator coordinator() {
        CoordinatorImpl coordinator = new CoordinatorImpl();

        coordinator.setContextRepository(contextRepository);
        coordinator.setErrorHandler(errorHandler());
        coordinator.setEventPublisher(eventPublisher);
        coordinator.setJobService(jobService);
        coordinator.setJobExecutor(jobExecutor());
        coordinator.setMessageBroker(messageBroker);
        coordinator.setTaskDispatcher(taskDispatcher());
        coordinator.setTaskExecutionRepository(taskExecutionRepository);
        coordinator.setTaskCompletionHandler(taskCompletionHandler());

        return coordinator;
    }

    @Bean
    DefaultTaskCompletionHandler defaultTaskCompletionHandler() {
        DefaultTaskCompletionHandler taskCompletionHandler = new DefaultTaskCompletionHandler();
        taskCompletionHandler.setContextRepository(contextRepository);
        taskCompletionHandler.setJobExecutor(jobExecutor());
        taskCompletionHandler.setJobRepository(jobRepository);
        taskCompletionHandler.setTaskExecutionRepository(taskExecutionRepository);
        taskCompletionHandler.setWorkflowRepository(workflowRepository);
        taskCompletionHandler.setEventPublisher(eventPublisher);
        taskCompletionHandler.setTaskEvaluator(taskEvaluator);
        return taskCompletionHandler;
    }

    @Bean
    DefaultTaskDispatcher defaultTaskDispatcher() {
        return new DefaultTaskDispatcher(messageBroker);
    }

    @Bean
    EachTaskCompletionHandler eachTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
        return new EachTaskCompletionHandler(taskExecutionRepository, aTaskCompletionHandler, counterRepository);
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
    ErrorHandler errorHandler() {
        return new ErrorHandlerChain(Arrays.asList(jobTaskErrorHandler()));
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
    IfTaskCompletionHandler ifTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new IfTaskCompletionHandler(
            contextRepository,
            taskCompletionHandler,
            taskDispatcher(),
            taskEvaluator,
            taskExecutionRepository
        );
    }

    @Bean
    IfTaskDefinitionHandler ifTaskDefinition() {
        return new IfTaskDefinitionHandler();
    }

    @Bean
    IfTaskDispatcher ifTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new IfTaskDispatcher(
            contextRepository,
            messageBroker,
            taskDispatcher,
            taskEvaluator,
            taskExecutionRepository
        );
    }

    @Bean
    DefaultJobExecutor jobExecutor() {
        DefaultJobExecutor jobExecutor = new DefaultJobExecutor();
        jobExecutor.setContextRepository(contextRepository);
        jobExecutor.setTaskExecutionRepository(taskExecutionRepository);
        jobExecutor.setWorkflowRepository(workflowRepository);
        jobExecutor.setTaskDispatcher(taskDispatcher());
        jobExecutor.setTaskEvaluator(taskEvaluator);
        return jobExecutor;
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
    LoopBreakTaskDefinitionHandler loopBreakTaskDefinition() {
        return new LoopBreakTaskDefinitionHandler();
    }

    @Bean
    LoopBreakTaskDispatcher loopBreakTaskDispatcher() {
        return new LoopBreakTaskDispatcher(messageBroker, taskExecutionRepository);
    }

    @Bean
    LoopTaskCompletionHandler loopTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new LoopTaskCompletionHandler(
            contextRepository,
            taskCompletionHandler,
            taskDispatcher(),
            taskEvaluator,
            taskExecutionRepository
        );
    }

    @Bean
    LoopTaskDefinitionHandler loopTaskDefinition() {
        return new LoopTaskDefinitionHandler();
    }

    @Bean
    LoopTaskDispatcher loopTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new LoopTaskDispatcher(
            contextRepository,
            messageBroker,
            taskDispatcher,
            taskEvaluator,
            taskExecutionRepository
        );
    }

    @Bean
    MapTaskCompletionHandler mapTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
        return new MapTaskCompletionHandler(taskExecutionRepository, aTaskCompletionHandler, counterRepository);
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
    ParallelTaskCompletionHandler parallelTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
        ParallelTaskCompletionHandler dispatcher = new ParallelTaskCompletionHandler();
        dispatcher.setCounterRepository(counterRepository);
        dispatcher.setTaskCompletionHandler(aTaskCompletionHandler);
        dispatcher.setTaskExecutionRepository(taskExecutionRepository);
        return dispatcher;
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
    SequenceTaskCompletionHandler sequenceTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new SequenceTaskCompletionHandler(
            contextRepository,
            taskCompletionHandler,
            taskDispatcher(),
            taskEvaluator,
            taskExecutionRepository
        );
    }

    @Bean
    SequenceTaskDefinitionHandler sequenceTaskDefinition() {
        return new SequenceTaskDefinitionHandler();
    }

    @Bean
    SequenceTaskDispatcher sequenceTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new SequenceTaskDispatcher(
            contextRepository,
            messageBroker,
            taskDispatcher,
            taskEvaluator,
            taskExecutionRepository
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
    SwitchTaskCompletionHandler switchTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new SwitchTaskCompletionHandler(
            contextRepository,
            taskExecutionRepository,
            taskCompletionHandler,
            taskDispatcher(),
            taskEvaluator
        );
    }

    @Bean
    SwitchTaskDispatcher switchTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new SwitchTaskDispatcher(
            contextRepository,
            messageBroker,
            taskDispatcher,
            taskExecutionRepository,
            taskEvaluator
        );
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

        taskDispatcher.setTaskDispatcherResolvers(resolvers);

        return taskDispatcher;
    }

    @Bean
    TaskProgressedEventListener taskProgressedEventListener() {
        return new TaskProgressedEventListener(taskExecutionRepository);
    }

    @Bean
    TaskStartedEventListener taskStartedEventListener() {
        return new TaskStartedEventListener(taskExecutionRepository, taskDispatcher(), jobRepository);
    }

    @Bean
    TaskStartedWebhookEventListener taskStartedWebhookEventListener() {
        return new TaskStartedWebhookEventListener(jobRepository);
    }
}
