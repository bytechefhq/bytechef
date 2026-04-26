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
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;

/**
 * Exposes {@link ApprovalRequestApprovalAction} as a {@link MultipleConnectionsToolFunction} cluster element so AI
 * agents can request human approval — including dispatching to approval-channel cluster elements when the agent passes
 * them through.
 *
 * <p>
 * The standard {@link ComponentDsl#tool(com.bytechef.component.definition.ActionDefinition)} helper does not fit here:
 * it casts the action's perform to a plain {@code PerformFunction}, but the underlying action registers a
 * {@link MultipleConnectionsPerformFunction}. The runtime dispatcher (in {@code ClusterElementDefinitionServiceImpl})
 * already understands {@code MultipleConnectionsToolFunction} and forwards the {@code componentConnections} map, so the
 * tool delegates straight to the action's perform via a real {@link ActionContext} obtained from
 * {@link ClusterElementContextAware#toActionContext}. That context supports {@code suspend(...)}, unlike the
 * {@code ActionContextAdapter} used by {@code AiAgentChatTool}.
 *
 * @author Ivica Cardic
 */
public class ApprovalRequestApprovalTool {

    public static ModifiableClusterElementDefinition<MultipleConnectionsToolFunction> of(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);
        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        return ComponentDsl.<MultipleConnectionsToolFunction>clusterElement("requestApproval")
            .title("Request Approval")
            .description("Sends an approval request and waits for a human to approve or reject.")
            .type(TOOLS)
            .properties(actionDefinition.getProperties()
                .orElse(List.of()))
            .object(() -> (inputParameters, connectionParameters, extensions, componentConnections, context) -> {
                ClusterElementContextAware clusterElementContextAware = (ClusterElementContextAware) context;

                ActionContext actionContext = clusterElementContextAware.toActionContext(
                    APPROVAL, 1, "requestApproval", null);

                return performFunction.apply(inputParameters, componentConnections, extensions, actionContext);
            });
    }

    private ApprovalRequestApprovalTool() {
    }
}
