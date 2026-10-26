/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails.GuardrailCheckResult;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.platform.ai.guardrails.PublishedInputSpans;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGuardrailsAdvisorPublishesSpansTest {

    private static final SensitiveSpan USER_SPAN = SensitiveSpan.of(SensitiveKind.PII, "EMAIL_ADDRESS", 11, 22);
    private static final SensitiveSpan SYSTEM_SPAN = SensitiveSpan.of(SensitiveKind.PII, "PHONE_NUMBER", 0, 12);

    @Test
    void testPublishesTheSpansItDetectedInUserMessagesOnly() {
        AiGuardrails aiGuardrails = mock(AiGuardrails.class);

        when(aiGuardrails.newTokenSession()).thenReturn(PiiTokenSession.create());
        when(aiGuardrails.tokenizeInputs(anyList(), any(), any(), any()))
            .thenReturn(List.of(
                new GuardrailCheckResult("555-123-4567", null, null, List.of(SYSTEM_SPAN)),
                new GuardrailCheckResult("contact me [PII_EMAIL_ADDRESS_1_abcd]", null, null, List.of(USER_SPAN))));
        when(aiGuardrails.scanResponseText(any(), any(AiGuardrailsSettingsTarget.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(aiGuardrails.restoreResponseText(any(), any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiGuardrailsAdvisor advisor = AiGuardrailsAdvisorTest.advisorOver(aiGuardrails);

        AtomicReference<ChatClientRequest> seenByTheChain = new AtomicReference<>();
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenAnswer(invocation -> {
            seenByTheChain.set(invocation.getArgument(0));

            return ChatClientResponse.builder()
                .chatResponse(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))))
                .context(invocation.<ChatClientRequest>getArgument(0)
                    .context())
                .build();
        });

        advisor.adviseCall(
            ChatClientRequest.builder()
                .prompt(new Prompt(List.of(new SystemMessage("555-123-4567"), new UserMessage("contact me a@b.com"))))
                .build(),
            chain);

        assertThat(PublishedInputSpans.from(seenByTheChain.get()
            .context()))
                .as("SYSTEM spans are the operator's, not the caller's; only USER spans are published")
                .containsExactly(USER_SPAN);
    }

    @Test
    void testPublishesNothingWhenTheFloorDetectedNothing() {
        AiGuardrails aiGuardrails = mock(AiGuardrails.class);

        when(aiGuardrails.newTokenSession()).thenReturn(PiiTokenSession.create());
        when(aiGuardrails.tokenizeInputs(anyList(), any(), any(), any()))
            .thenReturn(List.of(new GuardrailCheckResult("hello", null, null, List.of())));
        when(aiGuardrails.scanResponseText(any(), any(AiGuardrailsSettingsTarget.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(aiGuardrails.restoreResponseText(any(), any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiGuardrailsAdvisor advisor = AiGuardrailsAdvisorTest.advisorOver(aiGuardrails);

        AtomicReference<ChatClientRequest> seenByTheChain = new AtomicReference<>();
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenAnswer(invocation -> {
            seenByTheChain.set(invocation.getArgument(0));

            return ChatClientResponse.builder()
                .chatResponse(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))))
                .build();
        });

        advisor.adviseCall(
            ChatClientRequest.builder()
                .prompt(new Prompt(List.of(new UserMessage("hello"))))
                .build(),
            chain);

        assertThat(seenByTheChain.get()
            .context()).doesNotContainKey(PublishedInputSpans.CONTEXT_KEY);
    }
}
