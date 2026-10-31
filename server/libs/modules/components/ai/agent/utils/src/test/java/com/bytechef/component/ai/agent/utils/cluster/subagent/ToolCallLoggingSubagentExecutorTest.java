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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRef;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallScope;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springaicommunity.agent.common.task.subagent.SubagentExecutor;
import org.springaicommunity.agent.common.task.subagent.TaskCall;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ToolCallLoggingSubagentExecutorTest {

    private static final TaskCall TASK_CALL = new TaskCall(
        "Find it", "Find the thing", "Analyst", null, null, Boolean.FALSE);

    private final ActionContext context = mock(ActionContext.class);
    private final SubagentExecutor delegate = mock(SubagentExecutor.class);
    private final List<String> loggedCallIds = new CopyOnWriteArrayList<>();
    private final List<String> loggedErrors = new CopyOnWriteArrayList<>();
    private final List<String> loggedMessages = new CopyOnWriteArrayList<>();
    private final ClusterElementCallRecorder recorder = new ClusterElementCallRecorder();
    private final ClusterElementCallObservingSubagentExecutor subagentExecutor =
        new ClusterElementCallObservingSubagentExecutor(
            new ToolCallLoggingSubagentExecutor(delegate, context), 1, 10, recorder);

    @BeforeEach
    void beforeEach() {
        Context.Log log = mock(Context.Log.class);

        doAnswer(invocation -> {
            loggedMessages.add(invocation.getArgument(0));

            return null;
        }).when(log)
            .info(anyString());

        doAnswer(invocation -> {
            loggedErrors.add(invocation.getArgument(0));

            return null;
        }).when(log)
            .error(anyString(), any(Exception.class));

        doAnswer(invocation -> {
            ClusterElementCallRef clusterElementCallRef = ClusterElementCallScope.current();

            loggedCallIds.add(clusterElementCallRef == null ? "none" : clusterElementCallRef.id());

            Context.ContextConsumer<Context.Log> logConsumer = invocation.getArgument(0);

            logConsumer.accept(log);

            return null;
        }).when(context)
            .log(any());
    }

    @Test
    void testLogsThePromptAndResultInsideTheSubagentsCall() {
        ClusterElementSubagentDefinition subagentDefinition = ClusterElementSubagentDefinition.of(clusterElement());

        when(delegate.execute(TASK_CALL, subagentDefinition)).thenReturn("done");

        subagentExecutor.execute(TASK_CALL, subagentDefinition);

        List<ClusterElementCallRecord> records = recorder.drain(10)
            .records();

        assertThat(records).hasSize(1);
        assertThat(loggedCallIds).containsExactly(records.getFirst()
            .id());
        assertThat(loggedMessages).containsExactly(
            "{\"inputs\":{\"prompt\":\"Find the thing\"},\"output\":\"done\",\"toolName\":\"Analyst\"}");
    }

    @Test
    void testLogsAFailedRunAndRethrows() {
        ClusterElementSubagentDefinition subagentDefinition = ClusterElementSubagentDefinition.of(clusterElement());

        when(delegate.execute(TASK_CALL, subagentDefinition)).thenThrow(new IllegalStateException("model down"));

        assertThatThrownBy(() -> subagentExecutor.execute(TASK_CALL, subagentDefinition))
            .isInstanceOf(IllegalStateException.class);

        recorder.drain(10);

        assertThat(loggedErrors).containsExactly(
            "{\"error\":\"model down\",\"inputs\":{\"prompt\":\"Find the thing\"},\"toolName\":\"Analyst\"}");
    }

    private static ClusterElement clusterElement() {
        return new ClusterElement(
            null, null, Map.of(), null, "aiAgentUtils/v1/subagentTool",
            Map.of("subagentName", "Analyst", "description", "Reads", "instructions", "Answer."), "subagent_1");
    }
}
