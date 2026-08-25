/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.guardrail;

import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProjectSettingsService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProjectSettings;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.exception.AiGatewayGuardrailException;
import com.bytechef.ee.platform.ai.gateway.guardrail.AiGatewayInjectionClassifier;
import com.bytechef.ee.platform.ai.gateway.guardrail.AiGatewayModerationClassifier;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.StreamingResponseRedactor;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AI Gateway adapter over the standalone {@link AiGuardrails} engine (extracted to {@code platform-ai-guardrails}).
 * Preserves the exact pre-extraction public surface — chat/embedding DTO methods and {@code projectId} overloads — so
 * every caller of this class (the gateway facade, and this module's own tests) keeps working unchanged. The
 * {@code redactPii}/{@code redactSecrets}/{@code redactAll} static delegates this class used to expose were removed
 * once the detector SPI turned the engine's own static redactors into instance methods: this adapter has always had an
 * {@link AiGuardrails} instance to call them on directly, and nothing outside this module's own tests ever called the
 * delegates, so keeping them would only have been dead public API.
 *
 * <p>
 * The engine resolves the global-property + workspace-settings union for plain text; this adapter owns everything the
 * engine deliberately does not: gateway DTO shapes (chat messages, completion responses), model-based moderation (never
 * moved — it is a gateway/chat-only concept), and the project-level guardrail overlay. The overlay is additive (a
 * project can only enable a guardrail or add blocked terms, never turn one off) — for each call, this adapter first
 * delegates the text to the engine (global + workspace policy), then layers any project-only additions on top using the
 * engine's own {@code redactPii}/{@code redactSecrets} instance methods and its own moderation/injection classifiers.
 * </p>
 *
 * <p>
 * The gateway does not consult {@link AiGuardrailsWorkspaceSettings#blockingMode()} — a blocked request always throws
 * {@link AiGatewayGuardrailException}, which the gateway's exception handling turns into an HTTP 422.
 * Redact-and-continue blocking mode is a concept for other (non-gateway) surfaces that consume the standalone engine
 * directly.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayGuardrails {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayGuardrails.class);

    private final AiGuardrails aiGuardrails;
    private final @Nullable AiGatewayProjectSettingsService aiGatewayProjectSettingsService;
    private final AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService;
    private final @Nullable AiGatewayModerationClassifier moderationClassifier;
    private final @Nullable AiGatewayInjectionClassifier injectionClassifier;
    private final @Nullable AiGuardrailMetrics metrics;
    private final boolean globalModerationEnabled;
    private final boolean globalStreamingResponseScanEnabled;

    public AiGatewayGuardrails(
        AiGuardrails aiGuardrails,
        @Nullable AiGatewayProjectSettingsService aiGatewayProjectSettingsService,
        AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService,
        @Nullable AiGatewayModerationClassifier moderationClassifier,
        @Nullable AiGatewayInjectionClassifier injectionClassifier,
        @Nullable AiGuardrailMetrics metrics,
        // Property names kept for compatibility with the pre-extraction AiGatewayGuardrails — moderation and the
        // streaming operator gate are gateway-only concerns that never moved into the engine, so this adapter reads
        // them directly rather than asking the engine for them.
        @Value("${bytechef.ai.gateway.guardrails.moderation-enabled:false}") boolean moderationEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-streaming-enabled:false}") boolean streamingResponseScanEnabled) {

        this.aiGuardrails = aiGuardrails;
        this.aiGatewayProjectSettingsService = aiGatewayProjectSettingsService;
        this.aiGuardrailsWorkspaceSettingsService = aiGuardrailsWorkspaceSettingsService;
        this.moderationClassifier = moderationClassifier;
        this.injectionClassifier = injectionClassifier;
        this.metrics = metrics;
        this.globalModerationEnabled = moderationEnabled;
        this.globalStreamingResponseScanEnabled = streamingResponseScanEnabled;
    }

    /**
     * Returns the chat-completion request with request-direction guardrails applied for the given workspace: PII and
     * secrets redacted, blocked terms rejected, and — when enabled and a classifier is available — content flagged by
     * moderation or injection detection rejected. Returns the request unchanged when no guardrail is active.
     *
     * @param request     the inbound chat-completion request
     * @param workspaceId the workspace the request is attributed to, or {@code null} when unattributed (global
     *                    guardrails still apply)
     * @return the guardrailed request
     * @throws AiGatewayGuardrailException if a message contains a blocked term or is flagged by moderation or injection
     *                                     detection
     */
    public AiGatewayChatCompletionRequest apply(
        AiGatewayChatCompletionRequest request, @Nullable Long workspaceId) {

        return apply(request, workspaceId, null);
    }

    /**
     * As {@link #apply(AiGatewayChatCompletionRequest, Long)}, additionally layering the project's guardrail overrides
     * (union semantics) on top of the workspace/global policy.
     *
     * @param request     the inbound chat-completion request
     * @param workspaceId the workspace the request is attributed to, or {@code null} when unattributed
     * @param projectId   the project the request is attributed to, or {@code null} when none
     * @return the guardrailed request
     * @throws AiGatewayGuardrailException if a message contains a blocked term or is flagged by moderation or injection
     *                                     detection
     */
    public AiGatewayChatCompletionRequest apply(
        AiGatewayChatCompletionRequest request, @Nullable Long workspaceId, @Nullable Long projectId) {

        AiGatewayProjectSettings projectSettings = findProjectSettings(projectId);
        boolean moderate = moderationClassifier != null && resolveModerationEnabled(workspaceId, projectSettings);

        List<AiGatewayChatMessage> guardrailedMessages = new ArrayList<>();
        boolean changed = false;

        for (AiGatewayChatMessage message : request.messages()) {
            String content = message.content();
            String processed = content;

            if (content != null) {
                processed = aiGuardrails.applyToInputs(List.of(content), workspaceId)
                    .getFirst();
                processed = applyProjectOverlay(processed, projectSettings);
            }

            if (moderate && processed != null && moderationClassifier.isFlagged(processed)) {
                record("moderation_flagged");

                log.warn("AI Gateway request rejected by moderation classifier");

                throw new AiGatewayGuardrailException("Request rejected by content moderation");
            }

            if (!Objects.equals(processed, content)) {
                changed = true;
            }

            guardrailedMessages.add(
                new AiGatewayChatMessage(
                    message.role(), processed, message.contentBlocks(), message.toolCalls(), message.toolCallId()));
        }

        if (!changed) {
            return request;
        }

        return new AiGatewayChatCompletionRequest(
            request.model(), guardrailedMessages, request.temperature(), request.maxTokens(), request.topP(),
            request.stream(), request.routingPolicy(), request.cache(), request.toolChoice(), request.tools(),
            request.tags());
    }

    /**
     * Returns the embedding inputs with request-direction guardrails applied: PII and secrets redacted, blocked terms
     * and injection attempts rejected. Moderation is intentionally not run on embedding inputs (they are documents /
     * records, not conversational prompts). Returns the inputs unchanged when no relevant guardrail is active.
     *
     * @param inputs      the embedding input strings
     * @param workspaceId the workspace the request is attributed to, or {@code null} when unattributed
     * @return the guardrailed inputs
     * @throws AiGatewayGuardrailException if an input contains a blocked term or is flagged by injection detection
     */
    public List<String> applyToInputs(List<String> inputs, @Nullable Long workspaceId) {
        return applyToInputs(inputs, workspaceId, null);
    }

    /**
     * As {@link #applyToInputs(List, Long)}, additionally layering the project's guardrail overrides on top of the
     * workspace/global policy.
     *
     * @param inputs      the embedding input strings
     * @param workspaceId the workspace the request is attributed to, or {@code null} when unattributed
     * @param projectId   the project the request is attributed to, or {@code null} when none
     * @return the guardrailed inputs
     * @throws AiGatewayGuardrailException if an input contains a blocked term or is flagged by injection detection
     */
    public List<String> applyToInputs(List<String> inputs, @Nullable Long workspaceId, @Nullable Long projectId) {
        if (inputs == null || inputs.isEmpty()) {
            return inputs;
        }

        List<String> engineProcessed = aiGuardrails.applyToInputs(inputs, workspaceId);
        AiGatewayProjectSettings projectSettings = findProjectSettings(projectId);

        if (projectSettings == null) {
            return engineProcessed;
        }

        List<String> guardrailedInputs = new ArrayList<>(engineProcessed.size());
        boolean changed = false;

        for (String input : engineProcessed) {
            String processed = applyProjectOverlay(input, projectSettings);

            if (!processed.equals(input)) {
                changed = true;
            }

            guardrailedInputs.add(processed);
        }

        return changed ? guardrailedInputs : engineProcessed;
    }

    /**
     * Returns the completion response with each choice's message content redacted for PII and secrets when response
     * scanning is enabled for the workspace (or globally), otherwise the response unchanged. Response scanning is
     * redaction only — it never blocks — so a caller always receives an answer, just with internal data masked. Applies
     * to the non-streaming completion path; the SSE streaming path emits tokens incrementally where a value can
     * straddle chunk boundaries, so it is not scanned. Tool-call arguments are left untouched so function calling is
     * not corrupted.
     *
     * @param response    the completion response
     * @param workspaceId the workspace the request is attributed to, or {@code null} when unattributed
     * @return the response with redacted content, or the original when response scanning is inactive or nothing matched
     */
    public AiGatewayChatCompletionResponse redactResponse(
        AiGatewayChatCompletionResponse response, @Nullable Long workspaceId) {

        return redactResponse(response, workspaceId, null);
    }

    /**
     * As {@link #redactResponse(AiGatewayChatCompletionResponse, Long)}, additionally honoring the project's response
     * scanning override.
     *
     * @param response    the completion response
     * @param workspaceId the workspace the request is attributed to, or {@code null} when unattributed
     * @param projectId   the project the request is attributed to, or {@code null} when none
     * @return the response with redacted content, or the original when response scanning is inactive or nothing matched
     */
    public AiGatewayChatCompletionResponse redactResponse(
        AiGatewayChatCompletionResponse response, @Nullable Long workspaceId, @Nullable Long projectId) {

        if (response == null || response.choices() == null || response.choices()
            .isEmpty()) {

            return response;
        }

        AiGatewayProjectSettings projectSettings = findProjectSettings(projectId);
        boolean projectScanResponses = projectSettings != null && Boolean.TRUE.equals(projectSettings.scanResponses());

        List<AiGatewayChatCompletionResponse.Choice> choices = response.choices();
        List<AiGatewayChatCompletionResponse.Choice> redactedChoices = new ArrayList<>(choices.size());
        boolean changed = false;

        for (AiGatewayChatCompletionResponse.Choice choice : choices) {
            AiGatewayChatMessage message = choice.message();
            String content = message == null ? null : message.content();

            if (content == null) {
                redactedChoices.add(choice);

                continue;
            }

            String scanned = aiGuardrails.scanResponseText(content, workspaceId);

            if (projectScanResponses) {
                scanned = aiGuardrails.redactAll(scanned);
            }

            // scanResponseText/redactAll are declared @Nullable, so scanned is compared null-safely rather than
            // dereferenced -- the compiler cannot see that content != null (checked above) makes both calls return
            // non-null here. That is also why the assignment below hands scanned to the constructor unchecked: it is
            // provably non-null at this point, just not statically so.
            if (Objects.equals(scanned, content)) {
                redactedChoices.add(choice);

                continue;
            }

            changed = true;

            AiGatewayChatMessage redactedMessage = new AiGatewayChatMessage(
                message.role(), scanned, message.contentBlocks(), message.toolCalls(), message.toolCallId());

            redactedChoices.add(
                new AiGatewayChatCompletionResponse.Choice(choice.index(), redactedMessage, choice.finishReason()));
        }

        if (!changed) {
            return response;
        }

        record("response_redacted");

        return new AiGatewayChatCompletionResponse(
            response.id(), response.object(), response.created(), response.model(), redactedChoices, response.usage(),
            response.gatewayMetadata());
    }

    /**
     * Returns a stateful redactor for masking PII/secrets in a streamed completion when streaming response scanning is
     * active for the workspace, otherwise {@code null}. Streaming scanning requires BOTH response scanning to be
     * effective for the workspace (global {@code response-scan-enabled} or workspace {@code scanResponses}) AND the
     * global {@code response-scan-streaming-enabled} operator flag — because holding back a lookahead window to catch
     * values that straddle SSE chunk boundaries trades away some of streaming's incremental latency, which is an
     * operator-level decision. Caller uses the returned redactor across the token stream and flushes it at completion.
     *
     * @param workspaceId the workspace the request is attributed to, or {@code null} when unattributed
     * @return a fresh {@link StreamingResponseRedactor}, or {@code null} when streaming scanning is inactive
     */
    public @Nullable StreamingResponseRedactor newStreamingResponseRedactor(@Nullable Long workspaceId) {
        return newStreamingResponseRedactor(workspaceId, null);
    }

    /**
     * As {@link #newStreamingResponseRedactor(Long)}, additionally honoring the project's response scanning override.
     *
     * @param workspaceId the workspace the request is attributed to, or {@code null} when unattributed
     * @param projectId   the project the request is attributed to, or {@code null} when none
     * @return a fresh {@link StreamingResponseRedactor}, or {@code null} when streaming scanning is inactive
     */
    public @Nullable StreamingResponseRedactor newStreamingResponseRedactor(
        @Nullable Long workspaceId, @Nullable Long projectId) {

        StreamingResponseRedactor redactor = aiGuardrails.newStreamingResponseRedactor(workspaceId);

        if (redactor != null) {
            return redactor;
        }

        if (!globalStreamingResponseScanEnabled) {
            return null;
        }

        AiGatewayProjectSettings projectSettings = findProjectSettings(projectId);

        if (projectSettings != null && Boolean.TRUE.equals(projectSettings.scanResponses())) {
            return aiGuardrails.newStreamingResponseRedactor();
        }

        return null;
    }

    /**
     * Records a single {@code response_redacted} guardrail metric. Used by the streaming path to emit one metric per
     * stream (via {@link StreamingResponseRedactor#isRedacted()}) rather than one per chunk; the non-streaming path
     * records inline in {@link #redactResponse}.
     */
    public void recordResponseRedacted() {
        record("response_redacted");
    }

    /**
     * Applies the project's ADDITIVE guardrail overrides to {@code text} on top of whatever the engine already did for
     * the workspace/global policy: extra PII/secret redaction, extra blocked terms, and injection detection if the
     * project enables it and the workspace/global policy did not. A project can only enable a guardrail or add blocked
     * terms — it never turns one off — so it is safe to always layer this on top, regardless of what the engine already
     * applied.
     */
    private String applyProjectOverlay(String text, @Nullable AiGatewayProjectSettings projectSettings) {
        if (projectSettings == null) {
            return text;
        }

        String result = text;

        if (Boolean.TRUE.equals(projectSettings.redactPii())) {
            String redacted = aiGuardrails.redactPii(result);

            // aiGuardrails.redactPii is declared @Nullable, so redacted is compared null-safely rather than
            // dereferenced -- result is never actually null here (callers only ever pass non-null, non-empty text
            // into applyProjectOverlay), but the compiler has no way to know that from the declared signature.
            if (!Objects.equals(redacted, result)) {
                record("pii_redacted");
            }

            result = redacted;
        }

        if (Boolean.TRUE.equals(projectSettings.redactSecrets())) {
            String redacted = aiGuardrails.redactSecrets(result);

            // Same null-safety reasoning as the redactPii branch above.
            if (!Objects.equals(redacted, result)) {
                record("secret_redacted");
            }

            result = redacted;
        }

        checkAdditionalBlockedTerms(result, projectSettings.blockedTerms());

        if (Boolean.TRUE.equals(projectSettings.injectionDetectionEnabled()) && injectionClassifier != null
            && injectionClassifier.isInjection(result)) {

            record("injection_flagged");

            log.warn("AI Gateway request rejected by injection detection");

            throw new AiGatewayGuardrailException("Request rejected by prompt-injection detection");
        }

        return result;
    }

    private void checkAdditionalBlockedTerms(String content, @Nullable String blockedTermsCsv) {
        List<String> terms = parseBlockedTerms(blockedTermsCsv);

        if (terms.isEmpty()) {
            return;
        }

        String lowerContent = content.toLowerCase(Locale.ROOT);

        for (String term : terms) {
            if (lowerContent.contains(term)) {
                record("blocked_term");

                log.warn("AI Gateway request rejected by content guardrail (blocked term matched)");

                throw new AiGatewayGuardrailException(
                    "Request rejected by content guardrail: matched a blocked term");
            }
        }
    }

    private boolean resolveModerationEnabled(
        @Nullable Long workspaceId, @Nullable AiGatewayProjectSettings projectSettings) {

        if (globalModerationEnabled) {
            return true;
        }

        AiGuardrailsWorkspaceSettings settings = fetchWorkspaceSettings(workspaceId);

        if (settings != null && Boolean.TRUE.equals(settings.moderationEnabled())) {
            return true;
        }

        return projectSettings != null && Boolean.TRUE.equals(projectSettings.moderationEnabled());
    }

    private @Nullable AiGuardrailsWorkspaceSettings fetchWorkspaceSettings(@Nullable Long workspaceId) {
        try {
            Optional<AiGuardrailsWorkspaceSettings> settingsOptional =
                aiGuardrailsWorkspaceSettingsService.fetchSettings(workspaceId);

            return settingsOptional.orElse(null);
        } catch (Exception exception) {
            // A settings lookup failure must not take the request path down; global/project moderation policy still
            // applies.
            log.warn(
                "Failed to load AI guardrails workspace settings for workspace {} while resolving moderation: {}",
                workspaceId, exception.getMessage());

            return null;
        }
    }

    private @Nullable AiGatewayProjectSettings findProjectSettings(@Nullable Long projectId) {
        if (projectId == null || aiGatewayProjectSettingsService == null) {
            return null;
        }

        try {
            Optional<AiGatewayProjectSettings> settingsOptional =
                aiGatewayProjectSettingsService.findByProjectId(projectId);

            return settingsOptional.orElse(null);
        } catch (Exception exception) {
            // A settings lookup failure must not take the request path down; workspace/global guardrails still apply.
            log.warn(
                "Failed to load AI Gateway project settings for project {}: {}", projectId, exception.getMessage());

            return null;
        }
    }

    private void record(String event) {
        if (metrics != null) {
            metrics.record(event);
        }
    }

    private static List<String> parseBlockedTerms(@Nullable String blockedTerms) {
        if (StringUtils.isBlank(blockedTerms)) {
            return List.of();
        }

        List<String> terms = new ArrayList<>();

        for (String term : blockedTerms.split(",")) {
            String trimmed = term.strip();

            if (!trimmed.isEmpty()) {
                terms.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }

        return terms;
    }
}
