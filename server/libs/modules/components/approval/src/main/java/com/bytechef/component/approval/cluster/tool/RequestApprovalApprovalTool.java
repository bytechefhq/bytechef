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

import com.bytechef.component.approval.action.RequestApprovalApprovalAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.ai.tool.ToolSuspensionException;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class RequestApprovalApprovalTool {

    static final String EDITOR_ENVIRONMENT_RESULT =
        "Approval requests cannot be sent in a workflow editor test run, so nothing was approved. Do not perform the " +
            "action that needed approval; tell the user that it needs approval, which is only possible when the " +
            "workflow runs outside the editor.";

    public static ModifiableClusterElementDefinition<MultipleConnectionsToolFunction> of(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        ModifiableActionDefinition actionDefinition = RequestApprovalApprovalAction.of(clusterElementDefinitionService);
        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        return ComponentDsl.<MultipleConnectionsToolFunction>clusterElement("requestApproval")
            .title("Request Approval")
            .description("Sends an approval request and waits for a human to approve or reject.")
            .type(TOOLS)
            .properties(actionDefinition.getProperties())
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

    private RequestApprovalApprovalTool() {
    }
}
