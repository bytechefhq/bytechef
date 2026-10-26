/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.classifier.AiGuardrailModerationClassifier;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.exception.AiGuardrailViolationException;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.platform.ai.guardrails.GuardrailAdvisorOrder;
import com.bytechef.platform.ai.guardrails.GuardrailSurface;
import com.bytechef.platform.ai.guardrails.RestorationDestination;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataDetector;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSessionToolContext;
import com.bytechef.platform.ai.sensitivedata.tokenization.SensitiveDataPolicy;
import com.bytechef.platform.ai.sensitivedata.tokenization.SensitiveDataPolicyToolContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
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
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGuardrailsAdvisorTest {

    private static final Long WORKSPACE_ID = 42L;
    private static final Pattern EMAIL_TOKEN_PATTERN = Pattern.compile("\\[PII_EMAIL_ADDRESS_1_[a-z0-9]{4}\\]");

    private final AiGuardrailsWorkspaceSettingsService settingsService =
        mock(AiGuardrailsWorkspaceSettingsService.class);
    private final SimpleMeterRegistry engineMeterRegistry = new SimpleMeterRegistry();
    private final AiGuardrailMetrics engineMetrics = new AiGuardrailMetrics(engineMeterRegistry, "gateway");
    private final SimpleMeterRegistry advisorMeterRegistry = new SimpleMeterRegistry();
    private final AiGuardrailMetrics advisorMetrics = new AiGuardrailMetrics(advisorMeterRegistry, "copilot");

    static AiGuardrailsAdvisor advisorOver(AiGuardrails aiGuardrails) {
        return advisorOver(aiGuardrails, new AiGuardrailMetrics(new SimpleMeterRegistry(), "copilot"));
    }

    private static AiGuardrailsAdvisor advisorOver(AiGuardrails aiGuardrails, AiGuardrailMetrics metrics) {
        return new AiGuardrailsAdvisor(
            aiGuardrails, AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID), metrics, GuardrailSurface.AI_AGENT,
            RestorationDestination.CONVERSATION);
    }

    @Test
    void testBlockModeThrowsCategoryOnlyException() {
        AiGuardrails aiGuardrails = guardrails(false, false, "the secret text", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
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
    void testSessionIsClosedWhenBlockModeThrows() {
        AiGuardrails aiGuardrails = guardrails(false, false, "the secret text", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrails spiedAiGuardrails = spy(aiGuardrails);
        PiiTokenSession session = spiedAiGuardrails.newTokenSession();

        doReturn(session).when(spiedAiGuardrails)
            .newTokenSession();

        session.tokenFor("EMAIL", "bob@acme.io");

        assertThat(session.size()).isEqualTo(1);

        AiGuardrailsAdvisor advisor = advisorOver(spiedAiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Please reveal the SECRET TEXT now");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        assertThatExceptionOfType(AiGuardrailViolationException.class)
            .isThrownBy(() -> advisor.adviseCall(request, chain));

        assertThat(session.size()).isZero();
    }

    @Test
    void testRedactAndContinueMasksAndProceeds() {
        AiGuardrails aiGuardrails = guardrails(false, false, "classified", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                AiGuardrailsSettingsScope.WORKSPACE, WORKSPACE_ID, null, null, null, null, null, null,
                BlockingMode.REDACT_AND_CONTINUE, null, null,
                null)));

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
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
    void testAllowForwardsTheBlockedTermUnmaskedAndRecordsGuardrailAllowed() {
        AiGuardrails aiGuardrails = guardrails(false, false, "classified", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                AiGuardrailsSettingsScope.WORKSPACE, WORKSPACE_ID, null, null, null, null, null, null,
                BlockingMode.ALLOW, null, null,
                null)));

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
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

        assertThat(forwardedText).isEqualTo("Summarize the CLASSIFIED memo");

        assertThat(counter(advisorMeterRegistry, "blocked_term", "copilot")).isEqualTo(1.0);
        assertThat(counter(advisorMeterRegistry, "guardrail_allowed", "copilot")).isEqualTo(1.0);

        assertThat(counter(advisorMeterRegistry, "blocking_downgraded", "copilot")).isEqualTo(0.0);
    }

    @Test
    void testAllowDoesNotThrowOnAPayloadBlockModeWouldReject() {
        AiGuardrails aiGuardrails = guardrails(false, false, "classified", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                AiGuardrailsSettingsScope.WORKSPACE, WORKSPACE_ID, null, null, null, null, null, null,
                BlockingMode.ALLOW, null, null,
                null)));

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("the CLASSIFIED memo");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(emptyResponse());

        assertThatCode(() -> advisor.adviseCall(request, chain)).doesNotThrowAnyException();
    }

    @Test
    void testModerationAllowForwardsTheWholeMessageUnmodified() {
        AiGuardrails aiGuardrails = guardrails(content -> true, false, false, "", false, true, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                AiGuardrailsSettingsScope.WORKSPACE, WORKSPACE_ID, null, null, null, null, null, null,
                BlockingMode.ALLOW, null, null,
                null)));

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
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

        assertThat(forwardedText).isEqualTo("Describe something unsafe");
        assertThat(counter(advisorMeterRegistry, "moderation_flagged", "copilot")).isEqualTo(1.0);
        assertThat(counter(advisorMeterRegistry, "guardrail_allowed", "copilot")).isEqualTo(1.0);
    }

    @Test
    void testRequestPiiRedactionRecordsUnderAdvisorSurface() {
        AiGuardrails aiGuardrails = guardrails(true, false, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Contact bob@acme.io");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(emptyResponse());

        advisor.adviseCall(request, chain);

        assertThat(counter(advisorMeterRegistry, "pii_tokenized", "copilot")).isEqualTo(1.0);
        assertThat(counter(advisorMeterRegistry, "pii_redacted", "copilot")).isEqualTo(0.0);
        assertThat(counter(engineMeterRegistry, "pii_redacted", "gateway")).isEqualTo(0.0);
    }

    @Test
    void testRequestSecretRedactionRecordsUnderAdvisorSurface() {
        AiGuardrails aiGuardrails = guardrails(false, true, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
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

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("the CLASSIFIED memo");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        assertThatExceptionOfType(AiGuardrailViolationException.class)
            .isThrownBy(() -> advisor.adviseCall(request, chain));

        assertThat(counter(advisorMeterRegistry, "blocked_term", "copilot")).isEqualTo(1.0);
        assertThat(counter(engineMeterRegistry, "blocked_term", "gateway")).isEqualTo(0.0);
    }

    @Test
    void testStreamRedactsAcrossChunkBoundary() {
        AiGuardrails aiGuardrails = guardrails(false, true, "", false, true, true);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Summarize the incident report");
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);

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
    void testStreamTokenizesRequestAndRestoresResponseAcrossChunkBoundary() {
        AiGuardrails aiGuardrails = guardrails(true, false, "", false, true, true);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("forward bob@acme.io's note to the team");
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);
        ArgumentCaptor<ChatClientRequest> forwardedCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        when(chain.nextStream(forwardedCaptor.capture())).thenAnswer(invocation -> {
            ChatClientRequest forwardedRequest = invocation.getArgument(0);
            String forwardedText = forwardedRequest.prompt()
                .getInstructions()
                .getFirst()
                .getText();

            Matcher tokenMatcher = EMAIL_TOKEN_PATTERN.matcher(forwardedText);

            assertThat(tokenMatcher.find())
                .as("forwarded text should contain an email token, was: %s", forwardedText)
                .isTrue();

            String token = tokenMatcher.group();
            int splitIndex = token.length() / 2;
            ChatClientResponse chunk1 = responseChunk("Sure, forwarding to " + token.substring(0, splitIndex));
            ChatClientResponse chunk2 = responseChunk(token.substring(splitIndex) + " now.");

            return Flux.just(chunk1, chunk2);
        });

        List<ChatClientResponse> emitted = Objects.requireNonNull(
            advisor.adviseStream(request, chain)
                .collectList()
                .block(),
            "emitted");

        String forwardedText = forwardedCaptor.getValue()
            .prompt()
            .getInstructions()
            .getFirst()
            .getText();

        assertThat(forwardedText).doesNotContain("bob@acme.io");
        assertThat(forwardedText).containsPattern(EMAIL_TOKEN_PATTERN);

        String combinedText = emitted.stream()
            .map(response -> {
                ChatResponse chatResponse = Objects.requireNonNull(response.chatResponse(), "chatResponse");

                Generation generation = Objects.requireNonNull(chatResponse.getResult(), "generation");

                return generation.getOutput()
                    .getText();
            })
            .collect(Collectors.joining());

        assertThat(combinedText).isEqualTo("Sure, forwarding to bob@acme.io now.");
    }

    @Test
    void testStreamSessionClosedOnCompletion() {
        AiGuardrails aiGuardrails = guardrails(true, false, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrails spiedAiGuardrails = spy(aiGuardrails);
        PiiTokenSession session = spiedAiGuardrails.newTokenSession();

        doReturn(session).when(spiedAiGuardrails)
            .newTokenSession();

        session.tokenFor("EMAIL", "bob@acme.io");

        assertThat(session.size()).isEqualTo(1);

        AiGuardrailsAdvisor advisor = advisorOver(spiedAiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("contact bob@acme.io");
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);

        when(chain.nextStream(any())).thenReturn(Flux.just(responseChunk("done")));

        advisor.adviseStream(request, chain)
            .collectList()
            .block();

        assertThat(session.size()).isZero();
    }

    @Test
    void testStreamSessionClosedOnError() {
        AiGuardrails aiGuardrails = guardrails(true, false, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrails spiedAiGuardrails = spy(aiGuardrails);
        PiiTokenSession session = spiedAiGuardrails.newTokenSession();

        doReturn(session).when(spiedAiGuardrails)
            .newTokenSession();

        session.tokenFor("EMAIL", "bob@acme.io");

        assertThat(session.size()).isEqualTo(1);

        AiGuardrailsAdvisor advisor = advisorOver(spiedAiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("contact bob@acme.io");
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);

        when(chain.nextStream(any())).thenReturn(Flux.error(new IllegalStateException("boom")));

        Flux<ChatClientResponse> result = advisor.adviseStream(request, chain);

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> result.collectList()
                .block());

        assertThat(session.size()).isZero();
    }

    @Test
    void testStreamSessionClosedOnCancellation() {
        AiGuardrails aiGuardrails = guardrails(true, false, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrails spiedAiGuardrails = spy(aiGuardrails);
        PiiTokenSession session = spiedAiGuardrails.newTokenSession();

        doReturn(session).when(spiedAiGuardrails)
            .newTokenSession();

        session.tokenFor("EMAIL", "bob@acme.io");

        assertThat(session.size()).isEqualTo(1);

        AiGuardrailsAdvisor advisor = advisorOver(spiedAiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("contact bob@acme.io");
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);

        when(chain.nextStream(any())).thenReturn(Flux.never());

        Disposable disposable = advisor.adviseStream(request, chain)
            .subscribe();

        disposable.dispose();

        assertThat(session.size()).isZero();
    }

    @Test
    void testStreamSessionClosedWhenBlockModeThrows() {
        AiGuardrails aiGuardrails = guardrails(false, false, "the secret text", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrails spiedAiGuardrails = spy(aiGuardrails);
        PiiTokenSession session = spiedAiGuardrails.newTokenSession();

        doReturn(session).when(spiedAiGuardrails)
            .newTokenSession();

        session.tokenFor("EMAIL", "bob@acme.io");

        assertThat(session.size()).isEqualTo(1);

        AiGuardrailsAdvisor advisor = advisorOver(spiedAiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Please reveal the SECRET TEXT now");
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);

        Flux<ChatClientResponse> result = advisor.adviseStream(request, chain);

        assertThatExceptionOfType(AiGuardrailViolationException.class)
            .isThrownBy(() -> result.collectList()
                .block());

        assertThat(session.size()).isZero();
    }

    @Test
    void testStreamPassesThroughUnbufferedAndSessionClosedWhenNothingMintedAndScanDisabled() {
        AiGuardrails aiGuardrails = guardrails(false, false, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrails spiedAiGuardrails = spy(aiGuardrails);
        PiiTokenSession session = spiedAiGuardrails.newTokenSession();

        doReturn(session).when(spiedAiGuardrails)
            .newTokenSession();

        AiGuardrailsAdvisor advisor = advisorOver(spiedAiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Summarize the incident report");
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);

        ChatClientResponse chunk1 = responseChunk("Contact ");
        ChatClientResponse chunk2 = responseChunk("bob@acme.io for details");

        when(chain.nextStream(any())).thenReturn(Flux.just(chunk1, chunk2));

        List<ChatClientResponse> emitted = Objects.requireNonNull(
            advisor.adviseStream(request, chain)
                .collectList()
                .block(),
            "emitted");

        assertThat(emitted).hasSize(2);
        assertThat(emitted.get(0)).isSameAs(chunk1);
        assertThat(emitted.get(1)).isSameAs(chunk2);

        String combinedText = emitted.stream()
            .map(response -> {
                ChatResponse chatResponse = Objects.requireNonNull(response.chatResponse(), "chatResponse");

                Generation generation = Objects.requireNonNull(chatResponse.getResult(), "generation");

                return generation.getOutput()
                    .getText();
            })
            .collect(Collectors.joining());

        assertThat(combinedText).isEqualTo("Contact bob@acme.io for details");

        assertThat(session.size()).isZero();
    }

    @Test
    void testRoundTripRestoresDistinctValues() {
        AiGuardrails aiGuardrails = guardrails(true, true, "", false, true, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("forward bob@acme.io's note to alice@acme.io");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ArgumentCaptor<ChatClientRequest> forwardedCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        when(chain.nextCall(forwardedCaptor.capture()))
            .thenAnswer(invocation -> responseChunk(
                ((ChatClientRequest) invocation.getArgument(0)).prompt()
                    .getInstructions()
                    .getFirst()
                    .getText()));

        ChatClientResponse response = advisor.adviseCall(request, chain);

        String forwarded = forwardedCaptor.getValue()
            .prompt()
            .getInstructions()
            .getFirst()
            .getText();

        assertThat(forwarded).doesNotContain("bob@acme.io");
        assertThat(forwarded).doesNotContain("alice@acme.io");
        assertThat(forwarded).containsPattern("\\[PII_EMAIL_ADDRESS_1_[a-z0-9]{4}\\]");
        assertThat(forwarded).containsPattern("\\[PII_EMAIL_ADDRESS_2_[a-z0-9]{4}\\]");

        ChatResponse chatResponse = Objects.requireNonNull(response.chatResponse(), "chatResponse");
        Generation generation = Objects.requireNonNull(chatResponse.getResult(), "generation");
        String returned = generation.getOutput()
            .getText();

        assertThat(returned).isEqualTo("forward bob@acme.io's note to alice@acme.io");
    }

    @Test
    void testAdviseCallPutsTheSessionIntoTheForwardedToolContext() {
        AiGuardrails aiGuardrails = guardrails(false, false, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithToolCallingOptions("Summarize the incident report", Map.of());
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ArgumentCaptor<ChatClientRequest> forwardedRequestCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        when(chain.nextCall(forwardedRequestCaptor.capture())).thenReturn(emptyResponse());

        advisor.adviseCall(request, chain);

        assertThat(PiiTokenSessionToolContext.from(capturedToolContext(forwardedRequestCaptor))).isNotNull();
    }

    @Test
    void testAdviseCallPreservesExistingToolContextEntries() {
        AiGuardrails aiGuardrails = guardrails(false, false, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request =
            requestWithToolCallingOptions("Summarize the incident report", Map.of("existing", "kept"));
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ArgumentCaptor<ChatClientRequest> forwardedRequestCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        when(chain.nextCall(forwardedRequestCaptor.capture())).thenReturn(emptyResponse());

        advisor.adviseCall(request, chain);

        ToolContext forwardedToolContext = capturedToolContext(forwardedRequestCaptor);

        assertThat(forwardedToolContext).isNotNull();
        assertThat(Objects.requireNonNull(forwardedToolContext)
            .getContext()).containsEntry("existing", "kept");
        assertThat(PiiTokenSessionToolContext.from(forwardedToolContext)).isNotNull();
    }

    @Test
    void testAdviseCallPutsTheResolvedToolBoundaryPolicyIntoTheForwardedToolContext() {
        AiGuardrails aiGuardrails = guardrails(false, true, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithToolCallingOptions("Summarize the incident report", Map.of());
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ArgumentCaptor<ChatClientRequest> forwardedRequestCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        when(chain.nextCall(forwardedRequestCaptor.capture())).thenReturn(emptyResponse());

        advisor.adviseCall(request, chain);

        SensitiveDataPolicy policy =
            SensitiveDataPolicyToolContext.from(capturedToolContext(forwardedRequestCaptor));

        assertThat(policy).isNotNull();
        assertThat(Objects.requireNonNull(policy)
            .kinds()).containsExactly(SensitiveKind.SECRET);
    }

    @Test
    void testAdviseStreamPutsTheSessionIntoTheForwardedToolContext() {
        AiGuardrails aiGuardrails = guardrails(false, false, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithToolCallingOptions("Summarize the incident report", Map.of());
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);
        ArgumentCaptor<ChatClientRequest> forwardedRequestCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        when(chain.nextStream(forwardedRequestCaptor.capture())).thenReturn(Flux.just(responseChunk("done")));

        advisor.adviseStream(request, chain)
            .collectList()
            .block();

        assertThat(PiiTokenSessionToolContext.from(capturedToolContext(forwardedRequestCaptor))).isNotNull();
    }

    @Test
    void testAdviseStreamPreservesExistingToolContextEntries() {
        AiGuardrails aiGuardrails = guardrails(false, false, "", false, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request =
            requestWithToolCallingOptions("Summarize the incident report", Map.of("existing", "kept"));
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);
        ArgumentCaptor<ChatClientRequest> forwardedRequestCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        when(chain.nextStream(forwardedRequestCaptor.capture())).thenReturn(Flux.just(responseChunk("done")));

        advisor.adviseStream(request, chain)
            .collectList()
            .block();

        ToolContext forwardedToolContext = capturedToolContext(forwardedRequestCaptor);

        assertThat(forwardedToolContext).isNotNull();
        assertThat(Objects.requireNonNull(forwardedToolContext)
            .getContext()).containsEntry("existing", "kept");
        assertThat(PiiTokenSessionToolContext.from(forwardedToolContext)).isNotNull();
    }

    @Test
    void testAdvisorOrderIsHighestPrecedence() {
        AiGuardrails aiGuardrails = guardrails(false, false, "", false, false, false);
        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);

        assertThat(advisor.getOrder()).isEqualTo(GuardrailAdvisorOrder.WORKSPACE_FLOOR);
    }

    @Test
    void testResponseScanningRedactsCompletionAndRecordsMetric() {
        AiGuardrails aiGuardrails = guardrails(true, false, "", false, true, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Who do I contact?");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(responseChunk("Contact bob@acme.io for details"));

        ChatClientResponse response = advisor.adviseCall(request, chain);

        ChatResponse chatResponse = Objects.requireNonNull(response.chatResponse(), "chatResponse");

        Generation generation = Objects.requireNonNull(chatResponse.getResult(), "generation");

        String responseText = generation.getOutput()
            .getText();

        assertThat(responseText).contains("[REDACTED_EMAIL_ADDRESS]");
        assertThat(responseText).doesNotContain("bob@acme.io");
        assertThat(counter(advisorMeterRegistry, "response_redacted", "copilot")).isEqualTo(1.0);
    }

    @Test
    void testDetectorFailureDuringResponseScanIsRecordedUnderTheCallingSurface() {
        AiGuardrails aiGuardrails = guardrailsWithDetectors(List.of(throwingDetector()), true);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Who do I contact?");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(responseChunk("Contact bob@acme.io for details"));

        advisor.adviseCall(request, chain);

        assertThat(counter(advisorMeterRegistry, "detector_failed", "copilot")).isEqualTo(2.0);
        assertThat(counter(engineMeterRegistry, "detector_failed", "gateway")).isEqualTo(0.0);
    }

    @Test
    void testDetectorFailureDuringRequestOnlyIsRecordedOnceUnderTheCallingSurface() {
        AiGuardrails aiGuardrails = guardrailsWithDetectors(List.of(throwingDetector()), false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Who do I contact?");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(responseChunk("Contact bob@acme.io for details"));

        advisor.adviseCall(request, chain);

        assertThat(counter(advisorMeterRegistry, "detector_failed", "copilot")).isEqualTo(1.0);
        assertThat(counter(engineMeterRegistry, "detector_failed", "gateway")).isEqualTo(0.0);
    }

    @Test
    void testModerationBlockModeThrowsCategoryOnlyException() {
        AiGuardrails aiGuardrails = guardrails(content -> true, false, false, "", false, true, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
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
                AiGuardrailsSettingsScope.WORKSPACE, WORKSPACE_ID, null, null, null, null, null, null,
                BlockingMode.REDACT_AND_CONTINUE, null, null,
                null)));

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
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

        assertThat(forwardedText).isEqualTo("[REDACTED_MODERATED]");
        assertThat(counter(advisorMeterRegistry, "blocking_downgraded", "copilot")).isEqualTo(1.0);
    }

    @Test
    void testModerationSkippedWithoutClassifier() {
        AiGuardrails aiGuardrails = guardrails(null, false, false, "", false, true, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Describe something unsafe");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(emptyResponse());

        advisor.adviseCall(request, chain);

        assertThat(counter(advisorMeterRegistry, "moderation_flagged", "copilot")).isEqualTo(0.0);
    }

    @Test
    void testModerationClassifierFailsOpenAndIsNotFlagged() {
        AiGuardrailModerationClassifier failOpenClassifier = content -> false;

        AiGuardrails aiGuardrails = guardrails(failOpenClassifier, false, false, "", false, true, false, false);

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrailsAdvisor advisor = advisorOver(aiGuardrails, advisorMetrics);
        ChatClientRequest request = requestWithUserMessage("Describe something unsafe");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(emptyResponse());

        advisor.adviseCall(request, chain);

        assertThat(counter(advisorMeterRegistry, "moderation_flagged", "copilot")).isEqualTo(0.0);
    }

    private static SensitiveDataDetector throwingDetector() {
        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return "broken";
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                throw new IllegalStateException("detector is broken");
            }
        };
    }

    private AiGuardrails guardrailsWithDetectors(
        List<SensitiveDataDetector> sensitiveDataDetectors, boolean responseScanEnabled) {

        return new AiGuardrails(
            settingsService, null, null, engineMetrics, sensitiveDataDetectors, true, false, "", false, false,
            responseScanEnabled, false);
    }

    private static double counter(SimpleMeterRegistry meterRegistry, String event, String surface) {
        return meterRegistry.counter(AiGuardrailMetrics.COUNTER_NAME, "event", event, "surface", surface)
            .count();
    }

    private static ChatClientRequest requestWithUserMessage(String text) {
        List<Message> instructions = List.of(new UserMessage(text));

        return new ChatClientRequest(new Prompt(instructions), Map.of());
    }

    private static ChatClientRequest requestWithToolCallingOptions(String text, Map<String, Object> toolContext) {
        List<Message> instructions = List.of(new UserMessage(text));
        ChatOptions chatOptions = ToolCallingChatOptions.builder()
            .toolContext(toolContext)
            .build();

        return new ChatClientRequest(new Prompt(instructions, chatOptions), Map.of());
    }

    private static @Nullable ToolContext capturedToolContext(ArgumentCaptor<ChatClientRequest> forwardedRequestCaptor) {
        ChatOptions chatOptions = forwardedRequestCaptor.getValue()
            .prompt()
            .getOptions();

        if (!(chatOptions instanceof ToolCallingChatOptions toolCallingChatOptions)) {
            return null;
        }

        Map<String, Object> toolContext = toolCallingChatOptions.getToolContext();

        return toolContext == null ? null : new ToolContext(toolContext);
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
        AiGuardrailModerationClassifier moderationClassifier, boolean piiRedactionEnabled,
        boolean secretRedactionEnabled, String blockedTerms, boolean injectionDetectionEnabled,
        boolean moderationEnabled, boolean responseScanEnabled, boolean streamingResponseScanEnabled) {

        return new AiGuardrails(
            settingsService, null, moderationClassifier, engineMetrics, piiRedactionEnabled, secretRedactionEnabled,
            blockedTerms, injectionDetectionEnabled, moderationEnabled, responseScanEnabled,
            streamingResponseScanEnabled);
    }
}
