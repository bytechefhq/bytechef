/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import com.bytechef.ee.platform.ai.gateway.exception.AiGatewayGuardrailException;
import com.bytechef.ee.platform.ai.gateway.guardrail.AiGatewayInjectionClassifier;
import com.bytechef.ee.platform.ai.gateway.guardrail.AiGatewayModerationClassifier;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
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
import org.springframework.stereotype.Component;

/**
 * Applies inline content guardrails to text before it leaves ByteChef and, optionally, to model output before it is
 * returned to a caller. This is the standalone, DTO-free guardrail engine — it operates on plain strings and lists of
 * strings, not on any particular caller's request/response shapes. All guardrails are off by default; effective policy
 * per call is the union of global {@code bytechef.ai.gateway.guardrails.*} properties (property names kept for
 * compatibility — this engine was extracted from the AI Gateway, which is still the sole owner/reader of these keys
 * today) and the request workspace's {@link AiGuardrailsWorkspaceSettings}.
 *
 * <ul>
 * <li><b>PII redaction</b> — {@code pii-redaction-enabled} / workspace {@code redactPii}. Email, US SSN, credit-card,
 * phone, and IPv4 matches are replaced with {@code [REDACTED_*]} placeholders.</li>
 * <li><b>Secret redaction</b> — {@code secret-redaction-enabled} / workspace {@code redactSecrets}. High-signal
 * developer-secret shapes (cloud/provider API keys, tokens, JWTs, PEM private keys) are replaced with
 * {@code [REDACTED_SECRET]}.</li>
 * <li><b>Blocked terms</b> — union of the global {@code blocked-terms} list and the workspace's {@code blockedTerms}
 * (both comma-separated). Content containing any term (case-insensitive) is rejected.</li>
 * <li><b>Prompt-injection detection</b> — {@code injection-detection-enabled} / workspace
 * {@code injectionDetectionEnabled}, when an {@link AiGatewayInjectionClassifier} bean is present. Content the
 * classifier judges to be a jailbreak / instruction-override / exfiltration attempt is rejected. Fails open.</li>
 * <li><b>Model-based moderation</b> — {@code moderation-enabled} / workspace {@code moderationEnabled}, when an
 * {@link AiGatewayModerationClassifier} bean is present. Content the classifier judges unsafe is a blocking violation,
 * reported ONLY through the non-throwing {@link #checkInputs}/{@link #checkInput} path (see below) — never through
 * {@link #applyToInputs}. Fails open.</li>
 * <li><b>Response scanning</b> — {@code response-scan-enabled} / workspace {@code scanResponses}. Text is redacted for
 * PII and secrets before it is returned (redaction only, never blocking) so internal data does not leak back through
 * completions. See {@link #scanResponseText}.</li>
 * </ul>
 *
 * <p>
 * The gateway-DTO/project-overlay layer (chat-completion requests/responses, project-level guardrail overrides) does
 * not live here — it stays with the AI Gateway adapter that wraps this engine, since it is a caller-specific concern
 * rather than shared text-level guardrail logic. Model-based moderation is different: as of the standalone-advisor
 * follow-up, {@link #checkInputs} runs the optional {@link AiGatewayModerationClassifier} bean so every advisor-fronted
 * agent surface (canvas AI Agent, AI Hub) gets moderation coverage, not just gateway-routed traffic. The throwing
 * {@link #applyToInputs} entry point — the AI Gateway adapter's own request path — deliberately does NOT run moderation
 * here: the gateway adapter ({@code AiGatewayGuardrails}) already moderates at its own DTO level with its own
 * classifier wiring (plus its project overlay), and running it a second time inside this shared engine would
 * double-moderate every gateway call.
 * </p>
 *
 * <p>
 * <b>Moderation and {@code BlockingMode}</b> — {@code BlockingMode} is documented (see
 * {@link AiGuardrailsWorkspaceSettings#blockingMode()}) to govern all three blocking guardrails: blocked terms,
 * moderation, and injection. Blocked-term violations downgrade by masking only the matched term; a moderation verdict,
 * like an injection verdict, has no locatable span — the classifier judges the whole message. Unlike injection (which
 * today forwards the pii/secret-redacted original text unchanged on downgrade — a pre-existing behavior this change
 * does not touch), a downgraded moderation violation replaces the ENTIRE message with a fixed placeholder,
 * {@code [REDACTED_MODERATED]}, rather than letting any of the flagged content continue on to the model: "masks the
 * offending content" for a whole-message judgment means masking the whole message.
 * </p>
 *
 * <p>
 * Redaction runs before the blocked-term check and injection detection, so those checks evaluate the redacted text. The
 * redactors are deterministic and side-effect-free, so they are safe to run on every piece of content. Regexes
 * deliberately avoid nested optional quantifiers (no catastrophic backtracking / ReDoS).
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AiGuardrails {

    private static final Logger log = LoggerFactory.getLogger(AiGuardrails.class);

    private static final String SECRET_PLACEHOLDER = "[REDACTED_SECRET]";
    private static final String BLOCKED_TERM_PLACEHOLDER = "[REDACTED_BLOCKED_TERM]";
    // A moderation verdict has no locatable span (the classifier judges the whole message), so a REDACT_AND_CONTINUE
    // downgrade replaces the entire message rather than masking a substring -- see the class javadoc's "Moderation and
    // BlockingMode" section for why this differs from how injection currently downgrades.
    private static final String MODERATION_PLACEHOLDER = "[REDACTED_MODERATED]";

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

    private final AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService;
    private final List<String> globalBlockedTerms;
    private final boolean globalInjectionDetectionEnabled;
    private final boolean globalModerationEnabled;
    private final boolean globalPiiRedactionEnabled;
    private final boolean globalResponseScanEnabled;
    private final boolean globalSecretRedactionEnabled;
    private final boolean globalStreamingResponseScanEnabled;
    private final @Nullable AiGatewayInjectionClassifier injectionClassifier;
    private final @Nullable AiGatewayModerationClassifier moderationClassifier;
    private final @Nullable AiGuardrailMetrics metrics;

    public AiGuardrails(
        AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService,
        @Nullable AiGatewayInjectionClassifier injectionClassifier,
        @Nullable AiGatewayModerationClassifier moderationClassifier,
        @Nullable AiGuardrailMetrics metrics,
        // Property names kept for compatibility: this engine was extracted from the AI Gateway, which is still the
        // sole owner/reader of these keys today.
        @Value("${bytechef.ai.gateway.guardrails.pii-redaction-enabled:false}") boolean piiRedactionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.secret-redaction-enabled:false}") boolean secretRedactionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.blocked-terms:}") String blockedTerms,
        @Value("${bytechef.ai.gateway.guardrails.injection-detection-enabled:false}") boolean injectionDetectionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.moderation-enabled:false}") boolean moderationEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-enabled:false}") boolean responseScanEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-streaming-enabled:false}") boolean streamingResponseScanEnabled) {

        this.aiGuardrailsWorkspaceSettingsService = aiGuardrailsWorkspaceSettingsService;
        this.globalBlockedTerms = parseBlockedTerms(blockedTerms);
        this.globalInjectionDetectionEnabled = injectionDetectionEnabled;
        this.globalModerationEnabled = moderationEnabled;
        this.globalPiiRedactionEnabled = piiRedactionEnabled;
        this.globalResponseScanEnabled = responseScanEnabled;
        this.globalSecretRedactionEnabled = secretRedactionEnabled;
        this.globalStreamingResponseScanEnabled = streamingResponseScanEnabled;
        this.injectionClassifier = injectionClassifier;
        this.moderationClassifier = moderationClassifier;
        this.metrics = metrics;
    }

    /**
     * Returns the input strings with request-direction guardrails applied: PII and secrets redacted, blocked terms and
     * injection attempts rejected. Returns the inputs unchanged when no relevant guardrail is active.
     *
     * @param inputs      the input strings
     * @param workspaceId the workspace the call is attributed to, or {@code null} when unattributed (global guardrails
     *                    still apply)
     * @return the guardrailed inputs
     * @throws AiGatewayGuardrailException if an input contains a blocked term or is flagged by injection detection
     */
    public List<String> applyToInputs(List<String> inputs, @Nullable Long workspaceId) {
        if (inputs == null || inputs.isEmpty()) {
            return inputs;
        }

        EffectivePolicy policy = resolvePolicy(workspaceId);

        if (!policy.anyInputGuardrailActive()) {
            return inputs;
        }

        List<String> guardrailedInputs = new ArrayList<>(inputs.size());

        for (String input : inputs) {
            guardrailedInputs.add(checkAndRedact(input, policy));
        }

        return guardrailedInputs;
    }

    /**
     * Non-throwing counterpart to {@link #applyToInputs} for callers that need to choose HOW to handle a blocking
     * violation (a blocked-term match or a flagged prompt injection) instead of having this engine always throw
     * {@link AiGatewayGuardrailException} — e.g. an advisor implementing a workspace's configurable
     * {@link BlockingMode}. {@link #applyToInputs} keeps its unconditional-throw contract unchanged (the AI Gateway
     * adapter's HTTP 422 behavior must not change); this method exists alongside it, not instead of it.
     *
     * <p>
     * PII and secret redaction are always applied inline, exactly as in {@link #applyToInputs}. A blocking violation is
     * reported via {@link GuardrailCheckResult#category()} rather than thrown; for a blocked-term match the matched
     * term is additionally masked out of {@link GuardrailCheckResult#text()} (replaced with
     * {@code [REDACTED_BLOCKED_TERM]}) so a caller that decides to continue anyway never forwards the raw offending
     * text. An injection-flagged input has no single locatable span — the classifier judges the whole message — so its
     * {@code text} carries only the PII/secret redaction already applied. A moderation-flagged input also has no
     * locatable span, but unlike injection its {@code text} is replaced wholesale with {@code [REDACTED_MODERATED]} —
     * see the class javadoc's "Moderation and {@code BlockingMode}" section for why the two differ. Moderation is only
     * checked here, never in {@link #applyToInputs} (the AI Gateway adapter moderates its own DTO pipeline directly, so
     * running it here too would double-moderate gateway traffic).
     * </p>
     *
     * <p>
     * Unlike {@link #applyToInputs}, the request-direction events this method triggers ({@code pii_redacted},
     * {@code secret_redacted}, {@code blocked_term}, {@code injection_flagged}, {@code moderation_flagged}) are
     * recorded through the caller-supplied {@code metrics} instance rather than this engine's own constructor-injected
     * bean — this method has exactly one caller, {@code AiGuardrailsAdvisor}, which passes its own per-request,
     * surface-tagged instance so events land under the calling surface (e.g. {@code ai_agent}, {@code ai_hub}) instead
     * of the engine bean's fixed {@code gateway}-or-nothing tag. {@link #applyToInputs} is untouched and keeps
     * recording through the engine's own bean, so the AI Gateway adapter's metrics are unaffected.
     * </p>
     *
     * @param inputs      the input strings
     * @param workspaceId the workspace the call is attributed to, or {@code null} when unattributed
     * @param metrics     the metrics instance to record request-direction events through, tagged with the caller's own
     *                    surface
     * @return one {@link GuardrailCheckResult} per input, in order; empty when {@code inputs} is {@code null} or empty
     */
    public List<GuardrailCheckResult> checkInputs(
        @Nullable List<String> inputs, @Nullable Long workspaceId, AiGuardrailMetrics metrics) {

        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }

        EffectivePolicy policy = resolvePolicy(workspaceId);
        List<GuardrailCheckResult> results = new ArrayList<>(inputs.size());

        for (String input : inputs) {
            results.add(checkInput(input, policy, metrics));
        }

        return results;
    }

    /**
     * Returns {@code text} redacted for PII and secrets when response scanning is enabled for the workspace (or
     * globally), otherwise {@code text} unchanged. Response scanning is redaction only — it never blocks.
     *
     * @param text        the text to scan (e.g. a single completion choice's content)
     * @param workspaceId the workspace the call is attributed to, or {@code null} when unattributed
     * @return the redacted text, or the original when response scanning is inactive
     */
    public String scanResponseText(String text, @Nullable Long workspaceId) {
        if (text == null) {
            return null;
        }

        EffectivePolicy policy = resolvePolicy(workspaceId);

        if (!policy.scanResponses()) {
            return text;
        }

        return redactAll(text);
    }

    /**
     * Returns a stateful redactor for masking PII/secrets in a streamed completion when streaming response scanning is
     * active for the workspace, otherwise {@code null}. Streaming scanning requires BOTH response scanning to be
     * effective for the workspace (global {@code response-scan-enabled} or workspace {@code scanResponses}) AND the
     * global {@code response-scan-streaming-enabled} operator flag — because holding back a lookahead window to catch
     * values that straddle SSE chunk boundaries trades away some of streaming's incremental latency, which is an
     * operator-level decision. Caller uses the returned redactor across the token stream and flushes it at completion.
     *
     * @param workspaceId the workspace the call is attributed to, or {@code null} when unattributed
     * @return a fresh {@link StreamingResponseRedactor}, or {@code null} when streaming scanning is inactive
     */
    public @Nullable StreamingResponseRedactor newStreamingResponseRedactor(@Nullable Long workspaceId) {
        if (!globalStreamingResponseScanEnabled) {
            return null;
        }

        EffectivePolicy policy = resolvePolicy(workspaceId);

        if (!policy.scanResponses()) {
            return null;
        }

        return new StreamingResponseRedactor();
    }

    /**
     * Returns the effective {@link BlockingMode} for the workspace: the workspace's configured mode, or {@code BLOCK}
     * when no settings row exists (or the row does not configure a mode).
     *
     * @param workspaceId the workspace to resolve, or {@code null} for the tenant default
     * @return the effective blocking mode
     */
    public BlockingMode resolveBlockingMode(@Nullable Long workspaceId) {
        AiGuardrailsWorkspaceSettings settings = findSettings(workspaceId);

        if (settings == null || settings.blockingMode() == null) {
            return BlockingMode.BLOCK;
        }

        return settings.blockingMode();
    }

    /**
     * Returns whether at least one guardrail (PII/secret redaction, blocked terms, injection detection, model-based
     * moderation, or response scanning) is active for {@code workspaceId} once global and workspace-level policy are
     * unioned. Used by callers that want to skip attaching a guardrail advisor entirely when nothing would apply (e.g.
     * {@code AiGuardrailsAdvisorProviderImpl}), so a workspace with every guardrail disabled pays no per-call advisor
     * overhead. Moderation only counts as active when a {@link AiGatewayModerationClassifier} bean is present — a
     * workspace enabling {@code moderationEnabled} without a configured moderation model stays inert (see
     * {@link #resolvePolicy}), matching how injection detection already behaves.
     *
     * @param workspaceId the workspace to resolve, or {@code null} for the tenant default
     * @return {@code true} when at least one guardrail is active
     */
    public boolean isActive(@Nullable Long workspaceId) {
        EffectivePolicy policy = resolvePolicy(workspaceId);

        return policy.anyInputGuardrailActive() || policy.scanResponses() || policy.moderate();
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
        if (content == null) {
            return null;
        }

        String redacted = redactPiiAndSecrets(content, policy, metrics);

        if (findBlockedTerm(redacted, policy.blockedTerms()) != null) {
            record(metrics, "blocked_term");

            log.warn("Content rejected by content guardrail (blocked term matched)");

            throw new AiGatewayGuardrailException("Request rejected by content guardrail: matched a blocked term");
        }

        if (policy.detectInjection() && injectionClassifier != null && injectionClassifier.isInjection(redacted)) {
            record(metrics, "injection_flagged");

            log.warn("Content rejected by injection detection");

            throw new AiGatewayGuardrailException("Request rejected by prompt-injection detection");
        }

        return redacted;
    }

    /**
     * The non-throwing counterpart of {@link #checkAndRedact} used by {@link #checkInputs}: same PII/secret redaction
     * and blocking-violation detection, but a blocking violation is reported via
     * {@link GuardrailCheckResult#category()} (with the offending term masked out of the text, or — for moderation —
     * the whole text replaced) instead of being thrown. Records through {@code recordingMetrics} — the caller-supplied
     * instance from {@link #checkInputs} — rather than this engine's own bean. Moderation is checked LAST and only
     * here, never in {@link #checkAndRedact}: the throwing path is the AI Gateway adapter's, which already moderates
     * its own DTO pipeline with its own classifier wiring, so moderating here too would double-moderate gateway
     * traffic.
     */
    private GuardrailCheckResult checkInput(
        @Nullable String content, EffectivePolicy policy, AiGuardrailMetrics recordingMetrics) {

        if (content == null) {
            return new GuardrailCheckResult(null, null);
        }

        String redacted = redactPiiAndSecrets(content, policy, recordingMetrics);

        String blockedTerm = findBlockedTerm(redacted, policy.blockedTerms());

        if (blockedTerm != null) {
            record(recordingMetrics, "blocked_term");

            return new GuardrailCheckResult(maskBlockedTerm(redacted, blockedTerm), "blocked_term");
        }

        if (policy.detectInjection() && injectionClassifier != null && injectionClassifier.isInjection(redacted)) {
            record(recordingMetrics, "injection_flagged");

            return new GuardrailCheckResult(redacted, "injection_flagged");
        }

        if (policy.moderate() && moderationClassifier != null && moderationClassifier.isFlagged(redacted)) {
            record(recordingMetrics, "moderation_flagged");

            return new GuardrailCheckResult(MODERATION_PLACEHOLDER, "moderation_flagged");
        }

        return new GuardrailCheckResult(redacted, null);
    }

    /**
     * Redacts PII/secrets in {@code content}, recording {@code pii_redacted} / {@code secret_redacted} through
     * {@code recordingMetrics} when a redaction actually changed the text. Shared by both the throwing
     * ({@link #checkAndRedact}, passed this engine's own bean) and non-throwing ({@link #checkInput}, passed the
     * caller-supplied instance) paths, which differ only in which {@link AiGuardrailMetrics} instance they record
     * through.
     */
    private String
        redactPiiAndSecrets(String content, EffectivePolicy policy, @Nullable AiGuardrailMetrics recordingMetrics) {
        String redacted = content;

        if (policy.redactPii()) {
            String piiRedacted = redactPii(redacted);

            if (!piiRedacted.equals(redacted)) {
                record(recordingMetrics, "pii_redacted");
            }

            redacted = piiRedacted;
        }

        if (policy.redactSecrets()) {
            String secretRedacted = redactSecrets(redacted);

            if (!secretRedacted.equals(redacted)) {
                record(recordingMetrics, "secret_redacted");
            }

            redacted = secretRedacted;
        }

        return redacted;
    }

    private EffectivePolicy resolvePolicy(@Nullable Long workspaceId) {
        AiGuardrailsWorkspaceSettings settings = findSettings(workspaceId);

        // Union semantics across global -> workspace: a level can enable a guardrail (or add blocked terms) but never
        // turn one off. A null field on `settings` just means "not set at this level" -- it unions with the GLOBAL
        // properties above, not with the tenant-default (null-workspaceId) row; a real workspace's settings never
        // fall back to the tenant-default row's values.
        boolean redactPii = globalPiiRedactionEnabled ||
            (settings != null && Boolean.TRUE.equals(settings.redactPii()));
        boolean redactSecrets = globalSecretRedactionEnabled ||
            (settings != null && Boolean.TRUE.equals(settings.redactSecrets()));

        Set<String> blockedTerms = new LinkedHashSet<>(globalBlockedTerms);

        if (settings != null && settings.blockedTerms() != null) {
            blockedTerms.addAll(parseBlockedTerms(settings.blockedTerms()));
        }

        boolean detectInjection = injectionClassifier != null &&
            (globalInjectionDetectionEnabled ||
                (settings != null && Boolean.TRUE.equals(settings.injectionDetectionEnabled())));
        boolean moderate = moderationClassifier != null &&
            (globalModerationEnabled || (settings != null && Boolean.TRUE.equals(settings.moderationEnabled())));
        boolean scanResponses = globalResponseScanEnabled ||
            (settings != null && Boolean.TRUE.equals(settings.scanResponses()));

        return new EffectivePolicy(redactPii, redactSecrets, blockedTerms, detectInjection, moderate, scanResponses);
    }

    private @Nullable AiGuardrailsWorkspaceSettings findSettings(@Nullable Long workspaceId) {
        try {
            Optional<AiGuardrailsWorkspaceSettings> settingsOptional =
                aiGuardrailsWorkspaceSettingsService.fetchSettings(workspaceId);

            return settingsOptional.orElse(null);
        } catch (Exception exception) {
            // A settings lookup failure must not take the request path down; global guardrails still apply.
            log.warn(
                "Failed to load AI guardrails workspace settings for workspace {}: {}", workspaceId,
                exception.getMessage());

            return null;
        }
    }

    private static void record(@Nullable AiGuardrailMetrics recordingMetrics, String event) {
        if (recordingMetrics != null) {
            recordingMetrics.record(event);
        }
    }

    private static @Nullable String findBlockedTerm(String content, Set<String> blockedTerms) {
        if (blockedTerms.isEmpty()) {
            return null;
        }

        String lowerContent = content.toLowerCase(Locale.ROOT);

        for (String blockedTerm : blockedTerms) {
            if (lowerContent.contains(blockedTerm)) {
                return blockedTerm;
            }
        }

        return null;
    }

    /**
     * Masks every case-insensitive occurrence of {@code blockedTerm} in {@code content} with
     * {@link #BLOCKED_TERM_PLACEHOLDER}. Used by {@link #checkInput} so a REDACT_AND_CONTINUE caller never forwards the
     * raw matched term.
     */
    private static String maskBlockedTerm(String content, String blockedTerm) {
        Pattern pattern = Pattern.compile(Pattern.quote(blockedTerm), Pattern.CASE_INSENSITIVE);

        return pattern.matcher(content)
            .replaceAll(Matcher.quoteReplacement(BLOCKED_TERM_PLACEHOLDER));
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
     * Result of {@link #checkInputs} for one input. {@code category} is {@code null} when the input triggered no
     * blocking violation; otherwise it is one of {@code "blocked_term"}, {@code "injection_flagged"}, or
     * {@code "moderation_flagged"} — {@code "blocked_term"} and {@code "injection_flagged"} are the same categories
     * {@link #applyToInputs} throws for; {@code "moderation_flagged"} is checked ONLY here (see {@link #checkInput}).
     * {@code text} carries the redacted/masked content a REDACT_AND_CONTINUE caller can safely forward. See
     * {@link #checkInputs} for the full contract.
     */
    public record GuardrailCheckResult(@Nullable String text, @Nullable String category) {

        public boolean blocked() {
            return category != null;
        }
    }

    /**
     * The guardrail policy resolved for one call from the union of global properties and workspace settings.
     * {@code moderate} deliberately does NOT factor into {@link #anyInputGuardrailActive()} — moderation is checked
     * only by the non-throwing {@link #checkInput} path, never by {@link #checkAndRedact} (the throwing
     * {@link #applyToInputs} path {@code anyInputGuardrailActive} gates), so including it there would make
     * {@link #applyToInputs} loop over inputs it would still leave untouched.
     */
    private record EffectivePolicy(
        boolean redactPii, boolean redactSecrets, Set<String> blockedTerms, boolean detectInjection, boolean moderate,
        boolean scanResponses) {

        boolean anyInputGuardrailActive() {
            return redactPii || redactSecrets || !blockedTerms.isEmpty() || detectInjection;
        }
    }
}
