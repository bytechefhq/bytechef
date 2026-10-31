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

package com.bytechef.platform.worker.task;

import static com.bytechef.atlas.configuration.constant.WorkflowConstants.NAME;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.atlas.worker.config.TaskWorkerConfiguration;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCall;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCall.Status;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRef;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.worker.config.PlatformWorkerConfiguration;
import com.bytechef.platform.workflow.execution.constant.TaskExecutionMetadataConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@MockitoBean(types = {
    MessageBroker.class, TriggerScheduler.class
})
@SpringJUnitConfig({
    PlatformWorkerConfiguration.class, TaskWorkerConfiguration.class,
    ClusterElementCallsTaskExecutionPostOutputProcessorIntTest.ClusterElementCallsTestConfiguration.class
})
class ClusterElementCallsTaskExecutionPostOutputProcessorIntTest {

    private static final AtomicLong TASK_EXECUTION_ID_SEQUENCE = new AtomicLong(1000);

    private final ClusterElementCallRecorder recorder = ClusterElementCallRecorder.getInstance();

    @Autowired
    private ClusterElementCallsTaskExecutionPostOutputProcessor clusterElementCallsTaskExecutionPostOutputProcessor;

    @Autowired
    private CompletedTaskExecutionCollector completedTaskExecutionCollector;

    @Autowired
    private MetadataDuringProcessCollector metadataDuringProcessCollector;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private TaskWorker taskWorker;

    @Autowired
    private TestTaskHandlerRegistry testTaskHandlerRegistry;

    @Test
    void testWritesDrainedCallsToMetadata() {
        TaskExecution taskExecution = createTaskExecution("recordCompletedAndFailedCalls/v1");

        testTaskHandlerRegistry.register("recordCompletedAndFailedCalls/v1", clonedTaskExecution -> {
            long jobId = Objects.requireNonNull(clonedTaskExecution.getJobId());
            long taskExecutionId = Objects.requireNonNull(clonedTaskExecution.getId());

            ClusterElementCallRef httpClientRef = start(jobId, taskExecutionId, "httpClient_1");

            recorder.callCompleted(jobId, taskExecutionId, httpClientRef, "ok");

            ClusterElementCallRef slackRef = start(jobId, taskExecutionId, "slack_1");

            recorder.callFailed(jobId, taskExecutionId, slackRef, "boom");

            return "output";
        });

        Map<String, ClusterElementCall> calls = getCalls(execute(taskExecution));

        ClusterElementCall httpClientCall = calls.get("httpClient_1");
        ClusterElementCall slackCall = calls.get("slack_1");

        assertEquals(Status.COMPLETED, httpClientCall.status());
        assertEquals(Status.FAILED, slackCall.status());
        assertEquals("boom", slackCall.error());
    }

    @Test
    void testWritesRecordsWithoutTheirInputsAndOutputsToMetadata() {
        TaskExecution taskExecution = createTaskExecution("recordTwoCompletedCalls/v1");

        List<ClusterElementCallRef> refs = new CopyOnWriteArrayList<>();

        testTaskHandlerRegistry.register("recordTwoCompletedCalls/v1", clonedTaskExecution -> {
            long jobId = Objects.requireNonNull(clonedTaskExecution.getJobId());
            long taskExecutionId = Objects.requireNonNull(clonedTaskExecution.getId());

            ClusterElementCallRef firstRef = start(jobId, taskExecutionId, "httpClient_1");

            recorder.callCompleted(jobId, taskExecutionId, firstRef, "first");

            ClusterElementCallRef secondRef = start(jobId, taskExecutionId, "slack_1");

            recorder.callCompleted(jobId, taskExecutionId, secondRef, "second");

            refs.add(firstRef);
            refs.add(secondRef);

            return "output";
        });

        TaskExecution completedTaskExecution = execute(taskExecution);

        List<ClusterElementCallRecord> records = getRecords(completedTaskExecution);

        assertEquals(2, records.size());

        ClusterElementCallRecord firstRecord = records.get(0);
        ClusterElementCallRef firstRef = refs.get(0);
        ClusterElementCallRecord secondRecord = records.get(1);
        ClusterElementCallRef secondRef = refs.get(1);

        assertEquals(firstRef.id(), firstRecord.id());
        assertEquals(ClusterElementCallRecord.Status.COMPLETED, firstRecord.status());
        assertNull(firstRecord.input());
        assertNull(firstRecord.output());
        assertEquals(secondRef.id(), secondRecord.id());
        assertEquals(ClusterElementCallRecord.Status.COMPLETED, secondRecord.status());
        assertNull(secondRecord.input());
        assertNull(secondRecord.output());

        Map<String, ?> metadata = completedTaskExecution.getMetadata();

        assertTrue(metadata.containsKey(TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS));
    }

