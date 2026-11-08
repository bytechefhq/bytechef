/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.executor;

import com.bytechef.ee.automation.ai.eval.experiment.service.WorkspaceAiEvalExperimentService;
import com.bytechef.ee.automation.ai.gateway.evaluation.AiEvalExecutor;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetItemService;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRuleTarget;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayThrowables;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.facade.AiObservabilityTracingHeaders;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings({
    "EI", "REC_CATCH_EXCEPTION", "CT_CONSTRUCTOR_THROW"
})
public class AiEvalExperimentExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiEvalExperimentExecutor.class);

    private static final String EVAL_DISPATCH_FAIL_METRIC = "bytechef_ai_eval_experiment_eval_dispatch_failures";
    private static final String TRACE_PERSIST_MISSING_METRIC = "bytechef_ai_eval_experiment_trace_persist_missing";
    private static final String BOOKKEEPING_FAILURE_METRIC = "bytechef_ai_eval_experiment_bookkeeping_failures";

    private final AiEvalDatasetItemService aiEvalDatasetItemService;
    private final AiEvalExecutor aiEvalExecutor;
    private final AiEvalExperimentRunService aiEvalExperimentRunService;
    private final AiEvalExperimentService aiEvalExperimentService;
    private final WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService;
    private final AiGatewayFacade aiGatewayFacade;
    private final WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;
    private final RetryTemplate chatCompletionRetryTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private final int stopPollEveryN;

    public AiEvalExperimentExecutor(
        AiEvalDatasetItemService aiEvalDatasetItemService, AiEvalExecutor aiEvalExecutor,
        AiEvalExperimentRunService aiEvalExperimentRunService, AiEvalExperimentService aiEvalExperimentService,
        WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService,
        AiGatewayFacade aiGatewayFacade,
        WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService,
        RetryTemplate chatCompletionRetryTemplate, ObjectMapper objectMapper,
        ObjectProvider<MeterRegistry> meterRegistryProvider,
        @Value("${bytechef.ai.gateway.experiment.stop-poll-every-n:1}") int stopPollEveryN) {

        this.aiEvalDatasetItemService = aiEvalDatasetItemService;
        this.aiEvalExecutor = aiEvalExecutor;
        this.aiEvalExperimentRunService = aiEvalExperimentRunService;
        this.aiEvalExperimentService = aiEvalExperimentService;
        this.workspaceAiEvalExperimentService = workspaceAiEvalExperimentService;
        this.aiGatewayFacade = aiGatewayFacade;
        this.workspaceAiObservabilityTraceService = workspaceAiObservabilityTraceService;
        this.chatCompletionRetryTemplate = chatCompletionRetryTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.stopPollEveryN = Math.max(1, stopPollEveryN);
    }

    @Async("aiEvalExperimentTaskExecutor")
    public void execute(long experimentId) {
        AiEvalExperiment experiment;

        try {
            experiment = aiEvalExperimentService.markRunning(experimentId);
        } catch (IllegalArgumentException rowGone) {
            log.error(
                "Failed to mark experiment {} as RUNNING (row gone — skipping markFinished)",
                experimentId, rowGone);

            return;
        } catch (Exception markRunningFailure) {
            log.error(
                "Failed to mark experiment {} as RUNNING; failing it instead", experimentId, markRunningFailure);

            try {
                aiEvalExperimentService.markFinished(experimentId, true);
            } catch (RuntimeException secondaryFailure) {
                log.error(
                    "Could not mark experiment {} as FAILED after markRunning failure", experimentId,
                    secondaryFailure);
            }

            return;
        } catch (Error markRunningError) {
            log.error(
                "Experiment {} markRunning hit a JVM Error — propagating without markFinished",
                experimentId, markRunningError);

            throw markRunningError;
        }

        try {
            List<AiEvalDatasetItem> items =
                aiEvalDatasetItemService.getItemsByVersion(experiment.getDatasetVersionId());

            List<AiEvalExperimentRun> runs = items.stream()
                .map(item -> aiEvalExperimentRunService.create(new AiEvalExperimentRun(experimentId, item.getId())))
                .toList();

            boolean anyFailed = false;

            for (int index = 0; index < runs.size(); index++) {
                if (index == 0 || index % stopPollEveryN == 0) {
                    AiEvalExperiment fresh = aiEvalExperimentService.getExperiment(experimentId);

                    if (fresh.isStopRequested()) {
                        log.info(
                            "Experiment {} stopped by user after {}/{} runs",
                            experimentId, index, runs.size());

                        for (int remaining = index; remaining < runs.size(); remaining++) {
                            try {
                                aiEvalExperimentRunService.fail(
                                    runs.get(remaining)
                                        .getId(),
                                    remaining == index
                                        ? "Cancelled by user before run started"
                                        : "Cancelled — experiment stopped by user");
                            } catch (Exception exception) {
                                log.warn(
                                    "Failed to mark run {} as cancelled",
                                    runs.get(remaining)
                                        .getId(),
                                    exception);
                            }
                        }

                        anyFailed = true;

                        break;
                    }
                }

                AiEvalExperimentRun run = runs.get(index);
                AiEvalDatasetItem item = items.get(index);

                if (!replayItem(experiment, run, item)) {
                    anyFailed = true;
                }
            }

            try {
                aiEvalExperimentService.markFinished(experimentId, anyFailed);
            } catch (RuntimeException finalizationFailure) {
                log.error(
                    "Could not mark experiment {} as finished (anyFailed={}) — orphan reaper will sweep after the " +
                        "configured threshold elapses",
                    experimentId, anyFailed, finalizationFailure);
            }
        } catch (Exception executionFailure) {
            log.error(
                "Experiment {} execution failed with uncaught exception — transitioning to FAILED", experimentId,
                executionFailure);

            try {
                aiEvalExperimentService.markFinished(experimentId, true);
            } catch (RuntimeException finalizationFailure) {
                log.error(
                    "Could not mark experiment {} as FAILED after execution failure — orphan reaper will sweep " +
                        "after the configured threshold elapses",
                    experimentId, finalizationFailure);
            }
        } catch (Error executionError) {
            log.error(
                "Experiment {} execution hit a JVM Error — propagating to UncaughtExceptionHandler without " +
                    "attempting markFinished",
                experimentId, executionError);

            throw executionError;
        }
    }

    static int clampLatency(long latencyMs) {
        if (latencyMs < 0) {
            return 0;
        }

        return (int) Math.min(latencyMs, Integer.MAX_VALUE);
    }

    private boolean replayItem(AiEvalExperiment experiment, AiEvalExperimentRun run, AiEvalDatasetItem item) {
        long startTime = System.currentTimeMillis();

        try {
            aiEvalExperimentRunService.markRunning(run.getId());

            AiGatewayChatCompletionRequest request = buildRequest(experiment, item);
            String externalTraceId = UUID.randomUUID()
                .toString();
            AiObservabilityTracingHeaders tracingHeaders = new AiObservabilityTracingHeaders(
                externalTraceId, null, "experiment-replay", null, null, Map.of(), List.of(),
                AiObservabilityTraceSource.EXPERIMENT);

            chatCompletionRetryTemplate.execute(context -> {
                aiGatewayFacade.chatCompletion(request, tracingHeaders, null);

                return null;
            });

            long latencyMs = System.currentTimeMillis() - startTime;

            Long traceId = null;
            BigDecimal cost = null;
            Integer resolvedLatencyMs = clampLatency(latencyMs);

            Optional<AiObservabilityTrace> trace = workspaceAiObservabilityTraceService.findByExternalTraceId(
                workspaceAiEvalExperimentService.getWorkspaceId(experiment.getId()), externalTraceId);

            if (trace.isPresent()) {
                AiObservabilityTrace persisted = trace.get();

                traceId = persisted.getId();

                if (persisted.getTotalCost() != null) {
                    cost = persisted.getTotalCost();
                }

                if (persisted.getTotalLatencyMs() != null) {
                    resolvedLatencyMs = persisted.getTotalLatencyMs();
                }
            } else {
                log.warn(
                    "Experiment {} run {} completed chatCompletion but trace with externalTraceId={} was not " +
                        "persisted - experiment-scoped eval will be skipped",
                    experiment.getId(), run.getId(), externalTraceId);

                incrementTracePersistMissingCounter();
            }

            try {
                aiEvalExperimentRunService.complete(run.getId(), traceId, resolvedLatencyMs, cost);
            } catch (Exception bookkeepingException) {
                log.error(
                    "Experiment {} run {} chat completion succeeded but bookkeeping write (complete) failed - " +
                        "run will be stranded in RUNNING until the orphan reaper sweeps",
                    experiment.getId(), run.getId(), bookkeepingException);

                incrementBookkeepingFailureCounter();

                return true;
            }

            if (traceId != null) {
                try {
                    aiEvalExecutor.evaluateTrace(
                        traceId, workspaceAiEvalExperimentService.getWorkspaceId(experiment.getId()),
                        AiEvalRuleTarget.EXPERIMENT_TRACE);
                } catch (TaskRejectedException taskRejected) {
                    log.error(
                        "Eval dispatch rejected (queue saturated) for experiment {} run {} (traceId={}) - run " +
                            "completed but no eval scores will be produced",
                        experiment.getId(), run.getId(), traceId, taskRejected);

                    incrementEvalDispatchFailureCounter("queue_saturated");
                } catch (Exception evalException) {
                    log.error(
                        "Failed to dispatch experiment-scoped eval for experiment {} run {} (traceId={})",
                        experiment.getId(), run.getId(), traceId, evalException);

                    incrementEvalDispatchFailureCounter("dispatch_error");
                }
            }

            return true;
        } catch (Exception exception) {
            log.error(
                "Experiment {} run {} failed (item {})", experiment.getId(), run.getId(), item.getId(), exception);

            try {
                aiEvalExperimentRunService.fail(run.getId(), AiGatewayThrowables.summarize(exception));
            } catch (RuntimeException failException) {
                log.error(
                    "Failed to transition experiment {} run {} to FAILED — row will be stranded",
                    experiment.getId(), run.getId(), failException);
            }

            return false;
        }
    }

    private AiGatewayChatCompletionRequest buildRequest(AiEvalExperiment experiment, AiEvalDatasetItem item) {
        AiGatewayChatCompletionRequest parsed;

        try {
            parsed = objectMapper.readValue(item.getInput(), AiGatewayChatCompletionRequest.class);
        } catch (RuntimeException parseException) {
            throw new IllegalStateException(
                "Failed to parse dataset item " + item.getId() + " input as chat completion request",
                parseException);
        }

        String effectiveModel = experiment.getModel() != null ? experiment.getModel() : parsed.model();

        Map<String, String> tags = new HashMap<>(
            parsed.tags() == null ? Map.of() : parsed.tags());

        tags.put("workspace_id", String.valueOf(workspaceAiEvalExperimentService.getWorkspaceId(experiment.getId())));

        return new AiGatewayChatCompletionRequest(
            effectiveModel, parsed.messages(), parsed.temperature(), parsed.maxTokens(), parsed.topP(),
            false, parsed.routingPolicy(), parsed.cache(), parsed.toolChoice(), parsed.tools(), tags);
    }

    private void incrementEvalDispatchFailureCounter(String reasonTag) {
        incrementCounterQuietly(EVAL_DISPATCH_FAIL_METRIC, "reason", reasonTag);
    }

    private void incrementTracePersistMissingCounter() {
        incrementCounterQuietly(TRACE_PERSIST_MISSING_METRIC, null, null);
    }

    private void incrementBookkeepingFailureCounter() {
        incrementCounterQuietly(BOOKKEEPING_FAILURE_METRIC, null, null);
    }

    private void incrementCounterQuietly(String metricName, String tagKey, String tagValue) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.Builder builder = Counter.builder(metricName);

            if (tagKey != null) {
                builder = builder.tag(tagKey, tagValue);
            }

            builder.register(meterRegistry)
                .increment();
        } catch (RuntimeException meterFailure) {
            log.debug(
                "Failed to increment counter {} (tag {}={}); continuing",
                metricName, tagKey, tagValue, meterFailure);
        }
    }
}
