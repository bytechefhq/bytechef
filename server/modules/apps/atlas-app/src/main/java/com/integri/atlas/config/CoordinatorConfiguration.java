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
import com.integri.atlas.engine.counter.service.CounterService;
import com.integri.atlas.engine.error.ErrorHandler;
import com.integri.atlas.engine.event.EventPublisher;
import com.integri.atlas.engine.job.service.JobService;
import com.integri.atlas.engine.message.broker.MessageBroker;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.task.execution.evaluator.TaskEvaluator;
import com.integri.atlas.engine.task.execution.evaluator.spel.SpelTaskEvaluator;
import com.integri.atlas.engine.task.execution.servic.TaskExecutionService;
import com.integri.atlas.engine.workflow.service.WorkflowService;
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
        return new DefaultTaskDispatcher(messageBroker);
    }

    @Bean
    EachTaskCompletionHandler eachTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new EachTaskCompletionHandler(taskExecutionService, taskCompletionHandler, counterService);
    }

    @Bean
    EachTaskDispatcher eachTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new EachTaskDispatcher(
            taskDispatcher,
            taskExecutionService,
            messageBroker,
            contextService,
            counterService,
            taskEvaluator
        );
    }

    @Bean
    ErrorHandler errorHandler() {
        return new ErrorHandlerChain(Arrays.asList(jobTaskErrorHandler()));
    }

    @Bean
    ForkTaskCompletionHandler forkTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new ForkTaskCompletionHandler(
            taskExecutionService,
            taskCompletionHandler,
            counterService,
            taskDispatcher(),
            contextService,
            taskEvaluator
        );
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
            contextService,
            taskCompletionHandler,
            taskDispatcher(),
            taskEvaluator,
            taskExecutionService
        );
    }

    @Bean
    IfTaskDefinitionHandler ifTaskDefinitionHandler() {
        return new IfTaskDefinitionHandler();
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
    LoopBreakTaskDefinitionHandler loopBreakTaskDefinitionHandler() {
        return new LoopBreakTaskDefinitionHandler();
    }

    @Bean
    LoopBreakTaskDispatcher loopBreakTaskDispatcher() {
        return new LoopBreakTaskDispatcher(messageBroker, taskExecutionService);
    }

    @Bean
    LoopTaskCompletionHandler loopTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new LoopTaskCompletionHandler(
            contextService,
            taskCompletionHandler,
            taskDispatcher(),
            taskEvaluator,
            taskExecutionService
        );
    }

    @Bean
    LoopTaskDefinitionHandler loopTaskDefinitionHandler() {
        return new LoopTaskDefinitionHandler();
    }

    @Bean
    LoopTaskDispatcher loopTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new LoopTaskDispatcher(
            contextService,
            messageBroker,
            taskDispatcher,
            taskEvaluator,
            taskExecutionService
        );
    }

    @Bean
    MapTaskCompletionHandler mapTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        return new MapTaskCompletionHandler(taskExecutionService, taskCompletionHandler, counterService);
    }

    @Bean
    MapTaskDispatcher mapTaskDispatcher(TaskDispatcher taskDispatcher) {
        return MapTaskDispatcher
            .builder()
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
            contextService,
            taskCompletionHandler,
            taskDispatcher(),
            taskEvaluator,
            taskExecutionService
        );
    }

    @Bean
    SequenceTaskDefinitionHandler sequenceTaskDefinitionHandler() {
        return new SequenceTaskDefinitionHandler();
    }

    @Bean
    SequenceTaskDispatcher sequenceTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new SequenceTaskDispatcher(
            contextService,
            messageBroker,
            taskDispatcher,
            taskEvaluator,
            taskExecutionService
        );
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
            contextService,
            taskExecutionService,
            taskCompletionHandler,
            taskDispatcher(),
            taskEvaluator
        );
    }

    @Bean
    SwitchTaskDispatcher switchTaskDispatcher(TaskDispatcher taskDispatcher) {
        return new SwitchTaskDispatcher(
            contextService,
            messageBroker,
            taskDispatcher,
            taskExecutionService,
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