    @Test
    void testKeepsAnOutcomesDetailsOnItsStoredRecord() {
        TaskExecution taskExecution = createTaskExecution("recordRedactedOutcome/v1");

        testTaskHandlerRegistry.register("recordRedactedOutcome/v1", clonedTaskExecution -> {
            recorder.recordOutcome(
                Objects.requireNonNull(clonedTaskExecution.getJobId()),
                Objects.requireNonNull(clonedTaskExecution.getId()), "sanitizeText_1", "aiAgentGuardrails", null,
                ClusterElementCallRecord.Status.REDACTED, "{\"maskCount\":2,\"side\":\"input\"}", null);

            return "output";
        });

        List<ClusterElementCallRecord> records = getRecords(execute(taskExecution));

        assertEquals(1, records.size());

        ClusterElementCallRecord record = records.get(0);

        assertEquals(ClusterElementCallRecord.Status.REDACTED, record.status());
        assertEquals("{\"maskCount\":2,\"side\":\"input\"}", record.output());
    }

    @Test
    void testMergesWithExistingMetadata() {
        TaskExecution taskExecution = createTaskExecution("recordCompletedCall/v1");

        Instant startDate = Instant.parse("2026-10-23T10:00:00Z");

        ClusterElementCall storedCall = new ClusterElementCall(
            "httpClient_1", Status.FAILED, startDate, startDate.plusSeconds(1), "boom");

        taskExecution.putMetadata(TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS, List.of(storedCall.toMap()));

        testTaskHandlerRegistry.register("recordCompletedCall/v1", clonedTaskExecution -> {
            long jobId = Objects.requireNonNull(clonedTaskExecution.getJobId());
            long taskExecutionId = Objects.requireNonNull(clonedTaskExecution.getId());

            ClusterElementCallRef ref = start(jobId, taskExecutionId, "httpClient_1");

            recorder.callCompleted(jobId, taskExecutionId, ref, "ok");

            return "output";
        });

        ClusterElementCall call = getCalls(execute(taskExecution)).get("httpClient_1");

        assertEquals(Status.FAILED, call.status());
        assertEquals(startDate, call.startDate());
        assertEquals("boom", call.error());
    }

    @Test
    void testMergesRecordsWithStoredOnes() {
        TaskExecution taskExecution = createTaskExecution("completeStoredSuspendedCall/v1");

        Instant startDate = Instant.parse("2026-10-23T10:00:00Z");

        ClusterElementCallRecord storedRecord = new ClusterElementCallRecord(
            "a", null, "httpClient_1", "httpClient", "get", ClusterElementCallRecord.Status.SUSPENDED, startDate, null,
            "stored input", null, null);

        taskExecution.putMetadata(
            TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALL_RECORDS, List.of(storedRecord.toMap()));

        testTaskHandlerRegistry.register("completeStoredSuspendedCall/v1", clonedTaskExecution -> {
            recorder.callCompleted(
                Objects.requireNonNull(clonedTaskExecution.getJobId()),
                Objects.requireNonNull(clonedTaskExecution.getId()),
                new ClusterElementCallRef("a", "httpClient_1"), "answer");

            return "output";
        });

        List<ClusterElementCallRecord> records = getRecords(execute(taskExecution));

        assertEquals(1, records.size());

        ClusterElementCallRecord record = records.get(0);

        assertEquals("a", record.id());
        assertEquals(ClusterElementCallRecord.Status.COMPLETED, record.status());
        assertNull(record.input());
        assertNull(record.output());
    }

