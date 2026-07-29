/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageContext;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageRecorder;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageSource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Pins the recording behavior of {@link AiHubModelUsageAdvisor}: workspace-attributed turns are metered into
 * {@link LlmUsageRecorder}, context-less turns are skipped, and the streaming path accumulates the cumulative chunk
 * counters into a single record per turn.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubModelUsageAdvisorTest {

    private final LlmUsageRecorder llmUsageRecorder = mock(LlmUsageRecorder.class);

    private final AiHubModelUsageAdvisor advisor = new AiHubModelUsageAdvisor("ai_hub_ask", llmUsageRecorder);

    @Test
    void testCallRecordsUsageWithWorkspaceContext() {
        ChatClientRequest request = requestWithContext(
            Map.of(
                AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L,
                AiHubStateKeys.AUTHENTICATED_USER_ID, 3L,
                ChatMemory.CONVERSATION_ID, "thread-1"));

        CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);

        when(callAdvisorChain.nextCall(any())).thenReturn(response(100, 20, "claude-sonnet-5"));

        advisor.adviseCall(request, callAdvisorChain);

        ArgumentCaptor<LlmUsageContext> contextCaptor = ArgumentCaptor.forClass(LlmUsageContext.class);

        verify(llmUsageRecorder).recordLlm(
            contextCaptor.capture(), eq("claude-sonnet-5"), eq(100), eq(20), anyLong());

        LlmUsageContext recordedContext = contextCaptor.getValue();

        assertThat(recordedContext.workspaceId()).isEqualTo(7L);
        assertThat(recordedContext.userId()).isEqualTo(3L);
        assertThat(recordedContext.source()).isEqualTo(LlmUsageSource.AI_HUB);
        assertThat(recordedContext.agentName()).isEqualTo("ai_hub_ask");
        assertThat(recordedContext.metadata()).containsEntry("threadId", "thread-1");
    }

    @Test
    void testCallWithoutWorkspaceContextSkipsRecording() {
        ChatClientRequest request = requestWithContext(Map.of());

        CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);

        when(callAdvisorChain.nextCall(any())).thenReturn(response(100, 20, "claude-sonnet-5"));

        advisor.adviseCall(request, callAdvisorChain);

        verify(llmUsageRecorder, never()).recordLlm(any(), any(), anyInt(), anyInt(), anyLong());
    }

    @Test
    void testCallWithZeroTokensSkipsRecording() {
        ChatClientRequest request = requestWithContext(Map.of(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L));

        CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);

        when(callAdvisorChain.nextCall(any())).thenReturn(response(0, 0, "claude-sonnet-5"));

        advisor.adviseCall(request, callAdvisorChain);

        verify(llmUsageRecorder, never()).recordLlm(any(), any(), anyInt(), anyInt(), anyLong());
    }

    @Test
    void testStreamAccumulatesCumulativeCountersAndRecordsOnce() {
        ChatClientRequest request = requestWithContext(Map.of(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 9L));

        StreamAdvisorChain streamAdvisorChain = mock(StreamAdvisorChain.class);

        // Providers report cumulative counters on streaming chunks (Anthropic: prompt tokens on message_start,
        // cumulative output tokens on message_delta) — the advisor must record the maximum seen, once.
        when(streamAdvisorChain.nextStream(any())).thenReturn(Flux.just(
            response(120, 1, null),
            response(120, 34, "claude-sonnet-5"),
            response(120, 57, "claude-sonnet-5")));

        advisor.adviseStream(request, streamAdvisorChain)
            .blockLast();

        ArgumentCaptor<LlmUsageContext> contextCaptor = ArgumentCaptor.forClass(LlmUsageContext.class);

        verify(llmUsageRecorder).recordLlm(
            contextCaptor.capture(), eq("claude-sonnet-5"), eq(120), eq(57), anyLong());

        assertThat(contextCaptor.getValue()
            .workspaceId()).isEqualTo(9L);
    }

    @Test
    void testNullRecorderStillPassesResponseThrough() {
        AiHubModelUsageAdvisor loggingOnlyAdvisor = new AiHubModelUsageAdvisor("ai_hub_ask", null);

        ChatClientRequest request = requestWithContext(Map.of(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L));

        CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);

        ChatClientResponse response = response(10, 5, "claude-sonnet-5");

        when(callAdvisorChain.nextCall(any())).thenReturn(response);

        assertThat(loggingOnlyAdvisor.adviseCall(request, callAdvisorChain)).isSameAs(response);
    }

    private static ChatClientRequest requestWithContext(Map<String, Object> context) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(List.of(new UserMessage("hi"))))
            .context(context)
            .build();
    }

    private static ChatClientResponse response(int promptTokens, int completionTokens, String model) {
        ChatResponseMetadata.Builder metadataBuilder = ChatResponseMetadata.builder()
            .usage(new DefaultUsage(promptTokens, completionTokens));

        if (model != null) {
            metadataBuilder.model(model);
        }

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage("ok"))))
            .metadata(metadataBuilder.build())
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();
    }
}
