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

import static com.bytechef.component.approval.constant.ApprovalConstants.APPROVAL;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.approval.action.ApprovalRequestApprovalAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.ai.tool.ToolSuspensionException;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapts the {@link ApprovalRequestApprovalAction} into a {@link MultipleConnectionsToolFunction} so the AI agent can
 * invoke it as a tool. It is built by hand because {@code ComponentDsl#tool(...)} supports only single-connection
 * performs, while this action's {@link MultipleConnectionsPerformFunction} needs every connection and must be able to
 * suspend the agent.
 *
 * <p>
 * Each invocation gets its {@link ActionContext} from {@link ClusterElementContextAware#toActionContext}. That context
 * keeps the tool's own component and action (connections reach the action through {@code componentConnections}), but
 * forwards {@code suspend(...)} and the resume URL to the agent's action context, so the action's suspend suspends the
 * agent's task, where {@code SuspendableToolCallingManager} picks it up.
 *
 * <p>
 * The action's perform always suspends (or throws) and returns {@code null}, so the tool returns the suspended tool
 * result (see {@link ToolSuspension}) instead; on resume the approver's answer replaces it as this tool call's
 * response. When the request cannot be sent, the tool throws {@link ToolSuspensionException}, which fails the agent
 * rather than letting the model continue without the approval.
 *
 * <p>
 * A workflow editor test run's in-memory job cannot be resumed through the resume URL, so there the tool neither sends
 * the request nor suspends, and tells the model that nothing was approved.
 *
 * <p>
 * The suspend carries the tool's own input parameters, because the approval form would otherwise be built from the
 * suspended task's parameters, which are the agent's.
 *
 * @author Ivica Cardic
 */
public class ApprovalRequestApprovalTool {

    static final String EDITOR_ENVIRONMENT_RESULT =
        "Approval requests cannot be sent in a workflow editor test run, so nothing was approved. Do not perform the " +
            "action that needed approval; tell the user that it needs approval, which is only possible when the " +
            "workflow runs outside the editor.";

    public static ModifiableClusterElementDefinition<MultipleConnectionsToolFunction> of(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);
        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        Optional<List<? extends Property>> propertiesOptional = actionDefinition.getProperties();

        return ComponentDsl.<MultipleConnectionsToolFunction>clusterElement("requestApproval")
            .title("Request Approval")
            .description("Sends an approval request and waits for a human to approve or reject.")
            .type(TOOLS)
            .properties(propertiesOptional.orElse(List.of()))
            .object(() -> (inputParameters, connectionParameters, extensions, componentConnections, context) -> {
                if (context.isEditorEnvironment()) {
                    return EDITOR_ENVIRONMENT_RESULT;
                }

                ClusterElementContextAware clusterElementContextAware = (ClusterElementContextAware) context;

                ActionContext actionContext = clusterElementContextAware.toActionContext(
                    APPROVAL, 1, "requestApproval", null);

                try {
                    performFunction.apply(inputParameters, componentConnections, extensions, actionContext);
                } catch (Exception exception) {
                    throw new ToolSuspensionException(
                        "The approval request could not be sent: " + exception.getMessage(), exception);
                }

                Suspend suspend = ((ActionContextAware) actionContext).getSuspend();

                if (suspend != null) {
                    Map<String, Object> continueParameters = new HashMap<>(suspend.continueParameters());

                    continueParameters.put(MetadataConstants.APPROVAL_FORM_PARAMETERS, inputParameters.toMap());

                    actionContext.suspend(new Suspend(continueParameters, suspend.expiresAt()));
                }

                return ToolSuspension.suspendedToolResult(actionContext);
            });
    }

    private ApprovalRequestApprovalTool() {
    }
}
