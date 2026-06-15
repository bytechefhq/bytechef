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

package com.bytechef.component.approval.cluster.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the HITL contract for the approval tool: invoking the tool routes the suspend onto the agent's live
 * {@link ActionContext} (via the {@link ClusterElementContextAware#toActionContext} cast) and returns
 * {@link ToolSuspendConstants#SUSPENDED_SENTINEL} so {@code SuspendableToolCallingManager} can find the pending tool
 * response at suspend time.
 *
 * @author Ivica Cardic
 */
class ApprovalRequestApprovalToolTest {

    @Test
    void testToolReturnsSuspendSentinelAfterInvokingPerform() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);
        ClusterElementContextAware clusterElementContextAware = mock(ClusterElementContextAware.class);
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        // toActionContext returns an ActionContext that is also ActionContextAware — the action's perform casts to it.
        when(
            clusterElementContextAware.toActionContext(
                "approval", 1, "requestApproval", null)).thenReturn(actionContextAware);

        // Skip the approval-channel loop inside perform.
        when(actionContextAware.isEditorEnvironment()).thenReturn(true);
        when(actionContextAware.getResumeUrl()).thenReturn("https://example.com/job/resume/abc");

        ModifiableClusterElementDefinition<MultipleConnectionsToolFunction> definition =
            ApprovalRequestApprovalTool.of(clusterElementDefinitionService);

        MultipleConnectionsToolFunction toolFunction = definition.getElement();

        assertNotNull(toolFunction, "Cluster element must expose the tool function");

        Parameters inputParameters = mock(Parameters.class);
        Parameters connectionParameters = mock(Parameters.class);
        Parameters extensions = mock(Parameters.class);

        when(inputParameters.toMap()).thenReturn(Map.of());
        when(extensions.toMap()).thenReturn(Map.of());

        Object result = toolFunction.apply(
            inputParameters, connectionParameters, extensions, Map.of(), clusterElementContextAware);

        assertEquals(
            ToolSuspendConstants.SUSPENDED_SENTINEL, result,
            "Tool must return the suspend sentinel so the agent loop can pair it with the pending tool call id.");

        // Verify the action's perform actually ran against the same ActionContext the agent owns (this is the cast
        // that makes the cross-tool suspend protocol work — see ApprovalRequestApprovalTool's class Javadoc).
        verify(clusterElementContextAware, times(1)).toActionContext("approval", 1, "requestApproval", null);
        verify(actionContextAware, times(1)).suspend(
            org.mockito.ArgumentMatchers.any(ActionContext.Suspend.class));
    }

    @Test
    void testToolFunctionAndDefinitionMetadataIsStable() {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModifiableClusterElementDefinition<MultipleConnectionsToolFunction> definition =
            ApprovalRequestApprovalTool.of(clusterElementDefinitionService);

        assertEquals("requestApproval", definition.getName());
        assertEquals(
            "Request Approval", definition.getTitle()
                .orElseThrow());
        assertEquals(
            "Sends an approval request and waits for a human to approve or reject.",
            definition.getDescription()
                .orElseThrow());
    }
}
