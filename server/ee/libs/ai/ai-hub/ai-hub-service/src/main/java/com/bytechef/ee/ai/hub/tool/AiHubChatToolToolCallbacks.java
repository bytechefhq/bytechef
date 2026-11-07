/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolConnectionValidator;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubChatToolToolCallbacks {

    private final ToolCallback listChatToolsToolCallback;
    private final ToolCallback attachChatToolToolCallback;
    private final ToolCallback removeChatToolToolCallback;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubChatToolToolCallbacks(
        AiHubChatService chatService, AiHubChatToolFacade chatToolFacade,
        AiHubChatToolConnectionValidator connectionValidator, AiHubToolAttachMetrics metrics, JsonMapper jsonMapper) {

        this.listChatToolsToolCallback =
            new ListChatToolsToolCallback(chatService, chatToolFacade, metrics, jsonMapper);
        this.attachChatToolToolCallback = new AttachChatToolToolCallback(
            chatService, chatToolFacade, connectionValidator, metrics);
        this.removeChatToolToolCallback = new RemoveChatToolToolCallback(chatService, chatToolFacade);
    }

    public List<ToolCallback> read() {
        return List.of(listChatToolsToolCallback);
    }

    public List<ToolCallback> write() {
        return List.of(listChatToolsToolCallback, attachChatToolToolCallback, removeChatToolToolCallback);
    }
}
