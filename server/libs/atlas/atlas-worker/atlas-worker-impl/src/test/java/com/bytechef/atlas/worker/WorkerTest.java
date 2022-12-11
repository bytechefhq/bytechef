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

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.task.CancelControlTask;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.commons.utils.MapUtils;
import com.bytechef.commons.utils.UUIDUtils;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WorkerTest {

    @Test
    public void test1() {
        SyncMessageBroker messageBroker = new SyncMessageBroker();
        messageBroker.receive(
                Queues.COMPLETIONS,
                t -> Assertions.assertTrue(((TaskExecution) t).getOutput().equals("done")));
        messageBroker.receive(Queues.EVENTS, t -> {});

        Worker worker = Worker.builder()
                .withTaskHandlerResolver(jt -> t -> "done")
                .withMessageBroker(messageBroker)
                .withEventPublisher(e -> {})
                .withTaskEvaluator(TaskEvaluator.create())
                .build();

        TaskExecution task = new TaskExecution();
        task.setId("1234");
        task.setJobId("4567");
        worker.handle(task);
    }

    @Test
    public void test2() {
        SyncMessageBroker messageBroker = new SyncMessageBroker();
        messageBroker.receive(
                Queues.ERRORS,
                t -> Assertions.assertTrue(
                        ((TaskExecution) t).getError().getMessage().equals("bad input")));
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
        task.setId("1234");
        task.setJobId("4567");
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
                        return t2 -> MapUtils.getRequired(t2.getParameters(), "value");
                    } else {
                        throw new IllegalArgumentException("unknown type: " + type);
                    }
                })
                .withMessageBroker(messageBroker)
                .withTaskEvaluator(TaskEvaluator.create())
                .withEventPublisher(e -> {})
                .build();

        TaskExecution task = new TaskExecution(new WorkflowTask(Map.of(
                "pre", List.of(Map.of("name", "myVar", "type", "var", "value", "done")),
                "type", "var",
                "value", "${myVar}")));

        task.setId("1234");
        task.setJobId("4567");

        worker.handle(task);
    }

    @Test
    public void test4() {
        String tempDir =
                new File(new File(System.getProperty("java.io.tmpdir")), UUIDUtils.generate()).getAbsolutePath();

        SyncMessageBroker messageBroker = new SyncMessageBroker();
        messageBroker.receive(Queues.COMPLETIONS, t -> {
            Assertions.assertFalse(new File(tempDir).exists());
        });
        messageBroker.receive(Queues.EVENTS, t -> {});

        Worker worker = Worker.builder()
                .withTaskHandlerResolver(t1 -> {
                    String type = t1.getType();
                    if ("var".equals(type)) {
                        return t2 -> MapUtils.getRequired(t2.getParameters(), "value");
                    } else if ("mkdir".equals(type)) {
                        return t2 -> (new File(MapUtils.getString(t2.getParameters(), "path")).mkdirs());
                    } else if ("rm".equals(type)) {
                        return t2 ->
                                FileUtils.deleteQuietly((new File(MapUtils.getString(t2.getParameters(), "path"))));
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
                "post", List.of(Map.of("type", "rm", "path", tempDir)),
                "pre", List.of(Map.of("type", "mkdir", "path", tempDir)),
                "type", "pass")));

        taskExecution.setId("1234");
        taskExecution.setJobId("4567");

        worker.handle(taskExecution);
    }

    @Test
    public void test5() {
        String tempDir =
                new File(new File(System.getProperty("java.io.tmpdir")), UUIDUtils.generate()).getAbsolutePath();

        SyncMessageBroker messageBroker = new SyncMessageBroker();
        messageBroker.receive(Queues.ERRORS, t -> {
            Assertions.assertFalse(new File(tempDir).exists());
        });
        messageBroker.receive(Queues.EVENTS, t -> {});

        Worker worker = Worker.builder()
                .withTaskHandlerResolver(t1 -> {
                    String type = t1.getType();
                    if ("var".equals(type)) {
                        return t2 -> MapUtils.getRequired(t2.getParameters(), "value");
                    } else if ("mkdir".equals(type)) {
                        return t2 -> (new File(MapUtils.getString(t2.getParameters(), "path")).mkdirs());
                    } else if ("rm".equals(type)) {
                        return t2 ->
                                FileUtils.deleteQuietly((new File(MapUtils.getString(t2.getParameters(), "path"))));
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

        taskExecution.setId("1234");
        taskExecution.setJobId("4567");

        worker.handle(taskExecution);
    }

    @Test
    public void test6() throws InterruptedException {
        ExecutorService executors = Executors.newSingleThreadExecutor();
        SyncMessageBroker messageBroker = new SyncMessageBroker();

        Worker worker = Worker.builder()
                .withTaskHandlerResolver(jt -> t -> {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException interruptedException) {
                        throw new TaskExecutionException("Unable to sleep due interruption");
                    }

                    return null;
                })
                .withMessageBroker(messageBroker)
                .withEventPublisher(e -> {})
                .withTaskEvaluator(TaskEvaluator.create())
                .build();

        TaskExecution task = new TaskExecution();
        task.setId("1234");
        task.setJobId("4567");
        // execute the task
        executors.submit(() -> worker.handle(task));
        // give it a second to start executing
        TimeUnit.SECONDS.sleep(1);
        Assertions.assertEquals(1, worker.getTaskExecutions().size());
        // cancel the execution of the task
        worker.handle(new CancelControlTask(task.getJobId(), task.getId()));
        // give it a second to cancel
        TimeUnit.SECONDS.sleep(1);
        Assertions.assertEquals(0, worker.getTaskExecutions().size());
    }

    @Test
    public void test7() throws InterruptedException {
        ExecutorService executors = Executors.newFixedThreadPool(2);
        SyncMessageBroker messageBroker = new SyncMessageBroker();

        Worker worker = (Worker) Worker.builder()
                .withTaskHandlerResolver(jt -> t -> {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException interruptedException) {
                        throw new TaskExecutionException("Unable to sleep due interruption");
                    }

                    return null;
                })
                .withMessageBroker(messageBroker)
                .withEventPublisher(e -> {})
                .withTaskEvaluator(TaskEvaluator.create())
                .build();

        TaskExecution task1 = new TaskExecution();
        task1.setId("1111");
        task1.setJobId("2222");
        // execute the task
        executors.submit(() -> worker.handle(task1));

        TaskExecution task2 = new TaskExecution();
        task2.setId("3333");
        task2.setJobId("4444");
        // execute the task
        executors.submit(() -> worker.handle(task2));

        // give it a second to start executing
        TimeUnit.SECONDS.sleep(1);

        Assertions.assertEquals(2, worker.getTaskExecutions().size());
        // cancel the execution of the task
        worker.handle(new CancelControlTask(task1.getJobId(), task1.getId()));
        // give it a second to cancel
        TimeUnit.SECONDS.sleep(1);
        Assertions.assertEquals(1, worker.getTaskExecutions().size());
    }

    @Test
    public void test8() throws InterruptedException {
        ExecutorService executors = Executors.newFixedThreadPool(2);
        SyncMessageBroker messageBroker = new SyncMessageBroker();
        Worker worker = (Worker) Worker.builder()
                .withTaskHandlerResolver(jt -> t -> {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException interruptedException) {
                        throw new TaskExecutionException("Unable to sleep due interruption");
                    }

                    return null;
                })
                .withMessageBroker(messageBroker)
                .withEventPublisher(e -> {})
                .withTaskEvaluator(TaskEvaluator.create())
                .build();

        TaskExecution task1 = new TaskExecution();
        task1.setId("1111");
        task1.setJobId("2222");
        // execute the task
        executors.submit(() -> worker.handle(task1));

        TaskExecution task2 = new TaskExecution();
        task2.setId("3333");
        task2.setJobId("2222");
        task2.setParentId(task1.getId());
        // execute the task
        executors.submit(() -> worker.handle(task2));

        // give it a second to start executing
        TimeUnit.SECONDS.sleep(1);

        Assertions.assertEquals(2, worker.getTaskExecutions().size());
        // cancel the execution of the task
        worker.handle(new CancelControlTask(task1.getJobId(), task1.getId()));
        // give it a second to cancel
        TimeUnit.SECONDS.sleep(1);
        Assertions.assertEquals(0, worker.getTaskExecutions().size());
    }
}
