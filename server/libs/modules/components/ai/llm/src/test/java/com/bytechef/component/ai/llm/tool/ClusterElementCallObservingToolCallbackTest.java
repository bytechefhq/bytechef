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
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
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
    }

    @Test
    void testReportsCompleted() {
        when(delegate.call("{}")).thenReturn("result");

        String result = createToolCallback(null).call("{}");

        assertThat(result).isEqualTo("result");
        assertThat(getTypes()).containsExactly(Type.STARTED, Type.COMPLETED);
        assertThat(events.getFirst()
            .call()
            .workflowNodeName()).isEqualTo("httpClient_1");
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
            .call()
            .error()).isEqualTo("boom");
    }

    @Test
    void testReturnedErrorResultIsCompleted() {
        when(delegate.call("{}")).thenReturn("{\"error\":\"404\"}");

        createToolCallback(null).call("{}");

        assertThat(getTypes()).containsExactly(Type.STARTED, Type.COMPLETED);
    }

    @Test
    void testSuspendedResultReportsNoOutcome() {
        Suspend suspend = new Suspend(
            Map.of(ToolSuspension.SUSPENDED_TOOL_RESULT, "suspended-result"), Instant.parse("2026-10-24T10:00:00Z"));

        when(delegate.call("{}")).thenReturn(ToolSuspension.getSuspendedToolResult(suspend));

        createToolCallback(suspend).call("{}");

        assertThat(getTypes()).containsExactly(Type.STARTED);
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
            ClusterElementCallObservingToolCallback.wrap(toolCallbacks, "httpClient_1", mock(ActionContext.class)))
                .isSameAs(toolCallbacks);
    }

    private ToolCallback createToolCallback(Suspend suspend) {
        return new ClusterElementCallObservingToolCallback(delegate, "httpClient_1", 1, 10, recorder, () -> suspend);
    }

    private List<Type> getTypes() {
        return events.stream()
            .map(ClusterElementCallEvent::type)
            .toList();
    }
}
