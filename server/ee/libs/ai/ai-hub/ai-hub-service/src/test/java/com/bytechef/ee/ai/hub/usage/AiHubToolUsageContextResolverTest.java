/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageContext;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubToolUsageContextResolverTest {

    private final AiHubChatService aiHubChatService = mock(AiHubChatService.class);
    private final Function<ToolContext, ToolUsageContext> contextResolver =
        AiHubToolUsageContextResolver.create(aiHubChatService, "firecrawl_scrape");

    @Test
    void testCreateResolvesWorkspaceUserEnvironmentAndChat() {
        AiHubChat aiHubChat = new AiHubChat(7L);

        aiHubChat.setId(42L);

        when(aiHubChatService.findByThreadId("thread-1")).thenReturn(Optional.of(aiHubChat));

        ToolUsageContext toolUsageContext = contextResolver.apply(createToolContext("thread-1"));

        assertThat(toolUsageContext).isEqualTo(new ToolUsageContext(3L, 7L, 1, 42L));
    }

    @Test
    void testCreateKeepsUsageWhenChatIsNotFound() {
        when(aiHubChatService.findByThreadId("thread-1")).thenReturn(Optional.empty());

        ToolUsageContext toolUsageContext = contextResolver.apply(createToolContext("thread-1"));

        assertThat(toolUsageContext).isEqualTo(new ToolUsageContext(3L, 7L, 1, null));
    }

    @Test
    void testCreateKeepsUsageWhenChatLookupFails() {
        when(aiHubChatService.findByThreadId("thread-1")).thenThrow(new IllegalStateException("database down"));

        ToolUsageContext toolUsageContext = contextResolver.apply(createToolContext("thread-1"));

        assertThat(toolUsageContext).isEqualTo(new ToolUsageContext(3L, 7L, 1, null));
    }

    @Test
    void testCreateSkipsChatLookupWithoutThreadId() {
        ToolUsageContext toolUsageContext = contextResolver.apply(createToolContext(null));

        assertThat(toolUsageContext).isEqualTo(new ToolUsageContext(3L, 7L, 1, null));

        verify(aiHubChatService, never()).findByThreadId(anyString());
    }

    @Test
    void testCreateReturnsNullWithoutInvocationContext() {
        assertThat(contextResolver.apply(new ToolContext(Map.of()))).isNull();
        assertThat(contextResolver.apply(null)).isNull();
    }

    private static ToolContext createToolContext(String threadId) {
        AiHubToolInvocationContext aiHubToolInvocationContext =
            new AiHubToolInvocationContext(3L, 7L, null, null, 1L, threadId);

        return new ToolContext(aiHubToolInvocationContext.toToolContext());
    }
}
