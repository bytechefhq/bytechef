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

package com.bytechef.component.ai.agent.utils.cluster.subagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallEvent;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallEvent.Type;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentExecutor;
import org.springaicommunity.agent.common.task.subagent.TaskCall;

/**
 * @author Ivica Cardic
 */
class ClusterElementCallObservingSubagentExecutorTest {

    private static final TaskCall TASK_CALL = new TaskCall(
        "Find it", "Find the thing", "Analyst", null, null, Boolean.FALSE);

    private final SubagentExecutor delegate = mock(SubagentExecutor.class);
    private final List<ClusterElementCallEvent> events = new CopyOnWriteArrayList<>();
    private final ClusterElementCallRecorder recorder = new ClusterElementCallRecorder();
    private final ClusterElementCallObservingSubagentExecutor subagentExecutor =
        new ClusterElementCallObservingSubagentExecutor(delegate, 1, 10, recorder);

    @BeforeEach
    void beforeEach() {
        recorder.addListener(1, events::add);
    }

    @Test
    void testReportsCompleted() {
        ClusterElementSubagentDefinition subagentDefinition = ClusterElementSubagentDefinition.of(clusterElement());

        when(delegate.execute(TASK_CALL, subagentDefinition)).thenReturn("done");

        assertThat(subagentExecutor.execute(TASK_CALL, subagentDefinition)).isEqualTo("done");
        assertThat(getTypes()).containsExactly(Type.STARTED, Type.COMPLETED);
        assertThat(events.getFirst()
            .call()
            .workflowNodeName()).isEqualTo("subagent_1");
    }

    @Test
    void testReportsFailedAndRethrows() {
        ClusterElementSubagentDefinition subagentDefinition = ClusterElementSubagentDefinition.of(clusterElement());
        IllegalStateException exception = new IllegalStateException("no model");

        when(delegate.execute(TASK_CALL, subagentDefinition)).thenThrow(exception);

        assertThatThrownBy(() -> subagentExecutor.execute(TASK_CALL, subagentDefinition)).isSameAs(exception);
        assertThat(getTypes()).containsExactly(Type.STARTED, Type.FAILED);
        assertThat(events.getLast()
            .call()
            .error()).isEqualTo("no model");
    }

    @Test
    void testNonClusterElementDefinitionIsNotReported() {
        SubagentDefinition subagentDefinition = mock(SubagentDefinition.class);

        when(delegate.execute(TASK_CALL, subagentDefinition)).thenReturn("done");

        assertThat(subagentExecutor.execute(TASK_CALL, subagentDefinition)).isEqualTo("done");
        assertThat(events).isEmpty();
    }

    @Test
    void testBackgroundRunReportsAgainstCapturedIds() {
        ClusterElementSubagentDefinition subagentDefinition = ClusterElementSubagentDefinition.of(clusterElement());

        when(delegate.execute(TASK_CALL, subagentDefinition)).thenReturn("done");

        CompletableFuture.runAsync(() -> subagentExecutor.execute(TASK_CALL, subagentDefinition))
            .join();

        assertThat(events).extracting(ClusterElementCallEvent::taskExecutionId)
            .containsOnly(10L);
    }

    @Test
    void testGetKindDelegates() {
        when(delegate.getKind()).thenReturn(ClusterElementSubagentDefinition.KIND);

        assertThat(subagentExecutor.getKind()).isEqualTo(ClusterElementSubagentDefinition.KIND);
    }

    private static ClusterElement clusterElement() {
        return new ClusterElement(
            null, null, Map.of(), null, "aiAgentUtils/v1/subagentTool",
            Map.of("subagentName", "Analyst", "description", "Reads", "instructions", "Answer."), "subagent_1");
    }

    private List<Type> getTypes() {
        return events.stream()
            .map(ClusterElementCallEvent::type)
            .toList();
    }
}