    @Test
    void testStoresOutcomeRecordsWithTheTaskExecution() {
        TaskExecution taskExecution = createTaskExecution("recordBlockedOutcome/v1");

        String details = "{\"side\":\"INPUT\",\"matchCount\":2}";

        testTaskHandlerRegistry.register("recordBlockedOutcome/v1", clonedTaskExecution -> {
            recorder.recordOutcome(
                Objects.requireNonNull(clonedTaskExecution.getJobId()),
                Objects.requireNonNull(clonedTaskExecution.getId()), "guardrails_1", "guardrails", null,
                ClusterElementCallRecord.Status.BLOCKED, details, null);

            return "output";
        });

        TaskExecution completedTaskExecution = execute(taskExecution);

        List<ClusterElementCallRecord> records = getRecords(completedTaskExecution);

        assertEquals(1, records.size());

        ClusterElementCallRecord record = records.get(0);

        assertEquals(ClusterElementCallRecord.Status.BLOCKED, record.status());
        assertEquals(details, record.output());

        ClusterElementCall call = getCalls(completedTaskExecution).get("guardrails_1");

        assertEquals(Status.BLOCKED, call.status());
    }

    @Test
    void testLeavesOutputUnchanged() {
        TaskExecution taskExecution = createTaskExecution("recordStartedCallWithMapOutput/v1");

        Map<String, String> output = Map.of("response", "text");

        testTaskHandlerRegistry.register("recordStartedCallWithMapOutput/v1", clonedTaskExecution -> {
            start(
                Objects.requireNonNull(clonedTaskExecution.getJobId()),
                Objects.requireNonNull(clonedTaskExecution.getId()), "httpClient_1");

            return output;
        });

        TaskExecution completedTaskExecution = execute(taskExecution);

        assertEquals(
            output,
            taskFileStorage.readTaskExecutionOutput(Objects.requireNonNull(completedTaskExecution.getOutput())));
        assertSame(output, clusterElementCallsTaskExecutionPostOutputProcessor.process(taskExecution, output));
    }

    @Test
    void testProcessLeavesCallsForTheFinishedHook() {
        TaskExecution taskExecution = createTaskExecution("recordStartedCall/v1");

        testTaskHandlerRegistry.register("recordStartedCall/v1", clonedTaskExecution -> {
            start(
                Objects.requireNonNull(clonedTaskExecution.getJobId()),
                Objects.requireNonNull(clonedTaskExecution.getId()), "httpClient_1");

            return "output";
        });

        TaskExecution completedTaskExecution = execute(taskExecution);

        Map<String, ?> metadataDuringProcess = metadataDuringProcessCollector.get(
            Objects.requireNonNull(taskExecution.getId()));

        assertNotNull(metadataDuringProcess);
        assertFalse(metadataDuringProcess.containsKey(TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS));
        assertEquals(
            "output",
            taskFileStorage.readTaskExecutionOutput(Objects.requireNonNull(completedTaskExecution.getOutput())));

        ClusterElementCall call = getCalls(completedTaskExecution).get("httpClient_1");

        assertEquals(Status.STARTED, call.status());
    }

    @Test
    void testNoCallsLeavesMetadataUntouched() {
        TaskExecution taskExecution = createTaskExecution("recordNoCalls/v1");

        testTaskHandlerRegistry.register("recordNoCalls/v1", clonedTaskExecution -> "output");

        TaskExecution completedTaskExecution = execute(taskExecution);

        Map<String, ?> metadata = completedTaskExecution.getMetadata();

        assertFalse(metadata.containsKey(TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS));
        assertFalse(metadata.containsKey(TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALL_RECORDS));
    }

    private ClusterElementCallRef start(long jobId, long taskExecutionId, String workflowNodeName) {
        return recorder.callStarted(jobId, taskExecutionId, workflowNodeName, "component", "tool", null, "input");
    }

