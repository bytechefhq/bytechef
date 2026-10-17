/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.AgentType;

/**
 * The AI Hub agent types: the ASK and BUILD variants of the ai_hub chat flow.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubAgentType implements AgentType {

    AI_HUB_ASK("ai_hub_ask"),
    AI_HUB_BUILD("ai_hub_build");

    private final String key;

    AiHubAgentType(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public boolean isFallback() {
        return false;
    }
}
