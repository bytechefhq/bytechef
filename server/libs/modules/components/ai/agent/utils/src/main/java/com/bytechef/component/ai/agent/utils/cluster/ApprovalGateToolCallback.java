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
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.ai.constant.AiAgentToolContextKey;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.ComponentConnection;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Platform-enforced approval gate. Wraps a tool callback attached beneath an {@link AiAgentUtilsApprovalGate}: instead
 * of executing, the first call delivers a standard approval request (tool name + the AI-chosen arguments) through that
 * gate's APPROVAL_CHANNELS cluster elements — defaulting to the chat channel when none are configured — and suspends
 * the workflow. On resume, approval executes the tool with the original arguments and rejection feeds a denial back
 * into the agent loop (see {@code AbstractAiAgentChatAction.buildPatchedRequestSpec}). Enforcement lives here, in the
 * platform — the LLM cannot invoke a flagged tool un-gated.
 *
 * @author Ivica Cardic
 */
public class ApprovalGateToolCallback implements DelegatingToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ApprovalGateToolCallback.class);

    private static final String CHAT_APPROVAL_CHANNEL_COMPONENT = "chat";
    private static final String CHAT_APPROVAL_CHANNEL_NAME = "chat";

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

    /**
     * Exposes the gated tool so the resume path can execute the invocation the human approved instead of raising a
     * second approval request.
     */
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
        // Only ONE suspend may exist per tool round (SuspendableToolCallingManager throws on two sentinels). If
        // another tool call already suspended this round, defer this one with a plain tool response so the LLM
        // retries it after the pending approval is resolved.
        if (actionContext.getSuspend() != null) {
            return "{\"deferred\": true, \"reason\": \"Another tool call is awaiting approval. Retry this tool " +
                "call after the pending approval is resolved.\"}";
        }

        String resumeUrl = actionContext.getResumeUrl();

        if (resumeUrl == null) {
            throw new IllegalStateException(
                "Cannot raise an approval request for tool '" + getName() + "'. Ensure the server's public URL is " +
                    "configured and the workflow is running in a proper execution context.");
        }

        String formUrl = resumeUrl.replace("/job/resume/", "/resume/");

        Instant expiresAt = Instant.now()
            .plus(approvalExpiry != null ? approvalExpiry : DEFAULT_APPROVAL_EXPIRY);

        if (actionContext.isEditorEnvironment()) {
            // Editor test runs have no channel listeners (channels are production transports), but the agent's
            // SSE stream IS connected — send the approval card event through the ToolContext's emitter, the same
            // path ask_user_question uses, so the canvas test chat renders the card.
            sendEditorApprovalRequestEvent(toolContext, formUrl, toolInput, expiresAt);
        } else {
            deliverApprovalRequest(formUrl, toolInput, expiresAt);
        }

        Map<String, Object> continueParameters = new HashMap<>();

        continueParameters.put(ToolSuspendConstants.GATED_TOOL_NAME, getName());
        continueParameters.put(ToolSuspendConstants.GATED_TOOL_INPUT, toolInput);
        continueParameters.put("formUrl", formUrl);

        actionContext.suspend(new ActionContext.Suspend(continueParameters, expiresAt));

        recordGateRaised();

        return ToolSuspendConstants.SUSPENDED_SENTINEL;
    }

    /**
     * Emits a tool-invocation audit event for the raised gate. Name-and-outcome only — the AI-chosen arguments are
     * deliberately excluded from the audit trail, matching the recorder's payload-free event contract.
     */
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

    @SuppressWarnings("unchecked")
    private void sendEditorApprovalRequestEvent(
        @Nullable ToolContext toolContext, String formUrl, String toolInput, Instant expiresAt) {

        if (toolContext == null) {
            return;
        }

        Map<String, Object> eventData = new LinkedHashMap<>();

        eventData.put(AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.APPROVAL_REQUEST);
        eventData.put("resumeId", formUrl.substring(formUrl.lastIndexOf('/') + 1));
        eventData.put("formUrl", formUrl);
        eventData.put(FORM_TITLE, "Approve tool call: " + getName());
        eventData.put(
            FORM_DESCRIPTION,
            "The AI agent wants to call the tool '" + getName() + "' with these arguments:\n\n" + toolInput);
        eventData.put(EXPIRES_AT, expiresAt.toString());
        eventData.put("inputs", List.of());

        Map<String, Object> toolContextMap = toolContext.getContext();

        Object emitterReferenceObject = toolContextMap.get(AiAgentToolContextKey.SSE_EMITTER_REFERENCE);

        if (emitterReferenceObject instanceof AtomicReference<?> emitterReference
            && emitterReference.get() instanceof ActionDefinition.SseEmitterHandler.SseEmitter sseEmitter) {

            try {
                sseEmitter.send(eventData);

                return;
            } catch (Exception exception) {
                log.warn("SSE send of approval_request failed, falling back to buffering: {}", exception.getMessage());
            }
        }

        Object bufferedEventsObject = toolContextMap.get(AiAgentToolContextKey.SSE_BUFFERED_EVENTS);

        if (bufferedEventsObject instanceof Queue<?> queue) {
            ((Queue<Map<String, Object>>) queue).add(eventData);

            return;
        }

        // Neither an SSE emitter nor a buffered-events queue is present — only the streaming Chat action wires these
        // into the ToolContext, so an editor test run of the non-streaming Chat action reaches here and the approval
        // card is silently dropped (the run still suspends and is resolvable via the hosted form). Warn so a hung
        // test run is diagnosable instead of failing silently.
        log.warn(
            "No SSE emitter or buffered-events queue in the tool context; the editor approval card for tool '{}' was " +
                "not delivered. The run is still suspended and resolvable via the hosted approval form.",
            getName());
    }

    private void deliverApprovalRequest(String formUrl, String toolInput, Instant expiresAt) {
        Map<String, Object> channelInputParameters = new HashMap<>();

        channelInputParameters.put(FORM_TITLE, "Approve tool call: " + getName());
        channelInputParameters.put(
            FORM_DESCRIPTION,
            "The AI agent wants to call the tool '" + getName() + "' with these arguments:\n\n" + toolInput);
        channelInputParameters.put(EXPIRES_AT, expiresAt.toString());

        if (approvalChannelClusterElements.isEmpty()) {
            // No channels configured on the agent node — default to the chat channel targeting the run's
            // originating conversation. The chat channel throws when the run has no jobId (in-process runs);
            // a webhook/schedule run has a jobId but no chat listener, so the request is only reachable via the
            // pending-approvals inbox — workflow validation warns about that configuration at design time.
            clusterElementDefinitionService.executeApprovalChannel(
                CHAT_APPROVAL_CHANNEL_COMPONENT, 1, CHAT_APPROVAL_CHANNEL_NAME, channelInputParameters, formUrl,
                null, actionContext);

            return;
        }

        // Best-effort per channel: a failing channel is logged and skipped so the remaining channels still deliver
        // and the gate still suspends. Only when every configured channel fails is the call failed — then nobody
        // was notified and suspending would be a silent no-op.
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
