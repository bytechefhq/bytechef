/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.exception.AGUIException;
import com.agui.server.LocalAgent;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-turn dispatcher that picks {@link WebhookBridgeAgent} or the underlying {@link AiHubSpringAIAgent} based on the
 * chat's {@link AiHubChatKind}. Registered as the {@code ai_hub_ask} / {@code ai_hub_build} {@link LocalAgent} bean so
 * the controller's {@code agentId → agent} map dispatches every per-turn invocation through this router first.
 *
 * <p>
 * Routing rule:
 * </p>
 * <ul>
 * <li>{@code chat.kind == WORKFLOW_CHAT} — delegate to {@link WebhookBridgeAgent}, which translates webhook events to
 * AG-UI events through {@link com.bytechef.ee.ai.hub.agent.AgUiStreamBridge}.</li>
 * <li>{@code chat.kind == STANDARD} (default for legacy rows) — delegate to the wrapped {@link AiHubSpringAIAgent},
 * preserving today's LLM-driven behaviour for standard chats.</li>
 * <li>AiHubChat not found (race with delete, unknown threadId) — delegate to the LLM agent. The agent's own error
 * handling will surface the missing-chat case if it matters.</li>
 * </ul>
 *
 * <p>
 * <b>Why route at the agent layer and not the controller?</b> Keeping the routing inside a {@link LocalAgent}
 * implementation means the controller stays unchanged — {@code AiHubApiController} continues to dispatch by URL path
 * (agentId), so any future agent-level routing concern (per-chat feature flags, A/B test buckets) lands here too. The
 * controller's job remains "URL → agent"; the agent's job is "chat → behaviour".
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiHubRoutingAgent extends LocalAgent {

    private static final Logger log = LoggerFactory.getLogger(AiHubRoutingAgent.class);

    private final AiHubSpringAIAgent llmAgent;
    private final @Nullable WebhookBridgeAgent webhookBridgeAgent;
    private final AiHubChatService chatService;
    private final AssetFileFacade assetFileFacade;

    /**
     * Constructs the routing agent. {@code webhookBridgeAgent} is nullable so deployments that don't ship the webhook
     * stack (and thus don't register a {@link WebhookBridgeAgent} bean) still wire the routing agent cleanly — the
     * {@link #runAgent} dispatch falls through to the LLM agent in that case, surfacing workflow chats as plain LLM
     * chats. Sub-optimal but recoverable; the missing-bridge case logs a WARN so ops sees the misroute.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubRoutingAgent(
        String agentId, AiHubSpringAIAgent llmAgent, @Nullable WebhookBridgeAgent webhookBridgeAgent,
        AiHubChatService chatService, AssetFileFacade assetFileFacade) throws AGUIException {

        // LocalAgent's constructor needs a non-null systemMessage OR systemMessageProvider. The router never invokes
        // an LLM directly — `run()` always delegates to either llmAgent or webhookBridgeAgent — so a placeholder
        // satisfies the constructor invariant without being used.
        super(agentId, null, null, "(unused — routing agent delegates to a per-kind agent)", List.of());

        this.llmAgent = llmAgent;
        this.webhookBridgeAgent = webhookBridgeAgent;
        this.chatService = chatService;
        this.assetFileFacade = assetFileFacade;
    }

    @Override
    public CompletableFuture<Void> runAgent(RunAgentParameters parameters, AgentSubscriber subscriber) {
        // Look up the chat BEFORE LocalAgent's run() machinery spins up — we want to delegate the entire
        // run lifecycle to the chosen sub-agent so the underlying agent's own state-management, lifecycle events, and
        // tool-callback dispatch all run cleanly. Calling super.runAgent would drive THIS agent's run() (which we've
        // overridden to a no-op), losing the sub-agent's behaviour.
        Optional<AiHubChat> chatOptional = lookupChat(parameters.getThreadId());
        AiHubChatKind kind = chatOptional.map(AiHubChat::getKind)
            .orElse(AiHubChatKind.STANDARD);

        LocalAgent target;

        if (kind.isWebhookBridged()) {
            if (webhookBridgeAgent == null) {
                // Bridge bean isn't registered — happens in deployments without the webhook coordinator. Fall back to
                // the LLM agent so the user sees an LLM response instead of a hung stream. Log at WARN so the
                // misroute is visible in production logs.
                log.warn(
                    "Webhook-bridged chat (kind={}) routed but WebhookBridgeAgent bean is absent; falling back to LLM agent.",
                    kind);

                target = llmAgent;
            } else {
                target = webhookBridgeAgent;
            }
        } else {
            target = llmAgent;
        }

        // Promote attachments to first-class workspace asset files for STANDARD chats so the LLM agent's
        // existing list/get tools can reach them. WORKFLOW_CHAT chats skip this — the bridge agent runs the
        // upload itself with the chat's own workspace+environment context, which we'd otherwise duplicate.
        // This block is best-effort: a failed upload doesn't fail the turn (the user's text message still flows),
        // it just means the file isn't browsable yet — the LLM can still see whatever's in the workspace.
        if (target == llmAgent && chatOptional.isPresent()) {
            uploadAttachmentsBestEffort(parameters.getForwardedProps(), chatOptional.get());
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "AiHubRoutingAgent dispatching threadId={} kind={} → {}",
                parameters.getThreadId(), kind, target.getClass()
                    .getSimpleName());
        }

        return target.runAgent(parameters, subscriber);
    }

    /**
     * Best-effort upload of {@code forwardedProps.attachments} to AssetFileFacade for STANDARD chats. Each successful
     * upload becomes browsable in the Files panel and discoverable by the LLM through
     * {@code ListAssetFilesToolCallback}. The base64 stays in {@code forwardedProps} for the downstream agent — we
     * don't mutate the parameters to avoid coupling the LLM agent to attachment shape changes.
     *
     * <p>
     * <b>Side-effect-only intent:</b> we don't return the asset file ids or push them into state. The LLM agent's
     * existing system prompt already directs it to use list/get tools when the user references files; once the uploads
     * land in the workspace, those tools see them. A future iteration could surface "files attached this turn" in
     * {@link AiHubStateKeys} so the LLM doesn't have to discover them — but that's a separate prompt-engineering
     * concern.
     * </p>
     *
     * <p>
     * Failures are logged at WARN and the loop keeps going so a single bad attachment doesn't kill the turn or the
     * other attachments — same policy as {@link WebhookBridgeAgent#promoteAttachments}.
     * </p>
     */
    @SuppressWarnings("unchecked")
    private void
        uploadAttachmentsBestEffort(@Nullable Object forwardedProps, AiHubChat chat) {
        if (!(forwardedProps instanceof Map<?, ?> propsMap)) {
            return;
        }

        Object attachmentsValue = propsMap.get("attachments");

        if (!(attachmentsValue instanceof List<?> attachmentsList) || attachmentsList.isEmpty()) {
            return;
        }

        long workspaceId = chatService.getWorkspaceId(chat.getId());
        int environmentOrdinal = chat.getEnvironment()
            .ordinal();

        for (Object raw : attachmentsList) {
            if (!(raw instanceof Map<?, ?> attachmentMap)) {
                continue;
            }

            try {
                AssetFile uploaded = uploadAttachment((Map<String, Object>) attachmentMap, workspaceId,
                    environmentOrdinal);

                if (uploaded != null && log.isDebugEnabled()) {
                    log.debug(
                        "Uploaded attachment to workspace asset files: name={} id={} workspaceId={}",
                        uploaded.getName(), uploaded.getId(), workspaceId);
                }
            } catch (RuntimeException exception) {
                log.warn(
                    "Failed to upload attachment to workspace asset files; user-facing turn unaffected. name={}",
                    attachmentMap.get("name"), exception);
            }
        }
    }

    private @Nullable AssetFile uploadAttachment(
        Map<String, Object> attachmentMap, long workspaceId,
        int environmentOrdinal) {

        Object base64Object = attachmentMap.get("base64");

        if (!(base64Object instanceof String base64String) || base64String.isBlank()) {
            return null;
        }

        Object nameObject = attachmentMap.get("name");
        String filename = nameObject instanceof String nameString && !nameString.isBlank()
            ? nameString
            : "attachment-" + UUID.randomUUID();

        Object contentTypeObject = attachmentMap.get("contentType");
        String contentType = contentTypeObject instanceof String contentTypeString && !contentTypeString.isBlank()
            ? contentTypeString
            : "application/octet-stream";

        byte[] bytes = Base64.getDecoder()
            .decode(base64String);

        return assetFileFacade.createFromUpload(
            workspaceId, environmentOrdinal, filename, contentType, new ByteArrayInputStream(bytes));
    }

    @Override
    protected void run(RunAgentInput input, AgentSubscriber subscriber) {
        // This method only runs if a caller bypasses our overridden `runAgent` and goes through LocalAgent's default
        // path. Treat it as a defensive fallback: emit a RUN_ERROR rather than silently delegate, since the caller's
        // expectation about lifecycle event sequencing is broken in this case.
        log
            .warn("AiHubRoutingAgent.run() invoked directly — caller bypassed runAgent(). Threading unknown.");

        RunErrorEvent err = new RunErrorEvent();

        err.setError("AiHubRoutingAgent.run() invoked directly; please call runAgent() instead.");

        subscriber.onRunErrorEvent(err);
    }

    private Optional<AiHubChat> lookupChat(@Nullable String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return Optional.empty();
        }

        return chatService.findByThreadId(threadId);
    }
}
