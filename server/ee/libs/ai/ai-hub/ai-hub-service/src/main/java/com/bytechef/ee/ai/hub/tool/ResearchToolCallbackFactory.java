/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.usage.AiHubToolUsageContextResolver;
import com.bytechef.ee.platform.ai.tool.usage.MeteredToolCallback;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ResearchToolCallbackFactory {

    private final IntelligentToolChatClientFactory chatClientFactory;
    private final AiHubChatService chatService;
    private final @Nullable SubAgentChatModelResolver chatModelResolver;
    private final JsonMapper jsonMapper;
    private final @Nullable ToolUsageRecorder toolUsageRecorder;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ResearchToolCallbackFactory(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver,
        @Nullable ToolUsageRecorder toolUsageRecorder, AiHubChatService chatService, JsonMapper jsonMapper) {

        this.chatClientFactory = chatClientFactory;
        this.chatModelResolver = chatModelResolver;
        this.toolUsageRecorder = toolUsageRecorder;
        this.chatService = chatService;
        this.jsonMapper = jsonMapper;
    }

    public ToolCallback create(List<ToolCallback> webSearchToolCallbacks) {
        return new ResearchToolCallback(chatClientFactory, chatModelResolver, meter(webSearchToolCallbacks));
    }

    List<ToolCallback> meter(List<ToolCallback> webSearchToolCallbacks) {
        List<ToolCallback> meteredToolCallbacks = new ArrayList<>(webSearchToolCallbacks.size());

        for (ToolCallback webSearchToolCallback : webSearchToolCallbacks) {
            meteredToolCallbacks.add(meter(webSearchToolCallback));
        }

        return meteredToolCallbacks;
    }

    static @Nullable String mapMeteredToolName(String toolName) {
        return switch (toolName) {
            case "webSearch" -> "firecrawl_search";
            case "webpageScrape" -> "firecrawl_scrape";
            case "braveWebSearch" -> "brave_search";
            default -> null;
        };
    }

    private ToolCallback meter(ToolCallback webSearchToolCallback) {
        String toolName = webSearchToolCallback.getToolDefinition()
            .name();

        String meteredToolName = mapMeteredToolName(toolName);

        if (meteredToolName == null || toolUsageRecorder == null) {
            return webSearchToolCallback;
        }

        return new MeteredToolCallback(
            webSearchToolCallback, meteredToolName, 1, toolUsageRecorder,
            AiHubToolUsageContextResolver.create(chatService, meteredToolName),
            MeteredToolCallback.singleStringField("webpageScrape".equals(toolName) ? "url" : "query"), jsonMapper);
    }
}
