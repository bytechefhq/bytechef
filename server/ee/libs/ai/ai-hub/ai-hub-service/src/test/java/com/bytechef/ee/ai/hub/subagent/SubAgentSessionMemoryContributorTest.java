/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubAgentSessionMemoryContributorTest {

    private static final String TASK_AGENT = "task_agent";

    private AiHubSessionMemory aiHubSessionMemory;

    @BeforeEach
    void setUp() {
        aiHubSessionMemory = new AiHubSessionMemory(
            InMemorySessionRepository.builder()
                .build(),
            null);
    }

    @Test
    void testSessionKeyCombinesThreadIdAndAgentType() {
        assertThat(SubAgentSessionMemoryContributor.sessionKey("thread-1", TASK_AGENT))
            .isEqualTo("thread-1:task_agent");
    }

    @Test
    void testDifferentAgentTypesOnSameThreadGetDifferentKeys() {
        String firstKey = SubAgentSessionMemoryContributor.sessionKey("thread-1", TASK_AGENT);
        String secondKey = SubAgentSessionMemoryContributor.sessionKey("thread-1", "mcp_agent");

        assertThat(firstKey).isNotEqualTo(secondKey);
    }

    @Test
    void testDifferentThreadsForOneAgentTypeGetDifferentKeys() {
        String firstKey = SubAgentSessionMemoryContributor.sessionKey("thread-1", TASK_AGENT);
        String secondKey = SubAgentSessionMemoryContributor.sessionKey("thread-2", TASK_AGENT);

        assertThat(firstKey).isNotEqualTo(secondKey);
    }

    @Test
    void testContributeSkipsWhenNoConversationId() {
        ChatClientRequestSpec chatClientRequestSpec = mock(ChatClientRequestSpec.class);

        SubAgentSessionMemoryContributor contributor = new SubAgentSessionMemoryContributor(
            aiHubSessionMemory, TASK_AGENT);

        ChatClientRequestSpec result = contributor.contribute(chatClientRequestSpec, Map.of());

        assertThat(result).isSameAs(chatClientRequestSpec);
    }

    @Test
    void testContributeSkipsWhenToolContextIsNull() {
        ChatClientRequestSpec chatClientRequestSpec = mock(ChatClientRequestSpec.class);

        SubAgentSessionMemoryContributor contributor = new SubAgentSessionMemoryContributor(
            aiHubSessionMemory, TASK_AGENT);

        ChatClientRequestSpec result = contributor.contribute(chatClientRequestSpec, null);

        assertThat(result).isSameAs(chatClientRequestSpec);
    }

    @Test
    void testContributeAttachesMemoryAdvisorWhenConversationIdPresent() {
        ChatClientRequestSpec chatClientRequestSpec = mockFluentRequestSpec();

        SubAgentSessionMemoryContributor contributor = new SubAgentSessionMemoryContributor(
            aiHubSessionMemory, TASK_AGENT);

        contributor.contribute(
            chatClientRequestSpec,
            Map.of(AgentToolInvocationContext.TOOL_CONTEXT_CONVERSATION_ID_KEY, "thread-1"));

        ArgumentCaptor<Advisor[]> advisorsCaptor = ArgumentCaptor.forClass(Advisor[].class);

        verify(chatClientRequestSpec).advisors(advisorsCaptor.capture());

        assertThat(advisorsCaptor.getValue()).hasOnlyElementsOfType(SessionMemoryAdvisor.class);
    }

    /**
     * The advisor resolves its session from {@code SESSION_ID_CONTEXT_KEY} in the request context, and a delegate's
     * {@code chatClient.prompt(request)} sets no advisor params of its own. Without this param the advisor has no
     * session id and the memory silently does nothing, which is why it is pinned rather than assumed.
     */
    @Test
    void testContributePublishesTheSessionIdAdvisorParam() {
        ChatClientRequestSpec chatClientRequestSpec = mockFluentRequestSpec();

        SubAgentSessionMemoryContributor contributor = new SubAgentSessionMemoryContributor(
            aiHubSessionMemory, TASK_AGENT);

        contributor.contribute(
            chatClientRequestSpec,
            Map.of(AgentToolInvocationContext.TOOL_CONTEXT_CONVERSATION_ID_KEY, "thread-1"));

        ArgumentCaptor<Consumer<AdvisorSpec>> advisorSpecCaptor = ArgumentCaptor.captor();

        verify(chatClientRequestSpec).advisors(advisorSpecCaptor.capture());

        AdvisorSpec advisorSpec = mock(AdvisorSpec.class);

        Consumer<AdvisorSpec> advisorSpecConsumer = advisorSpecCaptor.getValue();

        advisorSpecConsumer.accept(advisorSpec);

        verify(advisorSpec).param(
            SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, "thread-1:" + TASK_AGENT);
    }

    private static ChatClientRequestSpec mockFluentRequestSpec() {
        ChatClientRequestSpec chatClientRequestSpec = mock(ChatClientRequestSpec.class);

        when(chatClientRequestSpec.advisors(any(Advisor[].class))).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any()))
            .thenReturn(chatClientRequestSpec);

        return chatClientRequestSpec;
    }
}
