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

package com.bytechef.component.ai.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiAgentChatToolTest {

    private final AtomicReference<ActionContext> performedContext = new AtomicReference<>();

    /**
     * A nested agent's own tools resolve the running workflow from the context they receive, so the parent run's job
     * must stay reachable through the adapter the nested agent is performed with.
     */
    @Test
    void testNestedAgentContextKeepsTheParentJobReachable() throws Exception {
        ClusterElementContextAware clusterElementContext = mock(ClusterElementContextAware.class);
        ActionContext parentJobActionContext = mock(ActionContext.class);

        when(clusterElementContext.getEnvironmentId()).thenReturn(2L);
        when(clusterElementContext.toActionContext("component", 1, "action", null))
            .thenReturn(parentJobActionContext);

        performWith(clusterElementContext);

        assertThat(performedContext.get()).isInstanceOf(JobContextAware.class);

        JobContextAware jobContextAware = (JobContextAware) performedContext.get();

        assertThat(jobContextAware.getEnvironmentId()).isEqualTo(2L);
        assertThat(jobContextAware.toActionContext("component", 1, "action", null)).isSameAs(parentJobActionContext);
    }

    @Test
    void testContextWithoutAJobIsNotJobAware() throws Exception {
        performWith(mock(ClusterElementContext.class));

        assertThat(performedContext.get()).isNotNull()
            .isNotInstanceOf(JobContextAware.class);
    }

    private void performWith(ClusterElementContext context) throws Exception {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);
        MultipleConnectionsPerformFunction performFunction = (
            inputParameters, componentConnections, extensions, actionContext) -> {

            performedContext.set(actionContext);

            return null;
        };

        doReturn(Optional.of(performFunction)).when(actionDefinition)
            .getPerform();

        MultipleConnectionsToolFunction toolFunction = AiAgentChatTool.of(actionDefinition)
            .getElement();

        toolFunction.apply(mock(Parameters.class), mock(Parameters.class), mock(Parameters.class), Map.of(), context);
    }
}
