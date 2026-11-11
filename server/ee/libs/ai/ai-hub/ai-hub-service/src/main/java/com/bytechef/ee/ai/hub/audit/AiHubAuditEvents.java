/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubAuditEvents {

    public static final String AI_HUB_CHAT_CREATED = "AI_HUB_CHAT_CREATED";
    public static final String AI_HUB_CHAT_DELETED = "AI_HUB_CHAT_DELETED";
    public static final String AI_HUB_TOOL_APPROVAL_DECIDED = "AI_HUB_TOOL_APPROVAL_DECIDED";
    public static final String AI_HUB_TOOL_APPROVAL_REQUESTED = "AI_HUB_TOOL_APPROVAL_REQUESTED";
    public static final String AI_HUB_TOOL_APPROVAL_RULE_CHANGED = "AI_HUB_TOOL_APPROVAL_RULE_CHANGED";

    private AiHubAuditEvents() {
    }
}
