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
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgent;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentResource;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentResourceKind;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskKind;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-turn dispatcher that picks {@link WebhookBridgeAgent} or the underlying {@link AiHubSpringAIAgent} based on the
 * task's {@link AiHubTaskKind}. Registered as the {@code ai_hub_ask} / {@code ai_hub_build} {@link LocalAgent} bean so
 * the controller's {@code agentId → agent} map dispatches every per-turn invocation through this router first.
 *
 * <p>
 * Routing rule:
 * </p>
 * <ul>
 * <li>{@code task.kind == WORKFLOW_CHAT} — delegate to {@link WebhookBridgeAgent}, which translates webhook events to
 * AG-UI events through {@link com.bytechef.ee.ai.hub.agent.AgUiStreamBridge}.</li>
 * <li>{@code task.kind == STANDARD} (default for legacy rows) — delegate to the wrapped {@link AiHubSpringAIAgent},
 * preserving today's LLM-driven behaviour for standard tasks.</li>
 * <li>AiHubTask not found (race with delete, unknown threadId) — delegate to the LLM agent. The agent's own error
 * handling will surface the missing-task case if it matters.</li>
 * </ul>
 *
 * <p>
 * <b>Why route at the agent layer and not the controller?</b> Keeping the routing inside a {@link LocalAgent}
 * implementation means the controller stays unchanged — {@code AiHubApiController} continues to dispatch by URL path
 * (agentId), so any future agent-level routing concern (per-task feature flags, A/B test buckets) lands here too. The
 * controller's job remains "URL → agent"; the agent's job is "task → behaviour".
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
    private final AiHubTaskService taskService;
    private final AssetFileFacade assetFileFacade;
    private final @Nullable AiHubPersonalAgentService aiHubPersonalAgentService;

    /**
     * Constructs the routing agent. {@code webhookBridgeAgent} is nullable so deployments that don't ship the webhook
     * stack (and thus don't register a {@link WebhookBridgeAgent} bean) still wire the routing agent cleanly — the
     * {@link #runAgent} dispatch falls through to the LLM agent in that case, surfacing workflow-chat tasks as plain
     * LLM chats. Sub-optimal but recoverable; the missing-bridge case logs a WARN so ops sees the misroute.
     *
     * <p>
     * {@code aiHubPersonalAgentService} is also nullable so deployments that don't ship the personal-agent feature
     * still wire cleanly — a PERSONAL_AGENT task in such a deployment falls through to plain LLM behaviour (the agent's
     * instructions are simply not applied). Same recoverable-degradation pattern as the bridge.
     * </p>
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubRoutingAgent(
        String agentId, AiHubSpringAIAgent llmAgent, @Nullable WebhookBridgeAgent webhookBridgeAgent,
        AiHubTaskService taskService, AssetFileFacade assetFileFacade,
        @Nullable AiHubPersonalAgentService aiHubPersonalAgentService) throws AGUIException {

        // LocalAgent's constructor needs a non-null systemMessage OR systemMessageProvider. The router never invokes
        // an LLM directly — `run()` always delegates to either llmAgent or webhookBridgeAgent — so a placeholder
        // satisfies the constructor invariant without being used.
        super(agentId, null, null, "(unused — routing agent delegates to a per-kind agent)", List.of());

        this.llmAgent = llmAgent;
        this.webhookBridgeAgent = webhookBridgeAgent;
        this.taskService = taskService;
        this.assetFileFacade = assetFileFacade;
        this.aiHubPersonalAgentService = aiHubPersonalAgentService;
    }

    @Override
    public CompletableFuture<Void> runAgent(RunAgentParameters parameters, AgentSubscriber subscriber) {
        // Look up the task BEFORE LocalAgent's run() machinery spins up — we want to delegate the entire
        // run lifecycle to the chosen sub-agent so the underlying agent's own state-management, lifecycle events, and
        // tool-callback dispatch all run cleanly. Calling super.runAgent would drive THIS agent's run() (which we've
        // overridden to a no-op), losing the sub-agent's behaviour.
        Optional<AiHubTask> taskOptional = lookupTask(parameters.getThreadId());
        AiHubTaskKind kind = taskOptional.map(AiHubTask::getKind)
            .orElse(AiHubTaskKind.STANDARD);

        LocalAgent target;

        if (kind == AiHubTaskKind.WORKFLOW_CHAT) {
            if (webhookBridgeAgent == null) {
                // Bridge bean isn't registered — happens in deployments without the webhook coordinator. Fall back to
                // the LLM agent so the user sees an LLM response instead of a hung stream. Log at WARN so the
                // misroute is visible in production logs.
                log.warn(
                    "WORKFLOW_CHAT task routed but WebhookBridgeAgent bean is absent; falling back to LLM agent.");

                target = llmAgent;
            } else {
                target = webhookBridgeAgent;
            }
        } else if (kind == AiHubTaskKind.PERSONAL_AGENT && taskOptional.isPresent()) {
            // PERSONAL_AGENT runs through the same LLM agent as STANDARD, but with the agent's instructions
            // injected as state so createSystemMessage picks them up as a Context entry. Failure to resolve the
            // agent (deleted while a task row still references it, no service bean wired) falls through
            // to plain LLM behaviour — same recoverable-degradation pattern as the bridge fallback above.
            applyAiHubPersonalAgentOverlay(taskOptional.get(), parameters);

            target = llmAgent;
        } else {
            target = llmAgent;
        }

        // Promote attachments to first-class workspace asset files for STANDARD tasks so the LLM agent's
        // existing list/get tools can reach them. WORKFLOW_CHAT tasks skip this — the bridge agent runs the
        // upload itself with the task's own workspace+environment context, which we'd otherwise duplicate.
        // This block is best-effort: a failed upload doesn't fail the turn (the user's text message still flows),
        // it just means the file isn't browsable yet — the LLM can still see whatever's in the workspace.
        if (target == llmAgent && taskOptional.isPresent()) {
            uploadAttachmentsBestEffort(parameters.getForwardedProps(), taskOptional.get());
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
     * Best-effort upload of {@code forwardedProps.attachments} to AssetFileFacade for STANDARD tasks. Each successful
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
        uploadAttachmentsBestEffort(@Nullable Object forwardedProps, AiHubTask task) {
        if (!(forwardedProps instanceof Map<?, ?> propsMap)) {
            return;
        }

        Object attachmentsValue = propsMap.get("attachments");

        if (!(attachmentsValue instanceof List<?> attachmentsList) || attachmentsList.isEmpty()) {
            return;
        }

        long workspaceId = taskService.getWorkspaceId(task.getId());
        int environmentOrdinal = task.getEnvironment()
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

    private Optional<AiHubTask> lookupTask(@Nullable String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return Optional.empty();
        }

        return taskService.findByThreadId(threadId);
    }

    /**
     * Resolves the personal agent referenced by the task and injects its instructions into the parameters'
     * {@link State} under {@link AiHubStateKeys#PERSONAL_AGENT_INSTRUCTIONS_KEY}. Best-effort — a missing service bean,
     * a missing agent id (data race with delete), or an agent with blank instructions are all "no-op overlay" rather
     * than surfaced errors. The user-facing turn still runs against plain LLM behaviour, just without the agent's
     * personality.
     *
     * <p>
     * Mutating {@link State} on the parameters is safe because {@link RunAgentParameters#getState()} returns the
     * mutable state object the LLM agent will read in {@code createSystemMessage}. If state is null on the parameters
     * (e.g. a controller path that didn't initialise it), we create one rather than fail — the LocalAgent base class
     * handles a null parameters state by falling back to {@code this.state}, which would silently bypass our overlay.
     * </p>
     */
    private void
        applyAiHubPersonalAgentOverlay(AiHubTask task, RunAgentParameters parameters) {
        if (aiHubPersonalAgentService == null) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "PERSONAL_AGENT task {} routed but AiHubPersonalAgentService bean is absent; running as plain LLM.",
                    task.getId());
            }

            return;
        }

        Long aiHubPersonalAgentId = task.getAiHubPersonalAgentId();

        if (aiHubPersonalAgentId == null) {
            // Defensive: PERSONAL_AGENT rows MUST carry a aiHubPersonalAgentId — the create path always stamps
            // it. A
            // null value indicates a kind/payload mismatch (e.g. a row mis-tagged as PERSONAL_AGENT). Run as plain
            // LLM rather than fail the turn.
            log.warn(
                "PERSONAL_AGENT task {} has null aiHubPersonalAgentId; running as plain LLM.",
                task.getId());

            return;
        }

        Optional<AiHubPersonalAgent> agentOptional = aiHubPersonalAgentService
            .findOwned(aiHubPersonalAgentId, taskService.getWorkspaceId(task.getId()), task.getUserId());

        if (agentOptional.isEmpty()) {
            // Agent was deleted while the task row still references it. The task continues to work
            // as a plain LLM chat — message history stays accessible. A future cleanup job can reconcile.
            if (log.isDebugEnabled()) {
                log.debug(
                    "PERSONAL_AGENT task {} references missing agent {}; running as plain LLM.",
                    task.getId(), aiHubPersonalAgentId);
            }

            return;
        }

        AiHubPersonalAgent agent = agentOptional.get();
        State state = ensureState(parameters);

        // §6: merge the agent's resource template (read live each turn) into the referencedResources state list so
        // AiHubSpringAIAgent.createSystemMessage renders them in the "Referenced Resources" Context block alongside
        // any resources the user @-mentioned this turn. Done before the blank-instructions early return below so an
        // agent with resources but no instructions still gets its resources injected.
        List<AiHubPersonalAgentResource> resources = aiHubPersonalAgentService.listResources(aiHubPersonalAgentId);

        if (!resources.isEmpty()) {
            state.set("referencedResources", mergeReferencedResources(resources, state.get("referencedResources")));
        }

        // Per-agent LLM model override. Both fields together or neither — service validation guarantees this. When
        // both are null the keys aren't injected at all, so AiHubSpringAIAgent.resolveChatClient falls back to the
        // workspace default ChatClient (the builder-time singleton).
        if (agent.hasLlmOverride()) {
            state.set(AiHubStateKeys.PERSONAL_AGENT_LLM_PROVIDER_KEY, agent.getLlmProvider());
            state.set(AiHubStateKeys.PERSONAL_AGENT_LLM_MODEL_KEY, agent.getLlmModel());
        }

        String instructions = agent.getInstructions();

        if (instructions == null || instructions.isBlank()) {
            // An agent with no instructions is effectively a labelled LLM — still set the title so the LLM can
            // refer to itself by name, but skip the empty instructions overlay.
            state.set(AiHubStateKeys.PERSONAL_AGENT_TITLE_KEY, agent.getTitle());

            return;
        }

        state.set(AiHubStateKeys.PERSONAL_AGENT_INSTRUCTIONS_KEY, instructions);
        state.set(AiHubStateKeys.PERSONAL_AGENT_TITLE_KEY, agent.getTitle());
    }

    /**
     * Returns the mutable {@link State} on {@code parameters}, creating one if absent. {@link RunAgentParameters} is
     * built from a Builder and does NOT expose a state setter, so we cannot attach a fresh state to an existing
     * parameters instance — but the LocalAgent base class reads {@code parameters.getState()} when non-null, so
     * mutating an existing State works. The fall-through to a fresh state is mostly for the test path; production
     * callers always populate state in the controller layer.
     */
    private static State ensureState(RunAgentParameters parameters) {
        State state = parameters.getState();

        if (state == null) {
            // No way to retrofit state onto an immutable RunAgentParameters — create a fresh one and rely on the
            // caller to pass it through. This branch is best-effort; if it's hit, the LocalAgent base will use
            // this.state (the agent's class-level state), bypassing our overlay. Log at DEBUG so the unusual path
            // is discoverable.
            return new State();
        }

        return state;
    }

    /**
     * Merges a personal agent's resource template into whatever {@code referencedResources} the client already put in
     * the AG-UI state for this turn. Client-supplied entries are kept verbatim and first; each agent resource is
     * appended as a {@code {kind, id, name}} map unless an entry with the same {@code (kind, id)} is already present
     * (so an @-mention of an already-pinned resource is not doubled). The result is what
     * {@code AiHubSpringAIAgent.createSystemMessage} renders as the "Referenced Resources" Context block.
     *
     * <p>
     * Package-private + static so the unit test can pin the merge/dedup/kind-mapping behaviour directly, mirroring
     * {@code AiHubSpringAIAgent.appendAiHubPersonalAgentContext}.
     * </p>
     */
    static List<Object> mergeReferencedResources(
        List<AiHubPersonalAgentResource> agentResources, @Nullable Object existingReferencedResources) {

        List<Object> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Keep every client-supplied entry verbatim. Record the (kind, id) of the map-shaped ones so the agent's
        // resources dedup against them; non-map entries (defensive) are kept but don't participate in dedup.
        if (existingReferencedResources instanceof List<?> existing) {
            for (Object entry : existing) {
                merged.add(entry);

                if (entry instanceof Map<?, ?> entryMap) {
                    seen.add(entryMap.get("kind") + "|" + entryMap.get("id"));
                }
            }
        }

        for (AiHubPersonalAgentResource resource : agentResources) {
            String kind = composerKind(resource.getKind());
            String dedupeKey = kind + "|" + resource.getResourceId();

            if (seen.add(dedupeKey)) {
                merged.add(Map.of(
                    "kind", kind, "id", resource.getResourceId(), "name", resource.getResourceName()));
            }
        }

        return merged;
    }

    /**
     * Maps an {@link AiHubPersonalAgentResourceKind} to the lowercase wire string the AI Hub composer uses for the same
     * kind ({@code ReferencedResourceKindType} in {@code useAiHubComposerStore.ts}), so agent-pinned resources and
     * per-message @-mentions render homogeneously in the prompt.
     */
    private static String composerKind(AiHubPersonalAgentResourceKind kind) {
        return switch (kind) {
            case WORKFLOW -> "workflow";
            case FILE -> "file";
            case DATA_TABLE -> "dataTable";
            case KNOWLEDGE_BASE -> "knowledgeBase";
            case MCP_SERVER -> "mcpServer";
            case API_COLLECTION -> "apiCollection";
            case WORKFLOW_EXECUTION -> "workflowExecution";
            case TASK -> "task";
        };
    }
}
