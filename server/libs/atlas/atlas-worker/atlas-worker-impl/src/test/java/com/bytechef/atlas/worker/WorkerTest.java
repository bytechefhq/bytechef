
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

import com.bytechef.atlas.constants.WorkflowConstants;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.task.CancelControlTask;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.commons.utils.MapValueUtils;
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
            Queues.COMPLETIONS,
            t -> Assertions.assertEquals("done", ((TaskExecution) t).getOutput()));
        messageBroker.receive(Queues.EVENTS, t -> {});

        Worker worker = Worker.builder()
            .withTaskHandlerResolver(jt -> t -> "done")
            .withMessageBroker(messageBroker)
            .withEventPublisher(e -> {})
            .withTaskEvaluator(TaskEvaluator.create())
            .build();

        TaskExecution task = new TaskExecution();
        task.setId(1234L);
        task.setJobId(4567L);
        worker.handle(task);
    }

    @Test
    public void test2() {
        SyncMessageBroker messageBroker = new SyncMessageBroker();
        messageBroker.receive(
            Queues.ERRORS,
            t -> Assertions.assertEquals("bad input", ((TaskExecution) t).getError()
                .getMessage()));
        messageBroker.receive(Queues.EVENTS, t -> {});
        Worker worker = Worker.builder()
            .withTaskHandlerResolver(jt -> t -> {
                throw new IllegalArgumentException("bad input");
            })
            .withMessageBroker(messageBroker)
            .withEventPublisher(e -> {})
            .withTaskEvaluator(TaskEvaluator.create())
            .build();
        TaskExecution task = new TaskExecution();
        task.setId(1234L);
        task.setJobId(4567L);
        worker.handle(task);
    }

    @Test
    public void test3() {
        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(
            Queues.COMPLETIONS, t -> Assertions.assertEquals("done", (((TaskExecution) t).getOutput())));
        messageBroker.receive(Queues.ERRORS, t -> {
            TaskExecution taskExecution = (TaskExecution) t;

            Assertions.assertNull(taskExecution.getError());
        });
        messageBroker.receive(Queues.EVENTS, t -> {});

        Worker worker = Worker.builder()
            .withTaskHandlerResolver(t1 -> {
                String type = t1.getType();
                if ("var".equals(type)) {
                    return t2 -> MapValueUtils.getRequired(t2.getParameters(), "value");
                } else {
                    throw new IllegalArgumentException("unknown type: " + type);
                }
            })
            .withMessageBroker(messageBroker)
            .withTaskEvaluator(TaskEvaluator.create())
            .withEventPublisher(e -> {})
            .build();

        TaskExecution task = new TaskExecution(new WorkflowTask(Map.of(
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
        messageBroker.receive(Queues.COMPLETIONS, t -> {
            Assertions.assertFalse(new File(tempDir).exists());
        });
        messageBroker.receive(Queues.EVENTS, t -> {});

        Worker worker = Worker.builder()
            .withTaskHandlerResolver(t1 -> {
                String type = t1.getType();
                if ("var".equals(type)) {
                    return t2 -> MapValueUtils.getRequired(t2.getParameters(), "value");
                } else if ("mkdir".equals(type)) {
                    return t2 -> (new File(MapValueUtils.getString(t2.getParameters(), "path")).mkdirs());
                } else if ("rm".equals(type)) {
                    return t2 -> FileSystemUtils
                        .deleteRecursively((new File(MapValueUtils.getString(t2.getParameters(), "path"))));
                } else if ("pass".equals(type)) {
                    Assertions.assertTrue(new File(tempDir).exists());
                    return t2 -> null;
                } else {
                    throw new IllegalArgumentException("unknown type: " + type);
                }
            })
            .withMessageBroker(messageBroker)
            .withEventPublisher(e -> {})
            .withTaskEvaluator(TaskEvaluator.create())
            .build();

        TaskExecution taskExecution = new TaskExecution(new WorkflowTask(Map.of(
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
        messageBroker.receive(Queues.ERRORS, t -> {
            Assertions.assertFalse(new File(tempDir).exists());
        });
        messageBroker.receive(Queues.EVENTS, t -> {});

        Worker worker = Worker.builder()
            .withTaskHandlerResolver(t1 -> {
                String type = t1.getType();
                if ("var".equals(type)) {
                    return t2 -> MapValueUtils.getRequired(t2.getParameters(), "value");
                } else if ("mkdir".equals(type)) {
                    return t2 -> (new File(MapValueUtils.getString(t2.getParameters(), "path")).mkdirs());
                } else if ("rm".equals(type)) {
                    return t2 -> FileSystemUtils
                        .deleteRecursively((new File(MapValueUtils.getString(t2.getParameters(), "path"))));
                } else if ("rogue".equals(type)) {
                    Assertions.assertTrue(new File(tempDir).exists());
                    return t2 -> {
                        throw new TaskExecutionException("Unexpected task type: rogue");
                    };
                } else {
                    throw new IllegalArgumentException("unknown type: " + type);
                }
            })
            .withMessageBroker(messageBroker)
            .withEventPublisher(e -> {})
            .withTaskEvaluator(TaskEvaluator.create())
            .build();

        TaskExecution taskExecution = new TaskExecution(new WorkflowTask(Map.of(
            "finalize", List.of(Map.of("type", "rm", "path", tempDir)),
            "pre", List.of(Map.of("type", "mkdir", "path", tempDir)),
            "type", "rogue")));

        taskExecution.setId(1234L);
        taskExecution.setJobId(4567L);

        worker.handle(taskExecution);
    }

    @Test
    public void test6() throws InterruptedException {
        ExecutorService executors = Executors.newSingleThreadExecutor();
        SyncMessageBroker messageBroker = new SyncMessageBroker();

        Worker worker = Worker.builder()
            .withTaskHandlerResolver(task -> taskExecution -> {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException interruptedException) {
                    throw new TaskExecutionException("Unable to sleep due interruption");
                }

                return null;
            })
            .withMessageBroker(messageBroker)
            .withEventPublisher(event -> {})
            .withTaskEvaluator(TaskEvaluator.create())
            .build();

        TaskExecution task = new TaskExecution();
        task.setId(1234L);
        task.setJobId(4567L);
        // execute the task
        executors.submit(() -> worker.handle(task));
        // give it a second to start executing
        TimeUnit.SECONDS.sleep(1);
        Assertions.assertEquals(1, worker.getTaskExecutions()
            .size());
        // cancel the execution of the task
        worker.handle(new CancelControlTask(task.getJobId(), task.getId()));
        // give it a second to cancel
        TimeUnit.SECONDS.sleep(1);
        Assertions.assertEquals(0, worker.getTaskExecutions()
            .size());
    }

    @Test
    public void test7() throws InterruptedException {
        ExecutorService executors = Executors.newFixedThreadPool(2);
        SyncMessageBroker messageBroker = new SyncMessageBroker();

        Worker worker = Worker.builder()
            .withTaskHandlerResolver(task -> taskExecution -> {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException interruptedException) {
                    throw new TaskExecutionException("Unable to sleep due interruption");
                }

                return null;
            })
            .withMessageBroker(messageBroker)
            .withEventPublisher(event -> {})
            .withTaskEvaluator(TaskEvaluator.create())
            .build();

        TaskExecution task1 = new TaskExecution();

        task1.setId(1111L);
        task1.setJobId(2222L);

        // execute the task
        executors.submit(() -> worker.handle(task1));

        TaskExecution task2 = new TaskExecution();

        task2.setId(3333L);
        task2.setJobId(4444L);

        // execute the task
        executors.submit(() -> worker.handle(task2));

        // give it a second to start executing
        TimeUnit.SECONDS.sleep(1);

        Assertions.assertEquals(2, worker.getTaskExecutions()
            .size());

        // cancel the execution of the task
        worker.handle(new CancelControlTask(task1.getJobId(), task1.getId()));
        // give it a second to cancel
        TimeUnit.SECONDS.sleep(1);

        Assertions.assertEquals(1, worker.getTaskExecutions()
            .size());
    }

    @Test
    public void test8() throws InterruptedException {
        ExecutorService executors = Executors.newFixedThreadPool(2);
        SyncMessageBroker messageBroker = new SyncMessageBroker();
        Worker worker = Worker.builder()
            .withTaskHandlerResolver(task -> taskExecution -> {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException interruptedException) {
                    throw new TaskExecutionException("Unable to sleep due interruption");
                }

                return null;
            })
            .withMessageBroker(messageBroker)
            .withEventPublisher(event -> {})
            .withTaskEvaluator(TaskEvaluator.create())
            .build();

        TaskExecution task1 = new TaskExecution();
        task1.setId(1111L);
        task1.setJobId(2222L);
        // execute the task
        executors.submit(() -> worker.handle(task1));

        TaskExecution task2 = new TaskExecution();
        task2.setId(3333L);
        task2.setJobId(2222L);
        task2.setParentId(task1.getId());
        // execute the task
        executors.submit(() -> worker.handle(task2));

        // give it a second to start executing
        TimeUnit.SECONDS.sleep(1);

        Assertions.assertEquals(2, worker.getTaskExecutions()
            .size());
        // cancel the execution of the task
        worker.handle(new CancelControlTask(task1.getJobId(), task1.getId()));
        // give it a second to cancel
        TimeUnit.SECONDS.sleep(1);
        Assertions.assertEquals(0, worker.getTaskExecutions()
            .size());
    }
}
