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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.ai.tool.ToolSuspensionException;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApprovalRequestApprovalToolTest {

    private static final String RESUME_URL = "https://example.com/job/resume/abc";

    private final ActionContextAware actionContextAware = mock(ActionContextAware.class);
    private final ClusterElementContextAware clusterElementContextAware = mock(ClusterElementContextAware.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final Parameters connectionParameters = mock(Parameters.class);
    private final Parameters extensions = mock(Parameters.class);
    private final Parameters inputParameters = mock(Parameters.class);
    private final AtomicReference<Suspend> suspendReference = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        when(clusterElementContextAware.toActionContext("approval", 1, "requestApproval", null))
            .thenReturn(actionContextAware);
        when(actionContextAware.isEditorEnvironment()).thenReturn(true);
        when(actionContextAware.getSuspend()).thenAnswer(invocation -> suspendReference.get());

        doAnswer(invocation -> {
            suspendReference.set(invocation.getArgument(0));

            return null;
        }).when(actionContextAware)
            .suspend(any(Suspend.class));

        doReturn(Map.of("formTitle", "Refund order 42")).when(inputParameters)
            .toMap();
        when(extensions.toMap()).thenReturn(Map.of());
    }

    @Test
    void testToolReturnsSuspendedToolResultAfterInvokingPerform() throws Exception {
        when(actionContextAware.getResumeUrl()).thenReturn(RESUME_URL);

        MultipleConnectionsToolFunction toolFunction = getToolFunction();

        assertNotNull(toolFunction, "Cluster element must expose the tool function");

        Object result = toolFunction.apply(
            inputParameters, connectionParameters, extensions, Map.of(), clusterElementContextAware);

        assertTrue(
            ToolSuspension.isSuspendedToolResult((String) result),
            "Tool must return the suspended tool result so the agent loop can pair it with the pending tool call id.");

        verify(clusterElementContextAware, times(1)).toActionContext("approval", 1, "requestApproval", null);
    }

    @Test
    void testToolStoresItsInputParametersAsTheApprovalFormParameters() throws Exception {
        when(actionContextAware.getResumeUrl()).thenReturn(RESUME_URL);

        getToolFunction().apply(
            inputParameters, connectionParameters, extensions, Map.of(), clusterElementContextAware);

        Suspend suspend = suspendReference.get();

        assertNotNull(suspend);
        assertEquals(
            Map.of("formTitle", "Refund order 42"),
            suspend.continueParameters()
                .get(MetadataConstants.APPROVAL_FORM_PARAMETERS));
        assertEquals(
            "https://example.com/resume/abc", suspend.continueParameters()
                .get("formUrl"));
        assertNotNull(suspend.expiresAt(), "the approval keeps the action's expiry so an unanswered request times out");
    }

    @Test
    void testToolInEditorEnvironmentNeitherSendsTheRequestNorSuspends() throws Exception {
        when(clusterElementContextAware.isEditorEnvironment()).thenReturn(true);
        when(actionContextAware.getResumeUrl()).thenReturn(RESUME_URL);

        Object result = getToolFunction().apply(
            inputParameters, connectionParameters, extensions, Map.of(), clusterElementContextAware);

        assertEquals(ApprovalRequestApprovalTool.EDITOR_ENVIRONMENT_RESULT, result);
        verify(clusterElementContextAware, never()).toActionContext("approval", 1, "requestApproval", null);
        verify(actionContextAware, never()).suspend(any(Suspend.class));
    }

    @Test
    void testToolThrowsToolSuspensionExceptionWithCauseWhenApprovalRequestCannotBeSent() {
        when(actionContextAware.getResumeUrl()).thenReturn(null);

        MultipleConnectionsToolFunction toolFunction = getToolFunction();

        ToolSuspensionException exception = assertThrows(
            ToolSuspensionException.class,
            () -> toolFunction.apply(
                inputParameters, connectionParameters, extensions, Map.of(), clusterElementContextAware));

        Throwable cause = exception.getCause();

        assertInstanceOf(IllegalStateException.class, cause);
        assertTrue(exception.getMessage()
            .startsWith("The approval request could not be sent: "));
        assertTrue(exception.getMessage()
            .contains(cause.getMessage()));
        verify(actionContextAware, never()).suspend(any(Suspend.class));
    }

    @Test
    void testToolThrowsToolSuspensionExceptionWithCauseWhenSuspendFails() {
        when(actionContextAware.getResumeUrl()).thenReturn(RESUME_URL);

        IllegalStateException suspendFailure = new IllegalStateException("suspend failed");

        doAnswer(invocation -> {
            throw suspendFailure;
        }).when(actionContextAware)
            .suspend(any(Suspend.class));

        MultipleConnectionsToolFunction toolFunction = getToolFunction();

        ToolSuspensionException exception = assertThrows(
            ToolSuspensionException.class,
            () -> toolFunction.apply(
                inputParameters, connectionParameters, extensions, Map.of(), clusterElementContextAware));

        assertSame(suspendFailure, exception.getCause());
        assertEquals("The approval request could not be sent: suspend failed", exception.getMessage());
    }

    @Test
    void testToolFunctionAndDefinitionMetadataIsStable() {
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

    private MultipleConnectionsToolFunction getToolFunction() {
        ModifiableClusterElementDefinition<MultipleConnectionsToolFunction> definition =
            ApprovalRequestApprovalTool.of(clusterElementDefinitionService);

        return definition.getElement();
    }
}
