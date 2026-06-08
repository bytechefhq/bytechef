/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.agent.tool;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum Agent {

    AI_HUB_ASK("ai_hub_ask", false),
    AI_HUB_BUILD("ai_hub_build", false),
    AI_HUB("ai_hub", true),
    WORKFLOW_EDITOR_ASK("workflow_editor_ask", false),
    WORKFLOW_EDITOR_BUILD("workflow_editor_build", false),
    WORKFLOW_EDITOR("workflow_editor", true),
    CODE_EDITOR_ASK("code_editor_ask", false),
    CODE_EDITOR_BUILD("code_editor_build", false),
    CODE_EDITOR("code_editor", true),
    CLUSTER_ELEMENT_ASK("cluster_element_ask", false),
    CLUSTER_ELEMENT_BUILD("cluster_element_build", false),
    CLUSTER_ELEMENT("cluster_element", true),
    FILES("files", true),
    RESEARCH("research", false),
    DATA_ANALYST("data_analyst", false),
    IMAGE_GENERATOR("image_generator", false),
    SLIDE_BUILDER("slide_builder", false),
    WORKFLOW_BUILDER("workflow_builder", false),
    UNKNOWN("unknown", true),
    SKILLS("skills", false),
    CLUSTER_ELEMENT_AGENT("cluster_element_agent", false),
    CODE_EDITOR_AGENT("code_editor_agent", false),
    WORKFLOW_EDITOR_AGENT("workflow_editor_agent", false),
    CONVERTER_AGENT("converter_agent", false),
    WORKFLOW_EXECUTION_AGENT("workflow_execution_agent", false);

    private final String key;
    private final boolean fallback;

    Agent(String key, boolean fallback) {
        this.key = key;
        this.fallback = fallback;
    }

    /**
     * The wire-format key for this agent — matches the {@code agentId} routed by the chat REST controller. Useful for
     * log lines and for callers (e.g. agent builders) that need the snake-case identifier.
     */
    public String key() {
        return key;
    }

    /**
     * Whether this enum value represents a coarse fallback emitted because no {@link CurrentAgentContext.AgentBinding}
     * was bound. Fallback values ({@link #AI_HUB}, {@link #WORKFLOW_EDITOR}, {@link #CODE_EDITOR},
     * {@link #CLUSTER_ELEMENT}, {@link #FILES}, {@link #UNKNOWN}) are mixed into the same persisted column as bound
     * agents — without filtering on {@code !isFallback()} a "cost by agent" query lumps unattributed traffic in with
     * the surface that should have been bound.
     */
    public boolean isFallback() {
        return fallback;
    }

    /**
     * Resolves an {@link Agent} from its wire-format key (case-sensitive). Returns {@link #UNKNOWN} when the key is
     * {@code null} or does not match any registered agent. Both inputs collapse to {@code UNKNOWN} by design — the
     * usage pipeline tolerates malformed/missing agent ids by attributing them to a single bucket rather than failing
     * the call. Callers that need to distinguish "not provided" from "unrecognized" should null-check before calling.
     */
    public static Agent fromKey(String key) {
        if (key == null) {
            return UNKNOWN;
        }

        for (Agent agent : values()) {
            if (agent.key.equals(key)) {
                return agent;
            }
        }

        return UNKNOWN;
    }
}
