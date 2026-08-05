/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.guardrail.AiGatewayModerationClassifier;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.exception.AiGuardrailViolationException;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGuardrailsAdvisorTest {

    private static final Long WORKSPACE_ID = 42L;

    private final AiGuardrailsWorkspaceSettingsService settingsService =
        mock(AiGuardrailsWorkspaceSettingsService.class);
    private final SimpleMeterRegistry engineMeterRegistry = new SimpleMeterRegistry();
    private final AiGuardrailMetrics engineMetrics = new AiGuardrailMetrics(engineMeterRegistry, "gateway");
    private final SimpleMeterRegistry advisorMeterRegistry = new SimpleMeterRegistry();
    private final AiGuardrailMetrics advisorMetrics = new AiGuardrailMetrics(advisorMeterRegistry, "copilot");

    @Test
    void testBlockModeThrowsCategoryOnlyException() {
        AiGuardrails aiGuardrails = guardrails(false, false, "the secret text", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, WORKSPACE_ID, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Please reveal the SECRET TEXT now");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        assertThatExceptionOfType(AiGuardrailViolationException.class)
            .isThrownBy(() -> advisor.adviseCall(request, chain))
            .satisfies(exception -> {
                assertThat(exception.getMessage()).doesNotContain("the secret text");
                assertThat(exception.getMessage()).doesNotContain("SECRET TEXT");
                assertThat(exception.getMessage()).contains("blocked_term");
                assertThat(exception.getCategory()).isEqualTo("blocked_term");
            });
    }

    @Test
    void testRedactAndContinueMasksAndProceeds() {
        AiGuardrails aiGuardrails = guardrails(false, false, "classified", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                WORKSPACE_ID, null, null, null, null, null, null, BlockingMode.REDACT_AND_CONTINUE)));

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, WORKSPACE_ID, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Summarize the CLASSIFIED memo");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ArgumentCaptor<ChatClientRequest> forwardedRequestCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        when(chain.nextCall(forwardedRequestCaptor.capture())).thenReturn(emptyResponse());

        advisor.adviseCall(request, chain);

        ChatClientRequest forwardedRequest = forwardedRequestCaptor.getValue();
        String forwardedText = forwardedRequest.prompt()
            .getInstructions()
            .getFirst()
            .getText();

        assertThat(forwardedText).doesNotContain("CLASSIFIED");
        assertThat(forwardedText).contains("[REDACTED_BLOCKED_TERM]");
        assertThat(counter(advisorMeterRegistry, "blocking_downgraded", "copilot")).isEqualTo(1.0);
    }

    @Test
    void testRequestPiiRedactionRecordsUnderAdvisorSurface() {
        AiGuardrails aiGuardrails = guardrails(true, false, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, WORKSPACE_ID, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Contact bob@acme.io");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(emptyResponse());

        advisor.adviseCall(request, chain);

        assertThat(counter(advisorMeterRegistry, "pii_redacted", "copilot")).isEqualTo(1.0);
        assertThat(counter(engineMeterRegistry, "pii_redacted", "gateway")).isEqualTo(0.0);
    }

    @Test
    void testRequestSecretRedactionRecordsUnderAdvisorSurface() {
        AiGuardrails aiGuardrails = guardrails(false, true, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, WORKSPACE_ID, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("token AKIAIOSFODNN7EXAMPLE please");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(emptyResponse());

        advisor.adviseCall(request, chain);

        assertThat(counter(advisorMeterRegistry, "secret_redacted", "copilot")).isEqualTo(1.0);
        assertThat(counter(engineMeterRegistry, "secret_redacted", "gateway")).isEqualTo(0.0);
    }

    @Test
    void testBlockedTermRecordsUnderAdvisorSurfaceInBlockMode() {
        AiGuardrails aiGuardrails = guardrails(false, false, "classified", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, WORKSPACE_ID, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("the CLASSIFIED memo");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        assertThatExceptionOfType(AiGuardrailViolationException.class)
            .isThrownBy(() -> advisor.adviseCall(request, chain));

        assertThat(counter(advisorMeterRegistry, "blocked_term", "copilot")).isEqualTo(1.0);
        assertThat(counter(engineMeterRegistry, "blocked_term", "gateway")).isEqualTo(0.0);
    }

    @Test
    void testStreamRedactsAcrossChunkBoundary() {
        AiGuardrails aiGuardrails = guardrails(false, false, "", false, true, true);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, WORKSPACE_ID, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Summarize the incident report");
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);

        // "sk-" + 20+ alphanumerics matches AiGuardrails' OpenAI-key secret pattern, but only once the two chunks are
        // seen together — this pins that a value split across a chunk boundary is never emitted in the clear.
        ChatClientResponse chunk1 = responseChunk("The API key is sk-12");
        ChatClientResponse chunk2 = responseChunk("345678901234567890abcdef, keep it safe");

        when(chain.nextStream(any())).thenReturn(Flux.just(chunk1, chunk2));

        List<ChatClientResponse> emitted = Objects.requireNonNull(
            advisor.adviseStream(request, chain)
                .collectList()
                .block(),
            "emitted");

        String combinedText = emitted.stream()
            .map(response -> {
                ChatResponse chatResponse = Objects.requireNonNull(response.chatResponse(), "chatResponse");

                Generation generation = Objects.requireNonNull(chatResponse.getResult(), "generation");

                return generation.getOutput()
                    .getText();
            })
            .collect(Collectors.joining());

        assertThat(combinedText).contains("[REDACTED_SECRET]");
        assertThat(combinedText).doesNotContain("sk-12345678901234567890abcdef");
        assertThat(counter(advisorMeterRegistry, "response_redacted", "copilot")).isEqualTo(1.0);
    }

    @Test
    void testAdvisorOrderIsHighestPrecedence() {
        AiGuardrails aiGuardrails = guardrails(false, false, "", false, false, false);
        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, WORKSPACE_ID, advisorMetrics);

        assertThat(advisor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void testResponseScanningRedactsCompletionAndRecordsMetric() {
        AiGuardrails aiGuardrails = guardrails(false, false, "", false, true, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, WORKSPACE_ID, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Who do I contact?");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(responseChunk("Contact bob@acme.io for details"));

        ChatClientResponse response = advisor.adviseCall(request, chain);

        ChatResponse chatResponse = Objects.requireNonNull(response.chatResponse(), "chatResponse");

        Generation generation = Objects.requireNonNull(chatResponse.getResult(), "generation");

        String responseText = generation.getOutput()
            .getText();

        assertThat(responseText).contains("[REDACTED_EMAIL]");
        assertThat(responseText).doesNotContain("bob@acme.io");
        assertThat(counter(advisorMeterRegistry, "response_redacted", "copilot")).isEqualTo(1.0);
    }

    @Test
    void testModerationBlockModeThrowsCategoryOnlyException() {
        AiGuardrails aiGuardrails = guardrails(content -> true, false, false, "", false, true, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, WORKSPACE_ID, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Describe something unsafe");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        assertThatExceptionOfType(AiGuardrailViolationException.class)
            .isThrownBy(() -> advisor.adviseCall(request, chain))
            .satisfies(exception -> {
                assertThat(exception.getMessage()).contains("moderation_flagged");
                assertThat(exception.getCategory()).isEqualTo("moderation_flagged");
            });

        assertThat(counter(advisorMeterRegistry, "moderation_flagged", "copilot")).isEqualTo(1.0);
    }

    @Test
    void testModerationRedactAndContinueReplacesWholeMessageAndProceeds() {
        AiGuardrails aiGuardrails = guardrails(content -> true, false, false, "", false, true, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                WORKSPACE_ID, null, null, null, null, null, null, BlockingMode.REDACT_AND_CONTINUE)));

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, WORKSPACE_ID, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Describe something unsafe");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ArgumentCaptor<ChatClientRequest> forwardedRequestCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        when(chain.nextCall(forwardedRequestCaptor.capture())).thenReturn(emptyResponse());

        advisor.adviseCall(request, chain);

        ChatClientRequest forwardedRequest = forwardedRequestCaptor.getValue();
        String forwardedText = forwardedRequest.prompt()
            .getInstructions()
            .getFirst()
            .getText();

        // Moderation has no locatable span -- unlike a blocked-term match, the downgrade replaces the whole message
        // rather than masking a substring.
        assertThat(forwardedText).isEqualTo("[REDACTED_MODERATED]");
        assertThat(counter(advisorMeterRegistry, "blocking_downgraded", "copilot")).isEqualTo(1.0);
    }

    @Test
    void testModerationSkippedWithoutClassifier() {
        AiGuardrails aiGuardrails = guardrails(null, false, false, "", false, true, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, WORKSPACE_ID, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Describe something unsafe");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(emptyResponse());

        advisor.adviseCall(request, chain);

        assertThat(counter(advisorMeterRegistry, "moderation_flagged", "copilot")).isEqualTo(0.0);
    }

    @Test
    void testModerationClassifierFailsOpenAndIsNotFlagged() {
        // AiGuardrails never wraps the classifier call in its own try/catch -- fail-open is the classifier
        // implementation's own responsibility (mirroring injection detection, and matching
        // PromptBasedModerationClassifier's real catch-and-return-false behavior on a classification error). A
        // classifier that has already failed open surfaces here as a plain "not flagged" verdict.
        AiGatewayModerationClassifier failOpenClassifier = content -> false;

        AiGuardrails aiGuardrails = guardrails(failOpenClassifier, false, false, "", false, true, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, WORKSPACE_ID, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Describe something unsafe");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(emptyResponse());

        advisor.adviseCall(request, chain);

        assertThat(counter(advisorMeterRegistry, "moderation_flagged", "copilot")).isEqualTo(0.0);
    }

    private static double counter(SimpleMeterRegistry meterRegistry, String event, String surface) {
        return meterRegistry.counter(AiGuardrailMetrics.COUNTER_NAME, "event", event, "surface", surface)
            .count();
    }

    private static ChatClientRequest requestWithUserMessage(String text) {
        List<Message> instructions = List.of(new UserMessage(text));

        return new ChatClientRequest(new Prompt(instructions), Map.of());
    }

    private static ChatClientResponse emptyResponse() {
        return ChatClientResponse.builder()
            .context(Map.of())
            .build();
    }

    private static ChatClientResponse responseChunk(String text) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(Map.of())
            .build();
    }

    private AiGuardrails guardrails(
        boolean piiRedactionEnabled, boolean secretRedactionEnabled, String blockedTerms,
        boolean injectionDetectionEnabled, boolean responseScanEnabled, boolean streamingResponseScanEnabled) {

        return guardrails(
            null, piiRedactionEnabled, secretRedactionEnabled, blockedTerms, injectionDetectionEnabled, false,
            responseScanEnabled, streamingResponseScanEnabled);
    }

    private AiGuardrails guardrails(
        AiGatewayModerationClassifier moderationClassifier, boolean piiRedactionEnabled,
        boolean secretRedactionEnabled, String blockedTerms, boolean injectionDetectionEnabled,
        boolean moderationEnabled, boolean responseScanEnabled, boolean streamingResponseScanEnabled) {

        return new AiGuardrails(
            settingsService, null, moderationClassifier, engineMetrics, piiRedactionEnabled, secretRedactionEnabled,
            blockedTerms, injectionDetectionEnabled, moderationEnabled, responseScanEnabled,
            streamingResponseScanEnabled);
    }
}