    private static TaskExecution createTaskExecution(String type) {
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of(NAME, "agent_1", TYPE, type)))
            .build();

        taskExecution.setId(TASK_EXECUTION_ID_SEQUENCE.incrementAndGet());
        taskExecution.setJobId(1L);

        return taskExecution;
    }

    private TaskExecution execute(TaskExecution taskExecution) {
        taskWorker.onTaskExecutionEvent(new TaskExecutionEvent(taskExecution));

        TaskExecution completedTaskExecution = completedTaskExecutionCollector.get(
            Objects.requireNonNull(taskExecution.getId()));

        assertNotNull(completedTaskExecution, "The worker must publish a completed task execution");

        return completedTaskExecution;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ClusterElementCall> getCalls(TaskExecution taskExecution) {
        Map<String, ?> metadata = taskExecution.getMetadata();

        List<Map<String, ?>> calls = (List<Map<String, ?>>) metadata.get(
            TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS);

        assertNotNull(calls, "The cluster element calls must be stored in the task execution metadata");

        return calls.stream()
            .map(ClusterElementCall::fromMap)
            .collect(Collectors.toMap(ClusterElementCall::workflowNodeName, call -> call));
    }

    @SuppressWarnings("unchecked")
    private static List<ClusterElementCallRecord> getRecords(TaskExecution taskExecution) {
        Map<String, ?> metadata = taskExecution.getMetadata();

        List<Map<String, ?>> records = (List<Map<String, ?>>) metadata.get(
            TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALL_RECORDS);

        assertNotNull(records, "The cluster element call records must be stored in the task execution metadata");

        return records.stream()
            .map(ClusterElementCallRecord::fromMap)
            .toList();
    }

    @TestConfiguration
    static class ClusterElementCallsTestConfiguration {

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }

        @Bean
        CompletedTaskExecutionCollector completedTaskExecutionCollector() {
            return new CompletedTaskExecutionCollector();
        }

        @Bean
        Evaluator evaluator() {
            return SpelEvaluator.create();
        }

        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE)
        MetadataDuringProcessCollector metadataDuringProcessCollector() {
            return new MetadataDuringProcessCollector();
        }

        @Bean
        TaskFileStorage taskFileStorage() {
            return new TaskFileStorageImpl(new Base64FileStorageService());
        }

        @Bean
        TestTaskHandlerRegistry testTaskHandlerRegistry() {
            return new TestTaskHandlerRegistry();
        }

        @Bean
        AsyncTaskExecutor workerExecutor() {
            return new SimpleAsyncTaskExecutor();
        }
    }

    static class CompletedTaskExecutionCollector {

        private final Map<Long, TaskExecution> completedTaskExecutions = new ConcurrentHashMap<>();

        @EventListener
        public void onTaskExecutionComplete(TaskExecutionCompleteEvent taskExecutionCompleteEvent) {
            TaskExecution taskExecution = taskExecutionCompleteEvent.getTaskExecution();

            completedTaskExecutions.put(Objects.requireNonNull(taskExecution.getId()), taskExecution);
        }

        TaskExecution get(long taskExecutionId) {
            return completedTaskExecutions.get(taskExecutionId);
        }
    }

    static class MetadataDuringProcessCollector implements TaskExecutionPostOutputProcessor {

        private final Map<Long, Map<String, ?>> metadataByTaskExecutionId = new ConcurrentHashMap<>();

        @Override
        public Object process(TaskExecution taskExecution, Object output) {
            metadataByTaskExecutionId.put(
                Objects.requireNonNull(taskExecution.getId()), Map.copyOf(taskExecution.getMetadata()));

            return output;
        }

        Map<String, ?> get(long taskExecutionId) {
            return metadataByTaskExecutionId.get(taskExecutionId);
        }
    }

    static class TestTaskHandlerRegistry implements TaskHandlerRegistry {

        private final Map<String, TaskHandler<?>> taskHandlers = new ConcurrentHashMap<>();

        @Override
        public TaskHandler<?> getTaskHandler(String type) {
            return taskHandlers.get(type);
        }

        void register(String type, TaskHandler<?> taskHandler) {
            taskHandlers.put(type, taskHandler);
        }
    }
}
