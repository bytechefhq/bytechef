/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.evaluation;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalExecution;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalExecutionStatus;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalRule;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScore;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreConfig;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreDataType;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.automation.ai.gateway.service.AiEvalExecutionService;
import com.bytechef.ee.automation.ai.gateway.service.AiEvalRuleService;
import com.bytechef.ee.automation.ai.gateway.service.AiEvalScoreConfigService;
import com.bytechef.ee.automation.ai.gateway.service.AiEvalScoreService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Executes LLM-as-judge evaluations for completed traces. For each enabled eval rule that matches the trace and passes
 * sampling, builds a prompt from the rule's template, calls the specified model via the gateway's own
 * {@link AiGatewayChatModelFactory}, parses the response as a score, and persists the result.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class AiEvalExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AiEvalExecutor.class);

    private final AiEvalExecutionService aiEvalExecutionService;
    private final AiEvalRuleService aiEvalRuleService;
    private final AiEvalScoreConfigService aiEvalScoreConfigService;
    private final AiEvalScoreService aiEvalScoreService;
    private final AiGatewayChatModelFactory aiGatewayChatModelFactory;
    private final AiGatewayProviderService aiGatewayProviderService;
    private final AiObservabilityTraceService aiObservabilityTraceService;

    public AiEvalExecutor(
        AiEvalExecutionService aiEvalExecutionService,
        AiEvalRuleService aiEvalRuleService,
        AiEvalScoreConfigService aiEvalScoreConfigService,
        AiEvalScoreService aiEvalScoreService,
        AiGatewayChatModelFactory aiGatewayChatModelFactory,
        AiGatewayProviderService aiGatewayProviderService,
        AiObservabilityTraceService aiObservabilityTraceService) {

        this.aiEvalExecutionService = aiEvalExecutionService;
        this.aiEvalRuleService = aiEvalRuleService;
        this.aiEvalScoreConfigService = aiEvalScoreConfigService;
        this.aiEvalScoreService = aiEvalScoreService;
        this.aiGatewayChatModelFactory = aiGatewayChatModelFactory;
        this.aiGatewayProviderService = aiGatewayProviderService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
    }

    /**
     * Re-runs a specific eval rule against a historical trace, bypassing sampling and delay. Used by the "Run on
     * History" batch re-evaluation flow.
     */
    @Async
    public void evaluateTraceForRule(long traceId, long evalRuleId) {
        try {
            AiObservabilityTrace trace = aiObservabilityTraceService.getTrace(traceId);
            AiEvalRule evalRule = aiEvalRuleService.getEvalRule(evalRuleId);

            if (!matchesFilters(evalRule, trace)) {
                logger.debug("Trace {} does not match filters for rule {}; skipping", traceId, evalRuleId);

                return;
            }

            AiEvalExecution evalExecution = new AiEvalExecution(evalRule.getId(), traceId);

            evalExecution = aiEvalExecutionService.create(evalExecution);

            executeEvaluation(evalExecution, evalRule, trace);
        } catch (Exception exception) {
            logger.error("Historical evaluation failed for rule {} on trace {}", evalRuleId, traceId, exception);
        }
    }

    @Async
    public void evaluateTrace(long traceId, Long workspaceId) {
        try {
            AiObservabilityTrace trace = aiObservabilityTraceService.getTrace(traceId);

            List<AiEvalRule> enabledRules = aiEvalRuleService.getEnabledEvalRulesByWorkspace(workspaceId);

            for (AiEvalRule evalRule : enabledRules) {
                if (!matchesFilters(evalRule, trace)) {
                    continue;
                }

                if (!passesSampling(evalRule)) {
                    continue;
                }

                AiEvalExecution evalExecution = new AiEvalExecution(evalRule.getId(), traceId);

                evalExecution = aiEvalExecutionService.create(evalExecution);

                if (evalRule.getDelaySeconds() != null && evalRule.getDelaySeconds() > 0) {
                    try {
                        Thread.sleep(Duration.ofSeconds(evalRule.getDelaySeconds())
                            .toMillis());
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread()
                            .interrupt();

                        markAsErrorSafely(evalExecution, "Interrupted during delay");

                        logger.warn(
                            "evaluateTrace interrupted while waiting for rule {} delay on trace {} — " +
                                "skipping remaining {} rule(s)",
                            evalRule.getId(), traceId, enabledRules.size() - enabledRules.indexOf(evalRule) - 1);

                        return;
                    }
                }

                executeEvaluation(evalExecution, evalRule, trace);
            }
        } catch (Exception exception) {
            logger.error(
                "evaluateTrace failed for trace {} in workspace {} — no eval rules applied", traceId, workspaceId,
                exception);
        }
    }

    private String buildPrompt(String promptTemplate, AiObservabilityTrace trace) {
        String result = promptTemplate;

        result = result.replace("{{input}}", trace.getInput() != null ? trace.getInput() : "");
        result = result.replace("{{output}}", trace.getOutput() != null ? trace.getOutput() : "");
        result = result.replace("{{metadata}}", trace.getMetadata() != null ? trace.getMetadata() : "");

        return result;
    }

    private void executeEvaluation(
        AiEvalExecution evalExecution, AiEvalRule evalRule, AiObservabilityTrace trace) {

        try {
            String[] modelParts = evalRule.getModel()
                .split("/", 2);
            String providerName = modelParts[0];

            AiGatewayProvider provider = aiGatewayProviderService.getProviders()
                .stream()
                .filter(
                    gatewayProvider -> providerName.equalsIgnoreCase(gatewayProvider.getName()))
                .findFirst()
                .orElseThrow(
                    () -> new IllegalArgumentException(
                        "No provider found with name: " + providerName));

            ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);

            String promptText = buildPrompt(evalRule.getPromptTemplate(), trace);

            ChatResponse chatResponse = chatModel.call(new Prompt(promptText));

            String responseContent = chatResponse.getResult()
                .getOutput()
                .getText();

            AiEvalScoreConfig scoreConfig = aiEvalScoreConfigService.getScoreConfig(evalRule.getScoreConfigId());

            AiEvalScore score = buildScoreFromResponse(trace, scoreConfig, responseContent);

            score.setEvalRuleId(evalRule.getId());
            score.setCreatedBy("system");

            AiEvalScore savedScore = aiEvalScoreService.create(score);

            evalExecution.setStatus(AiEvalExecutionStatus.COMPLETED);
            evalExecution.setScoreId(savedScore.getId());

            aiEvalExecutionService.update(evalExecution);
        } catch (Exception exception) {
            logger.error("Evaluation failed for rule {} on trace {}", evalRule.getId(), trace.getId(), exception);

            markAsErrorSafely(evalExecution, exception.getMessage());
        }
    }

    /**
     * Best-effort transition to ERROR. If the update itself throws (e.g., DB outage), log loudly so ops can reap the
     * stranded PENDING row instead of leaving an invisible "in-flight forever" state. Without this wrapper, a failing
     * {@code update()} call escapes the inner catch into the outer {@code evaluateTrace} catch, which only logs and
     * leaves the row in PENDING — a common regression after DB connection-pool exhaustion.
     */
    private void markAsErrorSafely(AiEvalExecution evalExecution, String errorMessage) {
        evalExecution.setStatus(AiEvalExecutionStatus.ERROR);
        evalExecution.setErrorMessage(errorMessage);

        try {
            aiEvalExecutionService.update(evalExecution);
        } catch (Exception updateException) {
            logger.error(
                "Failed to mark AiEvalExecution {} as ERROR — row will be stranded in PENDING. Requires ops reaper.",
                evalExecution.getId(), updateException);
        }
    }

    /**
     * Builds an {@link AiEvalScore} from the LLM's raw response via the typed factories, so the
     * {@code (dataType, value, stringValue)} triple cannot drift out of sync.
     */
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
                    // Surface the failure via the execution status so averages, thresholds, and alerts are not
                    // skewed by a fabricated zero. Callers catch IllegalStateException → mark execution ERROR.
                    throw new IllegalStateException(
                        "Failed to parse numeric score from LLM response: " + trimmedResponse,
                        numberFormatException);
                }

                yield AiEvalScore.numeric(
                    trace.getWorkspaceId(), trace.getId(), scoreConfig.getName(),
                    AiEvalScoreSource.LLM_JUDGE, parsed);
            }
            case BOOLEAN -> {
                String lowerResponse = trimmedResponse.toLowerCase();
                boolean booleanValue =
                    lowerResponse.equals("true") || lowerResponse.equals("yes") || lowerResponse.equals("1");

                yield AiEvalScore.bool(
                    trace.getWorkspaceId(), trace.getId(), scoreConfig.getName(),
                    AiEvalScoreSource.LLM_JUDGE, booleanValue);
            }
            case CATEGORICAL -> AiEvalScore.categorical(
                trace.getWorkspaceId(), trace.getId(), scoreConfig.getName(),
                AiEvalScoreSource.LLM_JUDGE, trimmedResponse);
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
            logger.error(
                "Failed to parse filters for rule {} ({}); skipping rule to avoid matching every trace",
                evalRule.getId(), filtersJson, exception);

            return false;
        }

        if (filters == null || filters.isEmpty()) {
            return true;
        }

        Map<String, Object> metadata = parseMetadata(trace.getMetadata());

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object expected = entry.getValue();

            if (expected == null) {
                continue;
            }

            String expectedString = String.valueOf(expected);
            String actual = resolveTraceAttribute(trace, metadata, key);

            if (actual == null || !actual.equals(expectedString)) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }

        try {
            Map<String, Object> parsed = JsonUtils.read(metadataJson, Map.class);

            return parsed == null ? Map.of() : parsed;
        } catch (Exception exception) {
            logger.error(
                "Failed to parse trace metadata JSON; eval filters on metadata keys will not match", exception);

            return Map.of();
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
