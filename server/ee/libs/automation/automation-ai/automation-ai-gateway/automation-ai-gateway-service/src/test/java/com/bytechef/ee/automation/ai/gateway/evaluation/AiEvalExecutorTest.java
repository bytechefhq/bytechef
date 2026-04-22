/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalRuleService;
import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalExecution;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRuleTarget;
import com.bytechef.ee.platform.ai.eval.service.AiEvalExecutionService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalRuleService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalScoreConfigService;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Pins three behaviors that have no other test coverage:
 * <ol>
 * <li>{@code parseMetadata} returns {@code null} on JSON parse failure (not {@code Map.of()}) so a downstream rule with
 * a metadata predicate can distinguish "no metadata" from "metadata corrupt and unmatchable".</li>
 * <li>The no-target {@code evaluateTrace(traceId, workspaceId)} overload dispatches to
 * {@link AiEvalRuleTarget#LIVE_TRACE} — verifiable through the rule-service mock, which is queried with that exact
 * target.</li>
 * <li>{@code recordTopLevelFailureBreadcrumbs} skips rules whose evaluations already landed earlier in the same call,
 * preventing duplicate ERROR rows on a partial-success batch.</li>
 * </ol>
 *
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiEvalExecutorTest {

    @Test
    void testParseMetadataReturnsEmptyMapForNullOrBlank() throws Exception {
        AiEvalExecutor executor = newExecutor();

        Method parseMetadata = parseMetadataMethod();

        assertThat((Map<?, ?>) parseMetadata.invoke(executor, (String) null)).isEmpty();
        assertThat((Map<?, ?>) parseMetadata.invoke(executor, "")).isEmpty();
        assertThat((Map<?, ?>) parseMetadata.invoke(executor, "   ")).isEmpty();
    }

    @Test
    void testParseMetadataReturnsParsedMapForValidJson() throws Exception {
        AiEvalExecutor executor = newExecutor();

        Method parseMetadata = parseMetadataMethod();

        Map<String, Object> result =
            castToStringObjectMap(parseMetadata.invoke(executor, "{\"environment\":\"prod\",\"k\":1}"));

        assertThat(result)
            .containsEntry("environment", "prod")
            .containsEntry("k", 1);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToStringObjectMap(Object value) {
        return (Map<String, Object>) value;
    }

    @Test
    void testParseMetadataReturnsNullOnUnparseableJson() throws Exception {
        // Pins the post-fix contract: a malformed metadata blob returns null so matchesFilters can SKIP rules
        // whose filter predicates require metadata, instead of letting the empty-map fallback let those
        // rules fire spuriously on every trace. A regression returning Map.of() instead of null would be
        // invisible at unit-test level except for this assertion.
        AiEvalExecutor executor = newExecutor();

        Method parseMetadata = parseMetadataMethod();

        Object result = parseMetadata.invoke(executor, "{not-valid-json");

        assertThat(result).isNull();
    }

    @Test
    void testBuildPromptSubstitutesContextFromRetrievalSpans() {
        AiEvalExecutor executor = newExecutor();

        AiObservabilityTrace trace = traceWith("q", "a", "{}");
        AiObservabilitySpan retrieval = spanOf(AiObservabilitySpanType.RETRIEVAL, "DOC-A");
        AiObservabilitySpan generation = spanOf(AiObservabilitySpanType.GENERATION, "ignored");

        String prompt = executor.buildPrompt(
            "ctx={{context}} in={{input}} out={{output}}", trace, List.of(retrieval, generation));

        assertThat(prompt).isEqualTo("ctx=DOC-A in=q out=a");
    }

    @Test
    void testBuildPromptContextEmptyWhenNoRetrievalSpans() {
        AiEvalExecutor executor = newExecutor();

        AiObservabilityTrace trace = traceWith("q", "a", "{}");

        String prompt = executor.buildPrompt("ctx=[{{context}}]", trace, List.of());

        assertThat(prompt).isEqualTo("ctx=[]");
    }

    private static AiObservabilityTrace traceWith(String input, String output, String metadata) {
        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);

        trace.setInput(input);
        trace.setOutput(output);
        trace.setMetadata(metadata);

        return trace;
    }

    private static AiObservabilitySpan spanOf(AiObservabilitySpanType type, String output) {
        AiObservabilitySpan span = new AiObservabilitySpan(1L, type);

        span.setOutput(output);

        return span;
    }

    @Test
    void testEvaluateTraceWithoutTargetDispatchesToLiveTraceTarget() {
        // Pins the post-fix dispatch: the no-target overload calls doEvaluateTrace with LIVE_TRACE, so
        // experiment-scoped rules never fire on live traffic. Because both overloads delegate to the private
        // doEvaluateTrace helper, we observe the dispatched target through the workspace rule-service mock.
        WorkspaceAiEvalRuleService workspaceAiEvalRuleService = mock(WorkspaceAiEvalRuleService.class);
        AiObservabilityTraceService aiObservabilityTraceService = mock(AiObservabilityTraceService.class);

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);

        when(aiObservabilityTraceService.getTrace(123L)).thenReturn(trace);
        when(workspaceAiEvalRuleService
            .getEnabledEvalRulesByWorkspaceAndTarget(eq(1L), eq(AiEvalRuleTarget.LIVE_TRACE)))
                .thenReturn(List.of());

        AiEvalExecutor executor = new AiEvalExecutor(
            mock(AiEvalExecutionService.class),
            mock(AiEvalRuleService.class),
            mock(AiEvalScoreConfigService.class),
            mock(WorkspaceAiEvalScoreService.class),
            workspaceAiEvalRuleService,
            mock(AiGatewayChatModelFactory.class),
            mock(AiGatewayProviderService.class),
            aiObservabilityTraceService, mock(WorkspaceAiObservabilityTraceService.class),
            mock(AiObservabilitySpanService.class),
            staticProvider(new SimpleMeterRegistry()));

        executor.evaluateTrace(123L, 1L);

        // Confirm the LIVE_TRACE target was used; the EXPERIMENT lookup must NEVER be triggered by the
        // no-target overload — a regression that conflated targets would silently make experiment-scoped
        // rules fire on production traffic and incur LLM-judge cost.
        verify(workspaceAiEvalRuleService, times(1))
            .getEnabledEvalRulesByWorkspaceAndTarget(1L, AiEvalRuleTarget.LIVE_TRACE);
        verify(workspaceAiEvalRuleService, never())
            .getEnabledEvalRulesByWorkspaceAndTarget(eq(1L), eq(AiEvalRuleTarget.EXPERIMENT_TRACE));
    }

    @Test
    void testRecordTopLevelFailureBreadcrumbsDoesNotMintRowsForAlreadyEvaluatedRules() throws Exception {
        // Pins the post-fix breadcrumb invariant: when runEvaluations partially succeeds (some rules already
        // landed an AiEvalExecution row) and then fails, recordTopLevelFailureBreadcrumbs must NOT double-mint
        // ERROR rows for the rules that already evaluated. Otherwise the eval-execution table has two rows per
        // completed rule (one COMPLETED, one ERROR) and the UI's "expected score per rule" assumption breaks.
        WorkspaceAiEvalRuleService workspaceAiEvalRuleService = mock(WorkspaceAiEvalRuleService.class);
        AiEvalExecutionService aiEvalExecutionService = mock(AiEvalExecutionService.class);

        AiEvalRule alreadyEvaluatedRule = aiEvalRule(11L, 1L, "already-evaluated");
        AiEvalRule notEvaluatedRule = aiEvalRule(22L, 1L, "not-evaluated");

        when(workspaceAiEvalRuleService
            .getEnabledEvalRulesByWorkspaceAndTarget(eq(1L), eq(AiEvalRuleTarget.LIVE_TRACE)))
                .thenReturn(List.of(alreadyEvaluatedRule, notEvaluatedRule));

        when(aiEvalExecutionService.create(any(AiEvalExecution.class))).thenAnswer(invocation -> {
            AiEvalExecution evalExecution = invocation.getArgument(0);

            stamp(evalExecution, "id", 999L);

            return evalExecution;
        });

        AiEvalExecutor executor = new AiEvalExecutor(
            aiEvalExecutionService,
            mock(AiEvalRuleService.class),
            mock(AiEvalScoreConfigService.class),
            mock(WorkspaceAiEvalScoreService.class),
            workspaceAiEvalRuleService,
            mock(AiGatewayChatModelFactory.class),
            mock(AiGatewayProviderService.class),
            mock(AiObservabilityTraceService.class), mock(WorkspaceAiObservabilityTraceService.class),
            mock(AiObservabilitySpanService.class),
            staticProvider(new SimpleMeterRegistry()));

        Method recordBreadcrumbs = AiEvalExecutor.class.getDeclaredMethod(
            "recordTopLevelFailureBreadcrumbs",
            long.class, Long.class, AiEvalRuleTarget.class, Exception.class, java.util.Set.class);

        recordBreadcrumbs.setAccessible(true);

        recordBreadcrumbs.invoke(
            executor, 100L, 1L, AiEvalRuleTarget.LIVE_TRACE, new RuntimeException("partial failure"),
            java.util.Set.of(alreadyEvaluatedRule.getId()));

        // Exactly one breadcrumb (for notEvaluatedRule) — alreadyEvaluatedRule must be skipped.
        verify(aiEvalExecutionService, times(1)).create(any(AiEvalExecution.class));
    }

    @Test
    void testEvaluateTraceForRulePreservesInterruptFlagWhenJdbcWrapsInterruption() throws Exception {
        // Pins containsInterruptedCause + Thread.currentThread().interrupt() in evaluateTraceForRule's outer
        // catch. The JDBC/Spring layer translates a wrapped InterruptedException into a RuntimeException
        // (DataAccessException subtype) and clears the interrupt flag during translation. Without the cause-chain
        // walk + re-interrupt, the @Async pool's task-cancellation logic loses the cancellation signal — the
        // dispatched task continues running on a thread that should already be unwinding.
        //
        // Construct a DataAccessException whose cause chain ENDS in InterruptedException — three layers deep so
        // we also pin that the walker traverses cause links beyond the immediate wrapper. Then verify (a) the
        // exception path runs without rethrowing (since the catch absorbs it), (b) the interrupt flag is set on
        // exit, and (c) we cleared the interrupt flag before invoking so we know the test re-set it through the
        // production path rather than through a leftover pre-test state.
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        AiEvalRuleService ruleService = mock(AiEvalRuleService.class);
        AiEvalExecutionService executionService = mock(AiEvalExecutionService.class);

        InterruptedException interruptedCause = new InterruptedException("dispatch-thread interrupted");
        IllegalStateException intermediateWrapper = new IllegalStateException(
            "JDBC wrapped the interrupt", interruptedCause);
        DataAccessResourceFailureException jdbcWrapped = new DataAccessResourceFailureException(
            "DB connection lost", intermediateWrapper);

        when(traceService.getTrace(123L)).thenThrow(jdbcWrapped);

        AiEvalExecutor executor = new AiEvalExecutor(
            executionService,
            ruleService,
            mock(AiEvalScoreConfigService.class),
            mock(WorkspaceAiEvalScoreService.class),
            mock(WorkspaceAiEvalRuleService.class),
            mock(AiGatewayChatModelFactory.class),
            mock(AiGatewayProviderService.class),
            traceService, mock(WorkspaceAiObservabilityTraceService.class),
            mock(AiObservabilitySpanService.class),
            staticProvider(new SimpleMeterRegistry()));

        // Best-effort breadcrumb create may also be invoked on the exception path. Stub it as a no-op so the
        // test focuses on the interrupt-flag invariant rather than the breadcrumb side effect.
        when(executionService.create(any(AiEvalExecution.class)))
            .thenAnswer(invocation -> {
                AiEvalExecution evalExecution = invocation.getArgument(0);

                stamp(evalExecution, "id", 999L);

                return evalExecution;
            });

        // Clear any leftover interrupt state so the post-call assertion proves the production path set the flag.
        Thread.interrupted();

        try {
            executor.evaluateTraceForRule(123L, 7L);

            // The catch absorbs the exception; method returns normally. Use Thread.interrupted() (which CLEARS
            // the flag as it reads) so the test does not pollute subsequent tests sharing the JUnit thread.
            boolean interruptedAfter = Thread.interrupted();

            assertThat(interruptedAfter)
                .as("evaluateTraceForRule must restore the interrupt flag when the cause chain contains "
                    + "InterruptedException — otherwise the @Async pool loses the cancellation signal")
                .isTrue();
        } finally {
            // Defence-in-depth: ensure no interrupt state leaks even if assertion fires.
            Thread.interrupted();
        }
    }

    @Test
    void testEvaluateTraceForRuleDoesNotSetInterruptFlagWhenCauseIsUnrelated() throws Exception {
        // The complement of the previous test: a DataAccessException whose cause is NOT an
        // InterruptedException must NOT set the interrupt flag, otherwise every JDBC-wrapped error would
        // mistakenly cancel the @Async pool's task cleanup. Pinning the negative path guards against an
        // over-eager refactor of containsInterruptedCause that would treat any DataAccessException as
        // interruption-driven.
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        AiEvalRuleService ruleService = mock(AiEvalRuleService.class);
        AiEvalExecutionService executionService = mock(AiEvalExecutionService.class);

        DataAccessResourceFailureException nonInterruptFailure = new DataAccessResourceFailureException(
            "connection refused", new RuntimeException("DNS down"));

        when(traceService.getTrace(456L)).thenThrow(nonInterruptFailure);

        AiEvalExecutor executor = new AiEvalExecutor(
            executionService,
            ruleService,
            mock(AiEvalScoreConfigService.class),
            mock(WorkspaceAiEvalScoreService.class),
            mock(WorkspaceAiEvalRuleService.class),
            mock(AiGatewayChatModelFactory.class),
            mock(AiGatewayProviderService.class),
            traceService, mock(WorkspaceAiObservabilityTraceService.class),
            mock(AiObservabilitySpanService.class),
            staticProvider(new SimpleMeterRegistry()));

        when(executionService.create(any(AiEvalExecution.class)))
            .thenAnswer(invocation -> {
                AiEvalExecution evalExecution = invocation.getArgument(0);

                stamp(evalExecution, "id", 999L);

                return evalExecution;
            });

        Thread.interrupted();

        try {
            executor.evaluateTraceForRule(456L, 7L);

            assertThat(Thread.interrupted())
                .as("a non-interrupt DataAccessException must NOT cause the executor to set the interrupt flag")
                .isFalse();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void testRunEvaluationsDoesNotMarkRuleEvaluatedWhenCreateFails() throws Exception {
        // evaluatedRuleIds.add(...) must run AFTER aiEvalExecutionService.create(...) succeeds. With the
        // post-create order, a failed create() leaves the rule UNMARKED so the breadcrumb path can mint a
        // fresh ERROR row for it. With a pre-create mark order, a create failure leaves the rule marked AND
        // row-less — the UI's "0 scores for trace X" state cannot distinguish "we never tried" from "we tried
        // and the DB lost it." A regression to the pre-mark order would re-introduce that silent-loss bug.
        AiEvalRuleService aiEvalRuleService = mock(AiEvalRuleService.class);
        AiEvalExecutionService aiEvalExecutionService = mock(AiEvalExecutionService.class);

        AiEvalRule failingRule = aiEvalRule(11L, 1L, "failing");

        when(aiEvalExecutionService.create(any(AiEvalExecution.class)))
            .thenThrow(new RuntimeException("simulated DB outage on create"));

        AiEvalExecutor executor = new AiEvalExecutor(
            aiEvalExecutionService,
            aiEvalRuleService,
            mock(AiEvalScoreConfigService.class),
            mock(WorkspaceAiEvalScoreService.class),
            mock(WorkspaceAiEvalRuleService.class),
            mock(AiGatewayChatModelFactory.class),
            mock(AiGatewayProviderService.class),
            mock(AiObservabilityTraceService.class), mock(WorkspaceAiObservabilityTraceService.class),
            mock(AiObservabilitySpanService.class),
            staticProvider(new SimpleMeterRegistry()));

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);

        Method runEvaluations = AiEvalExecutor.class.getDeclaredMethod(
            "runEvaluations", long.class, AiObservabilityTrace.class, List.class, java.util.Set.class);

        runEvaluations.setAccessible(true);

        java.util.Set<Long> evaluatedRuleIds = new java.util.HashSet<>();

        try {
            runEvaluations.invoke(executor, 100L, trace, List.of(failingRule), evaluatedRuleIds);
        } catch (java.lang.reflect.InvocationTargetException invocationTargetException) {
            // Expected: the simulated DB outage on create() propagates out of runEvaluations.
            assertThat(invocationTargetException.getCause()).isInstanceOf(RuntimeException.class);
        }

        // Critical: failingRule must NOT be in the set so recordTopLevelFailureBreadcrumbs can mint a fresh
        // ERROR breadcrumb for it. If a future change moves the add() back before create(), this assertion
        // will fail.
        assertThat(evaluatedRuleIds).doesNotContain(failingRule.getId());
    }

    private static AiEvalExecutor newExecutor() {
        return new AiEvalExecutor(
            mock(AiEvalExecutionService.class),
            mock(AiEvalRuleService.class),
            mock(AiEvalScoreConfigService.class),
            mock(WorkspaceAiEvalScoreService.class),
            mock(WorkspaceAiEvalRuleService.class),
            mock(AiGatewayChatModelFactory.class),
            mock(AiGatewayProviderService.class),
            mock(AiObservabilityTraceService.class), mock(WorkspaceAiObservabilityTraceService.class),
            mock(AiObservabilitySpanService.class),
            staticProvider(new SimpleMeterRegistry()));
    }

    private static Method parseMetadataMethod() throws NoSuchMethodException {
        Method method = AiEvalExecutor.class.getDeclaredMethod("parseMetadata", String.class);

        method.setAccessible(true);

        return method;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MeterRegistry> staticProvider(MeterRegistry registry) {
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(registry);

        return provider;
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static AiEvalRule aiEvalRule(long id, long workspaceId, String name) {
        AiEvalRule rule = new AiEvalRule(name, 7L, "prompt", "openai/gpt-4o", BigDecimal.ONE);

        stamp(rule, "id", id);

        return rule;
    }

    private static void stamp(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass()
                .getDeclaredField(fieldName);

            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException(
                "Could not stamp " + fieldName + " on " + target, reflectiveOperationException);
        }
    }
}
