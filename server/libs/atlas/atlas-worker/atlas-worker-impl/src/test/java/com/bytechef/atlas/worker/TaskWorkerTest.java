/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.worker;

import static com.bytechef.atlas.configuration.constant.WorkflowConstants.FINALIZE;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.NAME;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.PARAMETERS;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.POST;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.PRE;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.TYPE;

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.worker.event.CancelControlTaskEvent;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.memory.SyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public class TaskWorkerTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    private static final ExecutorService NEW_FIXED_THREAD_POOL = Executors.newFixedThreadPool(2);
    private static final ExecutorService NEW_SINGLE_THREAD_EXECUTOR = Executors.newSingleThreadExecutor();

    private final TaskFileStorage taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

    @Test
    public void testCalculateTimeout() {
        TaskWorker worker = new TaskWorker(
            5000L, EVALUATOR, event -> {}, NEW_SINGLE_THREAD_EXECUTOR::execute, t -> null, taskFileStorage, List.of());

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of(NAME, "name", TYPE, "type")))
            .build();

        Assertions.assertEquals(5000L, worker.calculateTimeout(taskExecution));

        taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of(NAME, "name", TYPE, "type", "timeout", "10S")))
            .build();

        Assertions.assertEquals(10000L, worker.calculateTimeout(taskExecution));

        worker = new TaskWorker(
            null, EVALUATOR, event -> {}, NEW_SINGLE_THREAD_EXECUTOR::execute, t -> null, taskFileStorage, List.of());

        taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of(NAME, "name", TYPE, "type")))
            .build();
        Assertions.assertEquals(24 * 60 * 60 * 1000L, worker.calculateTimeout(taskExecution));
    }

    @Test
    public void test1() {
        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        syncMessageBroker.receive(
            TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS,
            t -> Assertions.assertEquals(
                "done", taskFileStorage.readTaskExecutionOutput(
                    ((TaskExecutionCompleteEvent) t).getTaskExecution()
                        .getOutput())));
        syncMessageBroker.receive(TaskCoordinatorMessageRoute.APPLICATION_EVENTS,
            t -> {});

        TaskWorker worker =
            new TaskWorker(
                null, EVALUATOR,
                event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event),
                NEW_SINGLE_THREAD_EXECUTOR::execute, task -> taskExecution -> "done", taskFileStorage, List.of());

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution));
    }

    @Test
    public void test2() {
        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        syncMessageBroker.receive(
            TaskCoordinatorMessageRoute.ERROR_EVENTS,
            t -> Assertions.assertEquals("bad input", ((TaskExecutionErrorEvent) t).getTaskExecution()
                .getError()
                .getMessage()));
        syncMessageBroker.receive(TaskCoordinatorMessageRoute.APPLICATION_EVENTS,
            t -> {});

        TaskWorker worker = new TaskWorker(
            null, EVALUATOR,
            event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event),
            NEW_SINGLE_THREAD_EXECUTOR::execute,
            task -> taskExecution -> {
                throw new IllegalArgumentException("bad input");
            }, taskFileStorage, List.of());

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution));
    }

    @Test
    public void test3() {
        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        syncMessageBroker.receive(
            TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS,
            t -> Assertions.assertEquals(
                "done", taskFileStorage.readTaskExecutionOutput(
                    ((TaskExecutionCompleteEvent) t).getTaskExecution()
                        .getOutput())));
        syncMessageBroker.receive(TaskCoordinatorMessageRoute.ERROR_EVENTS,
            t -> {
                TaskExecution taskExecution = (TaskExecution) t;

                Assertions.assertNull(taskExecution.getError());
            });
        syncMessageBroker.receive(TaskCoordinatorMessageRoute.APPLICATION_EVENTS,
            t -> {});

        TaskWorker worker = new TaskWorker(
            null, EVALUATOR,
            event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event),
            NEW_SINGLE_THREAD_EXECUTOR::execute,
            task -> {
                String type = task.getType();
                if ("var".equals(type)) {
                    return taskExecution -> MapUtils.getRequired(taskExecution.getParameters(), "value");
                } else {
                    throw new IllegalArgumentException("unknown type: " + type);
                }
            }, taskFileStorage, List.of());

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        NAME, "name",
                        TYPE, "var",
                        PARAMETERS, Map.of("value", "${myVar}"),
                        PRE, List.of(
                            Map.of(
                                NAME, "myVar",
                                TYPE, "var",
                                PARAMETERS, Map.of("value", "done"))))))
            .build();

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution));
    }

    @Test
    public void test4() {
        UUID uuid = UUID.randomUUID();

        File tempFile = new File(new File(System.getProperty("java.io.tmpdir")), uuid.toString());

        String tempDir = tempFile.getAbsolutePath();

        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        syncMessageBroker.receive(
            TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS,
            t -> Assertions.assertFalse(new File(tempDir).exists()));
        syncMessageBroker.receive(TaskCoordinatorMessageRoute.APPLICATION_EVENTS, t -> {});

        TaskWorker worker = new TaskWorker(
            null, EVALUATOR,
            event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event),
            NEW_SINGLE_THREAD_EXECUTOR::execute,
            task -> {
                String type = task.getType();
                if ("var".equals(type)) {
                    return taskExecution -> MapUtils.getRequired(taskExecution.getParameters(), "value");
                } else if ("mkdir".equals(type)) {
                    return taskExecution -> new File(MapUtils.getString(taskExecution.getParameters(), "path"))
                        .mkdirs();
                } else if ("rm".equals(type)) {
                    return taskExecution -> org.springframework.util.FileSystemUtils.deleteRecursively(
                        new File(MapUtils.getString(taskExecution.getParameters(), "path")));
                } else if ("pass".equals(type)) {
                    Assertions.assertTrue(new File(tempDir).exists());

                    return taskExecution -> null;
                } else {
                    throw new IllegalArgumentException("unknown type: " + type);
                }
            }, taskFileStorage, List.of());

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        NAME, "name",
                        TYPE, "pass",
                        PRE, List.of(
                            Map.of(
                                NAME, "name",
                                TYPE, "mkdir",
                                PARAMETERS, Map.of("path", tempDir))),
                        POST, List.of(
                            Map.of(
                                NAME, "name",
                                TYPE, "rm",
                                PARAMETERS, Map.of("path", tempDir))))))
            .build();

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution));
    }

    @Test
    public void test5() {
        UUID uuid = UUID.randomUUID();

        File tempFile = new File(new File(System.getProperty("java.io.tmpdir")), uuid.toString());

        String tempDir = tempFile.getAbsolutePath();

        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        syncMessageBroker.receive(
            TaskCoordinatorMessageRoute.ERROR_EVENTS,
            t -> Assertions.assertFalse(new File(tempDir).exists()));
        syncMessageBroker.receive(TaskCoordinatorMessageRoute.APPLICATION_EVENTS, t -> {});

        TaskWorker worker = new TaskWorker(
            null, EVALUATOR,
            event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event),
            NEW_SINGLE_THREAD_EXECUTOR::execute,
            task -> {
                String type = task.getType();
                if ("var".equals(type)) {
                    return taskExecution -> MapUtils.getRequired(taskExecution.getParameters(), "value");
                } else if ("mkdir".equals(type)) {
                    return taskExecution -> new File(MapUtils.getString(taskExecution.getParameters(), "path"))
                        .mkdirs();
                } else if ("rm".equals(type)) {
                    return taskExecution -> org.springframework.util.FileSystemUtils.deleteRecursively(
                        new File(MapUtils.getString(taskExecution.getParameters(), "path")));
                } else if ("rogue".equals(type)) {
                    Assertions.assertTrue(new File(tempDir).exists());

                    return taskExecution -> {
                        throw new TaskExecutionException("Unexpected task type: rogue");
                    };
                } else {
                    throw new IllegalArgumentException("unknown type: " + type);
                }
            }, taskFileStorage, List.of());

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        NAME, "name",
                        TYPE, "rogue",
                        PRE, List.of(
                            Map.of(
                                NAME, "name",
                                TYPE, "mkdir",
                                PARAMETERS, Map.of("path", tempDir))),
                        FINALIZE, List.of(
                            Map.of(
                                NAME, "name",
                                TYPE, "rm",
                                PARAMETERS, Map.of("path", tempDir))))))
            .build();

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution));
    }

    @Test
    public void test6() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        syncMessageBroker.receive(TaskCoordinatorMessageRoute.APPLICATION_EVENTS, e -> {});

        TaskWorker worker = new TaskWorker(
            null, EVALUATOR,
            event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event),
            NEW_SINGLE_THREAD_EXECUTOR::execute,
            task -> taskExecution -> {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException interruptedException) {
                    throw new TaskExecutionException("Unable to sleep due interruption");
                }

                return null;
            }, taskFileStorage, List.of());

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        // execute the task
        executorService.submit(() -> worker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution)));

        // give it a second to start executing
        TimeUnit.SECONDS.sleep(2);

        Assertions.assertEquals(1, MapUtils.size(worker.getTaskExecutions()));

        // cancel the execution of the task
        worker.onCancelControlTaskEvent(
            new CancelControlTaskEvent(new CancelControlTask(
                Validate.notNull(taskExecution.getJobId(), "jobId"), Validate.notNull(taskExecution.getId(), "id"))));

        // give it a second to cancel
        TimeUnit.SECONDS.sleep(2);

        Assertions.assertEquals(0, MapUtils.size(worker.getTaskExecutions()));
    }

    @Test
    public void test7() throws InterruptedException {
        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        syncMessageBroker.receive(TaskCoordinatorMessageRoute.APPLICATION_EVENTS, e -> {});

        TaskWorker worker = new TaskWorker(
            null, EVALUATOR,
            event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event),
            NEW_SINGLE_THREAD_EXECUTOR::execute,
            task -> taskExecution -> {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException interruptedException) {
                    throw new TaskExecutionException("Unable to sleep due interruption");
                }

                return null;
            }, taskFileStorage, List.of());

        TaskExecution taskExecution1 = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution1.setId(1111L);
        taskExecution1.setJobId(2222L);

        // execute the task
        EXECUTOR_SERVICE.submit(() -> worker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution1)));

        TaskExecution taskExecution2 = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution2.setId(3333L);
        taskExecution2.setJobId(4444L);

        // execute the task
        EXECUTOR_SERVICE.submit(() -> worker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution2)));

        // give it a second to start executing
        TimeUnit.SECONDS.sleep(2);

        Assertions.assertEquals(2, MapUtils.size(worker.getTaskExecutions()));

        // cancel the execution of the task
        worker.onCancelControlTaskEvent(
            new CancelControlTaskEvent(new CancelControlTask(
                Validate.notNull(taskExecution1.getJobId(), "jobId"), Validate.notNull(taskExecution1.getId(), "id"))));

        // give it a second to cancel
        TimeUnit.SECONDS.sleep(2);

        Assertions.assertEquals(1, MapUtils.size(worker.getTaskExecutions()));
    }

    @Test
    public void test8() throws InterruptedException {
        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        syncMessageBroker.receive(TaskCoordinatorMessageRoute.APPLICATION_EVENTS, e -> {});

        TaskWorker worker = new TaskWorker(
            null, EVALUATOR,
            event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event),
            NEW_FIXED_THREAD_POOL::execute,
            task -> taskExecution -> {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException interruptedException) {
                    throw new TaskExecutionException("Unable to sleep due interruption");
                }

                return null;
            }, taskFileStorage, List.of());

        TaskExecution taskExecution1 = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution1.setId(1111L);
        taskExecution1.setJobId(2222L);

        // execute the task
        EXECUTOR_SERVICE.submit(() -> worker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution1)));

        TaskExecution taskExecution2 = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution2.setId(3333L);
        taskExecution2.setJobId(2222L);
        taskExecution2.setParentId(taskExecution1.getId());

        // execute the task
        EXECUTOR_SERVICE.submit(() -> worker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution2)));

        // give it a second to start executing
        TimeUnit.SECONDS.sleep(2);

        Assertions.assertEquals(2, MapUtils.size(worker.getTaskExecutions()));

        // cancel the execution of the task
        worker.onCancelControlTaskEvent(
            new CancelControlTaskEvent(new CancelControlTask(
                Validate.notNull(taskExecution1.getJobId(), "jobId"), Validate.notNull(taskExecution1.getId(), "id"))));

        // give it a second to cancel
        TimeUnit.SECONDS.sleep(2);

        Assertions.assertEquals(0, MapUtils.size(worker.getTaskExecutions()));
    }
}
