/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.mcp.tool.usage;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum Agent {

    AI_HUB("ai_hub"),
    RESEARCH("research"),
    DATA_ANALYST("data_analyst"),
    IMAGE_GENERATOR("image_generator"),
    SLIDE_BUILDER("slide_builder"),
    SKILLS("skills"),
    CLUSTER_ELEMENT_AGENT("cluster_element_agent"),
    CODE_EDITOR_AGENT("code_editor_agent"),
    WORKFLOW_EDITOR_AGENT("workflow_editor_agent"),
    CONVERTER_AGENT("converter_agent"),
    WORKFLOW_EXECUTION_AGENT("workflow_execution_agent");

    private final String key;

    Agent(String key) {
        this.key = key;
    }

    public static Agent fromKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        for (Agent agent : values()) {
            if (agent.key.equals(key)) {
                return agent;
            }
        }

        throw new IllegalArgumentException("Unknown agent key: " + key);
    }
}
