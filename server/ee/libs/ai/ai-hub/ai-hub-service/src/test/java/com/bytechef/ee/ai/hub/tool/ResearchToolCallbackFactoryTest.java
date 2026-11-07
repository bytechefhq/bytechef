/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.platform.ai.tool.usage.MeteredToolCallback;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageContext;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ResearchToolCallbackFactoryTest {

    private final AiHubChatService chatService = mock(AiHubChatService.class);
    private final ToolUsageRecorder toolUsageRecorder = mock(ToolUsageRecorder.class);

    @Test
    void testMapMeteredToolName() {
        assertThat(ResearchToolCallbackFactory.mapMeteredToolName("webSearch")).isEqualTo("firecrawl_search");
        assertThat(ResearchToolCallbackFactory.mapMeteredToolName("webpageScrape")).isEqualTo("firecrawl_scrape");
        assertThat(ResearchToolCallbackFactory.mapMeteredToolName("braveWebSearch")).isEqualTo("brave_search");
        assertThat(ResearchToolCallbackFactory.mapMeteredToolName("websiteMap")).isNull();
    }

    @Test
    void testCreateReturnsTheResearchTool() {
        ToolCallback toolCallback = newFactory(toolUsageRecorder).create(List.of(webSearchToolCallback("webSearch")));

        ToolDefinition toolDefinition = toolCallback.getToolDefinition();

        assertThat(toolDefinition.name()).isEqualTo("research");
        assertThat(toolDefinition.description()).containsIgnoringCase("report");
    }

    @Test
    void testMeterWrapsOnlyTheBilledWebSearchTools() {
        List<ToolCallback> toolCallbacks = newFactory(toolUsageRecorder).meter(
            List.of(
                webSearchToolCallback("webSearch"), webSearchToolCallback("webpageScrape"),
                webSearchToolCallback("braveWebSearch"), webSearchToolCallback("websiteMap")));

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactly("webSearch", "webpageScrape", "braveWebSearch", "websiteMap");
        assertThat(toolCallbacks.subList(0, 3)).allMatch(MeteredToolCallback.class::isInstance);
        assertThat(toolCallbacks.get(3)).isNotInstanceOf(MeteredToolCallback.class);
    }

    @Test
    void testMeterLeavesToolsUnwrappedWithoutARecorder() {
        List<ToolCallback> toolCallbacks = newFactory(null).meter(List.of(webSearchToolCallback("webSearch")));

        assertThat(toolCallbacks.getFirst()).isNotInstanceOf(MeteredToolCallback.class);
    }

    @Test
    void testMeteredWebSearchCallRecordsAToolUsageRow() {
        AiHubChat aiHubChat = new AiHubChat(7L);

        aiHubChat.setId(42L);

        when(chatService.findByThreadId("thread-1")).thenReturn(Optional.of(aiHubChat));

        ToolCallback meteredToolCallback = newFactory(toolUsageRecorder)
            .meter(List.of(webSearchToolCallback("braveWebSearch")))
            .getFirst();

        meteredToolCallback.call("{\"query\":\"workflow automation vendors\"}", parentToolContext());

        verify(toolUsageRecorder).recordTool(
            eq(new ToolUsageContext(3L, 7L, 2, 42L)), eq("brave_search"), eq(1), anyLong(),
            eq(Map.of("query", "workflow automation vendors")));
    }

    @Test
    void testMeteredWebSearchCallSkipsErrorResults() {
        ToolCallback webSearchToolCallback = webSearchToolCallback("webSearch");

        when(webSearchToolCallback.call(anyString(), any())).thenReturn("{\"error\":\"rate limited\"}");

        ToolCallback meteredToolCallback = newFactory(toolUsageRecorder)
            .meter(List.of(webSearchToolCallback))
            .getFirst();

        meteredToolCallback.call("{\"query\":\"anything\"}", parentToolContext());

        verify(toolUsageRecorder, never()).recordTool(any(), anyString(), eq(1), anyLong(), any());
    }

    @Test
    void testResearchHandsTheSubagentMeteredToolsAndTheParentContext() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);
        AtomicReference<List<ToolCallback>> subagentToolCallbacks = new AtomicReference<>();
        AtomicReference<Map<String, Object>> subagentToolContext = new AtomicReference<>();

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenAnswer(invocation -> {
            subagentToolCallbacks.set(invocation.getArgument(0));

            return requestSpec;
        });
        when(requestSpec.toolContext(anyMap())).thenAnswer(invocation -> {
            subagentToolContext.set(invocation.getArgument(0));

            return requestSpec;
        });
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenAnswer(invocation -> {
            subagentToolCallbacks.get()
                .getFirst()
                .call("{\"query\":\"ipaas pricing\"}", new ToolContext(subagentToolContext.get()));

            return "# Report";
        });

        ResearchToolCallbackFactory factory = new ResearchToolCallbackFactory(
            candidateChatModel -> chatClient, null, toolUsageRecorder, chatService, new JsonMapper());

        String result = factory.create(List.of(webSearchToolCallback("webSearch")))
            .call("{\"topic\":\"iPaaS pricing\"}", parentToolContext());

        assertThat(result).contains("# Report");

        verify(toolUsageRecorder).recordTool(
            eq(new ToolUsageContext(3L, 7L, 2, null)), eq("firecrawl_search"), eq(1), anyLong(),
            eq(Map.of("query", "ipaas pricing")));
    }

    private ResearchToolCallbackFactory newFactory(ToolUsageRecorder toolUsageRecorder) {
        return new ResearchToolCallbackFactory(
            candidateChatModel -> mock(ChatClient.class), null, toolUsageRecorder, chatService, new JsonMapper());
    }

    private static ToolContext parentToolContext() {
        AiHubToolInvocationContext invocationContext = new AiHubToolInvocationContext(
            3L, 7L, null, null, 2L, "thread-1");

        return new ToolContext(invocationContext.toToolContext());
    }

    private static ToolCallback webSearchToolCallback(String name) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name(name)
                .description("Search the web")
                .inputSchema("{}")
                .build());
        when(toolCallback.call(anyString(), any())).thenReturn("{\"results\":[]}");

        return toolCallback;
    }
}
