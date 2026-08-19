/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

/**
 * Discriminator for the flavours of {@code ai_hub_chat} rows. Drives both the client-side runtime provider selection
 * (standard chats use {@code AiHubRuntimeProvider}, workflow chats use {@code ChatRuntimeProvider}) and the per-row
 * column population (workflow chats carry {@code workflow_execution_id} and {@code project_deployment_id}; standard
 * chats leave both null).
 *
 * <p>
 * Persisted as INT ordinal — append new values at the end to preserve ordinal stability of historical rows. Enforced by
 * {@code EnumOrdinalStabilityTest} in the same module.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubChatKind {

    /**
     * Default kind. The user chats with the LLM agent; the runtime hits AG-UI's {@code runAgent} endpoint.
     * {@code workflow_execution_id} and {@code project_deployment_id} are null on these rows.
     */
    STANDARD,

    /**
     * The chat is bound to a specific workflow execution. The runtime hits the webhook endpoints
     * ({@code /webhooks/<id>} or {@code /webhooks/<id>/sse}) instead of the agent. Carries the workflow's
     * {@code workflow_execution_id} and the parent {@code project_deployment_id} for sidebar grouping.
     */
    WORKFLOW_CHAT,

    /**
     * The chat is bound to an AI Agent's generated workflow. Mechanically identical to {@link #WORKFLOW_CHAT} — same
     * webhook bridge, same {@code workflow_execution_id} + {@code project_deployment_id} columns — and separate from it
     * only so the UI can name what the user actually picked: an agent, not the hidden {@code __AI_AGENT__} project's
     * workflow that backs it.
     *
     * <p>
     * Before this kind existed the client derived the distinction by matching the chat's {@code workflow_execution_id}
     * against the workspace's chat-reachable agent listing, which stopped recognising a chat as soon as its agent was
     * undeployed. Persisting the distinction is what makes it survive that.
     * </p>
     */
    AGENT_CHAT;

    /**
     * Whether turns for this kind are served by {@code WebhookBridgeAgent} (the chat's messages are forwarded to a
     * workflow's webhook trigger) instead of by the AI Hub's own LLM agent.
     *
     * <p>
     * Both routing sites — {@code AiHubRoutingAgent} and {@code WebhookBridgeAgent}'s own misroute guard — ask this
     * rather than comparing against constants, so a future bridged kind cannot be added while leaving one of them
     * answering with the LLM. That failure mode is invisible in logs and reads to the user as the workflow ignoring
     * them.
     * </p>
     */
    public boolean isWebhookBridged() {
        return this == WORKFLOW_CHAT || this == AGENT_CHAT;
    }
}
