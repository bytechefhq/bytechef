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
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Executes experiments in the background using Spring {@link Async}. For each dataset item in the experiment's version,
 * replays the item's input through {@link AiGatewayFacade#chatCompletion}, records the resulting trace, and updates the
 * corresponding {@code ai_eval_experiment_run} row. Does NOT use the atlas-coordinator pipeline — a deliberate design
 * choice for single-JVM simplicity. Horizontal scale can migrate to atlas as a follow-up.
 *
 * <p>
 * Each replay stamps a freshly-minted UUID as the external trace id in {@link AiObservabilityTracingHeaders#traceId()}
 * so the facade's trace creation is addressable after the call. The numeric {@code AiObservabilityTrace.id}, latency,
 * and cost are then read back via {@link AiObservabilityTraceService#findByExternalTraceId}. Once the trace is in hand,
 * the executor fires an experiment-scoped evaluation via
 * {@link AiEvalExecutor#evaluateTrace(long, Long, AiEvalRuleTarget)} with {@link AiEvalRuleTarget#EXPERIMENT_TRACE} so
 * only experiment-targeted rules score the synthetic trace.
 *
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
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;
    private final RetryTemplate chatCompletionRetryTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    /**
     * How often (in iterations) to poll the experiment row for {@code stop_requested}. The N+1 DB read pattern
     * dominates async-thread time on 1000-item experiments under network latency; this property lets operators trade
     * off "stop responsiveness" (low N) against "throughput on large batches" (high N). Default 1 keeps the existing
     * per-iteration behaviour and the existing test contract; values >1 amortize the read across N items. Effective
     * cadence: {@code stopPollEveryN=10} = poll on iteration 0, 10, 20, ... so a stop click takes up to 10
     * per-item-replay times to converge. The first iteration always polls (covers the "stop clicked before dispatch"
     * case); the last is implicitly covered by the loop exit.
     */
    private final int stopPollEveryN;

    public AiEvalExperimentExecutor(
        AiEvalDatasetItemService aiEvalDatasetItemService, AiEvalExecutor aiEvalExecutor,
        AiEvalExperimentRunService aiEvalExperimentRunService, AiEvalExperimentService aiEvalExperimentService,
        WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService,
        AiGatewayFacade aiGatewayFacade, AiObservabilityTraceService aiObservabilityTraceService,
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
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.workspaceAiObservabilityTraceService = workspaceAiObservabilityTraceService;
        this.chatCompletionRetryTemplate = chatCompletionRetryTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        // Defensive lower bound: 0 / negative would skip stop polling entirely and silently break the stop button.
        this.stopPollEveryN = Math.max(1, stopPollEveryN);
    }

    /**
     * Fires execution in the background. Returns immediately; callers can poll experiment status.
     *
     * <p>
     * Lifecycle invariant: this method makes a best-effort attempt to converge the experiment to a terminal state
     * (COMPLETED / FAILED) on every exit path. If both the primary failure and the recovery {@code markFinished} call
     * fail, the experiment is left in PENDING/RUNNING for the orphan reaper to sweep after the configured threshold.
     * The catch-all in this method is the primary failure-handling boundary; Spring's default
     * {@code SimpleAsyncUncaughtExceptionHandler} only logs, so any escape from the catch-all would silently strand the
     * experiment until the reaper sweep.
     */
    @Async("aiEvalExperimentTaskExecutor")
    public void execute(long experimentId) {
        AiEvalExperiment experiment;

        // Two distinct catch idioms mirror replayItem and AiEvalExperimentOrphanRecoveryRunner.recoverOne so the
        // file is consistent: catch (Exception) for routine failures (markFinished best-effort), separate
        // catch (Error) for JVM-wide distress (rethrow without any further DB write — another write can
        // exhaust the connection pool further). Splitting them visibly in source means a future refactor
        // cannot accidentally swallow OOM through a generic catch (Throwable).
        try {
            experiment = aiEvalExperimentService.markRunning(experimentId);
        } catch (IllegalArgumentException rowGone) {
            // Row gone (delete-during-dispatch race) — a markFinished call would throw the same
            // IllegalArgumentException and produce a misleading second ERROR log within milliseconds.
            // The orphan recovery runner sweeps both PENDING and RUNNING after the configured threshold
            // (see AiEvalExperimentOrphanRecoveryRunner) so the stranded row is recoverable.
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
            // JVM-level distress — propagate without any further DB write. The orphan recovery runner sweeps
            // PENDING / RUNNING rows after the configured threshold so the stranded experiment is recoverable
            // without forcing a write under JVM distress.
            log.error(
                "Experiment {} markRunning hit a JVM Error — propagating without markFinished",
                experimentId, markRunningError);

            throw markRunningError;
        }

        // Outer catch-all: any uncaught exception (DB hiccup in getItemsByVersion, NPE in the per-iteration
        // loop, etc.) must transition the experiment to FAILED instead of leaving it stranded in RUNNING until
        // the orphan reaper sweeps after the configured threshold. catch (Exception) + separate catch (Error),
        // matching the replayItem discipline.
        try {
            List<AiEvalDatasetItem> items =
                aiEvalDatasetItemService.getItemsByVersion(experiment.getDatasetVersionId());

            // Pre-create one experiment_run row per dataset item in PENDING state.
            List<AiEvalExperimentRun> runs = items.stream()
                .map(item -> aiEvalExperimentRunService.create(new AiEvalExperimentRun(experimentId, item.getId())))
                .toList();

            boolean anyFailed = false;

            for (int index = 0; index < runs.size(); index++) {
                // Stop check polled every {@code stopPollEveryN} iterations (default 1, configurable via
                // {@code bytechef.ai.gateway.experiment.stop-poll-every-n}). Default cadence preserves the
                // existing test contract (AiEvalExperimentExecutorTest#testExecuteHonorsStopRequest,
                // AiEvalExperimentExecutorTest#testStopRequestObservedDuringInFlightChatCompletionDoesNotDoubleMarkRun)
                // — both pin per-iteration polling. Operators with 1000-item experiments under slow networks
                // can raise the value (e.g. 10 = poll every 10th iteration) to amortize the N+1 DB reads
                // against per-item replay latency, accepting up to {@code stopPollEveryN}-item-replay delay
                // before a Stop click is observed. The follow-up plan (tracked in the AI-gateway gap docs) is
                // to bridge stop signalling through an in-memory ConcurrentHashMap<Long, AtomicBoolean>
                // populated by the GraphQL stop mutation, with the DB read as the cross-instance fallback.
                // The first iteration always polls (covers the "stop clicked before dispatch" case).
                if (index == 0 || index % stopPollEveryN == 0) {
                    AiEvalExperiment fresh = aiEvalExperimentService.getExperiment(experimentId);

                    if (fresh.isStopRequested()) {
                        log.info(
                            "Experiment {} stopped by user after {}/{} runs",
                            experimentId, index, runs.size());

                        // Mark current + remaining runs FAILED
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

            // Inner try around the success-path markFinished so a transient DB failure here does not escape into
            // the outer catch (Exception), which would call markFinished AGAIN and risk double-bookkeeping plus a
            // misleading "execution failed" log line for an experiment whose runs all completed. The orphan reaper
            // sweeps the stranded row after the configured threshold elapses.
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
            // JVM-wide distress (OOM / StackOverflow / ThreadDeath). Calling markFinished under these
            // conditions can exhaust the connection pool further, cascade additional failures, and produce a
            // misleading "execution failed — transitioning to FAILED" line suggesting the lifecycle converged
            // when markFinished may have aborted mid-way. Propagate to the executor's UncaughtExceptionHandler
            // so the JVM can shed load or crash cleanly. The orphan recovery runner sweeps the stranded row.
            log.error(
                "Experiment {} execution hit a JVM Error — propagating to UncaughtExceptionHandler without " +
                    "attempting markFinished",
                experimentId, executionError);

            throw executionError;
        }
    }

    /**
     * Clamps a long millisecond duration to {@link Integer#MAX_VALUE} before casting to {@code int}. A hung HTTP
     * backend can produce {@code latencyMs} values exceeding 2^31 (>24.8d), which a naive narrowing cast turns into a
     * NEGATIVE int and surfaces as misleading negative latency in dashboards. The clamp converts overflow into a
     * recognisable "very large" sentinel that operators can filter on. Negative input (clock skew, monotonic violation
     * across NTP step) clamps to zero — negative latency is never a meaningful operational signal. Package-private so
     * {@code AiEvalExperimentExecutorTest} can pin both edges directly without freezing the system clock or wiring up a
     * 24.8d sleep.
     */
    static int clampLatency(long latencyMs) {
        if (latencyMs < 0) {
            return 0;
        }

        return (int) Math.min(latencyMs, Integer.MAX_VALUE);
    }

    /**
     * Performs one replay. Returns {@code true} on success, {@code false} on any failure (including item parse / replay
     * / bookkeeping errors). Callers aggregate these into the experiment-level {@code anyFailed} flag.
     */
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
                // Gateway accepted the chat completion but the synthetic trace was never persisted —
                // gateway-side trace dropping (e.g. transient observability outage). The run still completes
                // successfully because the chat call succeeded, but the experiment-scoped eval is skipped and
                // the UI will show a complete run with no linked trace. The counter lets operators alert when
                // the rate climbs, distinguishing systemic gateway-trace-dropping from individual late writes.
                log.warn(
                    "Experiment {} run {} completed chatCompletion but trace with externalTraceId={} was not " +
                        "persisted — experiment-scoped eval will be skipped",
                    experiment.getId(), run.getId(), externalTraceId);

                incrementTracePersistMissingCounter();
            }

            // The chat completion succeeded by this point — markRunning + chatCompletion + (optional) trace
            // resolution all returned without exception. Wrap the bookkeeping write in its own try/catch so a
            // DB hiccup or optimistic-lock contention does NOT get re-routed through the outer fail() path:
            // the run actually succeeded, and a misleading FAILED row sends operators chasing ghosts. The
            // run will appear stranded in RUNNING until the orphan reaper sweeps, which is the truthful
            // disposition for an unwritten success bookkeeping.
            try {
                aiEvalExperimentRunService.complete(run.getId(), traceId, resolvedLatencyMs, cost);
            } catch (Exception bookkeepingException) {
                log.error(
                    "Experiment {} run {} chat completion succeeded but bookkeeping write (complete) failed — " +
                        "run will be stranded in RUNNING until the orphan reaper sweeps",
                    experiment.getId(), run.getId(), bookkeepingException);

                incrementBookkeepingFailureCounter();

                // Return true: the chat completion did succeed. Misclassifying as a failure would corrupt the
                // experiment-level anyFailed aggregate. The orphan reaper restores the row's terminal status.
                return true;
            }

            if (traceId != null) {
                try {
                    aiEvalExecutor.evaluateTrace(
                        traceId, workspaceAiEvalExperimentService.getWorkspaceId(experiment.getId()),
                        AiEvalRuleTarget.EXPERIMENT_TRACE);
                } catch (org.springframework.core.task.TaskRejectedException taskRejected) {
                    // Queue saturation: the eval dispatch did not even reach the @Async pool. Tag the counter
                    // distinctly so a workspace dashboard can distinguish "the eval pool is overwhelmed" (capacity
                    // problem — operators can scale or throttle) from "the eval invocation itself broke" (a code
                    // defect we should investigate). Without the distinction both look like a single
                    // bytechef_ai_eval_experiment_eval_dispatch_failures bump and the run row reads as success with
                    // mysteriously missing scores.
                    log.error(
                        "Eval dispatch rejected (queue saturated) for experiment {} run {} (traceId={}) — run " +
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
            // catch (Exception) — NOT catch (Throwable) — so JVM-wide problems (OutOfMemoryError,
            // StackOverflowError, ThreadDeath) propagate to the outer execute()'s Throwable catch instead of
            // being treated as a routine run failure. Java's exception hierarchy lets Error propagate past
            // catch (Exception) automatically; an explicit catch (Error) { throw error; } would be dead code.
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

    /**
     * Parses the dataset item's {@code input} JSON into an {@link AiGatewayChatCompletionRequest}, overrides the model
     * when the experiment pins one, and injects the workspace id into the tags so
     * {@link AiGatewayFacade#chatCompletion} can resolve budgets/rate-limits/trace scope via its existing
     * {@code resolveWorkspaceIdFromTags} helper.
     */
    private AiGatewayChatCompletionRequest buildRequest(AiEvalExperiment experiment, AiEvalDatasetItem item) {
        AiGatewayChatCompletionRequest parsed;

        try {
            parsed = objectMapper.readValue(item.getInput(), AiGatewayChatCompletionRequest.class);
        } catch (RuntimeException parseException) {
            // catch RuntimeException (not just JacksonException) because the Jackson 3.x readValue chain can
            // surface NullPointerException when {@code item.getInput()} is null and IllegalArgumentException
            // when the discovered class binding fails — both are upstream-data faults that must reject this
            // run without aborting peer runs in the experiment. Variable name is intentionally generic
            // (not "jacksonException") since the catch type is wider than Jackson.
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

    /**
     * Bumps a Micrometer counter without ever propagating registry-side failures. A registry RuntimeException
     * (cardinality cap, shutdown race, IllegalStateException from a registry under teardown) thrown after a successful
     * chat completion would otherwise unwind into the outer {@code catch (Exception)} and corrupt a successful run
     * state to FAILED — defeating the very dashboard the counter feeds. Mirrors the
     * {@code AiObservabilityOtlpIngestFacadeImpl#incrementCounter} discipline.
     */
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
