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
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetItemService;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
    private AiEvalExperimentRunService aiEvalExperimentRunService;

    @Mock
    private AiEvalExperimentService aiEvalExperimentService;

    @Mock
    private WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService;

    @Mock
    private AiGatewayFacade aiGatewayFacade;

    @Mock
    private RetryTemplate chatCompletionRetryTemplate;

    private AiEvalExperimentExecutor aiEvalExperimentExecutor;

    @BeforeEach
    void setUp() throws Exception {
        lenient()
            .when(chatCompletionRetryTemplate.execute(any(RetryCallback.class)))
            .thenAnswer(invocation -> {
                RetryCallback<Object, Exception> callback = invocation.getArgument(0);

                return callback.doWithRetry(null);
            });

        lenient().when(workspaceAiEvalExperimentService.getWorkspaceId(anyLong()))
            .thenReturn(1L);

        aiEvalExperimentExecutor = new AiEvalExperimentExecutor(
            aiEvalDatasetItemService, aiEvalExperimentRunService, aiEvalExperimentService,
            workspaceAiEvalExperimentService,
            aiGatewayFacade, chatCompletionRetryTemplate,
            new ObjectMapper(), emptyMeterRegistryProvider(), 1);
    }

    @Test
    void testExecuteMarksAllItemsCompletedOnSuccess() {
        long experimentId = 7L;
        long datasetVersionId = 3L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);
        when(aiEvalExperimentService.getExperiment(experimentId)).thenReturn(experiment);

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

        when(aiGatewayFacade.chatCompletion(any(AiGatewayChatCompletionRequest.class), isNull()))
            .thenAnswer(invocation -> stubChatCompletionResponse());

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentService).markRunning(experimentId);
        verify(aiEvalExperimentRunService, times(3)).create(any(AiEvalExperimentRun.class));
        verify(aiEvalExperimentRunService, times(3)).markRunning(any(Long.class));

        verify(aiGatewayFacade, times(3)).chatCompletion(any(AiGatewayChatCompletionRequest.class), isNull());

        verify(aiEvalExperimentRunService, times(3))
            .complete(any(Long.class), isNull(), any(Integer.class), isNull());
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

        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId)).thenReturn(List.of());

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentService, times(1)).markRunning(experimentId);
        verify(aiEvalDatasetItemService, times(1)).getItemsByVersion(datasetVersionId);
        verify(aiEvalExperimentRunService, never()).create(any(AiEvalExperimentRun.class));
        verify(aiGatewayFacade, never()).chatCompletion(any(AiGatewayChatCompletionRequest.class), isNull());
        verify(aiEvalExperimentService, times(1)).markFinished(experimentId, false);
    }

    @Test
    void testExecuteMarksExperimentFailedWhenAnyItemFails() {
        long experimentId = 7L;
        long datasetVersionId = 3L;

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

        long failingRunId = 100L + 11L;

        doThrow(new RuntimeException("boom")).when(aiEvalExperimentRunService)
            .markRunning(failingRunId);

        when(aiGatewayFacade.chatCompletion(any(AiGatewayChatCompletionRequest.class), isNull()))
            .thenAnswer(invocation -> stubChatCompletionResponse());

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentRunService).fail(eq(failingRunId), any(String.class));
        verify(aiEvalExperimentService).markFinished(experimentId, true);
    }

    @Test
    void testExecuteHonorsStopRequest() {
        long experimentId = 7L;
        long datasetVersionId = 3L;

        AiEvalExperiment runningExperiment = seedExperimentId(
            new AiEvalExperiment(datasetVersionId), experimentId);

        AiEvalExperiment stopRequestedExperiment = seedExperimentId(
            new AiEvalExperiment(datasetVersionId), experimentId);

        stopRequestedExperiment.requestStop();

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(runningExperiment);

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

        when(aiGatewayFacade.chatCompletion(any(AiGatewayChatCompletionRequest.class), isNull()))
            .thenAnswer(invocation -> stubChatCompletionResponse());

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentRunService).markRunning(110L);
        verify(aiEvalExperimentRunService)
            .complete(eq(110L), isNull(), any(Integer.class), isNull());

        verify(aiEvalExperimentRunService, never()).markRunning(111L);
        verify(aiEvalExperimentRunService, never()).markRunning(112L);
        verify(aiEvalExperimentRunService).fail(eq(111L), anyString());
        verify(aiEvalExperimentRunService).fail(eq(112L), anyString());

        verify(aiEvalExperimentService).markFinished(experimentId, true);
    }

    @Test
    void testStopRequestObservedDuringInFlightChatCompletionDoesNotDoubleMarkRun() {
        long experimentId = 7L;
        long datasetVersionId = 3L;

        AiEvalExperiment runningExperiment = seedExperimentId(
            new AiEvalExperiment(datasetVersionId), experimentId);

        AiEvalExperiment stopRequestedExperiment = seedExperimentId(
            new AiEvalExperiment(datasetVersionId), experimentId);

        stopRequestedExperiment.requestStop();

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(runningExperiment);

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

        when(aiGatewayFacade.chatCompletion(any(AiGatewayChatCompletionRequest.class), isNull()))
            .thenAnswer(invocation -> stubChatCompletionResponse());

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentRunService).markRunning(110L);
        verify(aiEvalExperimentRunService)
            .complete(eq(110L), isNull(), any(Integer.class), isNull());

        verify(aiEvalExperimentRunService, never())
            .fail(eq(110L), anyString());

        verify(aiEvalExperimentRunService, never()).markRunning(111L);
        verify(aiEvalExperimentRunService).fail(eq(111L), anyString());

        verify(aiEvalExperimentService).markFinished(experimentId, true);
    }

    @Test
    void testBookkeepingFailureOnSuccessfulChatCompletionDoesNotMisclassifyAsRunFailure() {
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

        when(aiGatewayFacade.chatCompletion(any(AiGatewayChatCompletionRequest.class), isNull()))
            .thenAnswer(invocation -> stubChatCompletionResponse());

        doThrow(new RuntimeException("optimistic lock contention"))
            .when(aiEvalExperimentRunService)
            .complete(eq(110L), isNull(), any(Integer.class), isNull());

        aiEvalExperimentExecutor.execute(experimentId);

        verify(aiEvalExperimentRunService)
            .complete(eq(110L), isNull(), any(Integer.class), isNull());

        verify(aiEvalExperimentRunService, never()).fail(eq(110L), anyString());

        verify(aiEvalExperimentService).markFinished(experimentId, false);
    }

    @Test
    void testLatencyValueIsClampedAtIntegerMaxValueForLongRunningCalls() throws Exception {
        long experimentId = 7L;
        long datasetVersionId = 3L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);
        when(aiEvalExperimentService.getExperiment(experimentId)).thenReturn(experiment);

        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId))
            .thenReturn(List.of(seedDatasetItemId(new AiEvalDatasetItem(1L, datasetVersionId, SAMPLE_INPUT), 10L)));

        when(aiEvalExperimentRunService.create(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> seedRunId(invocation.getArgument(0), 110L));

        when(aiGatewayFacade.chatCompletion(any(AiGatewayChatCompletionRequest.class), isNull()))
            .thenAnswer(invocation -> stubChatCompletionResponse(((long) Integer.MAX_VALUE) + 1_000L));

        aiEvalExperimentExecutor.execute(experimentId);

        ArgumentCaptor<Integer> latencyCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(aiEvalExperimentRunService)
            .complete(eq(110L), isNull(), latencyCaptor.capture(), isNull());

        assertThat(latencyCaptor.getValue()).isGreaterThanOrEqualTo(0);
        assertThat(latencyCaptor.getValue()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void testClampLatencyHelperPinsMeasuredOverflowAndUnderflow() {
        long beyondIntRangeMillis = ((long) Integer.MAX_VALUE) + 1_000L;

        assertThat(AiEvalExperimentExecutor.clampLatency(beyondIntRangeMillis))
            .as("Overflow input must clamp to Integer.MAX_VALUE, not narrow into negative int range")
            .isEqualTo(Integer.MAX_VALUE);

        assertThat(AiEvalExperimentExecutor.clampLatency(-42L))
            .as("Negative input (clock skew) must clamp to zero — negative latency is never meaningful")
            .isZero();

        assertThat(AiEvalExperimentExecutor.clampLatency(0L)).isZero();
        assertThat(AiEvalExperimentExecutor.clampLatency(Integer.MAX_VALUE)).isEqualTo(Integer.MAX_VALUE);
        assertThat(AiEvalExperimentExecutor.clampLatency(Long.MAX_VALUE)).isEqualTo(Integer.MAX_VALUE);

        assertThat(AiEvalExperimentExecutor.clampLatency(150L)).isEqualTo(150);
    }

    @Test
    void testRetryRecoversFromTransientFailure() {
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

        AtomicInteger callCount = new AtomicInteger();

        when(aiGatewayFacade.chatCompletion(any(AiGatewayChatCompletionRequest.class), isNull()))
            .thenAnswer(invocation -> {
                if (callCount.incrementAndGet() == 1) {
                    throw new IOException("simulated transient failure");
                }

                return stubChatCompletionResponse();
            });

        RetryTemplate realTemplate = deterministicRetryTemplate();

        AiEvalExperimentExecutor executorWithRealRetry = new AiEvalExperimentExecutor(
            aiEvalDatasetItemService, aiEvalExperimentRunService, aiEvalExperimentService,
            workspaceAiEvalExperimentService,
            aiGatewayFacade, realTemplate,
            new ObjectMapper(), emptyMeterRegistryProvider(), 1);

        executorWithRealRetry.execute(experimentId);

        verify(aiGatewayFacade, times(2)).chatCompletion(any(AiGatewayChatCompletionRequest.class), isNull());
        verify(aiEvalExperimentRunService)
            .complete(eq(110L), isNull(), any(Integer.class), isNull());
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

        AtomicInteger callCount = new AtomicInteger();

        when(aiGatewayFacade.chatCompletion(any(AiGatewayChatCompletionRequest.class), isNull()))
            .thenAnswer(invocation -> {
                callCount.incrementAndGet();

                throw new IllegalArgumentException("bad input");
            });

        RetryTemplate realTemplate = deterministicRetryTemplate();

        AiEvalExperimentExecutor executorWithRealRetry = new AiEvalExperimentExecutor(
            aiEvalDatasetItemService, aiEvalExperimentRunService, aiEvalExperimentService,
            workspaceAiEvalExperimentService,
            aiGatewayFacade, realTemplate,
            new ObjectMapper(), emptyMeterRegistryProvider(), 1);

        executorWithRealRetry.execute(experimentId);

        verify(aiGatewayFacade, times(1)).chatCompletion(any(AiGatewayChatCompletionRequest.class), isNull());
        verify(aiEvalExperimentRunService).fail(eq(110L), anyString());
        verify(aiEvalExperimentService).markFinished(experimentId, true);

        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void testExecuteFailsExperimentWhenGetItemsByVersionThrows() {
        long experimentId = 7L;
        long datasetVersionId = 3L;

        AiEvalExperiment experiment = seedExperimentId(new AiEvalExperiment(datasetVersionId), experimentId);

        when(aiEvalExperimentService.markRunning(experimentId)).thenReturn(experiment);

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

        when(aiEvalDatasetItemService.getItemsByVersion(datasetVersionId))
            .thenThrow(new OutOfMemoryError("simulated heap exhaustion"));

        assertThatThrownBy(() -> aiEvalExperimentExecutor.execute(experimentId))
            .isInstanceOf(OutOfMemoryError.class)
            .hasMessage("simulated heap exhaustion");

        verify(aiEvalExperimentService, never()).markFinished(eq(experimentId), anyBoolean());
    }

    @Test
    void testSkipsMarkFinishedAndRethrowsWhenMarkRunningThrowsError() {
        long experimentId = 7L;

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

    private static AiGatewayChatCompletionResponse stubChatCompletionResponse(long gatewayLatencyMs) {
        return new AiGatewayChatCompletionResponse(
            "resp-1", "chat.completion", System.currentTimeMillis(), "openai/gpt-4", List.of(),
            new AiGatewayChatCompletionResponse.Usage(5, 5, 10),
            new AiGatewayChatCompletionResponse.GatewayMetadata(
                "openai", "openai/gpt-4", gatewayLatencyMs, false, null, "request-1"));
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

    private static void seedId(Class<?> clazz, Object target, long id) {
        try {
            Field idField = clazz.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id on " + clazz.getSimpleName(), reflectiveOperationException);
        }
    }

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
