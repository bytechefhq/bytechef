/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubChatKind {

    STANDARD,

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
