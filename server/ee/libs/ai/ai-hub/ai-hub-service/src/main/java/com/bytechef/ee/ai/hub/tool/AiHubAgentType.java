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
    WORKFLOW_BUILDER("workflow_builder", false),
    PERSONAL_AGENT_MANAGER("personal_agent_manager", false);

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
