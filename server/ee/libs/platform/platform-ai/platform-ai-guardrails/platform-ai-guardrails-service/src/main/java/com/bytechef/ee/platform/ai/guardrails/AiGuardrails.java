/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import com.bytechef.ee.platform.ai.guardrails.classifier.AiGuardrailInjectionClassifier;
import com.bytechef.ee.platform.ai.guardrails.classifier.AiGuardrailModerationClassifier;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailCustomRule;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.exception.AiGuardrailRejectionException;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailCustomRuleService;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.platform.ai.guardrails.ConversationScope;
import com.bytechef.platform.ai.sensitivedata.CustomPattern;
import com.bytechef.platform.ai.sensitivedata.CustomPatternEvaluator;
import com.bytechef.platform.ai.sensitivedata.MatchDeadline;
import com.bytechef.platform.ai.sensitivedata.PiiPatternCatalog;
import com.bytechef.platform.ai.sensitivedata.PiiTokenSessionStore;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataDetector;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataDetectors;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor.RedactionResult;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiToken;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import com.bytechef.platform.ai.sensitivedata.tokenization.SensitiveDataPolicy;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
public class AiGuardrails {

    private static final Logger log = LoggerFactory.getLogger(AiGuardrails.class);

    private static final SensitiveKind[] SENSITIVE_KIND_VALUES = SensitiveKind.values();

    private static final String BLOCKED_TERM_PLACEHOLDER = "[REDACTED_BLOCKED_TERM]";
    private static final String MODERATION_PLACEHOLDER = "[REDACTED_MODERATED]";

    private final AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService;
    private final List<String> globalBlockedTerms;
    private final boolean globalInjectionDetectionEnabled;
    private final boolean globalModerationEnabled;
    private final boolean globalPiiRedactionEnabled;
    private final boolean globalResponseScanEnabled;
    private final boolean globalSecretRedactionEnabled;
    private final boolean globalStreamingResponseScanEnabled;
    private final @Nullable AiGuardrailInjectionClassifier injectionClassifier;
    private final @Nullable AiGuardrailModerationClassifier moderationClassifier;
    private final @Nullable AiGuardrailMetrics metrics;
    private final @Nullable AiGuardrailCustomRuleService aiGuardrailCustomRuleService;
    private final boolean globalViolationRecordingEnabled;
    private final @Nullable PiiTokenSessionStore piiTokenSessionStore;
    private final SensitiveDataRedactor sensitiveDataRedactor;
    private final SensitiveDataRedactor streamSafeSensitiveDataRedactor;

