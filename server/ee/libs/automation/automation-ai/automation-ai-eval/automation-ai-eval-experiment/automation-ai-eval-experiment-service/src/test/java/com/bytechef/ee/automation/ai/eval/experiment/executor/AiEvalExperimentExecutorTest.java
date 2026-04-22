/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.facade.AiObservabilityTracingHeaders;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link AiEvalExperimentExecutor}. Verifies the run-per-item loop, the replay dispatch contract (real
 * {@code chatCompletion} call + experiment-scoped eval), and the failure-aggregation contract (mark experiment FAILED
 * if any individual run threw).
 *
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiEvalExperimentExecutorTest {

    private static final String SAMPLE_INPUT =
        "{\"model\":\"openai/gpt-4\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"stream\":false}";

    @Mock
    private AiEvalDatasetItemService aiEvalDatasetItemService;

    @Mock
    private AiEvalExecutor aiEvalExecutor;

    @Mock
    private AiEvalExperimentRunService aiEvalExperimentRunService;

    @Mock
    private AiEvalExperimentService aiEvalExperimentService;

    @Mock
    private WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService;

    @Mock
    private AiGatewayFacade aiGatewayFacade;

    @Mock
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Mock
    private WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

    @Mock
    private RetryTemplate chatCompletionRetryTemplate;

    private AiEvalExperimentExecutor aiEvalExperimentExecutor;

    @BeforeEach
    void setUp() throws Exception {
        // Pass-through stub so existing tests don't need retry semantics. Marked lenient so the retry-behavior
        // tests, which build their own executor wired to a real RetryTemplate, don't trip strict-stubbing.
        lenient()
            .when(chatCompletionRetryTemplate.execute(any(RetryCallback.class)))
            .thenAnswer(invocation -> {
                RetryCallback<Object, Exception> callback = invocation.getArgument(0);

                return callback.doWithRetry(null);
            });

        // Workspace lookup goes through the relation table — supply the legacy default for every test that doesn't
        // override it. Tests asserting cross-workspace behavior set their own stubs.
        lenient().when(workspaceAiEvalExperimentService.getWorkspaceId(anyLong()))
            .thenReturn(1L);

        aiEvalExperimentExecutor = new AiEvalExperimentExecutor(
            aiEvalDatasetItemService, aiEvalExecutor, aiEvalExperimentRunService, aiEvalExperimentService,
            workspaceAiEvalExperimentService, aiGatewayFacade, aiObservabilityTraceService,
            workspaceAiObservabilityTraceService, chatCompletionRetryTemplate,
            new ObjectMapper(), emptyMeterRegistryProvider(), 1);
    }

    @Test
    void testExecuteMarksAllItemsCompletedOnSuccess() {
        long experimentId = 7L;
        long datasetVersionId = 3L;
        long workspaceId = 1L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);
        when(aiEvalExperimentService.getExperiment(experimentId)).thenReturn(experiment);

        List<AiEvalDatasetItem> items = List.of(
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 10L),
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 11L),
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 12L));

        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId)).thenReturn(items);

        // create() must return a run that the executor can address by id.
        when(aiEvalExperimentRunService.create(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> {
                AiEvalExperimentRun incoming = invocation.getArgument(0);

                return seedRunId(incoming, 100L + incoming.getDatasetItemId());
            });

        // Each chatCompletion call yields a fresh synthetic trace with a monotonically increasing id.
        AtomicLong traceIdCounter = new AtomicLong(500L);

        when(aiGatewayFacade.chatCompletion(
            any(AiGatewayChatCompletionRequest.class), any(AiObservabilityTracingHeaders.class), isNull()))
                .thenAnswer(invocation -> stubChatCompletionResponse());

        when(workspaceAiObservabilityTraceService.findByExternalTraceId(eq(workspaceId), anyString()))
            .thenAnswer(invocation -> Optional.of(seedTrace(
                new AiObservabilityTrace(AiObservabilityTraceSource.EXPERIMENT),
                traceIdCounter.incrementAndGet(), 123, new BigDecimal("0.0042"))));

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentService).markRunning(experimentId);
        verify(aiEvalExperimentRunService, times(3)).create(any(AiEvalExperimentRun.class));
        verify(aiEvalExperimentRunService, times(3)).markRunning(any(Long.class));

        ArgumentCaptor<AiObservabilityTracingHeaders> tracingHeadersCaptor =
            ArgumentCaptor.forClass(AiObservabilityTracingHeaders.class);

        verify(aiGatewayFacade, times(3)).chatCompletion(
            any(AiGatewayChatCompletionRequest.class), tracingHeadersCaptor.capture(), isNull());

        // Every replay must propagate AiObservabilityTraceSource.EXPERIMENT through the tracing headers so the
        // facade persists synthetic traces with source = EXPERIMENT (not the default API).
        assertThat(tracingHeadersCaptor.getAllValues())
            .allMatch(headers -> headers.source() == AiObservabilityTraceSource.EXPERIMENT);

        verify(aiEvalExperimentRunService, times(3))
            .complete(any(Long.class), any(Long.class), any(Integer.class), any(BigDecimal.class));
        verify(aiEvalExecutor, times(3))
            .evaluateTrace(anyLong(), eq(workspaceId), eq(AiEvalRuleTarget.EXPERIMENT_TRACE));
        verify(aiEvalExperimentRunService, never())
            .fail(any(Long.class), any(String.class));
        verify(aiEvalExperimentService).markFinished(experimentId, false);
    }

    @Test
    void testExecuteCompletesExperimentWithZeroDatasetItems() {
        long experimentId = 7L;
        long datasetVersionId = 3L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);

        // Empty dataset: no runs created, no chatCompletion calls. Experiment must transition
        // PENDING -> RUNNING -> COMPLETED with anyFailed=false. A future maintainer who refactors
        // the loop and accidentally inverts the empty-list semantics (e.g., transitioning to FAILED
        // on empty) will fail this test.
        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId)).thenReturn(List.of());

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentService, times(1)).markRunning(experimentId);
        verify(aiEvalDatasetItemService, times(1)).getItemsByVersion(datasetVersionId);
        verify(aiEvalExperimentRunService, never()).create(any(AiEvalExperimentRun.class));
        verify(aiGatewayFacade, never()).chatCompletion(
            any(AiGatewayChatCompletionRequest.class), any(AiObservabilityTracingHeaders.class), isNull());
        verify(aiEvalExperimentService, times(1)).markFinished(experimentId, false);
    }

    @Test
    void testExecuteMarksExperimentFailedWhenAnyItemFails() {
        long experimentId = 7L;
        long datasetVersionId = 3L;
        long workspaceId = 1L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);
        when(aiEvalExperimentService.getExperiment(experimentId)).thenReturn(experiment);

        List<AiEvalDatasetItem> items = List.of(
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 10L),
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 11L));

        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId)).thenReturn(items);

        when(aiEvalExperimentRunService.create(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> {
                AiEvalExperimentRun incoming = invocation.getArgument(0);

                return seedRunId(incoming, 100L + incoming.getDatasetItemId());
            });

        // Force the second item's markRunning to throw — the executor must catch, record fail, and still process
        // the remaining items (there are none here) but crucially the final markFinished must be called with
        // anyRunFailed=true.
        long failingRunId = 100L + 11L;

        doThrow(new RuntimeException("boom")).when(aiEvalExperimentRunService)
            .markRunning(failingRunId);

        // First item succeeds through the full path; stub out the replay machinery for it.
        when(aiGatewayFacade.chatCompletion(
            any(AiGatewayChatCompletionRequest.class), any(AiObservabilityTracingHeaders.class), isNull()))
                .thenAnswer(invocation -> stubChatCompletionResponse());

        when(workspaceAiObservabilityTraceService.findByExternalTraceId(eq(workspaceId), anyString()))
            .thenAnswer(invocation -> Optional.of(seedTrace(
                new AiObservabilityTrace(AiObservabilityTraceSource.EXPERIMENT),
                501L, 100, new BigDecimal("0.0001"))));

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentRunService).fail(eq(failingRunId), any(String.class));
        verify(aiEvalExperimentService).markFinished(experimentId, true);
    }

    @Test
    void testExecuteSkipsEvalDispatchWhenTraceLookupReturnsEmpty() {
        long experimentId = 7L;
        long datasetVersionId = 3L;
        long workspaceId = 1L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);
        when(aiEvalExperimentService.getExperiment(experimentId)).thenReturn(experiment);

        List<AiEvalDatasetItem> items = List.of(
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 10L));

        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId)).thenReturn(items);

        when(aiEvalExperimentRunService.create(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> {
                AiEvalExperimentRun incoming = invocation.getArgument(0);

                return seedRunId(incoming, 100L + incoming.getDatasetItemId());
            });

        when(aiGatewayFacade.chatCompletion(
            any(AiGatewayChatCompletionRequest.class), any(AiObservabilityTracingHeaders.class), isNull()))
                .thenAnswer(invocation -> stubChatCompletionResponse());

        // Edge case: chatCompletion succeeded but the facade's internal trace persistence silently dropped. The
        // executor must still complete the run (with null traceId) rather than NPE, and must NOT dispatch eval
        // (traceId is null, so there's nothing to score).
        when(workspaceAiObservabilityTraceService.findByExternalTraceId(eq(workspaceId), anyString()))
            .thenReturn(Optional.empty());

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentRunService)
            .complete(eq(110L), isNull(), any(Integer.class), isNull());
        verify(aiEvalExecutor, never())
            .evaluateTrace(anyLong(), any(Long.class), any(AiEvalRuleTarget.class));
        verify(aiEvalExperimentService).markFinished(experimentId, false);
    }

    @Test
    void testExecuteHonorsStopRequest() {
        long experimentId = 7L;
        long datasetVersionId = 3L;
        long workspaceId = 1L;

        AiEvalExperiment runningExperiment = seedExperimentId(
            new AiEvalExperiment(datasetVersionId), experimentId);

        AiEvalExperiment stopRequestedExperiment = seedExperimentId(
            new AiEvalExperiment(datasetVersionId), experimentId);

        stopRequestedExperiment.requestStop();

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(runningExperiment);

        // First iteration sees the flag unset; second iteration sees it set. Any further calls stay set.
        when(aiEvalExperimentService.getExperiment(experimentId))
            .thenReturn(runningExperiment, stopRequestedExperiment);

        List<AiEvalDatasetItem> items = List.of(
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 10L),
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 11L),
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 12L));

        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId)).thenReturn(items);

        when(aiEvalExperimentRunService.create(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> {
                AiEvalExperimentRun incoming = invocation.getArgument(0);

                return seedRunId(incoming, 100L + incoming.getDatasetItemId());
            });

        when(aiGatewayFacade.chatCompletion(
            any(AiGatewayChatCompletionRequest.class), any(AiObservabilityTracingHeaders.class), isNull()))
                .thenAnswer(invocation -> stubChatCompletionResponse());

        when(workspaceAiObservabilityTraceService.findByExternalTraceId(eq(workspaceId), anyString()))
            .thenAnswer(invocation -> Optional.of(seedTrace(
                new AiObservabilityTrace(AiObservabilityTraceSource.EXPERIMENT),
                500L, 123, new BigDecimal("0.0042"))));

        aiEvalExperimentExecutor.execute(experimentId);

        // Run 0 completes normally (flag was false at its iteration).
        verify(aiEvalExperimentRunService).markRunning(110L);
        verify(aiEvalExperimentRunService)
            .complete(eq(110L), any(Long.class), any(Integer.class), any(BigDecimal.class));

        // Runs 1 and 2 are cancelled — no markRunning / complete / eval for them.
        verify(aiEvalExperimentRunService, never()).markRunning(111L);
        verify(aiEvalExperimentRunService, never()).markRunning(112L);
        verify(aiEvalExperimentRunService).fail(eq(111L), anyString());
        verify(aiEvalExperimentRunService).fail(eq(112L), anyString());

        // Eval dispatched once for run 0 only.
        verify(aiEvalExecutor, times(1))
            .evaluateTrace(anyLong(), eq(workspaceId), eq(AiEvalRuleTarget.EXPERIMENT_TRACE));

        // Final experiment transition FAILED because cancellation sets anyFailed=true.
        verify(aiEvalExperimentService).markFinished(experimentId, true);
    }

    @Test
    void testStopRequestObservedDuringInFlightChatCompletionDoesNotDoubleMarkRun() {
        // Race shape: stop flag is flipped DURING a long chatCompletion call (think slow LLM streaming
        // request). The in-flight run already passed its iteration's stop check, so it MUST complete the
        // success-path bookkeeping rather than double-marking the run as both completed AND cancelled. The
        // NEXT iteration's stop check then cancels the remaining runs. A regression that shoehorned a
        // second stop check into the success path could cause both complete() and fail() to be called for
        // the same run id — corrupting dashboards and audit trails.
        long experimentId = 7L;
        long datasetVersionId = 3L;
        long workspaceId = 1L;

        AiEvalExperiment runningExperiment = seedExperimentId(
            new AiEvalExperiment(datasetVersionId), experimentId);

        AiEvalExperiment stopRequestedExperiment = seedExperimentId(
            new AiEvalExperiment(datasetVersionId), experimentId);

        stopRequestedExperiment.requestStop();

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(runningExperiment);

        // Iteration 0 sees the flag unset; the chat completion (below) flips it. Iteration 1 sees it set.
        when(aiEvalExperimentService.getExperiment(experimentId))
            .thenReturn(runningExperiment, stopRequestedExperiment);

        List<AiEvalDatasetItem> items = List.of(
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 10L),
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 11L));

        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId)).thenReturn(items);

        when(aiEvalExperimentRunService.create(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> {
                AiEvalExperimentRun incoming = invocation.getArgument(0);

                return seedRunId(incoming, 100L + incoming.getDatasetItemId());
            });

        // Mid-flight cancellation: chatCompletion succeeds normally, but the act of returning represents the
        // boundary at which the operator may have flicked Stop. The next loop iteration observes the flag.
        when(aiGatewayFacade.chatCompletion(
            any(AiGatewayChatCompletionRequest.class), any(AiObservabilityTracingHeaders.class), isNull()))
                .thenAnswer(invocation -> stubChatCompletionResponse());

        when(workspaceAiObservabilityTraceService.findByExternalTraceId(eq(workspaceId), anyString()))
            .thenAnswer(invocation -> Optional.of(seedTrace(
                new AiObservabilityTrace(AiObservabilityTraceSource.EXPERIMENT),
                500L, 123, new BigDecimal("0.0042"))));

        aiEvalExperimentExecutor.execute(experimentId);

        // Run 0 (in-flight when stop flipped) MUST complete cleanly — chat call already returned success.
        verify(aiEvalExperimentRunService).markRunning(110L);
        verify(aiEvalExperimentRunService)
            .complete(eq(110L), any(Long.class), any(Integer.class), any(BigDecimal.class));

        // Crucial double-bookkeeping guard: run 0 MUST NOT also be marked failed/cancelled.
        verify(aiEvalExperimentRunService, never())
            .fail(eq(110L), anyString());

        // Run 1 (next iteration) gets cancelled by the stop check.
        verify(aiEvalExperimentRunService, never()).markRunning(111L);
        verify(aiEvalExperimentRunService).fail(eq(111L), anyString());

        // Eval dispatched once for run 0 — the success-path eval still happens for the in-flight run.
        verify(aiEvalExecutor, times(1))
            .evaluateTrace(anyLong(), eq(workspaceId), eq(AiEvalRuleTarget.EXPERIMENT_TRACE));

        verify(aiEvalExperimentService).markFinished(experimentId, true);
    }

    @Test
    void testBookkeepingFailureOnSuccessfulChatCompletionDoesNotMisclassifyAsRunFailure() {
        // The chat call succeeded; the failure is in the bookkeeping write (DB hiccup, optimistic-lock
        // contention). A regression that re-routed the bookkeeping failure through fail() would persist a
        // misleading FAILED row for a run that actually succeeded — operators chasing ghost incidents. The
        // fix: catch the bookkeeping failure inline, return success, let the orphan reaper restore the
        // truthful terminal status. The experiment-level anyFailed aggregate must NOT flip to true.
        long experimentId = 7L;
        long datasetVersionId = 3L;
        long workspaceId = 1L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);
        when(aiEvalExperimentService.getExperiment(experimentId)).thenReturn(experiment);

        List<AiEvalDatasetItem> items = List.of(
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 10L));

        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId)).thenReturn(items);

        when(aiEvalExperimentRunService.create(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> {
                AiEvalExperimentRun incoming = invocation.getArgument(0);

                return seedRunId(incoming, 100L + incoming.getDatasetItemId());
            });

        when(aiGatewayFacade.chatCompletion(
            any(AiGatewayChatCompletionRequest.class), any(AiObservabilityTracingHeaders.class), isNull()))
                .thenAnswer(invocation -> stubChatCompletionResponse());

        when(workspaceAiObservabilityTraceService.findByExternalTraceId(eq(workspaceId), anyString()))
            .thenAnswer(invocation -> Optional.of(seedTrace(
                new AiObservabilityTrace(AiObservabilityTraceSource.EXPERIMENT),
                500L, 123, new BigDecimal("0.0042"))));

        // Bookkeeping write fails after chatCompletion succeeded.
        doThrow(new RuntimeException("optimistic lock contention"))
            .when(aiEvalExperimentRunService)
            .complete(eq(110L), any(Long.class), any(Integer.class), any(BigDecimal.class));

        aiEvalExperimentExecutor.execute(experimentId);

        // The run was attempted via complete() — verifies we did not skip bookkeeping entirely.
        verify(aiEvalExperimentRunService)
            .complete(eq(110L), any(Long.class), any(Integer.class), any(BigDecimal.class));

        // Crucial: must NOT be re-routed through fail(). A misclassified FAILED row is what this fix prevents.
        verify(aiEvalExperimentRunService, never()).fail(eq(110L), anyString());

        // Eval dispatch is skipped because the bookkeeping return path short-circuits before evaluateTrace.
        verify(aiEvalExecutor, never())
            .evaluateTrace(anyLong(), any(Long.class), any(AiEvalRuleTarget.class));

        // Experiment-level aggregate stays clean — the chat call succeeded, so anyFailed=false.
        verify(aiEvalExperimentService).markFinished(experimentId, false);
    }

    @Test
    void testLatencyValueIsClampedAtIntegerMaxValueForLongRunningCalls() throws Exception {
        // A 24.8d+ chatCompletion (hung backend, network-stuck) used to surface as a NEGATIVE latencyMs in
        // dashboards via int-narrowing overflow. This test pins the clamp at Integer.MAX_VALUE — operators
        // get a recognizable "very large" sentinel rather than misleading negative latency.
        long experimentId = 7L;
        long datasetVersionId = 3L;
        long workspaceId = 1L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);
        when(aiEvalExperimentService.getExperiment(experimentId)).thenReturn(experiment);

        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId))
            .thenReturn(List.of(seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 10L)));

        when(aiEvalExperimentRunService.create(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> seedRunId(invocation.getArgument(0), 110L));

        when(aiGatewayFacade.chatCompletion(
            any(AiGatewayChatCompletionRequest.class), any(AiObservabilityTracingHeaders.class), isNull()))
                .thenAnswer(invocation -> stubChatCompletionResponse());

        // Trace returns null for totalLatencyMs so the executor falls back to its own measured latency. The
        // measured latency would be small in this test, so we instead pin via the trace-resolved value: set
        // totalLatencyMs = Integer.MAX_VALUE. This proves the clamp on the resolved value path. (The clamp
        // also runs on the measured-from-startTime path; testing it would require freezing the clock.)
        when(workspaceAiObservabilityTraceService.findByExternalTraceId(eq(workspaceId), anyString()))
            .thenAnswer(invocation -> Optional.of(seedTrace(
                new AiObservabilityTrace(AiObservabilityTraceSource.EXPERIMENT),
                500L, Integer.MAX_VALUE, new BigDecimal("0.0042"))));

        aiEvalExperimentExecutor.execute(experimentId);

        ArgumentCaptor<Integer> latencyCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(aiEvalExperimentRunService)
            .complete(eq(110L), any(Long.class), latencyCaptor.capture(), any(BigDecimal.class));

        // Clamped: must NEVER be negative for a long-running call.
        assertThat(latencyCaptor.getValue()).isGreaterThanOrEqualTo(0);
        assertThat(latencyCaptor.getValue()).isEqualTo(Integer.MAX_VALUE);
    }

    /**
     * Direct test of the static {@code clampLatency} helper. The full-flow test above pins the trace-resolved value
     * path; this one pins the measured-from-startTime path that the executor falls back to when the trace lookup
     * returns no recorded latency. A regression replacing {@code clampLatency(latencyMs)} with a naive
     * {@code (int) latencyMs} cast trips this test instead of slipping through under the more common production
     * scenario (gateway-side trace dropping, eventual-consistency lag).
     */
    @Test
    void testClampLatencyHelperPinsMeasuredOverflowAndUnderflow() {
        // Overflow: any long > Integer.MAX_VALUE clamps to MAX_VALUE rather than narrowing to a negative int.
        long beyondIntRangeMillis = ((long) Integer.MAX_VALUE) + 1_000L;

        assertThat(AiEvalExperimentExecutor.clampLatency(beyondIntRangeMillis))
            .as("Overflow input must clamp to Integer.MAX_VALUE, not narrow into negative int range")
            .isEqualTo(Integer.MAX_VALUE);

        // Underflow: a clock-skew NTP step or monotonic violation can make `now - startTime` go negative;
        // surfacing that as a negative latency on dashboards is worse than reporting zero.
        assertThat(AiEvalExperimentExecutor.clampLatency(-42L))
            .as("Negative input (clock skew) must clamp to zero — negative latency is never meaningful")
            .isZero();

        // Boundaries — these protect against off-by-one regressions in the clamp condition.
        assertThat(AiEvalExperimentExecutor.clampLatency(0L)).isZero();
        assertThat(AiEvalExperimentExecutor.clampLatency(Integer.MAX_VALUE)).isEqualTo(Integer.MAX_VALUE);
        assertThat(AiEvalExperimentExecutor.clampLatency(Long.MAX_VALUE)).isEqualTo(Integer.MAX_VALUE);

        // Common case: a typical sub-second latency passes through unchanged.
        assertThat(AiEvalExperimentExecutor.clampLatency(150L)).isEqualTo(150);
    }

    @Test
    void testRetryRecoversFromTransientFailure() {
        long experimentId = 7L;
        long datasetVersionId = 3L;
        long workspaceId = 1L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);
        when(aiEvalExperimentService.getExperiment(experimentId)).thenReturn(experiment);

        List<AiEvalDatasetItem> items = List.of(
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 10L));

        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId)).thenReturn(items);

        when(aiEvalExperimentRunService.create(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> {
                AiEvalExperimentRun incoming = invocation.getArgument(0);

                return seedRunId(incoming, 100L + incoming.getDatasetItemId());
            });

        // First invocation throws a simulated transient IOException wrapped in a RuntimeException
        // (chatCompletion is declared to throw unchecked only — transient network failures surface
        // as RestClientException / ResourceAccessException whose cause is IOException). The retry
        // template unwraps by matching on the thrown type itself, so we throw IOException directly
        // through a wrapper that Mockito accepts.
        AtomicInteger callCount = new AtomicInteger();

        when(aiGatewayFacade.chatCompletion(
            any(AiGatewayChatCompletionRequest.class), any(AiObservabilityTracingHeaders.class), isNull()))
                .thenAnswer(invocation -> {
                    if (callCount.incrementAndGet() == 1) {
                        throw new IOException("simulated transient failure");
                    }

                    return stubChatCompletionResponse();
                });

        when(workspaceAiObservabilityTraceService.findByExternalTraceId(eq(workspaceId), anyString()))
            .thenAnswer(invocation -> Optional.of(seedTrace(
                new AiObservabilityTrace(AiObservabilityTraceSource.EXPERIMENT),
                500L, 123, new BigDecimal("0.0042"))));

        // Construct a real retry template (same config as production) so we exercise actual retry behavior
        // rather than the pass-through mock installed in setUp. Wiring with the real template at the executor
        // level lets us verify the run completes successfully when the transient failure recovers on retry.
        RetryTemplate realTemplate = deterministicRetryTemplate();

        AiEvalExperimentExecutor executorWithRealRetry = new AiEvalExperimentExecutor(
            aiEvalDatasetItemService, aiEvalExecutor, aiEvalExperimentRunService, aiEvalExperimentService,
            workspaceAiEvalExperimentService, aiGatewayFacade, aiObservabilityTraceService,
            workspaceAiObservabilityTraceService, realTemplate,
            new ObjectMapper(), emptyMeterRegistryProvider(), 1);

        executorWithRealRetry.execute(experimentId);

        // chatCompletion was called twice (1 transient fail + 1 success), and the run landed COMPLETED.
        verify(aiGatewayFacade, times(2)).chatCompletion(
            any(AiGatewayChatCompletionRequest.class), any(AiObservabilityTracingHeaders.class), isNull());
        verify(aiEvalExperimentRunService)
            .complete(eq(110L), any(Long.class), any(Integer.class), any(BigDecimal.class));
        verify(aiEvalExperimentRunService, never()).fail(any(Long.class), anyString());
        verify(aiEvalExperimentService).markFinished(experimentId, false);
    }

    @Test
    void testRetryDoesNotRetryOnProgrammerError() {
        long experimentId = 7L;
        long datasetVersionId = 3L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);
        when(aiEvalExperimentService.getExperiment(experimentId)).thenReturn(experiment);

        List<AiEvalDatasetItem> items = List.of(
            seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 10L));

        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId)).thenReturn(items);

        when(aiEvalExperimentRunService.create(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> {
                AiEvalExperimentRun incoming = invocation.getArgument(0);

                return seedRunId(incoming, 100L + incoming.getDatasetItemId());
            });

        // Programmer error — should fail fast, NOT retry.
        AtomicInteger callCount = new AtomicInteger();

        when(aiGatewayFacade.chatCompletion(
            any(AiGatewayChatCompletionRequest.class), any(AiObservabilityTracingHeaders.class), isNull()))
                .thenAnswer(invocation -> {
                    callCount.incrementAndGet();

                    throw new IllegalArgumentException("bad input");
                });

        RetryTemplate realTemplate = deterministicRetryTemplate();

        AiEvalExperimentExecutor executorWithRealRetry = new AiEvalExperimentExecutor(
            aiEvalDatasetItemService, aiEvalExecutor, aiEvalExperimentRunService, aiEvalExperimentService,
            workspaceAiEvalExperimentService, aiGatewayFacade, aiObservabilityTraceService,
            workspaceAiObservabilityTraceService, realTemplate,
            new ObjectMapper(), emptyMeterRegistryProvider(), 1);

        executorWithRealRetry.execute(experimentId);

        // Only one call — IllegalArgumentException did not retry.
        verify(aiGatewayFacade, times(1)).chatCompletion(
            any(AiGatewayChatCompletionRequest.class), any(AiObservabilityTracingHeaders.class), isNull());
        verify(aiEvalExperimentRunService).fail(eq(110L), anyString());
        verify(aiEvalExperimentService).markFinished(experimentId, true);

        // Sanity check against the local counter too.
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void testExecuteFailsExperimentWhenGetItemsByVersionThrows() {
        long experimentId = 7L;
        long datasetVersionId = 3L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);

        // Simulate a transient DB failure between markRunning and the run-creation loop. Without the catch-all
        // around the executor body, the experiment stays in RUNNING for ORPHAN_THRESHOLD_MINUTES — the catch must
        // transition it to FAILED here.
        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId))
            .thenThrow(new RuntimeException("simulated DB failure"));

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentService).markRunning(experimentId);
        verify(aiEvalExperimentService).markFinished(experimentId, true);
        verify(aiEvalExperimentRunService, never()).create(any(AiEvalExperimentRun.class));
    }

    @Test
    void testExecuteFailsExperimentWhenMarkRunningThrows() {
        long experimentId = 7L;

        // markRunning failing leaves the experiment PENDING — the orphan reaper only sweeps RUNNING, so without
        // the explicit FAILED transition the experiment is invisible to recovery. The executor must mark FAILED
        // here.
        when(aiEvalExperimentService.markRunning(experimentId))
            .thenThrow(new RuntimeException("simulated markRunning failure"));

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentService).markRunning(experimentId);
        verify(aiEvalExperimentService).markFinished(experimentId, true);
        verify(aiEvalDatasetItemService, never()).getItemsByVersion(anyLong());
        verify(aiEvalExperimentRunService, never()).create(any(AiEvalExperimentRun.class));
    }

    @Test
    void testRethrowsErrorSubtypes() {
        long experimentId = 7L;
        long datasetVersionId = 3L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);

        // OutOfMemoryError surfacing inside the run loop must propagate to the @Async UncaughtExceptionHandler
        // so the JVM can shed load — swallowing it would let downstream code claim experiments succeeded while
        // the JVM is in distress. Choose a deterministic Error subtype to keep the test focused on the rethrow
        // contract rather than which Error in particular.
        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId))
            .thenThrow(new OutOfMemoryError("simulated heap exhaustion"));

        assertThatThrownBy(() -> aiEvalExperimentExecutor.execute(experimentId))
            .isInstanceOf(OutOfMemoryError.class)
            .hasMessage("simulated heap exhaustion");

        // markFinished must NOT be called on the Error path: under OOM, the connection pool may be exhausted
        // and a markFinished() attempt can cascade additional failures, hide the real Error in the logs, and
        // emit a misleading "transitioning to FAILED" message that suggests the lifecycle was converged when
        // it wasn't. The orphan reaper sweeps stranded RUNNING experiments instead — see
        // AiEvalExperimentOrphanRecoveryRunner.
        verify(aiEvalExperimentService, never()).markFinished(eq(experimentId), anyBoolean());
    }

    @Test
    void testSkipsMarkFinishedAndRethrowsWhenMarkRunningThrowsError() {
        long experimentId = 7L;

        // markRunning throwing a JVM Error means the experiment is stranded in PENDING. The executor MUST NOT
        // attempt markFinished here because under OOM another DB write can cascade additional failures and
        // exhaust the connection pool — symmetric with the post-execution Error path. Recovery instead happens
        // through AiEvalExperimentOrphanRecoveryRunner.findPendingOlderThan, which sweeps PENDING experiments
        // older than the configured threshold. The Error itself must propagate so the executor's
        // UncaughtExceptionHandler can shed load.
        when(aiEvalExperimentService.markRunning(experimentId))
            .thenThrow(new OutOfMemoryError("simulated heap exhaustion in markRunning"));

        assertThatThrownBy(() -> aiEvalExperimentExecutor.execute(experimentId))
            .isInstanceOf(OutOfMemoryError.class)
            .hasMessage("simulated heap exhaustion in markRunning");

        verify(aiEvalExperimentService).markRunning(experimentId);
        verify(aiEvalExperimentService, never()).markFinished(eq(experimentId), anyBoolean());
    }

    @Test
    void testSkipsMarkFinishedWhenMarkRunningRowMissing() {
        long experimentId = 7L;

        // Delete-during-dispatch race: getExperiment surfaces the missing row as IllegalArgumentException.
        // markFinished would re-throw the same IAE from its own getExperiment — emitting a misleading second
        // ERROR-level log line within milliseconds. The executor short-circuits markFinished on
        // IllegalArgumentException specifically because there is no row to converge.
        when(aiEvalExperimentService.markRunning(experimentId))
            .thenThrow(new IllegalArgumentException("Experiment 7 not found"));

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentService).markRunning(experimentId);
        verify(aiEvalExperimentService, never()).markFinished(eq(experimentId), anyBoolean());
    }

    private static AiGatewayChatCompletionResponse stubChatCompletionResponse() {
        return new AiGatewayChatCompletionResponse(
            "resp-1", "chat.completion", System.currentTimeMillis(), "openai/gpt-4", List.of(),
            new AiGatewayChatCompletionResponse.Usage(5, 5, 10));
    }

    private static AiEvalExperiment seedExperimentId(AiEvalExperiment experiment, long id) {
        seedId(AiEvalExperiment.class, experiment, id);

        return experiment;
    }

    private static AiEvalExperimentRun seedRunId(AiEvalExperimentRun run, long id) {
        seedId(AiEvalExperimentRun.class, run, id);

        return run;
    }

    private static AiEvalDatasetItem seedDatasetItemId(AiEvalDatasetItem item, long id) {
        seedId(AiEvalDatasetItem.class, item, id);

        return item;
    }

    private static AiObservabilityTrace seedTrace(
        AiObservabilityTrace trace, long id, Integer latencyMs, BigDecimal cost) {

        seedId(AiObservabilityTrace.class, trace, id);

        trace.setTotalLatencyMs(latencyMs);
        trace.setTotalCost(cost);

        return trace;
    }

    private static void seedId(Class<?> clazz, Object target, long id) {
        try {
            Field idField = clazz.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id on " + clazz.getSimpleName(), reflectiveOperationException);
        }
    }

    /**
     * Builds a {@link RetryTemplate} with the same retryable-exception map as
     * {@code AiEvalExperimentRetryConfiguration#chatCompletionRetryTemplate} but with NO backoff and a fixed 3-attempt
     * cap. Tests that exercise retry semantics use this instead of constructing the production retry template — the
     * production config wires an exponential backoff (initial 250 ms, multiplier 2, max 2 s) so a single retry test
     * that exhausts attempts adds 250 + 500 = 750 ms of unit-test wall clock for no behavioral coverage. A future bump
     * of the production timings would also silently shift the test runtime; decoupling the test fixture pins behavior
     * to the retry policy alone.
     */
    private static RetryTemplate deterministicRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        template.setBackOffPolicy(new NoBackOffPolicy());
        template.setRetryPolicy(new SimpleRetryPolicy(
            3,
            java.util.Map.of(
                IOException.class, true,
                ResourceAccessException.class, true,
                WebClientRequestException.class, true,
                HttpServerErrorException.class, true,
                RuntimeException.class, false)));

        return template;
    }

    @SuppressWarnings("unchecked")
    private static org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry>
        emptyMeterRegistryProvider() {

        org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider =
            mock(org.springframework.beans.factory.ObjectProvider.class);

        when(provider.getIfAvailable())
            .thenReturn(null);

        return provider;
    }
}
