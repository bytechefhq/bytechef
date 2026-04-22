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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;

/**
 * Executes LLM-as-judge evaluations for completed traces. For each enabled eval rule that matches the trace and passes
 * sampling, builds a prompt from the rule's template, calls the specified model via the gateway's own
 * {@link AiGatewayChatModelFactory}, parses the response as a score, and persists the result.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
// CT_CONSTRUCTOR_THROW: meterRegistryProvider.getIfAvailable() can throw on a misconfigured registry context;
// failing bean creation is preferable to silently losing observability. Matches the suppression already
// applied to AiEvalExperimentExecutor / AiEvalExperimentOrphanRecoveryRunner.
@SuppressFBWarnings({
    "EI", "CT_CONSTRUCTOR_THROW"
})
public class AiEvalExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiEvalExecutor.class);

    private static final String TOP_LEVEL_FAILURE_METRIC = "bytechef_ai_eval_top_level_failure";

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

    /**
     * Sentinel for "rule has no model identifier configured" so {@link #classifyEvaluationFailure(Exception)} can
     * dispatch on the type rather than scraping the message text — keeping classification stable across reword
     * refactors of the underlying message.
     */
    private static final class MissingModelException extends IllegalArgumentException {

        private static final long serialVersionUID = 1L;

        MissingModelException(String message) {
            super(message);
        }
    }

    /**
     * Sentinel for "model identifier names a provider that is not registered." Mirrors {@link MissingModelException} so
     * {@link #classifyEvaluationFailure(Exception)} dispatches on the type rather than substring-matching the
     * underlying provider-lookup message text.
     */
    private static final class ProviderNotFoundException extends IllegalArgumentException {

        private static final long serialVersionUID = 1L;

        ProviderNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Sentinel for "rule references a score-config row that does not exist." The underlying repository throws
     * {@code IllegalArgumentException("AiEvalScoreConfig not found with id: ...")}; without this sentinel a substring
     * match on {@code "score config"} never matches (the persisted class name carries no space). The wrapper rethrow at
     * the call site routes the failure here so dashboards distinguish "rule misconfigured" from generic argument
     * validation.
     */
    private static final class ScoreConfigNotFoundException extends IllegalArgumentException {

        private static final long serialVersionUID = 1L;

        ScoreConfigNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Re-runs a specific eval rule against a historical trace, bypassing sampling and delay. Used by the "Run on
     * History" batch re-evaluation flow.
     *
     * <p>
     * Mints an ERROR-status {@link AiEvalExecution} breadcrumb if the trace or rule lookup fails, so the re-run history
     * UI distinguishes "evaluation failed" from "evaluation never started" — same contract as the live-trace path.
     * {@link Error} subtypes (OOM, StackOverflow, ThreadDeath) are not absorbed: an async thread that lost the JVM
     * should not produce success-shaped no-ops.
     */
    @Async
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
            // catch (Exception) — not (Throwable) — so JVM-wide Errors propagate to the outer evaluator
            // unchanged. An explicit catch (Error) { throw error; } would be dead code.
            //
            // If a JDBC/Spring layer translated a wrapped InterruptedException into a RuntimeException, the
            // interrupt flag was typically cleared during translation. Restore it before continuing so the
            // @Async pool's task-cancellation logic (and any caller up the stack) observes the cancellation.
            // The runEvaluations sibling restores the flag explicitly inside its sleep() catch; this generic
            // catch must mirror that discipline for the wrapped form.
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

    /**
     * Default entry point — dispatches evaluation for {@link AiEvalRuleTarget#LIVE_TRACE} rules. Preserves legacy
     * behavior for callers (e.g. {@code AiGatewayFacade}) that do not distinguish live traffic from experiment replays.
     *
     * <p>
     * Calls go through Spring's {@link Async} proxy when invoked from another bean, executing on the async executor.
     * The actual work is delegated to the private {@link #doEvaluateTrace} helper so the proxy boundary is crossed
     * exactly once and the design intent is unambiguous (versus an overload-to-overload {@code this.} call which would
     * silently bypass the proxy).
     */
    @Async
    public void evaluateTrace(long traceId, Long workspaceId) {
        doEvaluateTrace(traceId, workspaceId, AiEvalRuleTarget.LIVE_TRACE);
    }

    /**
     * Target-aware entry point. Filters the enabled-rule set to those whose {@code target} column matches the supplied
     * scope so experiment-scoped rules never fire on live traces and vice versa. Both this overload and
     * {@link #evaluateTrace(long, Long)} dispatch to the private {@link #doEvaluateTrace} helper, so cross-bean callers
     * cross the {@link Async} proxy exactly once and there is no overload-to-overload {@code this.} call that would
     * silently bypass the proxy.
     */
    @Async
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
            // A workspace dashboard reading "0 scores for trace X" must distinguish "no rules matched" from
            // "we never got the chance to run a rule." Mint one ERROR-status breadcrumb per configured rule
            // so the UI shows the failure on every expected score row instead of dropping them silently.
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

            // Pass already-evaluated rule IDs so we do not double-mint ERROR rows for rules whose
            // AiEvalExecution already landed (in COMPLETED or ERROR state). Otherwise a mid-batch failure
            // produces two rows per completed rule and breaks the "one expected score per rule" UI invariant.
            recordTopLevelFailureBreadcrumbs(traceId, workspaceId, target, exception, evaluatedRuleIds);
        }
    }

    /**
     * Aggregate counter for {@code evaluateTrace}-wide failures so dashboards can alert on "the evaluator is broken
     * right now" without scraping per-rule ERROR breadcrumbs. {@code phase} distinguishes the load-side failure (no
     * trace/rules acquired) from the run-side failure (something blew up while iterating rules). Workspace and trace id
     * are intentionally NOT tags — high-cardinality dimensions belong in logs, not in Micrometer.
     */
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
            // A registry-side failure (cardinality cap, IllegalStateException from a registry under teardown)
            // must NOT propagate out of the load-side or run-side catch — the counter is observability for the
            // breadcrumb pass that follows; if the meter aborts the catch, recordTopLevelFailureBreadcrumbs
            // never runs and the UI loses the ERROR rows the catch is supposed to produce.
            log.debug(
                "Failed to increment {} (target={}, phase={}); continuing",
                TOP_LEVEL_FAILURE_METRIC, target, phase, meterFailure);
        }
    }

    /**
     * Best-effort: emit one ERROR-status {@link AiEvalExecution} per enabled rule that should have run BUT did not yet
     * have a breadcrumb minted in this run. The UI shows "evaluation failed" for each expected score row instead of
     * silently dropping them. If we never even loaded the rule list (e.g., the rule lookup itself threw) we re-fetch it
     * here to avoid leaving an outright-no-breadcrumbs hole. Rules whose IDs appear in {@code alreadyEvaluatedIds} are
     * skipped — those are rules that already produced an {@link AiEvalExecution} row earlier in the same call.
     */
    private void recordTopLevelFailureBreadcrumbs(
        long traceId, Long workspaceId, AiEvalRuleTarget target, Exception exception,
        Set<Long> alreadyEvaluatedIds) {

        List<AiEvalRule> rulesForBreadcrumbs;

        try {
            rulesForBreadcrumbs =
                workspaceAiEvalRuleService.getEnabledEvalRulesByWorkspaceAndTarget(workspaceId, target);
        } catch (Exception breadcrumbLookupException) {
            // If even the rule re-lookup fails, the original ERROR log above is the only signal — no
            // safe way to mint ERROR rows without knowing which rules to mint them for.
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

            // Mark only AFTER a successful create. If create() threw above, the loop's exception propagates
            // to doEvaluateTrace's catch and recordTopLevelFailureBreadcrumbs mints a fresh ERROR row for this
            // rule (not in evaluatedRuleIds → not skipped). The earlier pre-mark order looked symmetric with
            // AiObservabilityOtlpIngestFacadeImpl.persistSpan but produced a worse failure mode here: a
            // pre-marked rule whose create() throws would land NEITHER in this row nor in the breadcrumb
            // path, leaving the UI's "0 scores for trace X" state unable to distinguish "we never tried"
            // from "we tried and the DB lost it." Marking after create keeps that signal honest.
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

                    // Mint ERROR breadcrumbs for the current rule + every remaining rule INLINE — while the
                    // interrupt flag is still cleared by the InterruptedException catch. The breadcrumb
                    // writes happen here, with the rule list already in hand, so the JDBC ops complete
                    // before we re-interrupt below; an alternative load-the-rules-again approach would
                    // race Spring JdbcTemplate's re-set-flag check and lose every breadcrumb after the
                    // first JDBC op.
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
                            // Best-effort: log per-rule and continue. We are racing the re-set interrupt
                            // flag — JDBC may reject some rows even before we re-interrupt — but every
                            // success is a breadcrumb that would otherwise be dropped silently.
                            log.warn(
                                "Failed to record interrupt breadcrumb for rule {} on trace {}",
                                remainingRule.getId(), traceId, breadcrumbFailure);
                        }
                    }

                    // Re-set the interrupt flag AFTER the synchronous breadcrumb writes so future
                    // interruptible ops (the caller's @Async hop unwinding, or the next pool task) observe
                    // the cancellation. Doing this earlier would bias JDBC against the breadcrumb pass.
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

    /**
     * Concatenates the {@code output} of every {@link AiObservabilitySpanType#RETRIEVAL} span on the trace, so the
     * judge prompt's {@code {{context}}} placeholder reflects the documents actually retrieved rather than requiring
     * the rule author to hand-copy them. Spans of other types (e.g. GENERATION) are ignored — only retrieval spans
     * carry retrieved-document output by the OTLP ingestion contract established in Task 1/2.
     */
    private String buildContext(List<AiObservabilitySpan> spans) {
        return spans.stream()
            .filter(span -> span.getType() == AiObservabilitySpanType.RETRIEVAL)
            .map(AiObservabilitySpan::getOutput)
            .filter(output -> output != null && !output.isBlank())
            .collect(Collectors.joining("\n\n"));
    }

    private void executeEvaluation(
        AiEvalExecution evalExecution, AiEvalRule evalRule, AiObservabilityTrace trace) {

        try {
            // Explicit null check — without it, the .split("/", 2) NPE lands in the catch (Exception) below and
            // classifyEvaluationFailure buckets it as UNKNOWN, hiding the real diagnosis (a misconfigured rule
            // whose model was nulled out by a bad migration). Mirror the MODEL_IDENTIFIER_MISSING discipline
            // OtlpCostResolver applies for the analogous OTLP-side bug.
            String ruleModel = evalRule.getModel();

            if (ruleModel == null || ruleModel.isBlank()) {
                // Typed sentinel so classifyEvaluationFailure dispatches on instanceof rather than message-text.
                // Still extends IllegalArgumentException so existing catch (IllegalArgumentException) handlers
                // (and the outer catch (Exception) below) work unchanged.
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
                    // Typed sentinel so classifyEvaluationFailure dispatches on instanceof rather than
                    // message-text matching, which is fragile across message wording changes.
                    () -> new ProviderNotFoundException(
                        "No provider found with name: " + providerName));

            ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);

            List<AiObservabilitySpan> spans = aiObservabilitySpanService.getSpansByTrace(trace.getId());

            String promptText = buildPrompt(evalRule.getPromptTemplate(), trace, spans);

            ChatResponse chatResponse = chatModel.call(new Prompt(promptText));

            String responseContent = chatResponse.getResult()
                .getOutput()
                .getText();

            // Wrap the repository's generic "AiEvalScoreConfig not found" IllegalArgumentException in a typed
            // sentinel so classifyEvaluationFailure routes the failure to SCORE_CONFIG_NOT_FOUND via
            // instanceof rather than fragile message-text matching.
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

            // Use summarize() rather than raw getMessage(): messageless exceptions (Spring DataAccessException,
            // raw NPE) would otherwise persist literal "null" in AiEvalExecution.errorMessage and surface as
            // empty cells on operator dashboards.
            markAsErrorSafely(
                evalExecution, AiGatewayThrowables.summarize(exception), classifyEvaluationFailure(exception));
        }
    }

    /**
     * Categorises the failure into a stable enum value so dashboards branch on a structured field rather than grepping
     * the free-text {@code errorMessage}. Order matters: the more specific match wins (e.g., a Spring
     * {@link DataAccessException} that also happens to be an {@link IllegalArgumentException} is highly unlikely, but
     * if it occurs, the persistence classification reflects the failure boundary the operator can act on).
     */
    private static AiEvalExecutionFailureReason classifyEvaluationFailure(Exception exception) {
        // Typed-sentinel branches first — dispatching on the marker subclass rather than message text protects
        // against silent re-bucketing when the underlying message is reworded. Each sentinel is a private static
        // final class above; they extend IllegalArgumentException so any catch (IllegalArgumentException) handler
        // continues to work unchanged.
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
            // Distinguish "first attempt failed" from "all retries exhausted" so dashboards can alert
            // separately on a workspace targeting a hard-down provider vs. one with intermittent 5xx. Spring's
            // RetryTemplate stacks per-attempt failures as suppressed exceptions on the final throw — a
            // non-empty suppressed list is the canonical wire-level signal that retry was actually attempted
            // before failing. The check is conservative: a future retry implementation that does not stack
            // suppressed failures silently falls back to PROVIDER_REQUEST_FAILED rather than producing a false
            // PROVIDER_REQUEST_FAILED_AFTER_RETRIES, so under-attribution is the safe bias.
            return wasRetryAttempted(exception)
                ? AiEvalExecutionFailureReason.PROVIDER_REQUEST_FAILED_AFTER_RETRIES
                : AiEvalExecutionFailureReason.PROVIDER_REQUEST_FAILED;
        }

        return AiEvalExecutionFailureReason.UNKNOWN;
    }

    /**
     * Heuristic: Spring's {@link org.springframework.retry.support.RetryTemplate} adds the per-attempt failures as
     * {@code Throwable.getSuppressed()} entries on the final exception when retries are exhausted. A non-empty
     * suppressed array on the boundary {@link RestClientException} means the chat-client retried at least once before
     * giving up. Walking the cause chain is intentional — the wrapping layer (provider SDK, WebClient adapter) often
     * rewraps the original retry-exhausted exception, so the suppressed entries can land on the cause rather than the
     * outermost throwable.
     */
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

    /**
     * Best-effort transition to ERROR. If the update itself throws (e.g., DB outage), log loudly so ops can reap the
     * stranded PENDING row instead of leaving an invisible "in-flight forever" state. Without this wrapper, a failing
     * {@code update()} call escapes the inner catch into the outer {@code evaluateTrace} catch, which only logs and
     * leaves the row in PENDING — a common regression after DB connection-pool exhaustion.
     */
    private void markAsErrorSafely(AiEvalExecution evalExecution, String errorMessage) {
        markAsErrorSafely(evalExecution, errorMessage, AiEvalExecutionFailureReason.UNKNOWN);
    }

    private void markAsErrorSafely(
        AiEvalExecution evalExecution, String errorMessage, AiEvalExecutionFailureReason failureReason) {

        // markErrored is the FSM-aware transition: it sets status, failureReason, and errorMessage atomically and
        // throws if the source state is not PENDING (so a double-call from COMPLETED no longer silently overwrites
        // the already-final outcome). The previous trio of setStatus/setErrorMessage/setFailureReason calls bypassed
        // that guard and let any state transition succeed.
        try {
            evalExecution.markErrored(failureReason, errorMessage);
        } catch (IllegalArgumentException illegalFsmTransition) {
            // Already in a terminal state (e.g., a retry path raced with the original error path). Log loudly and
            // skip the update — the row is already final, attempting another write would either no-op or fail the
            // FSM check on the next reload.
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
                    trace.getId(), scoreConfig.getName(), AiEvalScoreSource.LLM_JUDGE, parsed);
            }
            case BOOLEAN -> {
                String lowerResponse = trimmedResponse.toLowerCase();
                boolean isTrue = lowerResponse.equals("true") || lowerResponse.equals("yes")
                    || lowerResponse.equals("1");
                boolean isFalse = lowerResponse.equals("false") || lowerResponse.equals("no")
                    || lowerResponse.equals("0");

                // Mirror the NUMERIC fail-loudly path. Without this guard, any unrecognized response (e.g.
                // "unknown", "refused", "I cannot determine") would silently coerce to false — biasing dashboards,
                // false-positive thresholds, and operator alerts toward "no". Surface the parse failure via the
                // execution status so callers catch IllegalStateException → mark execution ERROR. Operators
                // investigating an LLM-judge that starts refusing see the failure in the eval-execution table
                // rather than chasing a phantom drop in true-rate.
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

        // Distinguish "no metadata configured" (parsedMetadata is empty) from "metadata exists but is
        // unparseable" (parsedMetadata is null). The former is normal — metadata-keyed filters simply do
        // not match. The latter is a data-quality bug, but the rule should be skipped only when its
        // filter would actually read metadata; rules that filter solely on first-class trace fields
        // (name, source, status, user/userId) must still fire correctly even when metadata is corrupt.
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

    /**
     * Returns true if a filter key reads from the trace's metadata blob (rather than a first-class trace field).
     * Mirrors the dispatch table in {@link #resolveTraceAttribute}: {@code user}/{@code userId}/{@code name}/
     * {@code source}/{@code status} are direct getters and remain resolvable when metadata JSON is corrupt; everything
     * else falls through to the metadata map.
     */
    private static boolean requiresMetadata(String filterKey) {
        return switch (filterKey) {
            case "user", "userId", "name", "source", "status" -> false;
            default -> true;
        };
    }

    /**
     * Returns the parsed metadata map, an empty map when the trace has no metadata configured at all, or {@code null}
     * when metadata is present but unparseable. Callers MUST treat {@code null} as "skip this rule" rather than "no
     * metadata to filter on" — otherwise a rule that should have been filtered out by a metadata predicate would fire
     * spuriously, polluting score dashboards.
     *
     * <p>
     * Trace metadata may carry user prompts or other PII that should not be replicated verbatim into operator log
     * streams. The error log records only the JSON length plus a SHA-256 fingerprint (first 12 hex chars) so an
     * operator can correlate repeated failures across traces without leaking content — logging a head substring would
     * be the wrong choice because the first characters of the JSON blob are exactly where an embedded prompt sits.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }

        try {
            Map<String, Object> parsed = JsonUtils.read(metadataJson, Map.class);

            return parsed == null ? Map.of() : parsed;
        } catch (JacksonException | IllegalArgumentException exception) {
            // Narrow to the two exception classes JsonUtils.read can produce for malformed payloads —
            // catch (Exception) would absorb JVM-level distress (e.g. an OutOfDirectMemoryError wrapped
            // as RuntimeException by a future Jackson upgrade) and bucket it as "metadata corrupt"
            // when the actual problem is a JVM fault. Letting unexpected RuntimeExceptions propagate
            // keeps real bugs loud; this branch stays focused on the legitimate "JSON truly broken"
            // path that the metadata-filter contract is designed to tolerate.
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

            // 12 hex chars = 6 bytes = ample for cross-trace correlation; leaks nothing about the source.
            return HexFormat.of()
                .formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException noSha256OnThisJvm) {
            // SHA-256 is mandated by every JDK since 1.4. Surface as AssertionError so a misconfigured JVM
            // (e.g., FIPS profile excluding SHA-256) fails loudly rather than letting the surrounding log
            // line silently advertise "sha256-unavailable" forever.
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
