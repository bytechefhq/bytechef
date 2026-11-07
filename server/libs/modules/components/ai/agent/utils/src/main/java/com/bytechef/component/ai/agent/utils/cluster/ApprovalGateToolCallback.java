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

package com.bytechef.component.ai.agent.utils.cluster;

import com.bytechef.component.ai.llm.tool.GatedToolCallback;
import com.bytechef.component.ai.llm.tool.ToolApprovalRequests;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
public class ApprovalGateToolCallback implements GatedToolCallback {

    static final String EDITOR_ENVIRONMENT_RESULT = ToolApprovalRequests.EDITOR_ENVIRONMENT_RESULT;

    private final ToolCallback delegate;
    private final @Nullable String gateName;
    private final String gatedComponentName;
    private final String gatedWorkflowNodeName;

    private final List<ClusterElement> approvalChannelClusterElements;
    private final Map<String, ComponentConnection> componentConnections;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ActionContextAware actionContext;

    @Nullable
    private final Duration approvalExpiry;

    public ApprovalGateToolCallback(
        ToolCallback delegate, ClusterElement gatedClusterElement, @Nullable String gateName,
        List<ClusterElement> approvalChannelClusterElements, Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService, ActionContextAware actionContext,
        @Nullable Duration approvalExpiry) {

        this.delegate = delegate;
        this.gateName = gateName;
        this.gatedComponentName = gatedClusterElement.getComponentName();
        this.gatedWorkflowNodeName = gatedClusterElement.getWorkflowNodeName();
        this.approvalChannelClusterElements = List.copyOf(approvalChannelClusterElements);
        this.componentConnections = Map.copyOf(componentConnections);
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.actionContext = actionContext;
        this.approvalExpiry = approvalExpiry;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolCallback getDelegate() {
        return delegate;
    }

    @Override
    public String getGatedComponentName() {
        return gatedComponentName;
    }

    @Override
    public String getGatedWorkflowNodeName() {
        return gatedWorkflowNodeName;
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String toolName = getName();

        Instant expiresAt = Instant.now()
            .plus(approvalExpiry != null ? approvalExpiry : ActionContextAware.DEFAULT_APPROVAL_EXPIRY);

        String formTitle = gateName == null || gateName.isBlank()
            ? "Approve tool call: " + toolName : gateName + ": approve tool call " + toolName;
        String formDescription =
            "The AI agent wants to call the tool '" + toolName + "' with these arguments:\n\n" + toolInput;

        return ToolApprovalRequests.raise(
            new ToolApprovalRequests.Request(
                toolName, toolInput, formTitle, formDescription, expiresAt, approvalChannelClusterElements,
                componentConnections, Map.of(), clusterElementDefinitionService, actionContext));
    }

    private String getName() {
        ToolDefinition toolDefinition = delegate.getToolDefinition();

        return toolDefinition.name();
    }
}
