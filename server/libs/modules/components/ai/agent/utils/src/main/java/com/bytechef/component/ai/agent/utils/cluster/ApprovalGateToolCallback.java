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

import static com.bytechef.component.definition.approval.ApprovalChannelFunction.EXPIRES_AT;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;

import com.bytechef.component.ai.llm.tool.DelegatingToolCallback;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.ai.tool.ToolSuspensionException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
public class ApprovalGateToolCallback implements DelegatingToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ApprovalGateToolCallback.class);

    private static final String CHAT_APPROVAL_CHANNEL_COMPONENT = "chat";
    private static final String CHAT_APPROVAL_CHANNEL_NAME = "chat";
    private static final String FORM_URL = "formUrl";

    static final String EDITOR_ENVIRONMENT_RESULT =
        "The tool '%s' needs human approval, and approval requests cannot be sent in a workflow editor test run, so " +
            "the tool was not run. Do not perform the action; tell the user that it needs approval, which is only " +
            "possible when the workflow runs outside the editor.";

    private final ToolCallback delegate;
    private static final Duration DEFAULT_APPROVAL_EXPIRY = Duration.ofDays(60);

    private final List<ClusterElement> approvalChannelClusterElements;
    private final Map<String, ComponentConnection> componentConnections;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ActionContextAware actionContext;

    @Nullable
    private final ToolExecutionRecorder toolExecutionRecorder;

    @Nullable
    private final Duration approvalExpiry;

    public ApprovalGateToolCallback(
        ToolCallback delegate, List<ClusterElement> approvalChannelClusterElements,
        Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService, ActionContext actionContext) {

        this(
            delegate, approvalChannelClusterElements, componentConnections, clusterElementDefinitionService,
            actionContext, null, null);
    }

    public ApprovalGateToolCallback(
        ToolCallback delegate, List<ClusterElement> approvalChannelClusterElements,
        Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService, ActionContext actionContext,
        @Nullable ToolExecutionRecorder toolExecutionRecorder) {

        this(
            delegate, approvalChannelClusterElements, componentConnections, clusterElementDefinitionService,
            actionContext, toolExecutionRecorder, null);
    }

    public ApprovalGateToolCallback(
        ToolCallback delegate, List<ClusterElement> approvalChannelClusterElements,
        Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService, ActionContext actionContext,
        @Nullable ToolExecutionRecorder toolExecutionRecorder, @Nullable Duration approvalExpiry) {

        this.delegate = delegate;
        this.approvalChannelClusterElements = List.copyOf(approvalChannelClusterElements);
        this.componentConnections = Map.copyOf(componentConnections);
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.actionContext = (ActionContextAware) actionContext;
        this.toolExecutionRecorder = toolExecutionRecorder;
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
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        if (actionContext.isEditorEnvironment()) {
            return EDITOR_ENVIRONMENT_RESULT.formatted(getName());
        }

        String resumeUrl = actionContext.getResumeUrl();

        if (resumeUrl == null) {
            throw new ToolSuspensionException(
                "Cannot raise an approval request for tool '" + getName() + "'. Ensure the server's public URL is " +
                    "configured and the workflow is running in a proper execution context.",
                new IllegalStateException("No resume URL is available"));
        }

        String formUrl = resumeUrl.replace("/job/resume/", "/resume/");

        Instant expiresAt = Instant.now()
            .plus(approvalExpiry != null ? approvalExpiry : DEFAULT_APPROVAL_EXPIRY);

        String formTitle = "Approve tool call: " + getName();
        String formDescription =
            "The AI agent wants to call the tool '" + getName() + "' with these arguments:\n\n" + toolInput;

        try {
            deliverApprovalRequest(formUrl, formTitle, formDescription, expiresAt);
        } catch (RuntimeException exception) {
            throw new ToolSuspensionException(
                "The approval request for tool '" + getName() + "' could not be sent: " + exception.getMessage(),
                exception);
        }

        Map<String, Object> continueParameters = new HashMap<>();

        continueParameters.put(ToolSuspendConstants.GATED_TOOL_NAME, getName());
        continueParameters.put(ToolSuspendConstants.GATED_TOOL_INPUT, toolInput);
        continueParameters.put(FORM_URL, formUrl);
        continueParameters.put(MetadataConstants.APPROVAL_RESUME, true);
        continueParameters.put(
            MetadataConstants.APPROVAL_FORM_PARAMETERS,
            Map.of(FORM_TITLE, formTitle, FORM_DESCRIPTION, formDescription));

        actionContext.suspend(new ActionContext.Suspend(continueParameters, expiresAt));

        recordGateRaised();

        return ToolSuspension.suspendedToolResult(actionContext);
    }

    private void recordGateRaised() {
        if (toolExecutionRecorder == null) {
            return;
        }

        toolExecutionRecorder.record(
            ToolExecutionEvent
                .builder(ToolExecutionSurface.AI_AGENT, ToolExecutionKind.COMPONENT, getName())
                .jobId(actionContext.getJobId())
                .outcome(ToolExecutionOutcome.APPROVAL_REQUIRED)
                .build());
    }

    private void deliverApprovalRequest(
        String formUrl, String formTitle, String formDescription, Instant expiresAt) {

        Map<String, Object> channelInputParameters = new HashMap<>();

        channelInputParameters.put(FORM_TITLE, formTitle);
        channelInputParameters.put(FORM_DESCRIPTION, formDescription);
        channelInputParameters.put(EXPIRES_AT, expiresAt.toString());

        if (approvalChannelClusterElements.isEmpty()) {
            clusterElementDefinitionService.executeApprovalChannel(
                CHAT_APPROVAL_CHANNEL_COMPONENT, 1, CHAT_APPROVAL_CHANNEL_NAME, channelInputParameters, formUrl,
                null, actionContext);

            return;
        }

        int deliveredCount = 0;
        Exception lastException = null;

        for (ClusterElement approvalChannel : approvalChannelClusterElements) {
            ComponentConnection componentConnection = componentConnections.get(
                approvalChannel.getWorkflowNodeName());

            Map<String, Object> mergedInputParameters = new HashMap<>(channelInputParameters);

            mergedInputParameters.putAll(approvalChannel.getParameters());

            try {
                clusterElementDefinitionService.executeApprovalChannel(
                    approvalChannel.getComponentName(), approvalChannel.getComponentVersion(),
                    approvalChannel.getClusterElementName(), mergedInputParameters, formUrl, componentConnection,
                    actionContext);

                deliveredCount++;
            } catch (Exception exception) {
                lastException = exception;

                log.warn(
                    "Approval channel {}/{} failed to deliver the tool-gate approval request: {}",
                    approvalChannel.getComponentName(), approvalChannel.getClusterElementName(),
                    exception.getMessage());
            }
        }

        if (deliveredCount == 0) {
            throw new IllegalStateException(
                "None of the " + approvalChannelClusterElements.size() + " configured approval channels could " +
                    "deliver the approval request for tool '" + getName() + "'.",
                lastException);
        }
    }

    private String getName() {
        ToolDefinition toolDefinition = delegate.getToolDefinition();

        return toolDefinition.name();
    }
}
