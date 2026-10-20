/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

/**
 * Discriminator for the flavours of {@code ai_hub_chat} rows. Both kinds run through the client's
 * {@code AiHubRuntimeProvider} and the AI Hub's AG-UI endpoint; {@code AiHubRoutingAgent} picks the server-side agent
 * by kind (see {@link #isWebhookBridged()}). The kind also drives the per-row column population: workflow chats carry
 * {@code workflow_execution_id} and {@code project_deployment_id}, standard chats leave both null.
 *
 * <p>
 * Persisted as INT ordinal; append new values at the end to preserve ordinal stability of historical rows. Enforced by
 * {@code EnumOrdinalStabilityTest} in the ai-hub-service module.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubChatKind {

    /**
     * Default kind. The user chats with the AI Hub's LLM agent. {@code workflow_execution_id} and
     * {@code project_deployment_id} are null on these rows.
     */
    STANDARD,

    /**
     * The chat is bound to a specific workflow execution. Its turns reach the same AG-UI endpoint as standard chats;
     * {@code AiHubRoutingAgent} hands them to {@code WebhookBridgeAgent}, which runs the workflow through its webhook
     * trigger on the server and streams the result back. Carries the workflow's {@code workflow_execution_id} and the
     * parent {@code project_deployment_id} for sidebar grouping.
     */
    WORKFLOW_CHAT;

    /**
     * Whether turns for this kind are served by {@code WebhookBridgeAgent} (the chat's messages are forwarded to a
     * workflow's webhook trigger) instead of by the AI Hub's own LLM agent.
     *
     * <p>
     * Both routing sites, {@code AiHubRoutingAgent} and {@code WebhookBridgeAgent}'s own misroute guard, ask this
     * rather than comparing against constants, so a future bridged kind cannot be added while leaving one of them
     * answering with the LLM. That failure mode is invisible in logs and reads to the user as the workflow ignoring
     * them.
     * </p>
     */
    public boolean isWebhookBridged() {
        return this == WORKFLOW_CHAT;
    }
}
