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
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.StreamingResponseRedactor;
import com.bytechef.ee.platform.ai.guardrails.classifier.AiGuardrailInjectionClassifier;
import com.bytechef.ee.platform.ai.guardrails.classifier.AiGuardrailModerationClassifier;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.exception.AiGuardrailRejectionException;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
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
    private final @Nullable AiGuardrailModerationClassifier moderationClassifier;
    private final @Nullable AiGuardrailInjectionClassifier injectionClassifier;
    private final @Nullable AiGuardrailMetrics metrics;
    private final boolean globalModerationEnabled;
    private final boolean globalStreamingResponseScanEnabled;

    public AiGatewayGuardrails(
        AiGuardrails aiGuardrails,
        @Nullable AiGatewayProjectSettingsService aiGatewayProjectSettingsService,
        AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService,
        @Nullable AiGuardrailModerationClassifier moderationClassifier,
        @Nullable AiGuardrailInjectionClassifier injectionClassifier,
        @Nullable AiGuardrailMetrics metrics,
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

    public PiiTokenSession newTokenSession() {
        return aiGuardrails.newTokenSession();
    }

    public AiGatewayChatCompletionRequest apply(
        AiGatewayChatCompletionRequest request, @Nullable Long workspaceId) {
        return apply(request, workspaceId, null);
    }

    public AiGatewayChatCompletionRequest apply(
        AiGatewayChatCompletionRequest request, @Nullable Long workspaceId, @Nullable Long projectId) {
        return apply(request, workspaceId, projectId, null);
    }

    public AiGatewayChatCompletionRequest apply(
        AiGatewayChatCompletionRequest request, @Nullable Long workspaceId, @Nullable Long projectId,
        @Nullable PiiTokenSession session) {

        return apply(request, null, workspaceId, projectId, session);
    }

    public AiGatewayChatCompletionRequest apply(
        AiGatewayChatCompletionRequest request, @Nullable PlatformType platformType, @Nullable Long workspaceId,
        @Nullable Long projectId, @Nullable PiiTokenSession session) {

        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.resolve(platformType, workspaceId);
        AiGatewayProjectSettings projectSettings = findProjectSettings(projectId);
        boolean moderate = moderationClassifier != null && resolveModerationEnabled(target, projectSettings);

        List<AiGatewayChatMessage> guardrailedMessages = new ArrayList<>();
        boolean changed = false;

        for (AiGatewayChatMessage message : request.messages()) {
            String content = message.content();
            String processed = content;

            if (content != null) {
                processed = redactOrTokenize(content, target, session);
                processed = applyProjectOverlay(processed, projectSettings, target);
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

    private String redactOrTokenize(
        String content, AiGuardrailsSettingsTarget target, @Nullable PiiTokenSession session) {
        return applyEngine(List.of(content), target, session).getFirst();
    }

    private List<String> applyEngine(
        List<String> inputs, AiGuardrailsSettingsTarget target, @Nullable PiiTokenSession session) {

        try {
            return aiGuardrails.applyToInputs(inputs, target, session);
        } catch (AiGuardrailRejectionException rejectionException) {
            throw new AiGatewayGuardrailException(rejectionException.getMessage());
        }
    }

    public List<String> applyToInputs(List<String> inputs, @Nullable Long workspaceId) {
        return applyToInputs(inputs, workspaceId, null);
    }

    public List<String> applyToInputs(List<String> inputs, @Nullable Long workspaceId, @Nullable Long projectId) {
        if (inputs == null || inputs.isEmpty()) {
            return inputs;
        }

        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.resolve(null, workspaceId);
        List<String> engineProcessed = applyEngine(inputs, target, null);
        AiGatewayProjectSettings projectSettings = findProjectSettings(projectId);

        if (projectSettings == null) {
            return engineProcessed;
        }

        List<String> guardrailedInputs = new ArrayList<>(engineProcessed.size());
        boolean changed = false;

        for (String input : engineProcessed) {
            String processed = applyProjectOverlay(input, projectSettings, target);

            if (!processed.equals(input)) {
                changed = true;
            }

            guardrailedInputs.add(processed);
        }

        return changed ? guardrailedInputs : engineProcessed;
    }

    public AiGatewayChatCompletionResponse redactResponse(
        AiGatewayChatCompletionResponse response, @Nullable Long workspaceId) {
        return redactResponse(response, workspaceId, null);
    }

    public AiGatewayChatCompletionResponse redactResponse(
        AiGatewayChatCompletionResponse response, @Nullable Long workspaceId, @Nullable Long projectId) {
        return scanResponse(response, workspaceId, projectId);
    }

    public AiGatewayChatCompletionResponse redactResponse(
        AiGatewayChatCompletionResponse response, @Nullable Long workspaceId, @Nullable Long projectId,
        @Nullable PiiTokenSession session) {
        return restoreResponse(scanResponse(response, workspaceId, projectId), session);
    }

    public AiGatewayChatCompletionResponse scanResponse(
        AiGatewayChatCompletionResponse response, @Nullable Long workspaceId, @Nullable Long projectId) {

        return scanResponse(response, null, workspaceId, projectId);
    }

    public AiGatewayChatCompletionResponse scanResponse(
        AiGatewayChatCompletionResponse response, @Nullable PlatformType platformType, @Nullable Long workspaceId,
        @Nullable Long projectId) {

        if (response == null || response.choices() == null || response.choices()
            .isEmpty()) {
            return response;
        }

        AiGatewayProjectSettings projectSettings = findProjectSettings(projectId);
        boolean projectScanResponses = projectSettings != null && Boolean.TRUE.equals(projectSettings.scanResponses());
        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.resolve(platformType, workspaceId);
        double projectOverlayMinConfidence = projectScanResponses ? aiGuardrails.resolveMinConfidence(target) : 0;

        List<AiGatewayChatCompletionResponse.Choice> choices = response.choices();
        List<AiGatewayChatCompletionResponse.Choice> scannedChoices = new ArrayList<>(choices.size());
        boolean responseRedacted = false;
        boolean changed = false;

        for (AiGatewayChatCompletionResponse.Choice choice : choices) {
            AiGatewayChatMessage message = choice.message();
            String content = message == null ? null : message.content();

            if (content == null) {
                scannedChoices.add(choice);

                continue;
            }

            String scanned;

            if (projectScanResponses) {
                scanned = aiGuardrails.redactEnabledKinds(content, target, projectOverlayMinConfidence, metrics);

                if (Boolean.TRUE.equals(projectSettings.redactPii())) {
                    scanned = aiGuardrails.redactPii(scanned, projectOverlayMinConfidence);
                }

                if (Boolean.TRUE.equals(projectSettings.redactSecrets())) {
                    scanned = aiGuardrails.redactSecrets(scanned, projectOverlayMinConfidence);
                }
            } else {
                scanned = aiGuardrails.scanResponseText(content, target, metrics);
            }

            if (!Objects.equals(scanned, content)) {
                responseRedacted = true;
            }

            if (Objects.equals(scanned, content)) {
                scannedChoices.add(choice);

                continue;
            }

            changed = true;

            AiGatewayChatMessage scannedMessage = new AiGatewayChatMessage(
                message.role(), scanned, message.contentBlocks(), message.toolCalls(), message.toolCallId());

            scannedChoices.add(
                new AiGatewayChatCompletionResponse.Choice(choice.index(), scannedMessage, choice.finishReason()));
        }

        if (responseRedacted) {
            record("response_redacted");
        }

        if (!changed) {
            return response;
        }

        return new AiGatewayChatCompletionResponse(
            response.id(), response.object(), response.created(), response.model(), scannedChoices, response.usage(),
            response.gatewayMetadata());
    }

    public AiGatewayChatCompletionResponse restoreResponse(
        AiGatewayChatCompletionResponse response, @Nullable PiiTokenSession session) {
        if (session == null || response == null || response.choices() == null || response.choices()
            .isEmpty()) {
            return response;
        }

        List<AiGatewayChatCompletionResponse.Choice> choices = response.choices();
        List<AiGatewayChatCompletionResponse.Choice> restoredChoices = new ArrayList<>(choices.size());
        boolean changed = false;

        for (AiGatewayChatCompletionResponse.Choice choice : choices) {
            AiGatewayChatMessage message = choice.message();
            String content = message == null ? null : message.content();

            if (content == null) {
                restoredChoices.add(choice);

                continue;
            }

            String restored = aiGuardrails.restoreResponseText(content, session, metrics);

            if (Objects.equals(restored, content)) {
                restoredChoices.add(choice);

                continue;
            }

            changed = true;

            AiGatewayChatMessage restoredMessage = new AiGatewayChatMessage(
                message.role(), restored, message.contentBlocks(), message.toolCalls(), message.toolCallId());

            restoredChoices.add(
                new AiGatewayChatCompletionResponse.Choice(choice.index(), restoredMessage, choice.finishReason()));
        }

        if (!changed) {
            return response;
        }

        return new AiGatewayChatCompletionResponse(
            response.id(), response.object(), response.created(), response.model(), restoredChoices, response.usage(),
            response.gatewayMetadata());
    }

    public @Nullable StreamingResponseRedactor newStreamingResponseRedactor(@Nullable Long workspaceId) {
        return newStreamingResponseRedactor(workspaceId, null);
    }

    public @Nullable StreamingResponseRedactor newStreamingResponseRedactor(
        @Nullable Long workspaceId, @Nullable Long projectId) {

        return newStreamingResponseRedactor(null, workspaceId, projectId);
    }

    public @Nullable StreamingResponseRedactor newStreamingResponseRedactor(
        @Nullable PlatformType platformType, @Nullable Long workspaceId, @Nullable Long projectId) {

        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.resolve(platformType, workspaceId);
        StreamingResponseRedactor redactor = aiGuardrails.newStreamingResponseRedactor(target, metrics);

        if (redactor != null) {
            return redactor;
        }

        if (!globalStreamingResponseScanEnabled) {
            return null;
        }

        AiGatewayProjectSettings projectSettings = findProjectSettings(projectId);

        if (projectSettings != null && Boolean.TRUE.equals(projectSettings.scanResponses())) {
            return aiGuardrails.newStreamingResponseRedactor(target);
        }

        return null;
    }

    public void recordResponseRedacted() {
        record("response_redacted");
    }

    private String applyProjectOverlay(
        String text, @Nullable AiGatewayProjectSettings projectSettings, AiGuardrailsSettingsTarget target) {
        if (projectSettings == null) {
            return text;
        }

        String result = text;
        double minConfidence = aiGuardrails.resolveMinConfidence(target);

        if (Boolean.TRUE.equals(projectSettings.redactPii())) {
            String redacted = aiGuardrails.redactPii(result, minConfidence);

            if (!Objects.equals(redacted, result)) {
                record("pii_redacted");
            }

            result = redacted;
        }

        if (Boolean.TRUE.equals(projectSettings.redactSecrets())) {
            String redacted = aiGuardrails.redactSecrets(result, minConfidence);

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
        AiGuardrailsSettingsTarget target, @Nullable AiGatewayProjectSettings projectSettings) {
        if (globalModerationEnabled) {
            return true;
        }

        AiGuardrailsWorkspaceSettings settings = fetchSettings(target);

        if (settings != null && Boolean.TRUE.equals(settings.moderationEnabled())) {
            return true;
        }

        return projectSettings != null && Boolean.TRUE.equals(projectSettings.moderationEnabled());
    }

    private @Nullable AiGuardrailsWorkspaceSettings fetchSettings(AiGuardrailsSettingsTarget target) {
        try {
            Optional<AiGuardrailsWorkspaceSettings> settingsOptional =
                target.scope() == AiGuardrailsSettingsScope.EMBEDDED
                    ? aiGuardrailsWorkspaceSettingsService.fetchEmbeddedSettings()
                    : aiGuardrailsWorkspaceSettingsService.fetchSettings(target.workspaceId());

            return settingsOptional.orElse(null);
        } catch (Exception exception) {
            log.warn(
                "Failed to load AI guardrails settings for {} while resolving moderation: {}", target,
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
