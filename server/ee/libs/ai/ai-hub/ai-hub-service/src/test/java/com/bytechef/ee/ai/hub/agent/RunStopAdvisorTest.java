/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RunStopAdvisorTest {

    @Test
    void testAStopEndsTheRunsModelStreamAndCancelsItUpstream() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();

        registry.tryStart("thread-1", "run-1");

        AtomicBoolean upstreamCancelled = new AtomicBoolean();
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);

        when(chain.nextStream(any())).thenReturn(Flux.<ChatClientResponse>never()
            .doOnCancel(() -> upstreamCancelled.set(true)));

        Flux<ChatClientResponse> advised = new RunStopAdvisor(registry).adviseStream(request("thread-1"), chain);

        StepVerifier.create(advised)
            .expectSubscription()
            .then(() -> registry.cancel("thread-1"))
            .expectComplete()
            .verify(Duration.ofSeconds(5));

        assertThat(upstreamCancelled).isTrue();
    }

    @Test
    void testAnotherThreadsStopLeavesTheRunStreaming() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();

        registry.tryStart("thread-1", "run-1");
        registry.tryStart("thread-2", "run-2");

        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);

        when(chain.nextStream(any())).thenReturn(Flux.never());

        StepVerifier.create(new RunStopAdvisor(registry).adviseStream(request("thread-1"), chain))
            .expectSubscription()
            .then(() -> registry.cancel("thread-2"))
            .expectNoEvent(Duration.ofMillis(100))
            .thenCancel()
            .verify();
    }

    private static ChatClientRequest request(String threadId) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(List.of()))
            .context(Map.of(ChatMemory.CONVERSATION_ID, threadId))
            .build();
    }
}
