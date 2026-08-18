/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.AgentType;

/**
 * The AI-hub-owned agent types: the ai_hub chat flow (with its ASK/BUILD variants and a coarse fallback) and the AI-hub
 * subagents invoked as tools.
 *
 * <p>
 * The automation-owned counterpart of this enum, {@code AutomationSubAgentType} — which held {@code mcp_agent}, then
 * {@code api_collection_agent}, then {@code project_deployment_agent} as they were each dissolved in turn — was deleted
 * outright once {@code project_deployment_agent} (its last value, ticket 732, Task 3) was dissolved too. Its removal
 * means {@code AgentTypeRegistry.keys()} no longer reconstructs those specialists' session keys, so any rows created
 * under the old keys before this change are orphaned rather than migrated (bounded-replay caches, not user data); see
 * {@code AiHubConfiguration#wrapDelegate}'s javadoc for that family's full history.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubAgentType implements AgentType {

    AI_HUB_ASK("ai_hub_ask", false),
    AI_HUB_BUILD("ai_hub_build", false),
    AI_HUB("ai_hub", true),
    FILES("files", true),
    RESEARCH("research", false),
    DATA_ANALYST("data_analyst", false),
    IMAGE_GENERATOR("image_generator", false),
    SLIDE_BUILDER("slide_builder", false),
    WORKFLOW_BUILDER("workflow_builder", false);

    private final String key;
    private final boolean fallback;

    AiHubAgentType(String key, boolean fallback) {
        this.key = key;
        this.fallback = fallback;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public boolean isFallback() {
        return fallback;
    }
}
