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

package com.bytechef.component.ai.llm.tool;

import static com.bytechef.component.definition.approval.ApprovalChannelFunction.EXPIRES_AT;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.PUBLIC_URL;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.RESUME_ID;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.RESUME_URL;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.SHORT_RESUME_ID;

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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class ToolApprovalRequests {

    public static final String EDITOR_ENVIRONMENT_RESULT =
        "The tool '%s' needs human approval, and approval requests cannot be sent in a workflow editor test run, so " +
            "the tool was not run. Do not perform the action; tell the user that it needs approval, which is only " +
            "possible when the workflow runs outside the editor.";

    private static final String CHAT_APPROVAL_CHANNEL_COMPONENT = "chat";
    private static final String CHAT_APPROVAL_CHANNEL_NAME = "chat";

    private static final Logger log = LoggerFactory.getLogger(ToolApprovalRequests.class);

    private ToolApprovalRequests() {
    }

    @SuppressFBWarnings("EI")
    public record Request(
        String toolName, String toolInput, String formTitle, String formDescription, Instant expiresAt,
        List<ClusterElement> approvalChannelClusterElements, Map<String, ComponentConnection> componentConnections,
        Map<String, Object> additionalContinueParameters,
        ClusterElementDefinitionService clusterElementDefinitionService, ActionContextAware actionContext) {
    }

    public static String raise(Request request) {
        ActionContextAware actionContext = request.actionContext();

        if (actionContext.isEditorEnvironment()) {
            return EDITOR_ENVIRONMENT_RESULT.formatted(request.toolName());
        }

        Instant now = Instant.now();

        ApprovalLinks approvalLinks = actionContext.getApprovalLinks(request.expiresAt());

        if (approvalLinks == null) {
            throw new ToolSuspensionException(
                "Cannot raise an approval request for tool '" + request.toolName() + "'. Ensure the server's public " +
                    "URL is configured and the workflow is running in a proper execution context.",
                new IllegalStateException("No resume URL is available"));
        }

        try {
            deliverApprovalRequest(request, approvalLinks);
        } catch (RuntimeException exception) {
            throw new ToolSuspensionException(
                "The approval request for tool '" + request.toolName() + "' could not be sent: " +
                    exception.getMessage(),
                exception);
        }

        GatedToolSuspension gatedToolSuspension = new GatedToolSuspension(
            request.toolName(), request.toolInput(),
            new ApprovalRequest(request.formTitle(), request.formDescription(), approvalLinks.formUrl(), now));

        Map<String, Object> continueParameters = new HashMap<>(request.additionalContinueParameters());

        continueParameters.putAll(gatedToolSuspension.toContinueParameters());

        actionContext.suspend(new ActionContext.Suspend(continueParameters, request.expiresAt()));

        return ToolSuspension.suspendedToolResult(actionContext);
    }

    private static void deliverApprovalRequest(Request request, ApprovalLinks approvalLinks) {
        ActionContextAware actionContext = request.actionContext();
        List<ClusterElement> approvalChannelClusterElements = request.approvalChannelClusterElements();
        ClusterElementDefinitionService clusterElementDefinitionService = request.clusterElementDefinitionService();
        String formUrl = approvalLinks.formUrl();

        Map<String, Object> serverOwnedParameters = new HashMap<>();

        serverOwnedParameters.put(FORM_TITLE, request.formTitle());
        serverOwnedParameters.put(FORM_DESCRIPTION, request.formDescription());
        serverOwnedParameters.put(EXPIRES_AT, request.expiresAt()
            .toString());
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

        Map<String, ComponentConnection> componentConnections = request.componentConnections();

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
                    "Approval channel {}/{} failed to deliver the tool approval request",
                    approvalChannel.getComponentName(), approvalChannel.getClusterElementName(), exception);
            }
        }

        if (deliveredCount == 0) {
            throw new IllegalStateException(
                "None of the " + approvalChannelClusterElements.size() + " configured approval channels could " +
                    "deliver the approval request for tool '" + request.toolName() + "'.",
                lastException);
        }
    }
}
