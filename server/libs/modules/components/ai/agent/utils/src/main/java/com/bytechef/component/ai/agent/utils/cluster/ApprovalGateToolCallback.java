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
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.PUBLIC_URL;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.RESUME_ID;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.RESUME_URL;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.SHORT_RESUME_ID;

import com.bytechef.component.ai.llm.tool.DelegatingToolCallback;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.tool.GatedToolSuspension;
import com.bytechef.platform.ai.tool.GatedToolSuspension.ApprovalRequest;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.ai.tool.ToolSuspensionException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ActionContextAware.ApprovalLinks;
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

    static final String EDITOR_ENVIRONMENT_RESULT =
        "The tool '%s' needs human approval, and approval requests cannot be sent in a workflow editor test run, so " +
            "the tool was not run. Do not perform the action; tell the user that it needs approval, which is only " +
            "possible when the workflow runs outside the editor.";

    private final ToolCallback delegate;
    private final @Nullable String gateName;

    private final List<ClusterElement> approvalChannelClusterElements;
    private final Map<String, ComponentConnection> componentConnections;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ActionContextAware actionContext;

    @Nullable
    private final ToolExecutionRecorder toolExecutionRecorder;

    @Nullable
    private final Duration approvalExpiry;

    public ApprovalGateToolCallback(
        ToolCallback delegate, @Nullable String gateName, List<ClusterElement> approvalChannelClusterElements,
        Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService, ActionContextAware actionContext,
        @Nullable ToolExecutionRecorder toolExecutionRecorder, @Nullable Duration approvalExpiry) {

        this.delegate = delegate;
        this.gateName = gateName;
        this.approvalChannelClusterElements = List.copyOf(approvalChannelClusterElements);
        this.componentConnections = Map.copyOf(componentConnections);
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.actionContext = actionContext;
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

        Instant now = Instant.now();

        Instant expiresAt =
            now.plus(approvalExpiry != null ? approvalExpiry : ActionContextAware.DEFAULT_APPROVAL_EXPIRY);

        ApprovalLinks approvalLinks = actionContext.getApprovalLinks(expiresAt);

        if (approvalLinks == null) {
            throw new ToolSuspensionException(
                "Cannot raise an approval request for tool '" + getName() + "'. Ensure the server's public URL is " +
                    "configured and the workflow is running in a proper execution context.",
                new IllegalStateException("No resume URL is available"));
        }

        String formTitle = gateName == null || gateName.isBlank()
            ? "Approve tool call: " + getName() : gateName + ": approve tool call " + getName();
        String formDescription =
            "The AI agent wants to call the tool '" + getName() + "' with these arguments:\n\n" + toolInput;

        try {
            deliverApprovalRequest(approvalLinks, formTitle, formDescription, expiresAt);
        } catch (RuntimeException exception) {
            throw new ToolSuspensionException(
                "The approval request for tool '" + getName() + "' could not be sent: " + exception.getMessage(),
                exception);
        }

        GatedToolSuspension gatedToolSuspension = new GatedToolSuspension(
            getName(), toolInput, new ApprovalRequest(formTitle, formDescription, approvalLinks.formUrl(), now));

        Map<String, Object> continueParameters = gatedToolSuspension.toContinueParameters();

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
        ApprovalLinks approvalLinks, String formTitle, String formDescription, Instant expiresAt) {

        String formUrl = approvalLinks.formUrl();

        Map<String, Object> serverOwnedParameters = new HashMap<>();

        serverOwnedParameters.put(FORM_TITLE, formTitle);
        serverOwnedParameters.put(FORM_DESCRIPTION, formDescription);
        serverOwnedParameters.put(EXPIRES_AT, expiresAt.toString());
        serverOwnedParameters.put(PUBLIC_URL, approvalLinks.publicUrl());
        serverOwnedParameters.put(RESUME_ID, approvalLinks.resumeId());
        serverOwnedParameters.put(RESUME_URL, approvalLinks.resumeUrl());

        if (approvalLinks.shortResumeId() != null) {
            serverOwnedParameters.put(SHORT_RESUME_ID, approvalLinks.shortResumeId());
        }

        if (approvalChannelClusterElements.isEmpty()) {
            clusterElementDefinitionService.executeApprovalChannel(
                CHAT_APPROVAL_CHANNEL_COMPONENT, 1, CHAT_APPROVAL_CHANNEL_NAME, serverOwnedParameters, formUrl,
                null, actionContext);

            return;
        }

        int deliveredCount = 0;
        Exception lastException = null;

        for (ClusterElement approvalChannel : approvalChannelClusterElements) {
            ComponentConnection componentConnection = componentConnections.get(
                approvalChannel.getWorkflowNodeName());

            Map<String, Object> mergedInputParameters = new HashMap<>(approvalChannel.getParameters());

            mergedInputParameters.putAll(serverOwnedParameters);

            if (approvalLinks.shortResumeId() == null) {
                mergedInputParameters.remove(SHORT_RESUME_ID);
            }

            try {
                clusterElementDefinitionService.executeApprovalChannel(
                    approvalChannel.getComponentName(), approvalChannel.getComponentVersion(),
                    approvalChannel.getClusterElementName(), mergedInputParameters, formUrl, componentConnection,
                    actionContext);

                deliveredCount++;
            } catch (Exception exception) {
                lastException = exception;

                log.warn(
                    "Approval channel {}/{} failed to deliver the tool-gate approval request",
                    approvalChannel.getComponentName(), approvalChannel.getClusterElementName(), exception);
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
