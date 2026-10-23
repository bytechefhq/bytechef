/*
 * Copyright 2025 ByteChef
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

package com.bytechef.atlas.worker;

import static com.bytechef.atlas.configuration.constant.WorkflowConstants.FINALIZE;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.NAME;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.PARAMETERS;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.POST;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.PRE;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bytechef.async.config.AsyncConfiguration;
import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.worker.config.TaskHandlerRegistryConfiguration;
import com.bytechef.atlas.worker.config.TaskWorkerConfiguration;
import com.bytechef.atlas.worker.config.TaskWorkerMessageBrokerConfigurerConfiguration;
import com.bytechef.atlas.worker.event.CancelControlTaskEvent;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.memory.config.MemoryMessageBrokerConfiguration;
import com.bytechef.message.broker.memory.config.MemoryMessageBrokerListenerRegistrarConfiguration;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.event.listener.MessageEventListener;
import com.bytechef.message.event.tracing.MessageEventTracing;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileSystemUtils;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringJUnitConfig(TaskWorkerIntTest.TaskWorkerIntTestConfiguration.class)
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
@TestPropertySource(properties = {
    "bytechef.message-broker.provider=memory",
    "bytechef.worker.task.subscriptions.default=10"
})
class TaskWorkerIntTest {

    private static final long DEFAULT_TIMEOUT = 60_000L;
    private static final String FINISHED = "finished";
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(10);
    private static final ExecutorService CALLER_EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    private static final AtomicLong ID_SEQUENCE = new AtomicLong(1000);

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private Evaluator evaluator;

    @Autowired
    private CoordinatorEventRecorder coordinatorEventRecorder;

    @Autowired
    private HandlerObservations handlerObservations;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private List<TaskExecutionPostOutputProcessor> taskExecutionPostOutputProcessors;

    @Autowired
    private TaskHandlerResolver taskHandlerResolver;

    @Autowired
    private TaskWorker taskWorker;

    @Autowired
    @Qualifier("workerExecutor")
    private AsyncTaskExecutor workerExecutor;

    @AfterAll
    static void afterAll() {
        CALLER_EXECUTOR_SERVICE.shutdownNow();
    }

    @Test
    void testCalculateTimeout() {
        TaskExecution taskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "done"));

        assertThat(taskWorker.calculateTimeout(taskExecution)).isEqualTo(DEFAULT_TIMEOUT);

        taskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "done", "timeout", "10S"));

        assertThat(taskWorker.calculateTimeout(taskExecution)).isEqualTo(10_000L);

        TaskWorker taskWorkerWithoutDefaultTimeout = new TaskWorker(
            null, evaluator, eventPublisher, workerExecutor, taskHandlerResolver, taskFileStorage,
            taskExecutionPostOutputProcessors);

        taskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "done"));

        assertThat(taskWorkerWithoutDefaultTimeout.calculateTimeout(taskExecution))
            .isEqualTo(24 * 60 * 60 * 1000L);
    }

    @Test
    void testOnTaskExecutionEventClearsTheOutputOfAPreviousRunWhenTheTaskProducesNone() {
        TaskExecution taskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "streamed"));

        taskExecution.setOutput(
            taskFileStorage.storeTaskExecutionOutput(
                Objects.requireNonNull(taskExecution.getJobId()), Objects.requireNonNull(taskExecution.getId()),
                "previous run"));

        sendTaskExecutionEvent(taskExecution);

        TaskExecution completedTaskExecution = awaitCompletedTaskExecution(taskExecution);

        assertThat(completedTaskExecution.getOutput()).isNull();
    }

    @Test
    void testOnTaskExecutionEventPublishesTheHandlerOutputOnCompletion() {
        TaskExecution taskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "done"));

        sendTaskExecutionEvent(taskExecution);

        TaskExecution completedTaskExecution = awaitCompletedTaskExecution(taskExecution);

        assertThat(taskFileStorage.readTaskExecutionOutput(completedTaskExecution.getOutput())).isEqualTo("done");
        assertThat(completedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.COMPLETED);
    }

    @Test
    void testCompletedTaskExecutionCarriesStartAndEndDates() {
        TaskExecution taskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "done"));

        sendTaskExecutionEvent(taskExecution);

        TaskExecution completedTaskExecution = awaitCompletedTaskExecution(taskExecution);
        TaskStartedApplicationEvent taskStartedApplicationEvent = awaitTaskStartedApplicationEvent(taskExecution);

        assertThat(completedTaskExecution.getStartDate()).isEqualTo(taskStartedApplicationEvent.getCreateDate());
        assertThat(completedTaskExecution.getEndDate()).isNotNull();
    }

    @Test
    void testFailedTaskExecutionCarriesStartAndEndDates() {
        TaskExecution taskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "badInput"));

        sendTaskExecutionEvent(taskExecution);

        TaskExecution failedTaskExecution = awaitFailedTaskExecution(taskExecution);

        assertThat(failedTaskExecution.getStartDate()).isNotNull();
        assertThat(failedTaskExecution.getEndDate()).isNotNull();
    }

    @Test
    void testNotifiesPostOutputProcessorsBeforeTheErrorEventWhenTheTaskFails() {
        TaskExecution taskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "badInput"));

        sendTaskExecutionEvent(taskExecution);

        TaskExecution failedTaskExecution = awaitFailedTaskExecution(taskExecution);

        Map<String, ?> metadata = failedTaskExecution.getMetadata();

        assertThat(metadata.get(FINISHED)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void testNotifiesPostOutputProcessorsWhenTheTaskProducesNoOutput() {
        TaskExecution taskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "streamed"));

        sendTaskExecutionEvent(taskExecution);

        TaskExecution completedTaskExecution = awaitCompletedTaskExecution(taskExecution);

        Map<String, ?> metadata = completedTaskExecution.getMetadata();

        assertThat(completedTaskExecution.getOutput()).isNull();
        assertThat(metadata.get(FINISHED)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void testOnTaskExecutionEventPublishesTheHandlerErrorOnFailure() {
        TaskExecution taskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "badInput"));

        sendTaskExecutionEvent(taskExecution);

        TaskExecution failedTaskExecution = awaitFailedTaskExecution(taskExecution);

        assertThat(failedTaskExecution.getError()
            .getMessage()).isEqualTo("bad input");
        assertThat(failedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.FAILED);
        assertThat(findCompletedTaskExecution(taskExecution)).isEmpty();
    }

    @Test
    void testPreTaskOutputIsAvailableToTheTaskParameters() {
        TaskExecution taskExecution = createTaskExecution(
            Map.of(
                NAME, "name",
                TYPE, "var",
                PARAMETERS, Map.of("value", "${myVar}"),
                PRE, List.of(
                    Map.of(
                        NAME, "myVar",
                        TYPE, "var",
                        PARAMETERS, Map.of("value", "done")))));

        sendTaskExecutionEvent(taskExecution);

        TaskExecution completedTaskExecution = awaitCompletedTaskExecution(taskExecution);

        assertThat(taskFileStorage.readTaskExecutionOutput(completedTaskExecution.getOutput())).isEqualTo("done");
        assertThat(completedTaskExecution.getError()).isNull();
        assertThat(findFailedTaskExecution(taskExecution)).isEmpty();
    }

    @Test
    void testPostTaskRunsAfterTheTaskCompletes() {
        String tempDirectoryPath = createTempDirectoryPath();

        TaskExecution taskExecution = createTaskExecution(
            Map.of(
                NAME, "name",
                TYPE, "pass",
                PARAMETERS, Map.of("path", tempDirectoryPath),
                PRE, List.of(
                    Map.of(
                        NAME, "name",
                        TYPE, "mkdir",
                        PARAMETERS, Map.of("path", tempDirectoryPath))),
                POST, List.of(
                    Map.of(
                        NAME, "name",
                        TYPE, "rm",
                        PARAMETERS, Map.of("path", tempDirectoryPath)))));

        sendTaskExecutionEvent(taskExecution);

        awaitCompletedTaskExecution(taskExecution);

        assertThat(handlerObservations.existingPaths()).contains(tempDirectoryPath);
        assertThat(new File(tempDirectoryPath)).doesNotExist();
    }

    @Test
    void testFinalizeTaskRunsAfterTheTaskFails() {
        String tempDirectoryPath = createTempDirectoryPath();

        TaskExecution taskExecution = createTaskExecution(
            Map.of(
                NAME, "name",
                TYPE, "rogue",
                PARAMETERS, Map.of("path", tempDirectoryPath),
                PRE, List.of(
                    Map.of(
                        NAME, "name",
                        TYPE, "mkdir",
                        PARAMETERS, Map.of("path", tempDirectoryPath))),
                FINALIZE, List.of(
                    Map.of(
                        NAME, "name",
                        TYPE, "rm",
                        PARAMETERS, Map.of("path", tempDirectoryPath)))));

        sendTaskExecutionEvent(taskExecution);

        TaskExecution failedTaskExecution = awaitFailedTaskExecution(taskExecution);

        assertThat(failedTaskExecution.getError()
            .getMessage()).isEqualTo("Unexpected task type: rogue");
        assertThat(handlerObservations.existingPaths()).contains(tempDirectoryPath);
        assertThat(new File(tempDirectoryPath)).doesNotExist();
    }

    @Test
    void testCancelControlTaskEventCancelsTheRunningTask() {
        TaskExecution taskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "sleep"));

        sendTaskExecutionEvent(taskExecution);

        awaitTaskExecutionRegistered(taskExecution);

        awaitTaskStartedApplicationEvent(taskExecution);

        sendCancelControlTaskEvent(taskExecution);

        awaitTaskExecutionUnregistered(taskExecution);

        assertThat(findCompletedTaskExecution(taskExecution)).isEmpty();
        assertThat(findFailedTaskExecution(taskExecution)).isEmpty();
    }

    @Test
    void testCancelControlTaskEventCancelsOnlyTheTasksOfTheCancelledJob() {
        TaskExecution firstTaskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "sleep"));
        TaskExecution secondTaskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "sleep"));

        sendTaskExecutionEvent(firstTaskExecution);
        sendTaskExecutionEvent(secondTaskExecution);

        awaitTaskExecutionRegistered(firstTaskExecution);
        awaitTaskExecutionRegistered(secondTaskExecution);

        sendCancelControlTaskEvent(firstTaskExecution);

        awaitTaskExecutionUnregistered(firstTaskExecution);

        assertThat(taskWorker.getTaskExecutions()).containsKey(secondTaskExecution.getId());

        sendCancelControlTaskEvent(secondTaskExecution);

        awaitTaskExecutionUnregistered(secondTaskExecution);
    }

    @Test
    void testCancelControlTaskEventCancelsAllTasksOfTheJob() {
        TaskExecution parentTaskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "sleep"));

        TaskExecution childTaskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of(NAME, "name", TYPE, "sleep")))
            .build();

        childTaskExecution.setId(ID_SEQUENCE.incrementAndGet());
        childTaskExecution.setJobId(parentTaskExecution.getJobId());
        childTaskExecution.setParentId(parentTaskExecution.getId());

        sendTaskExecutionEvent(parentTaskExecution);
        sendTaskExecutionEvent(childTaskExecution);

        awaitTaskExecutionRegistered(parentTaskExecution);
        awaitTaskExecutionRegistered(childTaskExecution);

        sendCancelControlTaskEvent(parentTaskExecution);

        awaitTaskExecutionUnregistered(parentTaskExecution);
        awaitTaskExecutionUnregistered(childTaskExecution);
    }

    @Test
    void testCancelBeforeTheTaskStartsReleasesTheCaller() throws Exception {
        ThreadPoolTaskExecutor queuingTaskExecutor = new ThreadPoolTaskExecutor();

        queuingTaskExecutor.setCorePoolSize(1);
        queuingTaskExecutor.setMaxPoolSize(1);
        queuingTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);

        queuingTaskExecutor.initialize();

        CountDownLatch occupyingTaskLatch = new CountDownLatch(1);

        queuingTaskExecutor.execute(() -> awaitLatch(occupyingTaskLatch));

        TaskWorker queuingTaskWorker = new TaskWorker(
            DEFAULT_TIMEOUT, evaluator, eventPublisher, queuingTaskExecutor, taskHandlerResolver, taskFileStorage,
            taskExecutionPostOutputProcessors);

        TaskExecution taskExecution = createTaskExecution(Map.of(NAME, "name", TYPE, "recording"));

        Future<?> onTaskExecutionEventFuture = CALLER_EXECUTOR_SERVICE.submit(
            () -> queuingTaskWorker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution)));

        await().atMost(EVENT_TIMEOUT)
            .until(() -> queuingTaskWorker.getTaskExecutions()
                .containsKey(taskExecution.getId()));

        queuingTaskWorker.onCancelControlTaskEvent(
            new CancelControlTaskEvent(
                new CancelControlTask(
                    Objects.requireNonNull(taskExecution.getJobId()), Objects.requireNonNull(taskExecution.getId()))));

        onTaskExecutionEventFuture.get(5, TimeUnit.SECONDS);

        assertThat(queuingTaskWorker.getTaskExecutions()).isEmpty();

        occupyingTaskLatch.countDown();

        queuingTaskExecutor.shutdown();

        assertThat(handlerObservations.handledTaskExecutionIds()).doesNotContain(taskExecution.getId());
        assertThat(findTaskStartedApplicationEvent(taskExecution)).isEmpty();
    }

    private static void awaitLatch(CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The occupying task was never released");
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();
        }
    }

    private TaskExecution awaitCompletedTaskExecution(TaskExecution taskExecution) {
        return await().atMost(EVENT_TIMEOUT)
            .until(() -> findCompletedTaskExecution(taskExecution), Optional::isPresent)
            .orElseThrow();
    }

    private TaskExecution awaitFailedTaskExecution(TaskExecution taskExecution) {
        return await().atMost(EVENT_TIMEOUT)
            .until(() -> findFailedTaskExecution(taskExecution), Optional::isPresent)
            .orElseThrow();
    }

    private TaskStartedApplicationEvent awaitTaskStartedApplicationEvent(TaskExecution taskExecution) {
        return await().atMost(EVENT_TIMEOUT)
            .until(() -> findTaskStartedApplicationEvent(taskExecution), Optional::isPresent)
            .orElseThrow();
    }

    private void awaitTaskExecutionRegistered(TaskExecution taskExecution) {
        await().atMost(EVENT_TIMEOUT)
            .until(() -> taskWorker.getTaskExecutions()
                .containsKey(taskExecution.getId()));
    }

    private void awaitTaskExecutionUnregistered(TaskExecution taskExecution) {
        await().atMost(EVENT_TIMEOUT)
            .until(() -> !taskWorker.getTaskExecutions()
                .containsKey(taskExecution.getId()));
    }

    private TaskExecution createTaskExecution(Map<String, ?> workflowTaskDefinition) {
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(workflowTaskDefinition))
            .build();

        taskExecution.setId(ID_SEQUENCE.incrementAndGet());
        taskExecution.setJobId(ID_SEQUENCE.incrementAndGet());

        return taskExecution;
    }

    private static String createTempDirectoryPath() {
        File tempDirectory = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID()
            .toString());

        return tempDirectory.getAbsolutePath();
    }

    private Optional<TaskExecution> findCompletedTaskExecution(TaskExecution taskExecution) {
        return coordinatorEventRecorder.taskExecutionCompleteEvents()
            .stream()
            .map(TaskExecutionCompleteEvent::getTaskExecution)
            .filter(completedTaskExecution -> Objects.equals(completedTaskExecution.getId(), taskExecution.getId()))
            .findFirst();
    }

    private Optional<TaskExecution> findFailedTaskExecution(TaskExecution taskExecution) {
        return coordinatorEventRecorder.taskExecutionErrorEvents()
            .stream()
            .map(TaskExecutionErrorEvent::getTaskExecution)
            .filter(failedTaskExecution -> Objects.equals(failedTaskExecution.getId(), taskExecution.getId()))
            .findFirst();
    }

    private Optional<TaskStartedApplicationEvent> findTaskStartedApplicationEvent(TaskExecution taskExecution) {
        return coordinatorEventRecorder.applicationEvents()
            .stream()
            .filter(TaskStartedApplicationEvent.class::isInstance)
            .map(TaskStartedApplicationEvent.class::cast)
            .filter(taskStartedApplicationEvent -> Objects.equals(
                taskStartedApplicationEvent.getTaskExecutionId(), taskExecution.getId()))
            .findFirst();
    }

    private void sendCancelControlTaskEvent(TaskExecution taskExecution) {
        eventPublisher.publishEvent(
            new CancelControlTaskEvent(
                new CancelControlTask(
                    Objects.requireNonNull(taskExecution.getJobId()), Objects.requireNonNull(taskExecution.getId()))));
    }

    private void sendTaskExecutionEvent(TaskExecution taskExecution) {
        eventPublisher.publishEvent(new TaskExecutionEvent(taskExecution));
    }

    record CoordinatorEventRecorder(
        List<MessageEvent<?>> applicationEvents, List<TaskExecutionCompleteEvent> taskExecutionCompleteEvents,
        List<TaskExecutionErrorEvent> taskExecutionErrorEvents) {

        public void onApplicationEvent(MessageEvent<?> applicationEvent) {
            applicationEvents.add(applicationEvent);
        }

        public void onTaskExecutionCompleteEvent(TaskExecutionCompleteEvent taskExecutionCompleteEvent) {
            taskExecutionCompleteEvents.add(taskExecutionCompleteEvent);
        }

        public void onTaskExecutionErrorEvent(TaskExecutionErrorEvent taskExecutionErrorEvent) {
            taskExecutionErrorEvents.add(taskExecutionErrorEvent);
        }
    }

    record HandlerObservations(Set<String> existingPaths, Set<Long> handledTaskExecutionIds) {
    }

    @Configuration
    @Import({
        AsyncConfiguration.class, MemoryMessageBrokerConfiguration.class,
        MemoryMessageBrokerListenerRegistrarConfiguration.class, MessageEventListener.class,
        TaskHandlerRegistryConfiguration.class, TaskWorkerConfiguration.class,
        TaskWorkerMessageBrokerConfigurerConfiguration.class
    })
    static class TaskWorkerIntTestConfiguration {

        @Bean
        ApplicationProperties applicationProperties() {
            ApplicationProperties applicationProperties = new ApplicationProperties();

            ApplicationProperties.Worker.Task task = new ApplicationProperties.Worker.Task();

            task.setDefaultTimeout(DEFAULT_TIMEOUT);
            task.setSubscriptions(Map.of("default", 10));

            ApplicationProperties.Worker worker = new ApplicationProperties.Worker();

            worker.setTask(task);

            applicationProperties.setWorker(worker);

            return applicationProperties;
        }

        @Bean
        ContextPropagatingTaskDecorator contextPropagatingTaskDecorator() {
            return new ContextPropagatingTaskDecorator();
        }

        @Bean
        CoordinatorEventRecorder coordinatorEventRecorder() {
            return new CoordinatorEventRecorder(
                new CopyOnWriteArrayList<>(), new CopyOnWriteArrayList<>(), new CopyOnWriteArrayList<>());
        }

        @Bean
        MessageBrokerConfigurer<?> coordinatorEventRecorderMessageBrokerConfigurer(
            CoordinatorEventRecorder coordinatorEventRecorder) {

            return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
                messageBrokerListenerRegistrar.registerListenerEndpoint(
                    listenerEndpointRegistrar, TaskCoordinatorMessageRoute.APPLICATION_EVENTS, 1,
                    coordinatorEventRecorder, "onApplicationEvent");
                messageBrokerListenerRegistrar.registerListenerEndpoint(
                    listenerEndpointRegistrar, TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS, 1,
                    coordinatorEventRecorder, "onTaskExecutionCompleteEvent");
                messageBrokerListenerRegistrar.registerListenerEndpoint(
                    listenerEndpointRegistrar, TaskCoordinatorMessageRoute.ERROR_EVENTS, 1,
                    coordinatorEventRecorder, "onTaskExecutionErrorEvent");
            };
        }

        @Bean
        Evaluator evaluator() {
            return SpelEvaluator.create();
        }

        @Bean
        HandlerObservations handlerObservations() {
            return new HandlerObservations(ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet());
        }

        @Bean
        MessageEventTracing messageEventTracing() {
            return new MessageEventTracing(Propagator.NOOP, Tracer.NOOP);
        }

        @Bean
        TaskExecutionPostOutputProcessor finishedMarkingTaskExecutionPostOutputProcessor() {
            return new TaskExecutionPostOutputProcessor() {

                @Override
                public Object process(TaskExecution taskExecution, Object output) {
                    return output;
                }

                @Override
                public void onTaskExecutionFinished(TaskExecution taskExecution) {
                    taskExecution.putMetadata(FINISHED, true);
                }
            };
        }

        @Bean
        TaskExecutionPostOutputProcessor streamedOutputTaskExecutionPostOutputProcessor() {
            return (taskExecution, output) -> Objects.equals(taskExecution.getType(), "streamed") ? null : output;
        }

        @Bean
        TaskExecutionProperties taskExecutionProperties() {
            return new TaskExecutionProperties();
        }

        @Bean
        TaskFileStorage taskFileStorage() {
            return new TaskFileStorageImpl(new Base64FileStorageService());
        }

        @Bean(name = "badInput")
        TaskHandler<Object> badInputTaskHandler() {
            return taskExecution -> {
                throw new IllegalArgumentException("bad input");
            };
        }

        @Bean(name = "done")
        TaskHandler<String> doneTaskHandler() {
            return taskExecution -> "done";
        }

        @Bean(name = "mkdir")
        TaskHandler<Boolean> mkdirTaskHandler() {
            return taskExecution -> new File(MapUtils.getRequiredString(taskExecution.getParameters(), "path"))
                .mkdirs();
        }

        @Bean(name = "pass")
        TaskHandler<Object> passTaskHandler(HandlerObservations handlerObservations) {
            return taskExecution -> {
                recordExistingPath(handlerObservations, taskExecution);

                return null;
            };
        }

        @Bean(name = "recording")
        TaskHandler<Object> recordingTaskHandler(HandlerObservations handlerObservations) {
            return taskExecution -> {
                Set<Long> handledTaskExecutionIds = handlerObservations.handledTaskExecutionIds();

                handledTaskExecutionIds.add(taskExecution.getId());

                return null;
            };
        }

        @Bean(name = "rm")
        TaskHandler<Boolean> rmTaskHandler() {
            return taskExecution -> FileSystemUtils.deleteRecursively(
                new File(MapUtils.getRequiredString(taskExecution.getParameters(), "path")));
        }

        @Bean(name = "rogue")
        TaskHandler<Object> rogueTaskHandler(HandlerObservations handlerObservations) {
            return taskExecution -> {
                recordExistingPath(handlerObservations, taskExecution);

                throw new TaskExecutionException("Unexpected task type: rogue");
            };
        }

        @Bean(name = "sleep")
        TaskHandler<Object> sleepTaskHandler() {
            return taskExecution -> {
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException interruptedException) {
                    throw new TaskExecutionException("Unable to sleep due interruption");
                }

                return null;
            };
        }

        @Bean(name = "streamed")
        TaskHandler<String> streamedTaskHandler() {
            return taskExecution -> "streamed";
        }

        @Bean(name = "var")
        TaskHandler<Object> varTaskHandler() {
            return taskExecution -> MapUtils.getRequired(taskExecution.getParameters(), "value");
        }

        private static void recordExistingPath(HandlerObservations handlerObservations, TaskExecution taskExecution) {
            String path = MapUtils.getRequiredString(taskExecution.getParameters(), "path");

            if (new File(path).exists()) {
                Set<String> existingPaths = handlerObservations.existingPaths();

                existingPaths.add(path);
            }
        }
    }
}
