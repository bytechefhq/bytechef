
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

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.TaskQueues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.task.CancelControlTask;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.commons.util.MapValueUtils;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;

public class WorkerTest {

    @Test
    public void test1() {
        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(
            TaskQueues.TASKS_COMPLETIONS,
            t -> Assertions.assertEquals("done", ((TaskExecution) t).getOutput()));
        messageBroker.receive(TaskQueues.TASKS_EVENTS, t -> {});

        Worker worker = Worker.builder()
            .taskHandlerResolver(jt -> t -> "done")
            .messageBroker(messageBroker)
            .eventPublisher(e -> {})
            .taskEvaluator(TaskEvaluator.create())
            .build();

        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("type"));

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.handle(taskExecution);
    }

    @Test
    public void test2() {
        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(
            TaskQueues.TASKS_ERRORS,
            t -> Assertions.assertEquals("bad input", ((TaskExecution) t).getError()
                .getMessage()));
        messageBroker.receive(TaskQueues.TASKS_EVENTS, t -> {});

        Worker worker = Worker.builder()
            .taskHandlerResolver(jt -> t -> {
                throw new IllegalArgumentException("bad input");
            })
            .messageBroker(messageBroker)
            .eventPublisher(e -> {})
            .taskEvaluator(TaskEvaluator.create())
            .build();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("type"));

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.handle(taskExecution);
    }

    @Test
    public void test3() {
        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(
            TaskQueues.TASKS_COMPLETIONS, t -> Assertions.assertEquals("done", ((TaskExecution) t).getOutput()));
        messageBroker.receive(TaskQueues.TASKS_ERRORS, t -> {
            TaskExecution taskExecution = (TaskExecution) t;

            Assertions.assertNull(taskExecution.getError());
        });
        messageBroker.receive(TaskQueues.TASKS_EVENTS, t -> {});

        Worker worker = Worker.builder()
            .taskHandlerResolver(t1 -> {
                String type = t1.getType();
                if ("var".equals(type)) {
                    return t2 -> MapValueUtils.getRequired(t2.getParameters(), "value");
                } else {
                    throw new IllegalArgumentException("unknown type: " + type);
                }
            })
            .messageBroker(messageBroker)
            .taskEvaluator(TaskEvaluator.create())
            .eventPublisher(e -> {})
            .build();

        TaskExecution task = new TaskExecution(WorkflowTask.of(Map.of(
            "pre",
            List.of(Map.of("name", "myVar", "type", "var", WorkflowConstants.PARAMETERS, Map.of("value", "done"))),
            "type", "var",
            WorkflowConstants.PARAMETERS, Map.of("value", "${myVar}"))));

        task.setId(1234L);
        task.setJobId(4567L);

        worker.handle(task);
    }

    @Test
    public void test4() {
        UUID uuid = UUID.randomUUID();

        String tempDir = new File(new File(System.getProperty("java.io.tmpdir")), uuid.toString())
            .getAbsolutePath();

        SyncMessageBroker messageBroker = new SyncMessageBroker();
        messageBroker.receive(TaskQueues.TASKS_COMPLETIONS, t -> {
            Assertions.assertFalse(new File(tempDir).exists());
        });
        messageBroker.receive(TaskQueues.TASKS_EVENTS, t -> {});

        Worker worker = Worker.builder()
            .taskHandlerResolver(t1 -> {
                String type = t1.getType();
                if ("var".equals(type)) {
                    return t2 -> MapValueUtils.getRequired(t2.getParameters(), "value");
                } else if ("mkdir".equals(type)) {
                    return t2 -> new File(MapValueUtils.getString(t2.getParameters(), "path")).mkdirs();
                } else if ("rm".equals(type)) {
                    return t2 -> FileSystemUtils
                        .deleteRecursively(new File(MapValueUtils.getString(t2.getParameters(), "path")));
                } else if ("pass".equals(type)) {
                    Assertions.assertTrue(new File(tempDir).exists());
                    return t2 -> null;
                } else {
                    throw new IllegalArgumentException("unknown type: " + type);
                }
            })
            .messageBroker(messageBroker)
            .eventPublisher(e -> {})
            .taskEvaluator(TaskEvaluator.create())
            .build();

        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of(Map.of(
            "post", List.of(Map.of("type", "rm", WorkflowConstants.PARAMETERS, Map.of("path", tempDir))),
            "pre", List.of(Map.of("type", "mkdir", WorkflowConstants.PARAMETERS, Map.of("path", tempDir))),
            "type", "pass")));

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.handle(taskExecution);
    }

    @Test
    public void test5() {
        UUID uuid = UUID.randomUUID();

        String tempDir = new File(new File(System.getProperty("java.io.tmpdir")), uuid.toString())
            .getAbsolutePath();

        SyncMessageBroker messageBroker = new SyncMessageBroker();
        messageBroker.receive(TaskQueues.TASKS_ERRORS, t -> {
            Assertions.assertFalse(new File(tempDir).exists());
        });
        messageBroker.receive(TaskQueues.TASKS_EVENTS, t -> {});
        Worker worker = Worker.builder()
            .taskHandlerResolver(t1 -> {
                String type = t1.getType();
                if ("var".equals(type)) {
                    return t2 -> MapValueUtils.getRequired(t2.getParameters(), "value");
                } else if ("mkdir".equals(type)) {
                    return t2 -> new File(MapValueUtils.getString(t2.getParameters(), "path")).mkdirs();
                } else if ("rm".equals(type)) {
                    return t2 -> FileSystemUtils
                        .deleteRecursively(new File(MapValueUtils.getString(t2.getParameters(), "path")));
                } else if ("rogue".equals(type)) {
                    Assertions.assertTrue(new File(tempDir).exists());
                    return t2 -> {
                        throw new TaskExecutionException("Unexpected task type: rogue");
                    };
                } else {
                    throw new IllegalArgumentException("unknown type: " + type);
                }
            })
            .messageBroker(messageBroker)
            .eventPublisher(e -> {})
            .taskEvaluator(TaskEvaluator.create())
            .build();

        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of(Map.of(
            "finalize", List.of(Map.of("type", "rm", "path", tempDir)),
            "pre", List.of(Map.of("type", "mkdir", "path", tempDir)),
            "type", "rogue")));

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.handle(taskExecution);
    }

    @Test
    public void test6() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        SyncMessageBroker messageBroker = new SyncMessageBroker();
        Worker worker = Worker.builder()
            .taskHandlerResolver(task -> taskExecution -> {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException interruptedException) {
                    throw new TaskExecutionException("Unable to sleep due interruption");
                }

                return null;
            })
            .messageBroker(messageBroker)
            .eventPublisher(event -> {})
            .taskEvaluator(TaskEvaluator.create())
            .build();

        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("type"));

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
    public void test7() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        SyncMessageBroker messageBroker = new SyncMessageBroker();
        Worker worker = Worker.builder()
            .taskHandlerResolver(task -> taskExecution -> {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException interruptedException) {
                    throw new TaskExecutionException("Unable to sleep due interruption");
                }

                return null;
            })
            .messageBroker(messageBroker)
            .eventPublisher(event -> {})
            .taskEvaluator(TaskEvaluator.create())
            .build();

        TaskExecution taskExecution1 = new TaskExecution(WorkflowTask.of("type"));

        taskExecution1.setId(1111L);
        taskExecution1.setJobId(2222L);

        // execute the task
        executorService.submit(() -> worker.handle(taskExecution1));

        TaskExecution taskExecution2 = new TaskExecution(WorkflowTask.of("type"));

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
    public void test8() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        SyncMessageBroker messageBroker = new SyncMessageBroker();
        Worker worker = Worker.builder()
            .taskHandlerResolver(task -> taskExecution -> {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException interruptedException) {
                    throw new TaskExecutionException("Unable to sleep due interruption");
                }

                return null;
            })
            .messageBroker(messageBroker)
            .eventPublisher(event -> {})
            .taskEvaluator(TaskEvaluator.create())
            .build();

        TaskExecution taskExecution1 = new TaskExecution(WorkflowTask.of("type"));
        taskExecution1.setId(1111L);
        taskExecution1.setJobId(2222L);
        // execute the task
        executorService.submit(() -> worker.handle(taskExecution1));

        TaskExecution taskExecution2 = new TaskExecution(WorkflowTask.of("type"));
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
