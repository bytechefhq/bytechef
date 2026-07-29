/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.guardrail;

import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProjectSettingsService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayWorkspaceSettingsService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProjectSettings;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayWorkspaceSettings;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.exception.AiGatewayGuardrailException;
import com.bytechef.ee.platform.ai.gateway.guardrail.AiGatewayInjectionClassifier;
import com.bytechef.ee.platform.ai.gateway.guardrail.AiGatewayModerationClassifier;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Applies inline content guardrails to AI Gateway traffic before it is routed upstream and, optionally, to the model
 * output before it is returned. All guardrails are off by default; effective policy per request is the union of global
 * {@code bytechef.ai.gateway.guardrails.*} properties and the request workspace's {@link AiGatewayWorkspaceSettings}.
 *
 * <ul>
 * <li><b>PII redaction</b> — {@code pii-redaction-enabled} / workspace {@code redactPii}. Email, US SSN, credit-card,
 * phone, and IPv4 matches are replaced with {@code [REDACTED_*]} placeholders.</li>
 * <li><b>Secret redaction</b> — {@code secret-redaction-enabled} / workspace {@code redactSecrets}. High-signal
 * developer-secret shapes (cloud/provider API keys, tokens, JWTs, PEM private keys) are replaced with
 * {@code [REDACTED_SECRET]}.</li>
 * <li><b>Blocked terms</b> — union of the global {@code blocked-terms} list and the workspace's {@code blockedTerms}
 * (both comma-separated). A request whose content contains any term (case-insensitive) is rejected.</li>
 * <li><b>Model-based moderation</b> — {@code moderation-enabled} / workspace {@code moderationEnabled}, when an
 * {@link AiGatewayModerationClassifier} bean is present. Flagged content is rejected. Fails open.</li>
 * <li><b>Prompt-injection detection</b> — {@code injection-detection-enabled} / workspace
 * {@code injectionDetectionEnabled}, when an {@link AiGatewayInjectionClassifier} bean is present. Content the
 * classifier judges to be a jailbreak / instruction-override / exfiltration attempt is rejected. Fails open.</li>
 * <li><b>Response scanning</b> — {@code response-scan-enabled} / workspace {@code scanResponses}. Model output is
 * redacted for PII and secrets before it is returned (redaction only, never blocking) so internal data does not leak
 * back through completions. Applies to the non-streaming completion path; see {@link #redactResponse}.</li>
 * </ul>
 *
 * <p>
 * Redaction runs before the blocked-term check, moderation, and injection detection, so those checks all evaluate the
 * redacted text. The redactors are deterministic and side-effect-free, so they are safe to run on every message of
 * every request. Regexes deliberately avoid nested optional quantifiers (no catastrophic backtracking / ReDoS).
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

    private static final String SECRET_PLACEHOLDER = "[REDACTED_SECRET]";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b");
    // Linear, backtracking-safe: a 3-3-4 grouping with a single required separator between groups (no nested optional
    // quantifiers, so no catastrophic backtracking / ReDoS).
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.\\s]\\d{3}[-.\\s]\\d{4}\\b");
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "\\b(?:(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\b");

    // Developer-secret shapes. Each is anchored, fixed-length, or bounded by a single quantifier / literal terminator —
    // no nested optional quantifiers, so all are ReDoS-safe. This is the high-signal subset; entropy/random-string
    // detection lives in the workflow-layer SecretKeyDetectorUtils for callers who want it.
    private static final List<Pattern> SECRET_PATTERNS = List.of(
        // PEM private-key block (redact the whole block, not just the marker)
        Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----"),
        // AWS access key id
        Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"),
        // GitHub personal/OAuth/app tokens (classic) and fine-grained PATs
        Pattern.compile("\\bgh[pousr]_[A-Za-z0-9]{36}\\b"),
        Pattern.compile("\\bgithub_pat_[A-Za-z0-9_]{22,}\\b"),
        // OpenAI API keys (incl. project-scoped)
        Pattern.compile("\\bsk-(?:proj-)?[A-Za-z0-9_-]{20,}\\b"),
        // Slack tokens
        Pattern.compile("\\bxox[baprs]-[A-Za-z0-9-]{10,}\\b"),
        // Stripe secret / restricted live keys
        Pattern.compile("\\b[sr]k_live_[0-9a-zA-Z]{24}\\b"),
        // Google API keys
        Pattern.compile("\\bAIza[0-9A-Za-z_-]{35}\\b"),
        // JSON Web Tokens (three base64url segments)
        Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"));

    // Every PII + secret pattern, used by the streaming redactor to locate matched spans so it never emits across the
    // middle of one.
    private static final List<Pattern> ALL_SENSITIVE_PATTERNS = buildAllSensitivePatterns();

    private final AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService;
    private final @Nullable AiGatewayProjectSettingsService aiGatewayProjectSettingsService;
    private final List<String> globalBlockedTerms;
    private final boolean globalInjectionDetectionEnabled;
    private final boolean globalModerationEnabled;
    private final boolean globalPiiRedactionEnabled;
    private final boolean globalResponseScanEnabled;
    private final boolean globalSecretRedactionEnabled;
    private final boolean globalStreamingResponseScanEnabled;
    private final @Nullable AiGatewayInjectionClassifier injectionClassifier;
    private final @Nullable AiGatewayGuardrailMetrics metrics;
    private final @Nullable AiGatewayModerationClassifier moderationClassifier;

    public AiGatewayGuardrails(
        AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService,
        @Nullable AiGatewayProjectSettingsService aiGatewayProjectSettingsService,
        @Nullable AiGatewayModerationClassifier moderationClassifier,
        @Nullable AiGatewayInjectionClassifier injectionClassifier,
        @Nullable AiGatewayGuardrailMetrics metrics,
        @Value("${bytechef.ai.gateway.guardrails.pii-redaction-enabled:false}") boolean piiRedactionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.secret-redaction-enabled:false}") boolean secretRedactionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.blocked-terms:}") String blockedTerms,
        @Value("${bytechef.ai.gateway.guardrails.moderation-enabled:false}") boolean moderationEnabled,
        @Value("${bytechef.ai.gateway.guardrails.injection-detection-enabled:false}") boolean injectionDetectionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-enabled:false}") boolean responseScanEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-streaming-enabled:false}") boolean streamingResponseScanEnabled) {

        this.aiGatewayWorkspaceSettingsService = aiGatewayWorkspaceSettingsService;
        this.aiGatewayProjectSettingsService = aiGatewayProjectSettingsService;
        this.globalBlockedTerms = parseBlockedTerms(blockedTerms);
        this.globalInjectionDetectionEnabled = injectionDetectionEnabled;
        this.globalModerationEnabled = moderationEnabled;
        this.globalPiiRedactionEnabled = piiRedactionEnabled;
        this.globalResponseScanEnabled = responseScanEnabled;
        this.globalSecretRedactionEnabled = secretRedactionEnabled;
        this.globalStreamingResponseScanEnabled = streamingResponseScanEnabled;
        this.injectionClassifier = injectionClassifier;
        this.metrics = metrics;
        this.moderationClassifier = moderationClassifier;
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

        EffectivePolicy policy = resolvePolicy(workspaceId, projectId);

        if (!policy.anyChatGuardrailActive()) {
            return request;
        }

        List<AiGatewayChatMessage> guardrailedMessages = new ArrayList<>();

        for (AiGatewayChatMessage message : request.messages()) {
            String content = checkAndRedact(message.content(), policy);

            guardrailedMessages.add(
                new AiGatewayChatMessage(
                    message.role(), content, message.contentBlocks(), message.toolCalls(), message.toolCallId()));
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

        EffectivePolicy policy = resolvePolicy(workspaceId, projectId);

        if (!policy.anyInputGuardrailActive()) {
            return inputs;
        }

        List<String> guardrailedInputs = new ArrayList<>(inputs.size());

        for (String input : inputs) {
            guardrailedInputs.add(checkAndRedact(input, policy, false));
        }

        return guardrailedInputs;
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

        EffectivePolicy policy = resolvePolicy(workspaceId, projectId);

        if (!policy.scanResponses()) {
            return response;
        }

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

            String redacted = redactAll(content);

            if (redacted.equals(content)) {
                redactedChoices.add(choice);

                continue;
            }

            changed = true;

            AiGatewayChatMessage redactedMessage = new AiGatewayChatMessage(
                message.role(), redacted, message.contentBlocks(), message.toolCalls(), message.toolCallId());

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

        if (!globalStreamingResponseScanEnabled) {
            return null;
        }

        EffectivePolicy policy = resolvePolicy(workspaceId, projectId);

        if (!policy.scanResponses()) {
            return null;
        }

        return new StreamingResponseRedactor();
    }

    /**
     * Replaces common PII patterns in {@code content} with {@code [REDACTED_*]} placeholders.
     */
    public static String redactPii(@Nullable String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String redacted = EMAIL_PATTERN.matcher(content)
            .replaceAll("[REDACTED_EMAIL]");

        redacted = SSN_PATTERN.matcher(redacted)
            .replaceAll("[REDACTED_SSN]");
        redacted = CREDIT_CARD_PATTERN.matcher(redacted)
            .replaceAll("[REDACTED_CC]");
        redacted = PHONE_PATTERN.matcher(redacted)
            .replaceAll("[REDACTED_PHONE]");
        redacted = IPV4_PATTERN.matcher(redacted)
            .replaceAll("[REDACTED_IP]");

        return redacted;
    }

    /**
     * Replaces recognised developer-secret shapes (cloud/provider API keys, tokens, JWTs, PEM private keys) in
     * {@code content} with a {@code [REDACTED_SECRET]} placeholder.
     */
    public static String redactSecrets(@Nullable String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String redacted = content;

        for (Pattern secretPattern : SECRET_PATTERNS) {
            redacted = secretPattern.matcher(redacted)
                .replaceAll(SECRET_PLACEHOLDER);
        }

        return redacted;
    }

    /**
     * Applies both the PII and secret redactors to {@code content}. Used for response-direction scanning where both
     * categories are masked regardless of the request-direction toggles.
     */
    public static String redactAll(String content) {
        return redactSecrets(redactPii(content));
    }

    /**
     * Returns the {@code [start, end)} character spans of every PII/secret match in {@code content}, so the streaming
     * redactor can avoid emitting across the middle of a match. Order is unspecified and spans may overlap.
     */
    static List<int[]> sensitiveMatchRanges(String content) {
        List<int[]> ranges = new ArrayList<>();

        for (Pattern sensitivePattern : ALL_SENSITIVE_PATTERNS) {
            Matcher matcher = sensitivePattern.matcher(content);

            while (matcher.find()) {
                ranges.add(new int[] {
                    matcher.start(), matcher.end()
                });
            }
        }

        return ranges;
    }

    private static List<Pattern> buildAllSensitivePatterns() {
        List<Pattern> patterns = new ArrayList<>();

        patterns.add(EMAIL_PATTERN);
        patterns.add(SSN_PATTERN);
        patterns.add(CREDIT_CARD_PATTERN);
        patterns.add(PHONE_PATTERN);
        patterns.add(IPV4_PATTERN);
        patterns.addAll(SECRET_PATTERNS);

        return List.copyOf(patterns);
    }

    private String checkAndRedact(@Nullable String content, EffectivePolicy policy) {
        return checkAndRedact(content, policy, policy.moderate());
    }

    private String checkAndRedact(@Nullable String content, EffectivePolicy policy, boolean moderate) {
        if (content == null) {
            return null;
        }

        String redacted = content;

        if (policy.redactPii()) {
            String piiRedacted = redactPii(redacted);

            if (!piiRedacted.equals(redacted)) {
                record("pii_redacted");
            }

            redacted = piiRedacted;
        }

        if (policy.redactSecrets()) {
            String secretRedacted = redactSecrets(redacted);

            if (!secretRedacted.equals(redacted)) {
                record("secret_redacted");
            }

            redacted = secretRedacted;
        }

        try {
            checkBlockedTerms(redacted, policy.blockedTerms());
        } catch (AiGatewayGuardrailException exception) {
            record("blocked_term");

            throw exception;
        }

        if (moderate && moderationClassifier != null && moderationClassifier.isFlagged(redacted)) {
            record("moderation_flagged");

            log.warn("AI Gateway request rejected by moderation classifier");

            throw new AiGatewayGuardrailException("Request rejected by content moderation");
        }

        if (policy.detectInjection() && injectionClassifier != null && injectionClassifier.isInjection(redacted)) {
            record("injection_flagged");

            log.warn("AI Gateway request rejected by injection detection");

            throw new AiGatewayGuardrailException("Request rejected by prompt-injection detection");
        }

        return redacted;
    }

    private EffectivePolicy resolvePolicy(@Nullable Long workspaceId, @Nullable Long projectId) {
        AiGatewayWorkspaceSettings settings = findSettings(workspaceId);
        AiGatewayProjectSettings projectSettings = findProjectSettings(projectId);

        // Union semantics across global -> workspace -> project: a level can enable a guardrail (or add blocked terms)
        // but never turn one off. null = inherit.
        boolean redactPii = globalPiiRedactionEnabled ||
            (settings != null && Boolean.TRUE.equals(settings.redactPii())) ||
            (projectSettings != null && Boolean.TRUE.equals(projectSettings.redactPii()));
        boolean redactSecrets = globalSecretRedactionEnabled ||
            (settings != null && Boolean.TRUE.equals(settings.redactSecrets())) ||
            (projectSettings != null && Boolean.TRUE.equals(projectSettings.redactSecrets()));

        Set<String> blockedTerms = new LinkedHashSet<>(globalBlockedTerms);

        if (settings != null && settings.blockedTerms() != null) {
            blockedTerms.addAll(parseBlockedTerms(settings.blockedTerms()));
        }

        if (projectSettings != null && projectSettings.blockedTerms() != null) {
            blockedTerms.addAll(parseBlockedTerms(projectSettings.blockedTerms()));
        }

        boolean moderate = moderationClassifier != null &&
            (globalModerationEnabled || (settings != null && Boolean.TRUE.equals(settings.moderationEnabled())) ||
                (projectSettings != null && Boolean.TRUE.equals(projectSettings.moderationEnabled())));
        boolean detectInjection = injectionClassifier != null &&
            (globalInjectionDetectionEnabled ||
                (settings != null && Boolean.TRUE.equals(settings.injectionDetectionEnabled())) ||
                (projectSettings != null && Boolean.TRUE.equals(projectSettings.injectionDetectionEnabled())));
        boolean scanResponses = globalResponseScanEnabled ||
            (settings != null && Boolean.TRUE.equals(settings.scanResponses())) ||
            (projectSettings != null && Boolean.TRUE.equals(projectSettings.scanResponses()));

        return new EffectivePolicy(
            redactPii, redactSecrets, blockedTerms, moderate, detectInjection, scanResponses);
    }

    private @Nullable AiGatewayWorkspaceSettings findSettings(@Nullable Long workspaceId) {
        if (workspaceId == null) {
            return null;
        }

        try {
            Optional<AiGatewayWorkspaceSettings> settingsOptional =
                aiGatewayWorkspaceSettingsService.findByWorkspaceId(workspaceId);

            return settingsOptional.orElse(null);
        } catch (Exception exception) {
            // A settings lookup failure must not take the request path down; global guardrails still apply.
            log.warn(
                "Failed to load AI Gateway workspace settings for workspace {}: {}", workspaceId,
                exception.getMessage());

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

    /**
     * Records a single {@code response_redacted} guardrail metric. Used by the streaming path to emit one metric per
     * stream (via {@link StreamingResponseRedactor#isRedacted()}) rather than one per chunk; the non-streaming path
     * records inline in {@link #redactResponse}.
     */
    public void recordResponseRedacted() {
        record("response_redacted");
    }

    private void record(String event) {
        if (metrics != null) {
            metrics.record(event);
        }
    }

    private static void checkBlockedTerms(String content, Set<String> blockedTerms) {
        if (blockedTerms.isEmpty()) {
            return;
        }

        String lowerContent = content.toLowerCase(Locale.ROOT);

        for (String blockedTerm : blockedTerms) {
            if (lowerContent.contains(blockedTerm)) {
                log.warn("AI Gateway request rejected by content guardrail (blocked term matched)");

                throw new AiGatewayGuardrailException(
                    "Request rejected by content guardrail: matched a blocked term");
            }
        }
    }

    private static List<String> parseBlockedTerms(String blockedTerms) {
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

    /**
     * The guardrail policy resolved for one request from the union of global properties and workspace settings.
     */
    private record EffectivePolicy(
        boolean redactPii, boolean redactSecrets, Set<String> blockedTerms, boolean moderate, boolean detectInjection,
        boolean scanResponses) {

        boolean anyChatGuardrailActive() {
            return redactPii || redactSecrets || !blockedTerms.isEmpty() || moderate || detectInjection;
        }

        boolean anyInputGuardrailActive() {
            return redactPii || redactSecrets || !blockedTerms.isEmpty() || detectInjection;
        }
    }
}
