/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails.GuardrailCheckResult;
import com.bytechef.ee.platform.ai.guardrails.StreamingResponseRedactor;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.exception.AiGuardrailViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Spring AI {@link CallAdvisor}/{@link StreamAdvisor} wiring the standalone {@link AiGuardrails} engine into an agent
 * surface's {@code ChatClient}. Both the AI Hub and Copilot chat surfaces register one instance of this advisor each
 * (see the module javadoc on {@link AiGuardrails} for what the engine itself owns vs. what stays with the caller).
 *
 * <p>
 * <b>Request direction</b> — before the model call, every USER and SYSTEM message's text is run through
 * {@link AiGuardrails#checkInputs}, which always applies PII/secret redaction inline. When a message additionally trips
 * a <em>blocking</em> violation (a blocked-term match, a flagged prompt injection, or a flagged moderation verdict —
 * moderation only checked when a moderation classifier bean is configured), the workspace's {@link BlockingMode}
 * decides what happens next:
 * <ul>
 * <li>{@code BLOCK} (default) — the call is aborted with {@link AiGuardrailViolationException}, whose message carries
 * only the violation category, never the offending content.</li>
 * <li>{@code REDACT_AND_CONTINUE} — the offending content is masked (see {@link AiGuardrails#checkInputs} — a matched
 * blocked term is masked in place, a moderation verdict replaces the whole message since it has no locatable span), a
 * {@code blocking_downgraded} metric is recorded, and the call proceeds with the masked text.</li>
 * </ul>
 * Any redaction (blocking or not) rewrites the affected message in the forwarded request; other message types
 * (assistant, tool) are left untouched.
 * </p>
 *
 * <p>
 * <b>Response direction</b> — {@link CallAdvisor#adviseCall} scans the completion text via
 * {@link AiGuardrails#scanResponseText} once workspace/global policy enables response scanning, recording
 * {@code response_redacted} when anything was masked. {@link StreamAdvisor#adviseStream} instead pipes each chunk
 * through a single {@link StreamingResponseRedactor} obtained from {@link AiGuardrails#newStreamingResponseRedactor} so
 * a value split across a chunk boundary is never emitted in the clear, flushing the redactor's held-back remainder as
 * one trailing chunk once the upstream stream completes and recording {@code response_redacted} at most once per stream
 * (mirroring the AI Gateway's own SSE redaction path in {@code AiGatewayFacadeImpl}).
 * </p>
 *
 * <p>
 * {@code metrics} is a per-advisor-instance {@link AiGuardrailMetrics}, tagged with this surface's own {@code surface}
 * value (e.g. {@code ai_hub} or {@code ai_agent}) — deliberately independent of the engine's own internal metrics bean,
 * whose {@code surface} is a single deployment-wide property ({@code bytechef.ai.guardrails.surface}) and which is a
 * no-op unless {@code bytechef.ai.gateway.enabled=true}. Every event this advisor's request/response path can trigger —
 * the request-direction redaction/blocking events ({@code pii_redacted}, {@code secret_redacted}, {@code blocked_term},
 * {@code injection_flagged}, {@code moderation_flagged}, recorded inside {@link AiGuardrails#checkInputs}) as well as
 * the events decided here ({@code blocking_downgraded}, {@code response_redacted}) — goes through this instance, since
 * {@link AiGuardrails#checkInputs} takes {@code metrics} as a parameter and records through whatever instance the
 * caller passes rather than through the engine's own bean. This is deliberate: the advisor surface must be accurately
 * tagged and must emit regardless of the gateway toggle. Only {@link AiGuardrails#applyToInputs} — the gateway
 * adapter's throwing entry point, not used by this advisor — still records through the engine's own bean.
 * </p>
 *
 * <p>
 * Runs at {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE} — the guardrail floor must see (and, in
 * {@code BLOCK} mode, be able to reject) the final outbound request before any other advisor's rewrite, and must see
 * the model's raw completion before any other advisor post-processes it.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiGuardrailsAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String NAME = "AiGuardrailsAdvisor";

    private final AiGuardrails aiGuardrails;
    private final AiGuardrailMetrics metrics;
    private final @Nullable Long workspaceId;

    public AiGuardrailsAdvisor(AiGuardrails aiGuardrails, @Nullable Long workspaceId, AiGuardrailMetrics metrics) {
        this.aiGuardrails = Objects.requireNonNull(aiGuardrails, "aiGuardrails");
        this.workspaceId = workspaceId;
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientRequest guardedRequest = applyInputGuardrails(chatClientRequest);
        ChatClientResponse response = callAdvisorChain.nextCall(guardedRequest);

        return applyResponseGuardrails(response);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        ChatClientRequest guardedRequest;

        try {
            guardedRequest = applyInputGuardrails(chatClientRequest);
        } catch (AiGuardrailViolationException exception) {
            return Flux.error(exception);
        }

        StreamingResponseRedactor redactor = aiGuardrails.newStreamingResponseRedactor(workspaceId);
        Flux<ChatClientResponse> stream = streamAdvisorChain.nextStream(guardedRequest);

        if (redactor == null) {
            return stream;
        }

        ChatClientRequest finalGuardedRequest = guardedRequest;

        return stream.map(response -> redactStreamChunk(response, redactor))
            .concatWith(Flux.defer(() -> flushStreamTail(redactor, finalGuardedRequest)));
    }

    /**
     * Runs every USER/SYSTEM message's text through {@link AiGuardrails#checkInputs}, throwing
     * {@link AiGuardrailViolationException} for a blocking violation under {@code BLOCK} mode, or masking and recording
     * {@code blocking_downgraded} under {@code REDACT_AND_CONTINUE}. Returns {@code chatClientRequest} unchanged when
     * nothing was guarded or nothing changed.
     */
    private ChatClientRequest applyInputGuardrails(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();
        List<Message> instructions = prompt.getInstructions();

        List<Integer> guardedIndexes = new ArrayList<>();
        List<String> texts = new ArrayList<>();

        for (int index = 0; index < instructions.size(); index++) {
            Message message = instructions.get(index);
            MessageType messageType = message.getMessageType();

            if (messageType != MessageType.USER && messageType != MessageType.SYSTEM) {
                continue;
            }

            String text = message.getText();

            if (text == null || text.isEmpty()) {
                continue;
            }

            guardedIndexes.add(index);
            texts.add(text);
        }

        if (texts.isEmpty()) {
            return chatClientRequest;
        }

        List<GuardrailCheckResult> results = aiGuardrails.checkInputs(texts, workspaceId, metrics);
        boolean anyBlocked = results.stream()
            .anyMatch(GuardrailCheckResult::blocked);

        if (anyBlocked && aiGuardrails.resolveBlockingMode(workspaceId) == BlockingMode.BLOCK) {
            String category = results.stream()
                .filter(GuardrailCheckResult::blocked)
                .findFirst()
                .orElseThrow()
                .category();

            throw new AiGuardrailViolationException(category);
        }

        List<Message> patched = new ArrayList<>(instructions);
        boolean changed = false;

        for (int i = 0; i < guardedIndexes.size(); i++) {
            GuardrailCheckResult result = results.get(i);

            if (result.blocked()) {
                metrics.record("blocking_downgraded");
            }

            int index = guardedIndexes.get(i);
            Message original = instructions.get(index);
            String newText = result.text();

            if (!Objects.equals(newText, original.getText())) {
                patched.set(index, withText(original, newText));

                changed = true;
            }
        }

        if (!changed) {
            return chatClientRequest;
        }

        Prompt patchedPrompt = new Prompt(patched, prompt.getOptions());

        return chatClientRequest.mutate()
            .prompt(patchedPrompt)
            .build();
    }

    /**
     * Scans the completion's assistant text via {@link AiGuardrails#scanResponseText}, rewriting it and recording
     * {@code response_redacted} when response scanning is active and something was masked. Returns {@code response}
     * unchanged otherwise.
     */
    private ChatClientResponse applyResponseGuardrails(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null) {
            return response;
        }

        List<Generation> generations = chatResponse.getResults();
        List<Generation> rewrittenGenerations = new ArrayList<>(generations.size());
        boolean changed = false;

        for (Generation generation : generations) {
            AssistantMessage original = generation.getOutput();
            String text = original == null ? null : original.getText();

            if (text == null) {
                rewrittenGenerations.add(generation);

                continue;
            }

            String scanned = aiGuardrails.scanResponseText(text, workspaceId);

            if (scanned.equals(text)) {
                rewrittenGenerations.add(generation);

                continue;
            }

            changed = true;

            AssistantMessage rewritten = AssistantMessage.builder()
                .content(scanned)
                .properties(original.getMetadata())
                .toolCalls(original.getToolCalls())
                .media(original.getMedia())
                .build();

            rewrittenGenerations.add(new Generation(rewritten, generation.getMetadata()));
        }

        if (!changed) {
            return response;
        }

        metrics.record("response_redacted");

        ChatResponse rewrittenChatResponse = ChatResponse.builder()
            .generations(rewrittenGenerations)
            .metadata(chatResponse.getMetadata())
            .build();

        return response.mutate()
            .chatResponse(rewrittenChatResponse)
            .build();
    }

    /**
     * Pushes one streamed chunk's assistant text through {@code redactor}, replacing the chunk's content with whatever
     * is now safe to emit (which may be empty while a value straddling the chunk boundary is still held back). Chunks
     * that carry no assistant text are passed through unchanged.
     */
    private ChatClientResponse redactStreamChunk(ChatClientResponse response, StreamingResponseRedactor redactor) {
        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null) {
            return response;
        }

        Generation generation = chatResponse.getResult();

        if (generation == null) {
            return response;
        }

        AssistantMessage original = generation.getOutput();
        String chunkText = original == null ? null : original.getText();
        String deltaText = redactor.push(chunkText);

        AssistantMessage redactedMessage = AssistantMessage.builder()
            .content(deltaText)
            .properties(original == null ? Map.of() : original.getMetadata())
            .toolCalls(original == null ? List.of() : original.getToolCalls())
            .media(original == null ? List.of() : original.getMedia())
            .build();

        Generation redactedGeneration = new Generation(redactedMessage, generation.getMetadata());

        ChatResponse redactedChatResponse = ChatResponse.builder()
            .generations(List.of(redactedGeneration))
            .metadata(chatResponse.getMetadata())
            .build();

        return response.mutate()
            .chatResponse(redactedChatResponse)
            .build();
    }

    /**
     * Emitted once the upstream stream completes: flushes {@code redactor}'s held-back remainder as a single trailing
     * chunk (when non-empty) and records {@code response_redacted} exactly once for the whole stream when anything was
     * masked across any {@link StreamingResponseRedactor#push} or this flush.
     */
    private Flux<ChatClientResponse> flushStreamTail(StreamingResponseRedactor redactor, ChatClientRequest request) {
        String tail = redactor.flush();

        if (redactor.isRedacted()) {
            metrics.record("response_redacted");
        }

        if (tail.isEmpty()) {
            return Flux.empty();
        }

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(tail))))
            .build();

        ChatClientResponse tailResponse = ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(request.context())
            .build();

        return Flux.just(tailResponse);
    }

    /**
     * Returns {@code message} with its text replaced by {@code text}, preserving metadata (and media, for a
     * {@link UserMessage}). Message types other than USER/SYSTEM are returned unchanged — callers only ever pass one of
     * those two, since {@link #applyInputGuardrails} only guards USER/SYSTEM messages.
     */
    private static Message withText(Message message, String text) {
        if (message instanceof UserMessage userMessage) {
            return userMessage.mutate()
                .text(text)
                .build();
        }

        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.mutate()
                .text(text)
                .build();
        }

        return message;
    }
}
