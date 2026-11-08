/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.evaluation;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalRuleService;
import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalExecution;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalExecutionFailureReason;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRuleTarget;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreSource;
import com.bytechef.ee.platform.ai.eval.service.AiEvalExecutionService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalRuleService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalScoreConfigService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayThrowables;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.ai.guardrails.GuardrailSurface;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings({
    "EI", "CT_CONSTRUCTOR_THROW"
})
public class AiEvalExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiEvalExecutor.class);

    private static final String TOP_LEVEL_FAILURE_METRIC = "bytechef_ai_eval_top_level_failure";

    private final ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderProvider;
    private final AiEvalExecutionService aiEvalExecutionService;
    private final AiEvalRuleService aiEvalRuleService;
    private final AiEvalScoreConfigService aiEvalScoreConfigService;
    private final WorkspaceAiEvalScoreService workspaceAiEvalScoreService;
    private final WorkspaceAiEvalRuleService workspaceAiEvalRuleService;
    private final AiGatewayChatModelFactory aiGatewayChatModelFactory;
    private final AiGatewayProviderService aiGatewayProviderService;
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;
    private final AiObservabilitySpanService aiObservabilitySpanService;
    private final MeterRegistry meterRegistry;

    public AiEvalExecutor(
        ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderProvider,
        AiEvalExecutionService aiEvalExecutionService,
        AiEvalRuleService aiEvalRuleService,
        AiEvalScoreConfigService aiEvalScoreConfigService,
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService,
        WorkspaceAiEvalRuleService workspaceAiEvalRuleService,
        AiGatewayChatModelFactory aiGatewayChatModelFactory,
        AiGatewayProviderService aiGatewayProviderService,
        AiObservabilityTraceService aiObservabilityTraceService,
        WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService,
        AiObservabilitySpanService aiObservabilitySpanService,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.aiGuardrailsAdvisorProviderProvider = aiGuardrailsAdvisorProviderProvider;
        this.aiEvalExecutionService = aiEvalExecutionService;
        this.aiEvalRuleService = aiEvalRuleService;
        this.aiEvalScoreConfigService = aiEvalScoreConfigService;
        this.workspaceAiEvalScoreService = workspaceAiEvalScoreService;
        this.workspaceAiEvalRuleService = workspaceAiEvalRuleService;
        this.aiGatewayChatModelFactory = aiGatewayChatModelFactory;
        this.aiGatewayProviderService = aiGatewayProviderService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.workspaceAiObservabilityTraceService = workspaceAiObservabilityTraceService;
        this.aiObservabilitySpanService = aiObservabilitySpanService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    private static final class MissingModelException extends IllegalArgumentException {

        private static final long serialVersionUID = 1L;

        MissingModelException(String message) {
            super(message);
        }
    }

    private static final class ProviderNotFoundException extends IllegalArgumentException {

        private static final long serialVersionUID = 1L;

        ProviderNotFoundException(String message) {
            super(message);
        }
    }

    private static final class ScoreConfigNotFoundException extends IllegalArgumentException {

        private static final long serialVersionUID = 1L;

        ScoreConfigNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Async(AiEvalExecutorAsyncConfiguration.AI_EVAL_TASK_EXECUTOR)
    public void evaluateTraceForRule(long traceId, long evalRuleId) {
        AiEvalExecution evalExecution = null;

        try {
            AiObservabilityTrace trace = aiObservabilityTraceService.getTrace(traceId);
            AiEvalRule evalRule = aiEvalRuleService.getEvalRule(evalRuleId);

            if (!matchesFilters(evalRule, trace)) {
                log.debug("Trace {} does not match filters for rule {}; skipping", traceId, evalRuleId);

                return;
            }

            evalExecution = aiEvalExecutionService.create(new AiEvalExecution(evalRule.getId(), traceId));

            executeEvaluation(evalExecution, evalRule, trace);
        } catch (Exception exception) {
            if (containsInterruptedCause(exception)) {
                Thread.currentThread()
                    .interrupt();
            }

            log.error("Historical evaluation failed for rule {} on trace {}", evalRuleId, traceId, exception);

            if (evalExecution == null) {
                try {
                    evalExecution = aiEvalExecutionService.create(new AiEvalExecution(evalRuleId, traceId));
                } catch (Exception breadcrumbException) {
                    log.error(
                        "Could not mint ERROR breadcrumb for historical eval of rule {} on trace {}",
                        evalRuleId, traceId, breadcrumbException);

                    return;
                }
            }

            markAsErrorSafely(evalExecution, AiGatewayThrowables.summarize(exception));
        }
    }

    private static boolean containsInterruptedCause(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof InterruptedException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    @Async(AiEvalExecutorAsyncConfiguration.AI_EVAL_TASK_EXECUTOR)
    public void evaluateTrace(long traceId, Long workspaceId) {
        doEvaluateTrace(traceId, workspaceId, AiEvalRuleTarget.LIVE_TRACE);
    }

    @Async(AiEvalExecutorAsyncConfiguration.AI_EVAL_TASK_EXECUTOR)
    public void evaluateTrace(long traceId, Long workspaceId, AiEvalRuleTarget target) {
        doEvaluateTrace(traceId, workspaceId, target);
    }

    private void doEvaluateTrace(long traceId, Long workspaceId, AiEvalRuleTarget target) {
        List<AiEvalRule> enabledRules = List.of();
        AiObservabilityTrace trace = null;

        try {
            trace = aiObservabilityTraceService.getTrace(traceId);
            enabledRules = workspaceAiEvalRuleService.getEnabledEvalRulesByWorkspaceAndTarget(workspaceId, target);
        } catch (Exception exception) {
            log.error(
                "evaluateTrace failed to load trace/rules for trace {} in workspace {} (target={})",
                traceId, workspaceId, target, exception);

            incrementTopLevelFailureCounter(target, "load");

            recordTopLevelFailureBreadcrumbs(traceId, workspaceId, target, exception, Set.of());

            return;
        }

        Set<Long> evaluatedRuleIds = new HashSet<>();

        try {
            runEvaluations(traceId, trace, enabledRules, evaluatedRuleIds);
        } catch (Exception exception) {
            log.error(
                "evaluateTrace runEvaluations failed for trace {} in workspace {} (target={}) — partial " +
                    "scores may have been written before the failure",
                traceId, workspaceId, target, exception);

            incrementTopLevelFailureCounter(target, "run");

            recordTopLevelFailureBreadcrumbs(traceId, workspaceId, target, exception, evaluatedRuleIds);
        }
    }

    private void incrementTopLevelFailureCounter(AiEvalRuleTarget target, String phase) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(TOP_LEVEL_FAILURE_METRIC)
                .tag("target", target.name())
                .tag("phase", phase)
                .register(meterRegistry)
                .increment();
        } catch (RuntimeException meterFailure) {
            log.debug(
                "Failed to increment {} (target={}, phase={}); continuing",
                TOP_LEVEL_FAILURE_METRIC, target, phase, meterFailure);
        }
    }

    private void recordTopLevelFailureBreadcrumbs(
        long traceId, Long workspaceId, AiEvalRuleTarget target, Exception exception,
        Set<Long> alreadyEvaluatedIds) {

        List<AiEvalRule> rulesForBreadcrumbs;

        try {
            rulesForBreadcrumbs =
                workspaceAiEvalRuleService.getEnabledEvalRulesByWorkspaceAndTarget(workspaceId, target);
        } catch (Exception breadcrumbLookupException) {
            log.error(
                "Could not load enabled rules to record top-level failure breadcrumbs for trace {} workspace {}",
                traceId, workspaceId, breadcrumbLookupException);

            return;
        }

        String errorMessage = AiGatewayThrowables.summarize(exception);

        for (AiEvalRule evalRule : rulesForBreadcrumbs) {
            if (alreadyEvaluatedIds.contains(evalRule.getId())) {
                continue;
            }

            try {
                AiEvalExecution evalExecution = new AiEvalExecution(evalRule.getId(), traceId);

                evalExecution = aiEvalExecutionService.create(evalExecution);

                markAsErrorSafely(evalExecution, errorMessage);
            } catch (Exception perRuleBreadcrumbException) {
                log.warn(
                    "Failed to record top-level failure breadcrumb for rule {} on trace {}",
                    evalRule.getId(), traceId, perRuleBreadcrumbException);
            }
        }
    }

    private void runEvaluations(
        long traceId, AiObservabilityTrace trace, List<AiEvalRule> enabledRules, Set<Long> evaluatedRuleIds) {

        for (AiEvalRule evalRule : enabledRules) {
            if (!matchesFilters(evalRule, trace)) {
                continue;
            }

            if (!passesSampling(evalRule)) {
                continue;
            }

            AiEvalExecution evalExecution = new AiEvalExecution(evalRule.getId(), traceId);

            evalExecution = aiEvalExecutionService.create(evalExecution);

            evaluatedRuleIds.add(evalRule.getId());

            if (evalRule.getDelaySeconds() != null && evalRule.getDelaySeconds() > 0) {
                try {
                    Thread.sleep(Duration.ofSeconds(evalRule.getDelaySeconds())
                        .toMillis());
                } catch (InterruptedException interruptedException) {
                    int currentRuleIndex = enabledRules.indexOf(evalRule);

                    log.warn(
                        "evaluateTrace interrupted while waiting for rule {} delay on trace {} — " +
                            "skipping remaining {} rule(s)",
                        evalRule.getId(), traceId, enabledRules.size() - currentRuleIndex - 1);

                    markAsErrorSafely(evalExecution, "Interrupted during delay");

                    String interruptMessage =
                        "evaluateTrace interrupted at rule " + evalRule.getId() + " — skipped";

                    for (int remaining = currentRuleIndex + 1; remaining < enabledRules.size(); remaining++) {
                        AiEvalRule remainingRule = enabledRules.get(remaining);

                        try {
                            AiEvalExecution skippedExecution =
                                aiEvalExecutionService.create(new AiEvalExecution(remainingRule.getId(), traceId));

                            markAsErrorSafely(skippedExecution, interruptMessage);
                        } catch (Exception breadcrumbFailure) {
                            log.warn(
                                "Failed to record interrupt breadcrumb for rule {} on trace {}",
                                remainingRule.getId(), traceId, breadcrumbFailure);
                        }
                    }

                    Thread.currentThread()
                        .interrupt();

                    return;
                }
            }

            executeEvaluation(evalExecution, evalRule, trace);
        }
    }

    String buildPrompt(String promptTemplate, AiObservabilityTrace trace, List<AiObservabilitySpan> spans) {
        String result = promptTemplate;

        result = result.replace("{{input}}", trace.getInput() != null ? trace.getInput() : "");
        result = result.replace("{{output}}", trace.getOutput() != null ? trace.getOutput() : "");
        result = result.replace("{{metadata}}", trace.getMetadata() != null ? trace.getMetadata() : "");
        result = result.replace("{{context}}", buildContext(spans));

        return result;
    }

    private String buildContext(List<AiObservabilitySpan> spans) {
        return spans.stream()
            .filter(span -> span.getType() == AiObservabilitySpanType.RETRIEVAL)
            .map(AiObservabilitySpan::getOutput)
            .filter(output -> output != null && !output.isBlank())
            .collect(Collectors.joining("\n\n"));
    }

    private ChatClient guardedChatClient(ChatModel chatModel, @Nullable Long workspaceId) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);

        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = aiGuardrailsAdvisorProviderProvider.getIfAvailable();

        if (aiGuardrailsAdvisorProvider != null) {
            aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(workspaceId, GuardrailSurface.AI_EVAL)
                .ifPresent(builder::defaultAdvisors);
        }

        return builder.build();
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    private void executeEvaluation(
        AiEvalExecution evalExecution, AiEvalRule evalRule, AiObservabilityTrace trace) {

        try {
            String ruleModel = evalRule.getModel();

            if (ruleModel == null || ruleModel.isBlank()) {
                throw new MissingModelException(
                    "Eval rule " + evalRule.getId() + " has no model identifier — refusing to dispatch evaluation");
            }

            String[] modelParts = ruleModel.split("/", 2);
            String providerName = modelParts[0];

            AiGatewayProvider provider = aiGatewayProviderService.getProviders()
                .stream()
                .filter(
                    gatewayProvider -> providerName.equalsIgnoreCase(gatewayProvider.getName()))
                .findFirst()
                .orElseThrow(
                    () -> new ProviderNotFoundException(
                        "No provider found with name: " + providerName));

            ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);

            List<AiObservabilitySpan> spans = aiObservabilitySpanService.getSpansByTrace(trace.getId());

            String promptText = buildPrompt(evalRule.getPromptTemplate(), trace, spans);

            ChatResponse chatResponse = guardedChatClient(chatModel, trace.getWorkspaceId())
                .prompt(promptText)
                .call()
                .chatResponse();

            Generation generation = chatResponse == null ? null : chatResponse.getResult();

            if (generation == null) {
                throw new IllegalStateException(
                    "Evaluation model returned no result for rule " + evalRule.getId());
            }

            String responseContent = generation.getOutput()
                .getText();

            if (responseContent == null) {
                throw new IllegalStateException(
                    "Evaluation model returned no text for rule " + evalRule.getId());
            }

            AiEvalScoreConfig scoreConfig;

            try {
                scoreConfig = aiEvalScoreConfigService.getScoreConfig(evalRule.getScoreConfigId());
            } catch (IllegalArgumentException scoreConfigLookupFailure) {
                throw new ScoreConfigNotFoundException(
                    "AiEvalScoreConfig not found for rule " + evalRule.getId(), scoreConfigLookupFailure);
            }

            AiEvalScore score = buildScoreFromResponse(trace, scoreConfig, responseContent);

            score.setEvalRuleId(evalRule.getId());
            score.setCreatedBy("system");

            AiEvalScore savedScore =
                workspaceAiEvalScoreService.createInWorkspace(score,
                    workspaceAiObservabilityTraceService.getWorkspaceId(trace.getId()));

            evalExecution.markCompleted();
            evalExecution.setScoreId(savedScore.getId());

            aiEvalExecutionService.update(evalExecution);
        } catch (Exception exception) {
            log.error("Evaluation failed for rule {} on trace {}", evalRule.getId(), trace.getId(), exception);

            markAsErrorSafely(
                evalExecution, AiGatewayThrowables.summarize(exception), classifyEvaluationFailure(exception));
        }
    }

    private static AiEvalExecutionFailureReason classifyEvaluationFailure(Exception exception) {
        if (exception instanceof MissingModelException) {
            return AiEvalExecutionFailureReason.MISSING_MODEL;
        }

        if (exception instanceof ProviderNotFoundException) {
            return AiEvalExecutionFailureReason.PROVIDER_NOT_FOUND;
        }

        if (exception instanceof ScoreConfigNotFoundException) {
            return AiEvalExecutionFailureReason.SCORE_CONFIG_NOT_FOUND;
        }

        if (exception instanceof IllegalStateException) {
            return AiEvalExecutionFailureReason.LLM_RESPONSE_PARSE_FAILED;
        }

        if (exception instanceof DataAccessException) {
            return AiEvalExecutionFailureReason.PERSISTENCE_FAILED;
        }

        if (exception instanceof RestClientException) {
            return wasRetryAttempted(exception)
                ? AiEvalExecutionFailureReason.PROVIDER_REQUEST_FAILED_AFTER_RETRIES
                : AiEvalExecutionFailureReason.PROVIDER_REQUEST_FAILED;
        }

        return AiEvalExecutionFailureReason.UNKNOWN;
    }

    private static boolean wasRetryAttempted(Throwable boundary) {
        Throwable current = boundary;

        while (current != null) {
            Throwable[] suppressed = current.getSuppressed();

            if (suppressed.length > 0) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private void markAsErrorSafely(AiEvalExecution evalExecution, String errorMessage) {
        markAsErrorSafely(evalExecution, errorMessage, AiEvalExecutionFailureReason.UNKNOWN);
    }

    private void markAsErrorSafely(
        AiEvalExecution evalExecution, String errorMessage, AiEvalExecutionFailureReason failureReason) {

        try {
            evalExecution.markErrored(failureReason, errorMessage);
        } catch (IllegalArgumentException illegalFsmTransition) {
            log.warn(
                "AiEvalExecution {} not transitioned to ERROR (already terminal): {}",
                evalExecution.getId(), illegalFsmTransition.getMessage());

            return;
        }

        try {
            aiEvalExecutionService.update(evalExecution);
        } catch (Exception updateException) {
            log.error(
                "Failed to mark AiEvalExecution {} as ERROR — row will be stranded in PENDING. Requires ops reaper.",
                evalExecution.getId(), updateException);
        }
    }

    private AiEvalScore buildScoreFromResponse(
        AiObservabilityTrace trace, AiEvalScoreConfig scoreConfig, String responseContent) {

        String trimmedResponse = responseContent.trim();

        AiEvalScoreDataType dataType =
            scoreConfig.getDataType() != null ? scoreConfig.getDataType() : AiEvalScoreDataType.NUMERIC;

        return switch (dataType) {
            case NUMERIC -> {
                BigDecimal parsed;

                try {
                    parsed = new BigDecimal(trimmedResponse);
                } catch (NumberFormatException numberFormatException) {
                    throw new IllegalStateException(
                        "Failed to parse numeric score from LLM response: " + trimmedResponse,
                        numberFormatException);
                }

                yield AiEvalScore.numeric(
                    trace.getId(), scoreConfig.getName(), AiEvalScoreSource.LLM_JUDGE, parsed);
            }
            case BOOLEAN -> {
                String lowerResponse = trimmedResponse.toLowerCase();
                boolean isTrue = lowerResponse.equals("true") || lowerResponse.equals("yes")
                    || lowerResponse.equals("1");
                boolean isFalse = lowerResponse.equals("false") || lowerResponse.equals("no")
                    || lowerResponse.equals("0");

                if (!isTrue && !isFalse) {
                    throw new IllegalStateException(
                        "Failed to parse boolean score from LLM response: " + trimmedResponse);
                }

                yield AiEvalScore.bool(
                    trace.getId(), scoreConfig.getName(), AiEvalScoreSource.LLM_JUDGE, isTrue);
            }
            case CATEGORICAL -> AiEvalScore.categorical(
                trace.getId(), scoreConfig.getName(), AiEvalScoreSource.LLM_JUDGE, trimmedResponse);
        };
    }

    @SuppressWarnings("unchecked")
    private boolean matchesFilters(AiEvalRule evalRule, AiObservabilityTrace trace) {
        String filtersJson = evalRule.getFilters();

        if (filtersJson == null || filtersJson.isBlank()) {
            return true;
        }

        Map<String, Object> filters;

        try {
            filters = JsonUtils.read(filtersJson, Map.class);
        } catch (Exception exception) {
            log.error(
                "Failed to parse filters for rule {} ({}); skipping rule to avoid matching every trace",
                evalRule.getId(), filtersJson, exception);

            return false;
        }

        if (filters == null || filters.isEmpty()) {
            return true;
        }

        Map<String, Object> parsedMetadata = parseMetadata(trace.getMetadata());
        boolean metadataUnparseable = parsedMetadata == null;
        Map<String, Object> resolutionMetadata = metadataUnparseable ? Map.of() : parsedMetadata;

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object expected = entry.getValue();

            if (expected == null) {
                continue;
            }

            if (metadataUnparseable && requiresMetadata(key)) {
                log.warn(
                    "Skipping rule {} for trace {}: filter key '{}' requires metadata but trace metadata is " +
                        "unparseable JSON",
                    evalRule.getId(), trace.getId(), key);

                return false;
            }

            String expectedString = String.valueOf(expected);
            String actual = resolveTraceAttribute(trace, resolutionMetadata, key);

            if (actual == null || !actual.equals(expectedString)) {
                return false;
            }
        }

        return true;
    }

    private static boolean requiresMetadata(String filterKey) {
        return switch (filterKey) {
            case "user", "userId", "name", "source", "status" -> false;
            default -> true;
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }

        try {
            Map<String, Object> parsed = JsonUtils.read(metadataJson, Map.class);

            return parsed == null ? Map.of() : parsed;
        } catch (JacksonException | IllegalArgumentException exception) {
            log.error(
                "Failed to parse trace metadata JSON (length={}, sha256Prefix={}); rules with metadata filters will " +
                    "be skipped for this trace",
                metadataJson.length(), sha256Prefix(metadataJson), exception);

            return null;
        }
    }

    private static String sha256Prefix(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of()
                .formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException noSha256OnThisJvm) {
            throw new AssertionError("SHA-256 unavailable on this JVM", noSha256OnThisJvm);
        }
    }

    private String resolveTraceAttribute(AiObservabilityTrace trace, Map<String, Object> metadata, String key) {
        switch (key) {
            case "user", "userId" -> {
                return trace.getUserId();
            }
            case "name" -> {
                return trace.getName();
            }
            case "source" -> {
                return trace.getSource() == null ? null : trace.getSource()
                    .name();
            }
            case "status" -> {
                return trace.getStatus() == null ? null : trace.getStatus()
                    .name();
            }
            case "environment", "model", "provider" -> {
                Object metadataValue = metadata.get(key);

                return metadataValue == null ? null : String.valueOf(metadataValue);
            }
            default -> {
                Object metadataValue = metadata.get(key);

                return metadataValue == null ? null : String.valueOf(metadataValue);
            }
        }
    }

    @SuppressFBWarnings("PREDICTABLE_RANDOM")
    private boolean passesSampling(AiEvalRule evalRule) {
        BigDecimal samplingRate = evalRule.getSamplingRate();

        if (samplingRate.compareTo(BigDecimal.ONE) >= 0) {
            return true;
        }

        if (samplingRate.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        return ThreadLocalRandom.current()
            .nextDouble() < samplingRate.doubleValue();
    }
}
