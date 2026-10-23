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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCall;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCall.Status;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.workflow.execution.constant.TaskExecutionMetadataConstants;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ClusterElementCallsTaskExecutionPostOutputProcessorTest {

    private final ClusterElementCallRecorder recorder = new ClusterElementCallRecorder();
    private final ClusterElementCallsTaskExecutionPostOutputProcessor processor =
        new ClusterElementCallsTaskExecutionPostOutputProcessor(recorder);

    @Test
    void testWritesDrainedCallsToMetadata() {
        TaskExecution taskExecution = createTaskExecution();

        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callCompleted(1, 10, "httpClient_1");
        recorder.callStarted(1, 10, "slack_1");
        recorder.callFailed(1, 10, "slack_1", "boom");

        processor.onTaskExecutionFinished(taskExecution);

        Map<String, ClusterElementCall> calls = getCalls(taskExecution);

        assertEquals(Status.COMPLETED, calls.get("httpClient_1")
            .status());
        assertEquals(Status.FAILED, calls.get("slack_1")
            .status());
        assertEquals("boom", calls.get("slack_1")
            .error());
    }

    @Test
    void testMergesWithExistingMetadata() {
        TaskExecution taskExecution = createTaskExecution();

        Instant startDate = Instant.parse("2026-10-23T10:00:00Z");

        taskExecution.putMetadata(
            TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS,
            List.of(
                new ClusterElementCall("httpClient_1", Status.FAILED, startDate, startDate.plusSeconds(1), "boom")
                    .toMap()));

        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callCompleted(1, 10, "httpClient_1");

        processor.onTaskExecutionFinished(taskExecution);

        ClusterElementCall call = getCalls(taskExecution).get("httpClient_1");

        assertEquals(Status.FAILED, call.status());
        assertEquals(startDate, call.startDate());
        assertEquals("boom", call.error());
    }

    @Test
    void testLeavesOutputUnchanged() {
        Object output = Map.of("response", "text");

        recorder.callStarted(1, 10, "httpClient_1");

        assertSame(output, processor.process(createTaskExecution(), output));
    }

    @Test
    void testProcessLeavesCallsForTheFinishedHook() {
        TaskExecution taskExecution = createTaskExecution();

        recorder.callStarted(1, 10, "httpClient_1");

        assertEquals("output", processor.process(taskExecution, "output"));
        assertFalse(taskExecution.getMetadata()
            .containsKey(TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS));

        processor.onTaskExecutionFinished(taskExecution);

        assertEquals(Status.STARTED, getCalls(taskExecution).get("httpClient_1")
            .status());
    }

    @Test
    void testNoCallsLeavesMetadataUntouched() {
        TaskExecution taskExecution = createTaskExecution();

        processor.onTaskExecutionFinished(taskExecution);

        assertFalse(taskExecution.getMetadata()
            .containsKey(TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS));
    }

    private static TaskExecution createTaskExecution() {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(10L);

        return taskExecution;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ClusterElementCall> getCalls(TaskExecution taskExecution) {
        List<Map<String, ?>> calls = (List<Map<String, ?>>) taskExecution.getMetadata()
            .get(TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS);

        return calls.stream()
            .map(ClusterElementCall::fromMap)
            .collect(java.util.stream.Collectors.toMap(ClusterElementCall::workflowNodeName, call -> call));
    }
}