    public AiGuardrails(
        AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService,
        @Nullable AiGuardrailInjectionClassifier injectionClassifier,
        @Nullable AiGuardrailModerationClassifier moderationClassifier,
        @Nullable AiGuardrailMetrics metrics,
        @Value("${bytechef.ai.gateway.guardrails.pii-redaction-enabled:false}") boolean piiRedactionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.secret-redaction-enabled:false}") boolean secretRedactionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.blocked-terms:}") String blockedTerms,
        @Value("${bytechef.ai.gateway.guardrails.injection-detection-enabled:false}") boolean injectionDetectionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.moderation-enabled:false}") boolean moderationEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-enabled:false}") boolean responseScanEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-streaming-enabled:false}") boolean streamingResponseScanEnabled) {

        this(
            aiGuardrailsWorkspaceSettingsService, injectionClassifier, moderationClassifier, metrics,
            piiRedactionEnabled, secretRedactionEnabled, blockedTerms, injectionDetectionEnabled, moderationEnabled,
            responseScanEnabled, streamingResponseScanEnabled, null);
    }

    public AiGuardrails(
        AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService,
        @Nullable AiGuardrailInjectionClassifier injectionClassifier,
        @Nullable AiGuardrailModerationClassifier moderationClassifier,
        @Nullable AiGuardrailMetrics metrics,
        boolean piiRedactionEnabled,
        boolean secretRedactionEnabled,
        String blockedTerms,
        boolean injectionDetectionEnabled,
        boolean moderationEnabled,
        boolean responseScanEnabled,
        boolean streamingResponseScanEnabled,
        @Nullable PiiTokenSessionStore piiTokenSessionStore) {

        this(
            aiGuardrailsWorkspaceSettingsService, injectionClassifier, moderationClassifier, metrics,
            SensitiveDataDetectors.builtIn(), piiRedactionEnabled, secretRedactionEnabled, blockedTerms,
            injectionDetectionEnabled, moderationEnabled, responseScanEnabled, streamingResponseScanEnabled,
            SensitiveDataRedactor.DetectionBounds.DEFAULTS.timeout(),
            SensitiveDataRedactor.DetectionBounds.DEFAULTS.maxUnwindowableInput(), false,
            null, piiTokenSessionStore);
    }

    public AiGuardrails(
        AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService,
        @Nullable AiGuardrailInjectionClassifier injectionClassifier,
        @Nullable AiGuardrailModerationClassifier moderationClassifier,
        @Nullable AiGuardrailMetrics metrics,
        List<SensitiveDataDetector> sensitiveDataDetectors,
        boolean piiRedactionEnabled,
        boolean secretRedactionEnabled,
        String blockedTerms,
        boolean injectionDetectionEnabled,
        boolean moderationEnabled,
        boolean responseScanEnabled,
        boolean streamingResponseScanEnabled) {

        this(
            aiGuardrailsWorkspaceSettingsService, injectionClassifier, moderationClassifier, metrics,
            sensitiveDataDetectors, piiRedactionEnabled, secretRedactionEnabled, blockedTerms,
            injectionDetectionEnabled, moderationEnabled, responseScanEnabled, streamingResponseScanEnabled,
            SensitiveDataRedactor.DetectionBounds.DEFAULTS.timeout(),
            SensitiveDataRedactor.DetectionBounds.DEFAULTS.maxUnwindowableInput(), false,
            null, (PiiTokenSessionStore) null);
    }

    @Autowired
    public AiGuardrails(
        AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService,
        @Nullable AiGuardrailInjectionClassifier injectionClassifier,
        @Nullable AiGuardrailModerationClassifier moderationClassifier,
        @Nullable AiGuardrailMetrics metrics,
        List<SensitiveDataDetector> sensitiveDataDetectors,
        @Value("${bytechef.ai.gateway.guardrails.pii-redaction-enabled:false}") boolean piiRedactionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.secret-redaction-enabled:false}") boolean secretRedactionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.blocked-terms:}") String blockedTerms,
        @Value("${bytechef.ai.gateway.guardrails.injection-detection-enabled:false}") boolean injectionDetectionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.moderation-enabled:false}") boolean moderationEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-enabled:false}") boolean responseScanEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-streaming-enabled:false}") boolean streamingResponseScanEnabled,
        @Value("${bytechef.ai.guardrails.detection.timeout:2s}") Duration detectionTimeout,
        @Value("${bytechef.ai.guardrails.detection.max-unwindowable-input:262144}") int maxUnwindowableInput,
        @Value("${bytechef.ai.guardrails.violation.enabled:false}") boolean violationRecordingEnabled,
        @Nullable ObjectProvider<AiGuardrailCustomRuleService> aiGuardrailCustomRuleServiceProvider,
        @Nullable ObjectProvider<PiiTokenSessionStore> piiTokenSessionStoreProvider) {

        this(
            aiGuardrailsWorkspaceSettingsService, injectionClassifier, moderationClassifier, metrics,
            sensitiveDataDetectors, piiRedactionEnabled, secretRedactionEnabled, blockedTerms,
            injectionDetectionEnabled, moderationEnabled, responseScanEnabled, streamingResponseScanEnabled,
            detectionTimeout, maxUnwindowableInput, violationRecordingEnabled,
            aiGuardrailCustomRuleServiceProvider == null
                ? null : aiGuardrailCustomRuleServiceProvider.getIfAvailable(),
            piiTokenSessionStoreProvider == null ? null : piiTokenSessionStoreProvider.getIfAvailable());
    }

    private AiGuardrails(
        AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService,
        @Nullable AiGuardrailInjectionClassifier injectionClassifier,
        @Nullable AiGuardrailModerationClassifier moderationClassifier,
        @Nullable AiGuardrailMetrics metrics,
        List<SensitiveDataDetector> sensitiveDataDetectors,
        boolean piiRedactionEnabled,
        boolean secretRedactionEnabled,
        String blockedTerms,
        boolean injectionDetectionEnabled,
        boolean moderationEnabled,
        boolean responseScanEnabled,
        boolean streamingResponseScanEnabled,
        Duration detectionTimeout,
        int maxUnwindowableInput,
        boolean violationRecordingEnabled,
        @Nullable AiGuardrailCustomRuleService aiGuardrailCustomRuleService,
        @Nullable PiiTokenSessionStore piiTokenSessionStore) {

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
        this.aiGuardrailCustomRuleService = aiGuardrailCustomRuleService;
        this.globalViolationRecordingEnabled = violationRecordingEnabled;
        this.piiTokenSessionStore = piiTokenSessionStore;
        this.sensitiveDataRedactor = new SensitiveDataRedactor(
            sensitiveDataDetectors,
            new SensitiveDataRedactor.DetectionBounds(detectionTimeout, maxUnwindowableInput));
        this.streamSafeSensitiveDataRedactor = this.sensitiveDataRedactor.streamSafeView();
    }

    public List<String> applyToInputs(
        List<String> inputs, AiGuardrailsSettingsTarget target, @Nullable PiiTokenSession session) {

        if (inputs == null || inputs.isEmpty()) {
            return inputs;
        }

        EffectivePolicy policy = resolvePolicy(target);

        if (!policy.anyInputGuardrailActive()) {
            return inputs;
        }

        List<String> guardrailedInputs = new ArrayList<>(inputs.size());

        for (String input : inputs) {
            guardrailedInputs.add(checkAndRedact(input, policy, session));
        }

        return guardrailedInputs;
    }

    public List<GuardrailCheckResult> checkInputs(
        @Nullable List<String> inputs, AiGuardrailsSettingsTarget target, AiGuardrailMetrics metrics) {

        return checkOrTokenizeInputs(inputs, target, metrics, null);
    }

    public PiiTokenSession newTokenSession() {
        TokenSessionHandle tokenSessionHandle = newTokenSession(null);

        return tokenSessionHandle.session();
    }

    public TokenSessionHandle newTokenSession(ConversationScope.@Nullable Key key) {
        if (key == null || piiTokenSessionStore == null) {
            return new TokenSessionHandle(PiiTokenSession.create(), true);
        }

        PiiTokenSessionStore.LoadResult loadResult = piiTokenSessionStore.load(toSessionKey(key));

        Map<String, String> tokens = loadResult.tokens();

        PiiTokenSession session = PiiToken.sessionIdOf(tokens)
            .map(sessionId -> PiiTokenSession.rehydrate(sessionId, tokens))
            .orElseGet(PiiTokenSession::create);

        return new TokenSessionHandle(session, loadResult.available());
    }

    public void saveTokenSession(ConversationScope.Key key, TokenSessionHandle tokenSessionHandle) {
        if (piiTokenSessionStore == null || !tokenSessionHandle.savable()) {
            return;
        }

        PiiTokenSession session = tokenSessionHandle.session();

        Map<String, String> tokens = session.tokens();

        if (tokens.isEmpty()) {
            return;
        }

        piiTokenSessionStore.save(toSessionKey(key), tokens);
    }

    public List<GuardrailCheckResult> tokenizeInputs(
        @Nullable List<String> inputs, AiGuardrailsSettingsTarget target, PiiTokenSession session,
        AiGuardrailMetrics metrics) {

        return checkOrTokenizeInputs(inputs, target, metrics, session);
    }

    public @Nullable String restoreResponseText(
        String text, PiiTokenSession session, @Nullable AiGuardrailMetrics recordingMetrics) {

        PiiTokenSession.RestoreResult restoreResult = session.restoreWithUnresolvedCount(text);
        String restored = restoreResult.text();

        if (restoreResult.unresolvedCount() > 0) {
            record(recordingMetrics, "token_unresolved");
        }

        if (!Objects.equals(restored, text)) {
            record(recordingMetrics, "pii_restored");
        }

        return restored;
    }

    public String scanResponseText(
        String text, AiGuardrailsSettingsTarget target, @Nullable AiGuardrailMetrics recordingMetrics) {

        if (text == null) {
            return null;
        }

        EffectivePolicy policy = resolvePolicy(target);

        if (!policy.scanResponses()) {
            return text;
        }

        return redactEnabledKinds(text, policy, policy.minConfidence(), recordingMetrics);
    }

    public @Nullable StreamingResponseRedactor newStreamingResponseRedactor(
        AiGuardrailsSettingsTarget target, @Nullable AiGuardrailMetrics recordingMetrics) {
        if (!globalStreamingResponseScanEnabled) {
            return null;
        }

        EffectivePolicy policy = resolvePolicy(target);

        if (!policy.scanResponses()) {
            return null;
        }

        EnumSet<SensitiveKind> kinds = enabledKinds(policy);

        return new StreamingResponseRedactor(
            streamSafeSensitiveDataRedactor, policy.minConfidence(), recordingMetrics, kinds);
    }

    public @Nullable StreamingResponseRedactor newStreamingResponseRedactor(
        AiGuardrailsSettingsTarget target, @Nullable AiGuardrailMetrics recordingMetrics, PiiTokenSession session) {

        EffectivePolicy policy = resolvePolicy(target);
        boolean streamingScanActive = globalStreamingResponseScanEnabled && policy.scanResponses();

        if (!streamingScanActive && session.size() == 0) {
            return null;
        }

        EnumSet<SensitiveKind> kinds =
            streamingScanActive ? enabledKinds(policy) : EnumSet.noneOf(SensitiveKind.class);

        return new StreamingResponseRedactor(
            streamSafeSensitiveDataRedactor, policy.minConfidence(), recordingMetrics, session, kinds);
    }

    public StreamingResponseRedactor newStreamingResponseRedactor(AiGuardrailsSettingsTarget target) {
        EffectivePolicy policy = resolvePolicy(target);

        return new StreamingResponseRedactor(
            streamSafeSensitiveDataRedactor, policy.minConfidence(), metrics, enabledKinds(policy));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    public boolean isViolationRecordingEnabled(AiGuardrailsSettingsTarget target) {
        return globalViolationRecordingEnabled;
    }

    public BlockingMode resolveBlockingMode(AiGuardrailsSettingsTarget target) {
        AiGuardrailsWorkspaceSettings settings = findSettings(target);

        if (settings == null || settings.blockingMode() == null) {
            return BlockingMode.BLOCK;
        }

        return settings.blockingMode();
    }

    public double resolveMinConfidence(AiGuardrailsSettingsTarget target) {
        return resolvePolicy(target).minConfidence();
    }

    public SensitiveDataPolicy resolveToolBoundaryPolicy(AiGuardrailsSettingsTarget target) {
        return resolveToolBoundaryPolicy(target, false);
    }

    public SensitiveDataPolicy resolveToolBoundaryPolicy(
        AiGuardrailsSettingsTarget target, boolean workflowSurface) {

        return toolBoundaryPolicyOf(
            resolvePolicy(target), !workflowSurface || isRestoreIntoWorkflowOutput(target));
    }

    public boolean isRestoreIntoWorkflowOutput(AiGuardrailsSettingsTarget target) {
        AiGuardrailsWorkspaceSettings settings = findSettings(target);

        return settings != null && Boolean.TRUE.equals(settings.restoreIntoWorkflowOutput());
    }

    private static SensitiveDataPolicy toolBoundaryPolicyOf(
        EffectivePolicy policy, boolean restoreOutboundArguments) {

        return new SensitiveDataPolicy(enabledKinds(policy), policy.minConfidence(), restoreOutboundArguments);
    }

    public @Nullable SensitiveDataPolicy resolveMcpOutboundPolicy(AiGuardrailsSettingsTarget target) {
        AiGuardrailsWorkspaceSettings settings = target.scope() == AiGuardrailsSettingsScope.EMBEDDED
            ? aiGuardrailsWorkspaceSettingsService.fetchEmbeddedSettings()
                .orElse(null)
            : aiGuardrailsWorkspaceSettingsService.fetchSettings(target.workspaceId())
                .orElse(null);

        if (settings == null || !Boolean.TRUE.equals(settings.redactMcpResults())) {
            return null;
        }

        return toolBoundaryPolicyOf(effectivePolicyOf(settings), false);
    }

    public @Nullable SensitiveDataPolicy resolveEmbeddedMcpOutboundPolicy() {
        AiGuardrailsWorkspaceSettings settings = aiGuardrailsWorkspaceSettingsService.fetchEmbeddedSettings()
            .orElse(null);

        if (settings == null || !Boolean.TRUE.equals(settings.redactMcpResults())) {
            return null;
        }

        return toolBoundaryPolicyOf(effectivePolicyOf(settings), false);
    }

    public boolean isActive(AiGuardrailsSettingsTarget target) {
        EffectivePolicy policy = resolvePolicy(target);

        return policy.anyInputGuardrailActive() || policy.scanResponses() || policy.moderate();
    }

    public @Nullable String redactPii(@Nullable String content) {
        return redactPii(content, SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE);
    }

    public @Nullable String redactPii(@Nullable String content, double minConfidence) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        return sensitiveDataRedactor.redact(content, EnumSet.of(SensitiveKind.PII), minConfidence, metrics);
    }

    public @Nullable String redactSecrets(@Nullable String content) {
        return redactSecrets(content, SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE);
    }

    public @Nullable String redactSecrets(@Nullable String content, double minConfidence) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        return sensitiveDataRedactor.redact(content, EnumSet.of(SensitiveKind.SECRET), minConfidence, metrics);
    }

    public @Nullable String redactAll(@Nullable String content) {
        return redactAll(content, SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, metrics);
    }

    public @Nullable String redactAll(@Nullable String content, double minConfidence) {
        return redactAll(content, minConfidence, metrics);
    }

    public @Nullable String redactAll(@Nullable String content, @Nullable AiGuardrailMetrics recordingMetrics) {
        return redactAll(content, SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, recordingMetrics);
    }

    public @Nullable String redactAll(
        @Nullable String content, double minConfidence, @Nullable AiGuardrailMetrics recordingMetrics) {

        if (content == null || content.isEmpty()) {
            return content;
        }

        return sensitiveDataRedactor.redact(content, EnumSet.allOf(SensitiveKind.class), minConfidence,
            recordingMetrics);
    }

    public @Nullable String redactEnabledKinds(
        @Nullable String content, AiGuardrailsSettingsTarget target, double minConfidence,
        @Nullable AiGuardrailMetrics recordingMetrics) {

        if (content == null || content.isEmpty()) {
            return content;
        }

        return redactEnabledKinds(content, resolvePolicy(target), minConfidence, recordingMetrics);
    }

    private @Nullable String redactEnabledKinds(
        @Nullable String content, EffectivePolicy policy, double minConfidence,
        @Nullable AiGuardrailMetrics recordingMetrics) {

        if (content == null || content.isEmpty()) {
            return content;
        }

        Set<SensitiveKind> kinds = enabledKinds(policy);

        if (kinds.isEmpty()) {
            return content;
        }

        return sensitiveDataRedactor.redact(content, kinds, minConfidence, recordingMetrics);
    }

    private List<GuardrailCheckResult> checkOrTokenizeInputs(
        @Nullable List<String> inputs, AiGuardrailsSettingsTarget target, AiGuardrailMetrics metrics,
        @Nullable PiiTokenSession session) {

        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }

        EffectivePolicy policy = resolvePolicy(target);
        List<GuardrailCheckResult> results = new ArrayList<>(inputs.size());

        for (String input : inputs) {
            results.add(checkInput(input, policy, metrics, session));
        }

        return results;
    }

    private String checkAndRedact(@Nullable String content, EffectivePolicy policy, @Nullable PiiTokenSession session) {
        if (content == null) {
            return null;
        }

        String result = redactPiiAndSecrets(content, policy, metrics, session).text();

        if (findBlockedTerm(result, policy.blockedTerms()) != null) {
            record(metrics, "blocked_term");

            log.warn("Content rejected by content guardrail (blocked term matched)");

            throw new AiGuardrailRejectionException("Request rejected by content guardrail: matched a blocked term");
        }

        if (policy.detectInjection() && injectionClassifier != null && injectionClassifier.isInjection(result)) {
            record(metrics, "injection_flagged");

            log.warn("Content rejected by injection detection");

            throw new AiGuardrailRejectionException("Request rejected by prompt-injection detection");
        }

        return result;
    }

    private GuardrailCheckResult checkInput(
        @Nullable String content, EffectivePolicy policy, AiGuardrailMetrics recordingMetrics,
        @Nullable PiiTokenSession session) {

        if (content == null) {
            return new GuardrailCheckResult(null, null, null, List.of());
        }

        RedactedContent redactedContent = redactPiiAndSecrets(content, policy, recordingMetrics, session);
        String redacted = redactedContent.text();
        List<SensitiveSpan> spans = redactedContent.accepted();

        String blockedTerm = findBlockedTerm(redacted, policy.blockedTerms());

        if (blockedTerm != null) {
            record(recordingMetrics, "blocked_term");

            return new GuardrailCheckResult(
                maskBlockedTerm(redacted, blockedTerm), redacted, "blocked_term", spans);
        }

        if (policy.detectInjection() && injectionClassifier != null && injectionClassifier.isInjection(redacted)) {
            record(recordingMetrics, "injection_flagged");

            return new GuardrailCheckResult(redacted, redacted, "injection_flagged", spans);
        }

        if (policy.moderate() && moderationClassifier != null && moderationClassifier.isFlagged(redacted)) {
            record(recordingMetrics, "moderation_flagged");

            return new GuardrailCheckResult(MODERATION_PLACEHOLDER, redacted, "moderation_flagged", spans);
        }

        return new GuardrailCheckResult(redacted, redacted, null, spans);
    }

    private static EnumSet<SensitiveKind> enabledKinds(EffectivePolicy policy) {
        EnumSet<SensitiveKind> kinds = EnumSet.noneOf(SensitiveKind.class);

        if (policy.redactPii()) {
            kinds.add(SensitiveKind.PII);
        }

        if (policy.redactSecrets()) {
            kinds.add(SensitiveKind.SECRET);
        }

        return kinds;
    }

    private RedactedContent redactPiiAndSecrets(
        String content, EffectivePolicy policy, @Nullable AiGuardrailMetrics recordingMetrics,
        @Nullable PiiTokenSession session) {

        Set<SensitiveKind> kinds = enabledKinds(policy);

        List<SensitiveSpan> customSpans = CustomPatternEvaluator.detect(
            content, policy.customRules(), MatchDeadline.unbounded());

        RedactionResult redactionResult = session == null
            ? sensitiveDataRedactor.redactWithSpans(
                content, kinds, policy.minConfidence(), recordingMetrics, customSpans)
            : sensitiveDataRedactor.tokenizeWithSpans(
                content, kinds, session, policy.minConfidence(), recordingMetrics, customSpans);

        List<SensitiveSpan> accepted = redactionResult.accepted();

        if (containsKind(accepted, SensitiveKind.PII)) {
            record(recordingMetrics, session == null ? "pii_redacted" : "pii_tokenized");
        }

        if (containsKind(accepted, SensitiveKind.SECRET)) {
            record(recordingMetrics, "secret_redacted");
        }

        return new RedactedContent(redactionResult.text(), accepted);
    }

    private record RedactedContent(String text, List<SensitiveSpan> accepted) {
    }

    private static boolean containsKind(List<SensitiveSpan> spans, SensitiveKind kind) {
        for (SensitiveSpan span : spans) {
            if (span.kind() == kind) {
                return true;
            }
        }

        return false;
    }

    private EffectivePolicy resolvePolicy(AiGuardrailsSettingsTarget target) {
        return effectivePolicyOf(findSettings(target), resolveCustomRules(target));
    }

    private List<CustomPattern> resolveCustomRules(AiGuardrailsSettingsTarget target) {
        Long workspaceId = target.workspaceId();

        if (aiGuardrailCustomRuleService == null || workspaceId == null) {
            return List.of();
        }

        List<AiGuardrailCustomRule> customRules = aiGuardrailCustomRuleService.getEnabledRules(workspaceId);

        if (customRules.isEmpty()) {
            return List.of();
        }

        List<CustomPattern> customPatterns = new ArrayList<>(customRules.size());

        for (AiGuardrailCustomRule customRule : customRules) {
            try {
                customPatterns.add(toCustomPattern(customRule));
            } catch (RuntimeException runtimeException) {
                log.warn(
                    "Custom guardrail rule '{}' in workspace {} could not be compiled and is skipped for this call",
                    customRule.getType(), workspaceId, runtimeException);
            }
        }

        return customPatterns;
    }

    private static CustomPattern toCustomPattern(AiGuardrailCustomRule customRule) {
        String keywords = customRule.getContextKeywords();
        Integer contextWindow = customRule.getContextWindow();
        BigDecimal contextScore = customRule.getContextScore();
        PiiPatternCatalog.ContextRule contextRule = null;

        if (keywords != null && !keywords.isBlank() && contextWindow != null && contextScore != null) {
            contextRule = new PiiPatternCatalog.ContextRule(
                Arrays.stream(keywords.split(","))
                    .map(String::trim)
                    .filter(keyword -> !keyword.isEmpty())
                    .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet()),
                contextWindow, contextScore.doubleValue());
        }

        return new CustomPattern(
            customRule.getType(), Pattern.compile(customRule.getPattern()),
            SENSITIVE_KIND_VALUES[customRule.getKind()],
            customRule.getScore()
                .doubleValue(),
            contextRule);
    }

    private EffectivePolicy effectivePolicyOf(@Nullable AiGuardrailsWorkspaceSettings settings) {
        return effectivePolicyOf(settings, List.of());
    }

    private EffectivePolicy effectivePolicyOf(
        @Nullable AiGuardrailsWorkspaceSettings settings, List<CustomPattern> customRules) {
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

        double minConfidence = settings != null && settings.minConfidence() != null
            ? settings.minConfidence()
            : SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE;

        return new EffectivePolicy(
            redactPii, redactSecrets, blockedTerms, detectInjection, moderate, scanResponses, minConfidence,
            customRules);
    }

    private @Nullable AiGuardrailsWorkspaceSettings findSettings(AiGuardrailsSettingsTarget target) {
        try {
            Optional<AiGuardrailsWorkspaceSettings> settingsOptional =
                target.scope() == AiGuardrailsSettingsScope.EMBEDDED
                    ? aiGuardrailsWorkspaceSettingsService.fetchEmbeddedSettings()
                    : aiGuardrailsWorkspaceSettingsService.fetchSettings(target.workspaceId());

            return settingsOptional.orElse(null);
        } catch (Exception exception) {
            log.warn(
                "Failed to load AI guardrails workspace settings for {}: {}", target, exception.getMessage());

            return null;
        }
    }

    private static PiiTokenSessionStore.SessionKey toSessionKey(ConversationScope.Key key) {
        return new PiiTokenSessionStore.SessionKey(key.workspaceId(), key.userId(), key.conversationId());
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

    public record GuardrailCheckResult(
        @Nullable String text, @Nullable String unmaskedText, @Nullable String category,
        List<SensitiveSpan> spans) {

        public GuardrailCheckResult {
            spans = spans == null ? List.of() : List.copyOf(spans);
        }

        public boolean blocked() {
            return category != null;
        }
    }

    public record TokenSessionHandle(PiiTokenSession session, boolean savable) {
    }

    private record EffectivePolicy(
        boolean redactPii, boolean redactSecrets, Set<String> blockedTerms, boolean detectInjection, boolean moderate,
        boolean scanResponses, double minConfidence, List<CustomPattern> customRules) {

        boolean anyInputGuardrailActive() {
            return redactPii || redactSecrets || !blockedTerms.isEmpty() || detectInjection;
        }
    }
}
