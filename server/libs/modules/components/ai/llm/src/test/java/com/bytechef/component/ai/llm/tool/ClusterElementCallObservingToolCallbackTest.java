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

package com.bytechef.component.ai.llm.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallEvent;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallEvent.Type;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord.Status;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRef;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallScope;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * @author Ivica Cardic
 */
class ClusterElementCallObservingToolCallbackTest {

    private final List<ClusterElementCallEvent> events = new CopyOnWriteArrayList<>();
    private final ClusterElementCallRecorder recorder = new ClusterElementCallRecorder();
    private final ToolCallback delegate = mock(ToolCallback.class);

    @BeforeEach
    void beforeEach() {
        recorder.addListener(1, events::add);

        when(delegate.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("get")
                .inputSchema("{}")
                .build());
    }

    @AfterEach
    void afterEach() {
        recorder.drain(10);
    }

    @Test
    void testRecordsInputOutputAndToolName() {
        when(delegate.call("{\"q\":1}")).thenReturn("result");

        String result = createToolCallback(null).call("{\"q\":1}");

        assertThat(result).isEqualTo("result");
        assertThat(getTypes()).containsExactly(Type.STARTED, Type.COMPLETED);
        assertThat(events.getFirst()
            .call()
            .workflowNodeName()).isEqualTo("httpClient_1");

        List<ClusterElementCallRecord> records = recorder.drain(10)
            .records();

        assertThat(records).hasSize(1);

        ClusterElementCallRecord callRecord = records.getFirst();

        assertThat(callRecord.status()).isEqualTo(Status.COMPLETED);
        assertThat(callRecord.input()).isEqualTo("{\"q\":1}");
        assertThat(callRecord.output()).isEqualTo("result");
        assertThat(callRecord.toolName()).isEqualTo("get");
        assertThat(callRecord.componentName()).isEqualTo("httpClient");
    }

    @Test
    void testReportsCompletedWithToolContext() {
        ToolContext toolContext = new ToolContext(Map.of());

        when(delegate.call("{}", toolContext)).thenReturn("result");

        assertThat(createToolCallback(null).call("{}", toolContext)).isEqualTo("result");
        assertThat(getTypes()).containsExactly(Type.STARTED, Type.COMPLETED);
    }

    @Test
    void testReportsFailedAndRethrows() {
        IllegalStateException exception = new IllegalStateException("boom");

        when(delegate.call("{}")).thenThrow(exception);

        assertThatThrownBy(() -> createToolCallback(null).call("{}")).isSameAs(exception);
        assertThat(getTypes()).containsExactly(Type.STARTED, Type.FAILED);
        assertThat(events.getLast()
            .record()
            .error()).isEqualTo("boom");
    }

    @Test
    void testReturnedErrorResultIsCompleted() {
        when(delegate.call("{}")).thenReturn("{\"error\":\"404\"}");

        createToolCallback(null).call("{}");

        assertThat(getTypes()).containsExactly(Type.STARTED, Type.COMPLETED);
    }

    @Test
    void testBindsTheCallWhileTheToolRuns() {
        AtomicReference<ClusterElementCallRef> seen = new AtomicReference<>();

        when(delegate.call("{}")).thenAnswer(invocation -> {
            seen.set(ClusterElementCallScope.current());

            return "result";
        });

        createToolCallback(null).call("{}");

        ClusterElementCallRecord callRecord = recorder.drain(10)
            .records()
            .getFirst();

        assertThat(seen.get()).isNotNull();
        assertThat(seen.get()
            .id()).isEqualTo(callRecord.id());
        assertThat(seen.get()
            .workflowNodeName()).isEqualTo("httpClient_1");
        assertThat(ClusterElementCallScope.current()).isNull();
    }

    @Test
    void testTakesItsParentFromTheEnclosingCall() {
        when(delegate.call("{}")).thenReturn("result");

        ClusterElementCallRef parentRef = new ClusterElementCallRef("parent-id", "aiAgent_2");

        ClusterElementCallScope.callWith(parentRef, () -> createToolCallback(null).call("{}"));

        assertThat(recorder.drain(10)
            .records()
            .getFirst()
            .parentId()).isEqualTo("parent-id");
    }

    @Test
    void testSuspendedResultMarksTheCallSuspended() {
        Suspend suspend = new Suspend(
            Map.of(ToolSuspension.SUSPENDED_TOOL_RESULT, "suspended-result"), Instant.parse("2026-10-24T10:00:00Z"));

        when(delegate.call("{}")).thenReturn(ToolSuspension.getSuspendedToolResult(suspend));

        createToolCallback(suspend).call("{}");

        assertThat(getTypes()).containsExactly(Type.STARTED);

        ClusterElementCallRef suspendedRef = recorder.findSuspendedCall(10)
            .orElseThrow();

        List<ClusterElementCallRecord> records = recorder.drain(10)
            .records();

        assertThat(records).hasSize(1);
        assertThat(records.getFirst()
            .status()).isEqualTo(Status.SUSPENDED);
        assertThat(records.getFirst()
            .id()).isEqualTo(suspendedRef.id());
    }

    @Test
    void testConcurrentCallsKeepTheirOwnScope() throws Exception {
        CountDownLatch bothStarted = new CountDownLatch(2);
        List<String> seenIds = new CopyOnWriteArrayList<>();

        when(delegate.call("{}")).thenAnswer(invocation -> {
            bothStarted.countDown();

            assertThat(bothStarted.await(5, TimeUnit.SECONDS)).isTrue();

            ClusterElementCallRef current = ClusterElementCallScope.current();

            seenIds.add(current == null ? "none" : current.id());

            return "result";
        });

        ToolCallback toolCallback = createToolCallback(null);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            Future<String> first = executorService.submit(() -> toolCallback.call("{}"));
            Future<String> second = executorService.submit(() -> toolCallback.call("{}"));

            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executorService.shutdownNow();
        }

        List<ClusterElementCallRecord> records = recorder.drain(10)
            .records();

        assertThat(records).hasSize(2);
        assertThat(records).extracting(ClusterElementCallRecord::parentId)
            .containsOnlyNulls();
        assertThat(seenIds).containsExactlyInAnyOrderElementsOf(
            records.stream()
                .map(ClusterElementCallRecord::id)
                .toList());
    }

    @Test
    void testDelegatesDefinitionAndMetadata() {
        ToolDefinition toolDefinition = ToolDefinition.builder()
            .name("get")
            .inputSchema("{}")
            .build();
        ToolMetadata toolMetadata = ToolMetadata.builder()
            .returnDirect(true)
            .build();

        when(delegate.getToolDefinition()).thenReturn(toolDefinition);
        when(delegate.getToolMetadata()).thenReturn(toolMetadata);

        ToolCallback toolCallback = createToolCallback(null);

        assertThat(toolCallback.getToolDefinition()).isSameAs(toolDefinition);
        assertThat(toolCallback.getToolMetadata()).isSameAs(toolMetadata);
    }

    @Test
    void testWrapWithoutIdsReturnsSameCallbacks() {
        List<ToolCallback> toolCallbacks = List.of(delegate);

        assertThat(
            ClusterElementCallObservingToolCallback.wrap(
                toolCallbacks, "httpClient_1", "httpClient", mock(ActionContext.class))).isSameAs(toolCallbacks);
    }

    private ToolCallback createToolCallback(Suspend suspend) {
        return new ClusterElementCallObservingToolCallback(
            delegate, "httpClient_1", "httpClient", 1, 10, recorder, () -> suspend);
    }

    private List<Type> getTypes() {
        return events.stream()
            .map(ClusterElementCallEvent::type)
            .toList();
    }
}
