
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

package com.bytechef.atlas.worker;

import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacadeImpl;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.message.broker.TaskMessageRoute;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.configuration.task.CancelControlTask;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.commons.util.MapUtils;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;

import static com.bytechef.atlas.configuration.constant.WorkflowConstants.FINALIZE;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.NAME;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.PARAMETERS;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.POST;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.PRE;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.TYPE;

public class TaskWorkerTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            registerModule(new JavaTimeModule());
            registerModule(new Jdk8Module());
        }
    };
    private final WorkflowFileStorageFacade workflowFileStorageFacade = new WorkflowFileStorageFacadeImpl(
        new Base64FileStorageService(), objectMapper);

    @Test
    public void test1() {
        SyncMessageBroker messageBroker = new SyncMessageBroker(objectMapper);

        messageBroker.receive(
            TaskMessageRoute.TASKS_COMPLETE,
            t -> Assertions.assertEquals(
                "done", workflowFileStorageFacade.readTaskExecutionOutput(((TaskExecution) t).getOutput())));
        messageBroker.receive(SystemMessageRoute.EVENTS, t -> {});

        TaskWorker worker =
            new TaskWorker(e -> {}, messageBroker, task -> taskExecution -> "done", workflowFileStorageFacade);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(WorkflowTask.of(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.handle(taskExecution);
    }

    @Test
    public void test2() {
        SyncMessageBroker messageBroker = new SyncMessageBroker(objectMapper);

        messageBroker.receive(
            SystemMessageRoute.ERRORS,
            t -> Assertions.assertEquals("bad input", ((TaskExecution) t).getError()
                .getMessage()));
        messageBroker.receive(SystemMessageRoute.EVENTS, t -> {});

        TaskWorker worker = new TaskWorker(e -> {}, messageBroker, task -> taskExecution -> {
            throw new IllegalArgumentException("bad input");
        }, workflowFileStorageFacade);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(WorkflowTask.of(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.handle(taskExecution);
    }

    @Test
    public void test3() {
        SyncMessageBroker messageBroker = new SyncMessageBroker(objectMapper);

        messageBroker.receive(
            TaskMessageRoute.TASKS_COMPLETE, t -> Assertions.assertEquals(
                "done", workflowFileStorageFacade.readTaskExecutionOutput(((TaskExecution) t).getOutput())));
        messageBroker.receive(SystemMessageRoute.ERRORS, t -> {
            TaskExecution taskExecution = (TaskExecution) t;

            Assertions.assertNull(taskExecution.getError());
        });
        messageBroker.receive(SystemMessageRoute.EVENTS, t -> {});

        TaskWorker worker = new TaskWorker(e -> {}, messageBroker, task -> {
            String type = task.getType();
            if ("var".equals(type)) {
                return taskExecution -> MapUtils.getRequired(taskExecution.getParameters(), "value");
            } else {
                throw new IllegalArgumentException("unknown type: " + type);
            }
        }, workflowFileStorageFacade);

        TaskExecution task = TaskExecution.builder()
            .workflowTask(
                WorkflowTask.of(
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

        task.setId(1234L);
        task.setJobId(4567L);

        worker.handle(task);
    }

    @Test
    public void test4() {
        UUID uuid = UUID.randomUUID();

        String tempDir = new File(new File(System.getProperty("java.io.tmpdir")), uuid.toString())
            .getAbsolutePath();

        SyncMessageBroker messageBroker = new SyncMessageBroker(objectMapper);

        messageBroker.receive(
            TaskMessageRoute.TASKS_COMPLETE, t -> Assertions.assertFalse(new File(tempDir).exists()));
        messageBroker.receive(SystemMessageRoute.EVENTS, t -> {});

        TaskWorker worker = new TaskWorker(e -> {}, messageBroker, task -> {
            String type = task.getType();
            if ("var".equals(type)) {
                return taskExecution -> MapUtils.getRequired(taskExecution.getParameters(), "value");
            } else if ("mkdir".equals(type)) {
                return taskExecution -> new File(MapUtils.getString(taskExecution.getParameters(), "path")).mkdirs();
            } else if ("rm".equals(type)) {
                return taskExecution -> FileSystemUtils
                    .deleteRecursively(new File(MapUtils.getString(taskExecution.getParameters(), "path")));
            } else if ("pass".equals(type)) {
                Assertions.assertTrue(new File(tempDir).exists());

                return taskExecution -> null;
            } else {
                throw new IllegalArgumentException("unknown type: " + type);
            }
        }, workflowFileStorageFacade);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                WorkflowTask.of(
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

        worker.handle(taskExecution);
    }

    @Test
    public void test5() {
        UUID uuid = UUID.randomUUID();

        String tempDir = new File(new File(System.getProperty("java.io.tmpdir")), uuid.toString())
            .getAbsolutePath();

        SyncMessageBroker messageBroker = new SyncMessageBroker(objectMapper);

        messageBroker.receive(SystemMessageRoute.ERRORS, t -> {
            Assertions.assertFalse(new File(tempDir).exists());
        });
        messageBroker.receive(SystemMessageRoute.EVENTS, t -> {});

        TaskWorker worker = new TaskWorker(e -> {}, messageBroker, task -> {
            String type = task.getType();
            if ("var".equals(type)) {
                return taskExecution -> MapUtils.getRequired(taskExecution.getParameters(), "value");
            } else if ("mkdir".equals(type)) {
                return taskExecution -> new File(MapUtils.getString(taskExecution.getParameters(), "path")).mkdirs();
            } else if ("rm".equals(type)) {
                return taskExecution -> FileSystemUtils.deleteRecursively(
                    new File(MapUtils.getString(taskExecution.getParameters(), "path")));
            } else if ("rogue".equals(type)) {
                Assertions.assertTrue(new File(tempDir).exists());

                return taskExecution -> {
                    throw new TaskExecutionException("Unexpected task type: rogue");
                };
            } else {
                throw new IllegalArgumentException("unknown type: " + type);
            }
        }, workflowFileStorageFacade);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                WorkflowTask.of(
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

        worker.handle(taskExecution);
    }

    @Test
    @SuppressFBWarnings("NP")
    public void test6() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        SyncMessageBroker messageBroker = new SyncMessageBroker(objectMapper);

        TaskWorker worker = new TaskWorker(e -> {}, messageBroker, task -> taskExecution -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException interruptedException) {
                throw new TaskExecutionException("Unable to sleep due interruption");
            }

            return null;
        }, workflowFileStorageFacade);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(WorkflowTask.of(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        // execute the task
        executorService.submit(() -> worker.handle(taskExecution));

        // give it a second to start executing
        TimeUnit.SECONDS.sleep(1);

        Assertions.assertEquals(1, worker.getTaskExecutions()
            .size());

        // cancel the execution of the task
        worker.handle(new CancelControlTask(taskExecution.getJobId(), taskExecution.getId()));
        // give it a second to cancel
        TimeUnit.SECONDS.sleep(1);

        Assertions.assertEquals(0, worker.getTaskExecutions()
            .size());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void test7() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        SyncMessageBroker messageBroker = new SyncMessageBroker(objectMapper);

        TaskWorker worker = new TaskWorker(e -> {}, messageBroker, task -> taskExecution -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException interruptedException) {
                throw new TaskExecutionException("Unable to sleep due interruption");
            }

            return null;
        }, workflowFileStorageFacade);

        TaskExecution taskExecution1 = TaskExecution.builder()
            .workflowTask(WorkflowTask.of(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution1.setId(1111L);
        taskExecution1.setJobId(2222L);

        // execute the task
        executorService.submit(() -> worker.handle(taskExecution1));

        TaskExecution taskExecution2 = TaskExecution.builder()
            .workflowTask(WorkflowTask.of(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution2.setId(3333L);
        taskExecution2.setJobId(4444L);

        // execute the task
        executorService.submit(() -> worker.handle(taskExecution2));

        // give it a second to start executing
        TimeUnit.SECONDS.sleep(1);

        Assertions.assertEquals(2, worker.getTaskExecutions()
            .size());

        // cancel the execution of the task
        worker.handle(new CancelControlTask(taskExecution1.getJobId(), taskExecution1.getId()));
        // give it a second to cancel
        TimeUnit.SECONDS.sleep(1);

        Assertions.assertEquals(1, worker.getTaskExecutions()
            .size());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void test8() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        SyncMessageBroker messageBroker = new SyncMessageBroker(objectMapper);

        TaskWorker worker = new TaskWorker(e -> {}, messageBroker, task -> taskExecution -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException interruptedException) {
                throw new TaskExecutionException("Unable to sleep due interruption");
            }

            return null;
        }, workflowFileStorageFacade);

        TaskExecution taskExecution1 = TaskExecution.builder()
            .workflowTask(WorkflowTask.of(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution1.setId(1111L);
        taskExecution1.setJobId(2222L);

        // execute the task
        executorService.submit(() -> worker.handle(taskExecution1));

        TaskExecution taskExecution2 = TaskExecution.builder()
            .workflowTask(WorkflowTask.of(Map.of(NAME, "name", TYPE, "type")))
            .build();

        taskExecution2.setId(3333L);
        taskExecution2.setJobId(2222L);
        taskExecution2.setParentId(taskExecution1.getId());

        // execute the task
        executorService.submit(() -> worker.handle(taskExecution2));

        // give it a second to start executing
        TimeUnit.SECONDS.sleep(1);

        Assertions.assertEquals(2, worker.getTaskExecutions()
            .size());

        // cancel the execution of the task
        worker.handle(new CancelControlTask(taskExecution1.getJobId(), taskExecution1.getId()));
        // give it a second to cancel
        TimeUnit.SECONDS.sleep(1);

        Assertions.assertEquals(0, worker.getTaskExecutions()
            .size());
    }
}
