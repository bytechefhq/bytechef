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

package com.bytechef.platform.workflow.test.facade;

import static com.bytechef.platform.workflow.test.config.TestWorkflowExecutorIntTestConfiguration.COMPONENT_NAME;
import static com.bytechef.platform.workflow.test.config.TestWorkflowExecutorIntTestConfiguration.COMPONENT_TITLE;
import static com.bytechef.platform.workflow.test.config.TestWorkflowExecutorIntTestConfiguration.FAILED_TASK_MESSAGE;
import static com.bytechef.platform.workflow.test.config.TestWorkflowExecutorIntTestConfiguration.STREAMED_CHUNK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRef;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import com.bytechef.platform.workflow.test.config.TestWorkflowExecutorIntTestConfiguration;
import com.bytechef.platform.workflow.test.config.TestWorkflowExecutorIntTestConfiguration.EvaluationProbe;
import com.bytechef.platform.workflow.test.config.TestWorkflowExecutorIntTestConfiguration.TaskGate;
import com.bytechef.platform.workflow.test.config.TestWorkflowExecutorIntTestConfiguration.TaskGates;
import com.bytechef.platform.workflow.test.dto.WorkflowTestExecutionDTO;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(
    classes = TestWorkflowExecutorIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
class TestWorkflowExecutorIntTest {

    private static final long ENVIRONMENT_ID = 10L;
    private static final long TIMEOUT_SECONDS = 10;

    @Autowired
    private EvaluationProbe evaluationProbe;

    @Autowired
    private TaskGates taskGates;

    @Autowired
    private TestWorkflowExecutor testWorkflowExecutor;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkflowNodeTestOutputRepository workflowNodeTestOutputRepository;

    @Autowired
    private WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTestConfigurationRepository workflowTestConfigurationRepository;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @AfterEach
    void afterEach() {
        evaluationProbe.reset();
        taskGates.reset();

        workflowNodeTestOutputRepository.deleteAll();
        workflowTestConfigurationRepository.deleteAll();

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(Objects.requireNonNull(workflow.getId()));
        }
    }

    @Test
    void executeSyncNoTriggersMergesInputsAndExecutesJob() {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "echo",
                            "type": "testComponent/v1/echo",
                            "parameters": {"inValue": "${inKey}", "configurationValue": "${configurationKey}"}
                        }
                    ]
                }
                """);

        workflowTestConfigurationService.saveWorkflowTestConfiguration(
            new WorkflowTestConfiguration(
                ENVIRONMENT_ID, Map.of("configurationKey", "configurationValue"), workflowId,
                List.of(new WorkflowTestConfigurationConnection(42L, "connectionKey", "nodeA"))));

        Map<String, Object> inputs = new HashMap<>();

        inputs.put("inKey", "inValue");

        WorkflowTestExecutionDTO result = testWorkflowExecutor.executeSync(workflowId, inputs, ENVIRONMENT_ID);

        JobDTO jobDTO = result.job();

        assertThat(jobDTO.status()).isEqualTo(Job.Status.COMPLETED);
        assertThat(jobDTO.workflowId()).isEqualTo(workflowId);
        assertThat(toObjectMap(jobDTO.inputs()))
            .containsEntry("inKey", "inValue")
            .containsEntry("configurationKey", "configurationValue");

        Map<String, ?> metadata = jobDTO.metadata();

        assertThat(metadata.get(MetadataConstants.CONNECTION_IDS))
            .isInstanceOfSatisfying(
                Map.class, connectionIds -> assertThat(connectionIds.get("nodeA"))
                    .isInstanceOfSatisfying(
                        Map.class, nodeConnectionIds -> assertThat(nodeConnectionIds.get("connectionKey"))
                            .isInstanceOfSatisfying(
                                Number.class, connectionId -> assertThat(connectionId.longValue()).isEqualTo(42L))));

        assertThat(jobDTO.taskExecutions())
            .singleElement()
            .satisfies(taskExecutionDTO -> {
                assertThat(taskExecutionDTO.status()).isEqualTo(TaskExecution.Status.COMPLETED);
                assertThat(taskExecutionDTO.title()).isEqualTo(COMPONENT_TITLE);
                assertThat(taskExecutionDTO.output()).isEqualTo(
                    Map.of("inValue", "inValue", "configurationValue", "configurationValue"));
            });

        assertThat(result.triggerExecution()).isNull();
    }

    @Test
    void executeSyncWithTriggerEmptyInputsBuildsTriggerDTOAndFlattensSampleForNonBatch() {
        String workflowId = createWorkflow(
            """
                {
                    "triggers": [{"name": "myTrigger", "type": "testComponent/v1/start"}],
                    "tasks": [
                        {"name": "echo", "type": "testComponent/v1/echo", "parameters": {"x": "=myTrigger.x"}}
                    ]
                }
                """);

        workflowNodeTestOutputService.save(
            workflowId, "myTrigger", WorkflowNodeType.ofType(COMPONENT_NAME + "/v1/start"),
            new OutputResponse(
                new StringProperty(ComponentDsl.string("sample")), List.of(Map.of("x", 1), Map.of("y", 2))),
            false, ENVIRONMENT_ID);

        workflowTestConfigurationService.saveWorkflowTestConfiguration(
            new WorkflowTestConfiguration(ENVIRONMENT_ID, Map.of("foo", "bar"), workflowId, List.of()));

        WorkflowTestExecutionDTO result = testWorkflowExecutor.executeSync(workflowId, Map.of(), ENVIRONMENT_ID);

        JobDTO jobDTO = result.job();

        assertThat(jobDTO.status()).isEqualTo(Job.Status.COMPLETED);
        assertThat(toObjectMap(jobDTO.inputs())).containsEntry("foo", "bar");
        assertThat(jobDTO.inputs()
            .get("myTrigger")).isEqualTo(Map.of("x", 1));
        assertThat(jobDTO.taskExecutions())
            .singleElement()
            .satisfies(taskExecutionDTO -> assertThat(taskExecutionDTO.output()).isEqualTo(Map.of("x", 1)));

        TriggerExecutionDTO triggerExecutionDTO = result.triggerExecution();

        assertThat(triggerExecutionDTO).isNotNull();
        assertThat(triggerExecutionDTO.title()).isEqualTo(COMPONENT_TITLE);
        assertThat(triggerExecutionDTO.input()).isEqualTo(Map.of("foo", "bar"));
    }

    @Test
    void executeAsyncSuccessfulExecutionFlow() {
        String workflowId = createWorkflow(getEchoWorkflowDefinition());

        AtomicReference<String> afterStartKey = new AtomicReference<>();
        AtomicReference<String> afterFutureKey = new AtomicReference<>();
        AtomicReference<String> whenCompleteKey = new AtomicReference<>();
        RecordingSseStreamBridge sseStreamBridge = new RecordingSseStreamBridge();

        testWorkflowExecutor.executeAsync(
            workflowId, Map.of("key", "value"), ENVIRONMENT_ID, afterStartKey::set, key -> sseStreamBridge,
            (key, future) -> {
                afterFutureKey.set(key);

                future.join();
            },
            whenCompleteKey::set);

        assertThat(afterStartKey.get())
            .isNotNull()
            .isEqualTo(afterFutureKey.get())
            .isEqualTo(whenCompleteKey.get());

        long jobId = getStartedJobId(sseStreamBridge);

        assertThat(afterStartKey.get()).isEqualTo(TenantCacheKeyUtils.getKey(jobId));

        List<String> eventNames = sseStreamBridge.getEventNames();

        assertThat(eventNames)
            .filteredOn(eventName -> eventName.equals("start") || eventName.equals("result"))
            .containsExactly("start", "result");

        WorkflowTestExecutionDTO result = getResult(sseStreamBridge);

        JobDTO jobDTO = result.job();

        assertThat(jobDTO.id()).isEqualTo(jobId);
        assertThat(toObjectMap(jobDTO.inputs())).containsEntry("key", "value");
        assertThat(jobDTO.status()).isEqualTo(Job.Status.COMPLETED);
    }

    @Test
    void executeAsyncGenericExceptionSendsErrorEvent() throws InterruptedException {
        String workflowId = createWorkflow(getProbedBlockingWorkflowDefinition());

        AtomicReference<String> whenCompleteKey = new AtomicReference<>();
        CountDownLatch whenCompleteLatch = new CountDownLatch(1);
        RecordingSseStreamBridge sseStreamBridge = new RecordingSseStreamBridge();

        testWorkflowExecutor.executeAsync(
            workflowId, Map.of(), ENVIRONMENT_ID, key -> {}, key -> sseStreamBridge, (key, future) -> {}, key -> {
                whenCompleteKey.set(key);

                whenCompleteLatch.countDown();
            });

        TaskGate taskGate = taskGates.get("first");

        assertThat(taskGate.awaitEntered(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        evaluationProbe.armFailure();

        taskGate.release();

        assertThat(whenCompleteLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        assertThat(sseStreamBridge.getEventNames()).contains("start", "error")
            .doesNotContain("result");
        assertThat(sseStreamBridge.getEvents("error"))
            .singleElement()
            .satisfies(errorEvent -> assertThat(errorEvent.get("payload")).asString()
                .contains("Execution failed"));
        assertThat(whenCompleteKey.get()).isNotNull();
        assertThat(getJobStatuses(sseStreamBridge))
            .contains(Job.Status.COMPLETED.name())
            .doesNotContain(Job.Status.STOPPED.name());
        assertThat(taskGate.isInterrupted()).isFalse();
    }

    @Test
    void executeAsyncCallbackExecutionOrder() {
        String workflowId = createWorkflow(getEchoWorkflowDefinition());

        List<String> callbackOrder = new CopyOnWriteArrayList<>();

        testWorkflowExecutor.executeAsync(
            workflowId, Map.of(), ENVIRONMENT_ID, key -> callbackOrder.add("afterStart"),
            key -> {
                callbackOrder.add("sseStreamBridgeFactory");

                return new RecordingSseStreamBridge();
            },
            (key, future) -> {
                callbackOrder.add("afterFuture");

                future.join();

                callbackOrder.add("afterFutureComplete");
            },
            key -> callbackOrder.add("whenComplete"));

        assertThat(callbackOrder).containsExactly(
            "afterStart", "sseStreamBridgeFactory", "afterFuture", "afterFutureComplete", "whenComplete");
    }

    @Test
    void executeAsyncListenerRegistrationAndUnregistration() throws InterruptedException {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "first", "type": "testComponent/v1/echo", "parameters": {"value": "first"}},
                        {"name": "streamer", "type": "testComponent/v1/stream"},
                        {"name": "failing", "type": "testComponent/v1/fail"}
                    ]
                }
                """);

        CountDownLatch whenCompleteLatch = new CountDownLatch(1);
        RecordingSseStreamBridge sseStreamBridge = new RecordingSseStreamBridge();

        testWorkflowExecutor.executeAsync(
            workflowId, Map.of(), ENVIRONMENT_ID, key -> {}, key -> sseStreamBridge, (key, future) -> future.join(),
            key -> whenCompleteLatch.countDown());

        assertThat(whenCompleteLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        long jobId = getStartedJobId(sseStreamBridge);

        assertThat(getTaskEventPayloads(sseStreamBridge, "task_started"))
            .extracting(payload -> payload.get("name"))
            .containsExactly("first", "streamer", "failing");
        assertThat(getTaskEventPayloads(sseStreamBridge, "task_completed"))
            .extracting(payload -> payload.get("name"), payload -> payload.get("status"))
            .containsExactly(
                tuple("first", TaskExecution.Status.COMPLETED.name()),
                tuple("streamer", TaskExecution.Status.COMPLETED.name()));
        assertThat(getTaskEventPayloads(sseStreamBridge, "task_failed"))
            .singleElement()
            .satisfies(payload -> {
                assertThat(payload.get("name")).isEqualTo("failing");
                assertThat(payload.get("status")).isEqualTo(TaskExecution.Status.FAILED.name());
                assertThat(payload.get("error")).asString()
                    .contains(FAILED_TASK_MESSAGE);
                assertThat(payload).containsKeys("taskExecutionId", "startDate", "endDate");
            });
        assertThat(sseStreamBridge.getPayloads())
            .contains(Map.of("chunk", STREAMED_CHUNK));
        assertThat(sseStreamBridge.getEvents("job_status"))
            .extracting(event -> event.get("payload"))
            .contains(Map.of("jobId", String.valueOf(jobId), "status", Job.Status.FAILED.name()));

        List<String> eventNames = sseStreamBridge.getEventNames();

        assertThat(eventNames.getLast()).isEqualTo("result");

        List<Object> payloads = sseStreamBridge.getPayloads();

        int eventCount = payloads.size();

        assertThat(sseStreamBridge.awaitPayloadCount(eventCount + 1, 1500, TimeUnit.MILLISECONDS)).isFalse();
    }

    @Test
    void executeAsyncForwardsClusterElementCallEvents() throws Exception {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "agent", "type": "testComponent/v1/block", "parameters": {"gate": "agent"}}
                    ]
                }
                """);

        RecordingSseStreamBridge sseStreamBridge = new RecordingSseStreamBridge();

        CompletableFuture<Void> executeAsyncFuture = CompletableFuture.runAsync(
            () -> testWorkflowExecutor.executeAsync(
                workflowId, Map.of(), ENVIRONMENT_ID, key -> {}, key -> sseStreamBridge,
                (key, future) -> future.join(), key -> {}));

        TaskGate taskGate = taskGates.get("agent");

        assertThat(taskGate.awaitEntered(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(sseStreamBridge.awaitEvent(isEvent("task_started"), TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        long jobId = getStartedJobId(sseStreamBridge);

        List<Map<String, Object>> taskStartedPayloads = getTaskEventPayloads(sseStreamBridge, "task_started");

        Map<String, Object> taskStartedPayload = taskStartedPayloads.getFirst();

        long taskExecutionId = Long.parseLong((String) taskStartedPayload.get("taskExecutionId"));

        ClusterElementCallRecorder clusterElementCallRecorder = ClusterElementCallRecorder.getInstance();

        try {
            ClusterElementCallRef clusterElementCallRef = clusterElementCallRecorder.callStarted(
                jobId, taskExecutionId, "httpClient_1", "httpClient", null, null, null);

            clusterElementCallRecorder.callFailed(jobId, taskExecutionId, clusterElementCallRef, "boom");

            assertThat(
                sseStreamBridge.awaitEvent(isEvent("cluster_element_failed"), TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .isTrue();

            taskGate.release();

            executeAsyncFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            clusterElementCallRecorder.callStarted(jobId, taskExecutionId, "slack_1", "slack", null, null, null);
        } finally {
            clusterElementCallRecorder.drain(taskExecutionId);
        }

        List<String> clusterElementEventNames = sseStreamBridge.getEventNames()
            .stream()
            .filter(eventName -> eventName.startsWith("cluster_element_"))
            .toList();

        assertThat(clusterElementEventNames).containsExactly("cluster_element_started", "cluster_element_failed");

        List<Map<String, Object>> startedPayloads = getTaskEventPayloads(sseStreamBridge, "cluster_element_started");

        Map<String, Object> startedPayload = startedPayloads.getFirst();

        assertThat(startedPayload).containsEntry("status", "STARTED")
            .doesNotContainKey("endDate")
            .doesNotContainKey("error");

        List<Map<String, Object>> failedPayloads = getTaskEventPayloads(sseStreamBridge, "cluster_element_failed");

        Map<String, Object> failedPayload = failedPayloads.getFirst();

        assertThat(failedPayload).containsEntry("name", "httpClient_1")
            .containsEntry("status", "FAILED")
            .containsEntry("error", "boom")
            .containsEntry("taskExecutionId", String.valueOf(taskExecutionId))
            .containsKey("startDate")
            .containsKey("endDate");
    }

    @Test
    void executeAsyncForwardsEachCallRecordWithCappedValues() throws Exception {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "agent", "type": "testComponent/v1/block", "parameters": {"gate": "agent"}}
                    ]
                }
                """);

        RecordingSseStreamBridge sseStreamBridge = new RecordingSseStreamBridge();

        CompletableFuture<Void> executeAsyncFuture = CompletableFuture.runAsync(
            () -> testWorkflowExecutor.executeAsync(
                workflowId, Map.of(), ENVIRONMENT_ID, key -> {}, key -> sseStreamBridge,
                (key, future) -> future.join(), key -> {}));

        TaskGate taskGate = taskGates.get("agent");

        assertThat(taskGate.awaitEntered(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(sseStreamBridge.awaitEvent(isEvent("task_started"), TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        long jobId = getStartedJobId(sseStreamBridge);

        List<Map<String, Object>> taskStartedPayloads = getTaskEventPayloads(sseStreamBridge, "task_started");

        Map<String, Object> taskStartedPayload = taskStartedPayloads.getFirst();

        long taskExecutionId = Long.parseLong((String) taskStartedPayload.get("taskExecutionId"));

        ClusterElementCallRecorder clusterElementCallRecorder = ClusterElementCallRecorder.getInstance();

        try {
            ClusterElementCallRef clusterElementCallRef = clusterElementCallRecorder.callStarted(
                jobId, taskExecutionId, "agent_1", "tool", "search", "parent-1", "{\"q\":\"x\"}");

            clusterElementCallRecorder.callCompleted(jobId, taskExecutionId, clusterElementCallRef, "y".repeat(70_000));

            assertThat(
                sseStreamBridge.awaitEvent(isEvent("cluster_element_completed"), TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .isTrue();

            taskGate.release();

            executeAsyncFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } finally {
            clusterElementCallRecorder.drain(taskExecutionId);
        }

        List<Map<String, Object>> startedPayloads = getTaskEventPayloads(sseStreamBridge, "cluster_element_started");
        List<Map<String, Object>> completedPayloads = getTaskEventPayloads(
            sseStreamBridge, "cluster_element_completed");

        Map<String, Object> startedPayload = startedPayloads.getFirst();
        Map<String, Object> completedPayload = completedPayloads.getFirst();

        assertThat(startedPayload).containsEntry("parentCallId", "parent-1")
            .containsEntry("componentName", "tool")
            .containsEntry("toolName", "search")
            .containsEntry("callStatus", "STARTED")
            .containsEntry("input", "{\"q\":\"x\"}")
            .doesNotContainKey("output");
        assertThat(completedPayload).containsEntry("callStatus", "COMPLETED")
            .containsEntry("callId", startedPayload.get("callId"));
        assertThat((String) completedPayload.get("output")).hasSize(
            ClusterElementCallRecord.MAX_VALUE_LENGTH + "\n... [truncated, 70000 characters]".length());
    }

    @Test
    void executeAsyncForwardsEachCallsOwnDatesAndError() throws Exception {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "agent", "type": "testComponent/v1/block", "parameters": {"gate": "agent"}}
                    ]
                }
                """);

        RecordingSseStreamBridge sseStreamBridge = new RecordingSseStreamBridge();

        CompletableFuture<Void> executeAsyncFuture = CompletableFuture.runAsync(
            () -> testWorkflowExecutor.executeAsync(
                workflowId, Map.of(), ENVIRONMENT_ID, key -> {}, key -> sseStreamBridge,
                (key, future) -> future.join(), key -> {}));

        TaskGate taskGate = taskGates.get("agent");

        assertThat(taskGate.awaitEntered(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(sseStreamBridge.awaitEvent(isEvent("task_started"), TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        long jobId = getStartedJobId(sseStreamBridge);

        List<Map<String, Object>> taskStartedPayloads = getTaskEventPayloads(sseStreamBridge, "task_started");

        Map<String, Object> taskStartedPayload = taskStartedPayloads.getFirst();

        long taskExecutionId = Long.parseLong((String) taskStartedPayload.get("taskExecutionId"));

        ClusterElementCallRecorder clusterElementCallRecorder = ClusterElementCallRecorder.getInstance();

        try {
            ClusterElementCallRef firstClusterElementCallRef = clusterElementCallRecorder.callStarted(
                jobId, taskExecutionId, "agent_1", "tool", "search", null, "{\"q\":\"first\"}");

            clusterElementCallRecorder.callFailed(jobId, taskExecutionId, firstClusterElementCallRef, "first failed");

            TimeUnit.MILLISECONDS.sleep(20);

            ClusterElementCallRef secondClusterElementCallRef = clusterElementCallRecorder.callStarted(
                jobId, taskExecutionId, "agent_1", "tool", "search", null, "{\"q\":\"second\"}");

            clusterElementCallRecorder.callCompleted(jobId, taskExecutionId, secondClusterElementCallRef, "done");

            assertThat(
                sseStreamBridge.awaitEventCount(isClusterElementEvent(), 4, TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .isTrue();

            taskGate.release();

            executeAsyncFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } finally {
            clusterElementCallRecorder.drain(taskExecutionId);
        }

        List<Map<String, Object>> clusterElementPayloads = getClusterElementEventPayloads(sseStreamBridge);

        assertThat(clusterElementPayloads).hasSize(4);

        Map<String, Object> firstStartedPayload = clusterElementPayloads.get(0);
        Map<String, Object> firstFailedPayload = clusterElementPayloads.get(1);
        Map<String, Object> secondStartedPayload = clusterElementPayloads.get(2);
        Map<String, Object> secondCompletedPayload = clusterElementPayloads.get(3);

        assertThat(firstFailedPayload).containsEntry("callError", "first failed")
            .containsKey("callEndDate")
            .doesNotContainKey("input");
        assertThat(secondStartedPayload.get("callStartDate"))
            .isNotEqualTo(firstStartedPayload.get("callStartDate"));
        assertThat(secondStartedPayload).doesNotContainKey("callEndDate")
            .doesNotContainKey("callError")
            .containsEntry("input", "{\"q\":\"second\"}");
        assertThat(secondCompletedPayload).containsEntry("callStartDate", secondStartedPayload.get("callStartDate"))
            .containsKey("callEndDate")
            .containsEntry("output", "done")
            .doesNotContainKey("callError")
            .doesNotContainKey("input");
    }

    @Test
    void executeAsyncSendsStartAndResultEvents() {
        String workflowId = createWorkflow(getEchoWorkflowDefinition());

        RecordingSseStreamBridge sseStreamBridge = new RecordingSseStreamBridge();

        testWorkflowExecutor.executeAsync(
            workflowId, Map.of(), ENVIRONMENT_ID, key -> {}, key -> sseStreamBridge, (key, future) -> future.join(),
            key -> {});

        List<Map<String, Object>> startEvents = sseStreamBridge.getEvents("start");

        assertThat(startEvents).hasSize(1);

        Map<String, Object> startEvent = startEvents.getFirst();

        Object startPayload = startEvent.get("payload");

        assertThat(startPayload).isInstanceOf(Map.class);

        WorkflowTestExecutionDTO result = getResult(sseStreamBridge);

        JobDTO jobDTO = result.job();

        assertThat(((Map<?, ?>) startPayload).get("jobId")).isEqualTo(String.valueOf(jobDTO.id()));

        List<String> eventNames = sseStreamBridge.getEventNames();

        assertThat(eventNames.indexOf("start")).isLessThan(eventNames.indexOf("result"));
    }

    @Test
    void executeAsyncStreamsProgressSnapshotsBeforeResult() throws InterruptedException {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "first", "type": "testComponent/v1/echo"},
                        {"name": "second", "type": "testComponent/v1/block", "parameters": {"gate": "second"}}
                    ]
                }
                """);

        CountDownLatch whenCompleteLatch = new CountDownLatch(1);
        RecordingSseStreamBridge sseStreamBridge = new RecordingSseStreamBridge();

        testWorkflowExecutor.executeAsync(
            workflowId, Map.of(), ENVIRONMENT_ID, key -> {}, key -> sseStreamBridge, (key, future) -> {},
            key -> whenCompleteLatch.countDown());

        TaskGate taskGate = taskGates.get("second");

        assertThat(taskGate.awaitEntered(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(sseStreamBridge.awaitEvent(isEvent("progress"), TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        long jobId = getStartedJobId(sseStreamBridge);

        assertThat(sseStreamBridge.getEvents("progress"))
            .first()
            .satisfies(progressEvent -> assertThat(progressEvent.get("payload"))
                .isInstanceOfSatisfying(
                    WorkflowTestExecutionDTO.class, snapshot -> {
                        JobDTO snapshotJobDTO = snapshot.job();

                        assertThat(snapshotJobDTO.id()).isEqualTo(jobId);
                        assertThat(snapshotJobDTO.status()).isEqualTo(Job.Status.STARTED);
                        assertThat(snapshotJobDTO.taskExecutions())
                            .extracting(TaskExecutionDTO::workflowTask)
                            .extracting(WorkflowTask::getName)
                            .containsExactly("first", "second");
                    }));

        taskGate.release();

        assertThat(whenCompleteLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        List<String> eventNames = sseStreamBridge.getEventNames();

        assertThat(eventNames.getLast()).isEqualTo("result");
        assertThat(eventNames.stream()
            .filter(eventName -> eventName.equals("progress"))
            .count()).isEqualTo(1);
    }

    @Test
    void executeAsyncBuildsOneProgressSnapshotAtATime() throws InterruptedException {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "first",
                            "type": "testComponent/v1/block",
                            "parameters": {"gate": "first", "probe": "=evaluationProbe()"}
                        },
                        {"name": "second", "type": "testComponent/v1/block", "parameters": {"gate": "second"}}
                    ]
                }
                """);

        CountDownLatch whenCompleteLatch = new CountDownLatch(1);
        RecordingSseStreamBridge sseStreamBridge = new RecordingSseStreamBridge();

        testWorkflowExecutor.executeAsync(
            workflowId, Map.of(), ENVIRONMENT_ID, key -> {}, key -> sseStreamBridge, (key, future) -> {},
            key -> whenCompleteLatch.countDown());

        TaskGate firstTaskGate = taskGates.get("first");

        assertThat(firstTaskGate.awaitEntered(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        evaluationProbe.armBlockingFirstCall();

        assertThat(evaluationProbe.awaitFirstCallEntered(5, TimeUnit.SECONDS)).isTrue();

        firstTaskGate.release();

        TaskGate secondTaskGate = taskGates.get("second");

        assertThat(secondTaskGate.awaitEntered(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(evaluationProbe.awaitOverlappingCall(1500, TimeUnit.MILLISECONDS)).isFalse();

        evaluationProbe.releaseFirstCall();

        assertThat(sseStreamBridge.awaitEventCount(isEvent("progress"), 2, 5, TimeUnit.SECONDS)).isTrue();
        assertThat(evaluationProbe.getArmedCalls()).isEqualTo(2);

        evaluationProbe.reset();

        secondTaskGate.release();

        assertThat(whenCompleteLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void testStop() throws InterruptedException {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "first", "type": "testComponent/v1/block", "parameters": {"gate": "first"}}
                    ]
                }
                """);

        CountDownLatch whenCompleteLatch = new CountDownLatch(1);
        RecordingSseStreamBridge sseStreamBridge = new RecordingSseStreamBridge();

        testWorkflowExecutor.executeAsync(
            workflowId, Map.of(), ENVIRONMENT_ID, key -> {}, key -> sseStreamBridge, (key, future) -> {},
            key -> whenCompleteLatch.countDown());

        TaskGate taskGate = taskGates.get("first");

        assertThat(taskGate.awaitEntered(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(sseStreamBridge.awaitEvent(isEvent("start"), TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        long jobId = getStartedJobId(sseStreamBridge);

        testWorkflowExecutor.stop(jobId);

        assertThat(whenCompleteLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        WorkflowTestExecutionDTO result = getResult(sseStreamBridge);

        JobDTO jobDTO = result.job();

        assertThat(jobDTO.id()).isEqualTo(jobId);
        assertThat(jobDTO.status()).isEqualTo(Job.Status.STOPPED);
        assertThat(getJobStatuses(sseStreamBridge)).contains(Job.Status.STOPPED.name());
    }

    @Test
    void executeAsyncCancelledFutureStopsJob() throws InterruptedException {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "first", "type": "testComponent/v1/block", "parameters": {"gate": "first"}}
                    ]
                }
                """);

        AtomicReference<CompletableFuture<WorkflowTestExecutionDTO>> futureReference = new AtomicReference<>();
        CountDownLatch whenCompleteLatch = new CountDownLatch(1);
        RecordingSseStreamBridge sseStreamBridge = new RecordingSseStreamBridge();

        testWorkflowExecutor.executeAsync(
            workflowId, Map.of(), ENVIRONMENT_ID, key -> {}, key -> sseStreamBridge,
            (key, future) -> futureReference.set(future), key -> whenCompleteLatch.countDown());

        TaskGate taskGate = taskGates.get("first");

        assertThat(taskGate.awaitEntered(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<WorkflowTestExecutionDTO> future = futureReference.get();

        future.cancel(true);

        assertThat(whenCompleteLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(sseStreamBridge.getEventNames()).contains("error")
            .doesNotContain("result");
        assertThat(waitUntil(taskGate::isInterrupted)).isTrue();
    }

    private String createWorkflow(String definition) {
        Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        return Objects.requireNonNull(workflow.getId());
    }

    private static String getEchoWorkflowDefinition() {
        return """
            {
                "tasks": [
                    {"name": "echo", "type": "testComponent/v1/echo", "parameters": {"value": "echoed"}}
                ]
            }
            """;
    }

    private static List<String> getJobStatuses(RecordingSseStreamBridge sseStreamBridge) {
        List<String> jobStatuses = new ArrayList<>();

        for (Map<String, Object> event : sseStreamBridge.getEvents("job_status")) {
            Map<?, ?> payload = (Map<?, ?>) event.get("payload");

            jobStatuses.add((String) payload.get("status"));
        }

        return jobStatuses;
    }

    private static String getProbedBlockingWorkflowDefinition() {
        return """
            {
                "tasks": [
                    {
                        "name": "first",
                        "type": "testComponent/v1/block",
                        "parameters": {"gate": "first", "probe": "=evaluationProbe()"}
                    }
                ]
            }
            """;
    }

    private static WorkflowTestExecutionDTO getResult(RecordingSseStreamBridge sseStreamBridge) {
        List<Map<String, Object>> resultEvents = sseStreamBridge.getEvents("result");

        assertThat(resultEvents).hasSize(1);

        Map<String, Object> resultEvent = resultEvents.getFirst();

        Object payload = resultEvent.get("payload");

        assertThat(payload).isInstanceOf(WorkflowTestExecutionDTO.class);

        return (WorkflowTestExecutionDTO) payload;
    }

    private static long getStartedJobId(RecordingSseStreamBridge sseStreamBridge) {
        List<Map<String, Object>> startEvents = sseStreamBridge.getEvents("start");

        assertThat(startEvents).hasSize(1);

        Map<String, Object> startEvent = startEvents.getFirst();

        Map<?, ?> payload = (Map<?, ?>) startEvent.get("payload");

        return Long.parseLong((String) payload.get("jobId"));
    }

    private static List<Map<String, Object>> getClusterElementEventPayloads(RecordingSseStreamBridge sseStreamBridge) {
        List<Map<String, Object>> payloads = new ArrayList<>();

        for (Object payload : sseStreamBridge.getPayloads()) {
            if (isClusterElementEvent().test(payload)) {
                Map<?, ?> event = (Map<?, ?>) payload;

                payloads.add(toObjectMap((Map<?, ?>) event.get("payload")));
            }
        }

        return payloads;
    }

    private static List<Map<String, Object>> getTaskEventPayloads(
        RecordingSseStreamBridge sseStreamBridge, String eventName) {

        List<Map<String, Object>> payloads = new ArrayList<>();

        for (Map<String, Object> event : sseStreamBridge.getEvents(eventName)) {
            payloads.add(toObjectMap((Map<?, ?>) event.get("payload")));
        }

        return payloads;
    }

    private static Map<String, Object> toObjectMap(Map<?, ?> map) {
        Map<String, Object> objectMap = new HashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            objectMap.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        return objectMap;
    }

    private static Predicate<Object> isClusterElementEvent() {
        return payload -> payload instanceof Map<?, ?> map && map.get("event") instanceof String eventName &&
            eventName.startsWith("cluster_element_");
    }

    private static Predicate<Object> isEvent(String eventName) {
        return payload -> payload instanceof Map<?, ?> map && eventName.equals(map.get("event"));
    }

    private static boolean waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);

        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }

            TimeUnit.MILLISECONDS.sleep(10);
        }

        return condition.getAsBoolean();
    }

    private static final class RecordingSseStreamBridge implements SseStreamBridge {

        private final List<Object> payloads = new ArrayList<>();

        @Override
        public synchronized void onEvent(Object payload) {
            payloads.add(payload);

            notifyAll();
        }

        synchronized boolean awaitEvent(Predicate<Object> predicate, long timeout, TimeUnit timeUnit)
            throws InterruptedException {

            return awaitEventCount(predicate, 1, timeout, timeUnit);
        }

        synchronized boolean awaitEventCount(Predicate<Object> predicate, int count, long timeout, TimeUnit timeUnit)
            throws InterruptedException {

            long deadline = System.nanoTime() + timeUnit.toNanos(timeout);

            while (countPayloads(predicate) < count) {
                long remainingNanos = deadline - System.nanoTime();

                if (remainingNanos <= 0) {
                    return false;
                }

                TimeUnit.NANOSECONDS.timedWait(this, remainingNanos);
            }

            return true;
        }

        synchronized boolean awaitPayloadCount(int count, long timeout, TimeUnit timeUnit)
            throws InterruptedException {

            return awaitEventCount(payload -> true, count, timeout, timeUnit);
        }

        synchronized List<String> getEventNames() {
            List<String> eventNames = new ArrayList<>();

            for (Object payload : payloads) {
                if (payload instanceof Map<?, ?> map && map.get("event") instanceof String eventName) {
                    eventNames.add(eventName);
                }
            }

            return eventNames;
        }

        synchronized List<Map<String, Object>> getEvents(String eventName) {
            List<Map<String, Object>> events = new ArrayList<>();

            for (Object payload : payloads) {
                if (payload instanceof Map<?, ?> map && eventName.equals(map.get("event"))) {
                    events.add(toObjectMap(map));
                }
            }

            return events;
        }

        synchronized List<Object> getPayloads() {
            return new ArrayList<>(payloads);
        }

        private int countPayloads(Predicate<Object> predicate) {
            int count = 0;

            for (Object payload : payloads) {
                if (predicate.test(payload)) {
                    count++;
                }
            }

            return count;
        }
    }
}
