/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * @version ee
 */
@SuppressFBWarnings("EI")
public record AiGatewayChatMessage(
    AiGatewayChatRole role,
    String content,
    List<AiGatewayContentBlock> contentBlocks,
    List<ToolCall> toolCalls,
    String toolCallId) {

    public AiGatewayChatMessage {
        Validate.notNull(role, "role must not be null");

        if (role == AiGatewayChatRole.TOOL) {
            Validate.notBlank(toolCallId, "toolCallId is required for TOOL messages");
        }

        if (role == AiGatewayChatRole.SYSTEM || role == AiGatewayChatRole.USER) {
            Validate.isTrue(
                toolCalls == null || toolCalls.isEmpty(),
                "toolCalls must not be set for %s messages", role);
        }

        if (content != null && contentBlocks != null && !contentBlocks.isEmpty()) {
            Validate.isTrue(false, "message must have either content or contentBlocks, not both");
        }
    }

    public AiGatewayChatMessage(AiGatewayChatRole role, String content) {
        this(role, content, null, null, null);
    }

    public AiGatewayChatMessage(String role, String content) {
        this(AiGatewayChatRole.fromValue(role), content, null, null, null);
    }

    public AiGatewayChatMessage(
        AiGatewayChatRole role, String content, List<ToolCall> toolCalls, String toolCallId) {

        this(role, content, null, toolCalls, toolCallId);
    }

    public boolean hasContentBlocks() {
        return contentBlocks != null && !contentBlocks.isEmpty();
    }

    /**
     * @version ee
     */
    public record ToolCall(String id, String type, ToolCallFunction function) {
    }

    /**
     * @version ee
     */
    public record ToolCallFunction(String name, String arguments) {
    }
}
