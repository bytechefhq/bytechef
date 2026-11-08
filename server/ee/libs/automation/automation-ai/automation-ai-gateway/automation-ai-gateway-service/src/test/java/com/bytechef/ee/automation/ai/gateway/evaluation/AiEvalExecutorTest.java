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
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.ai.guardrails.GuardrailSurface;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;

/**
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
        WorkspaceAiEvalRuleService workspaceAiEvalRuleService = mock(WorkspaceAiEvalRuleService.class);
        AiObservabilityTraceService aiObservabilityTraceService = mock(AiObservabilityTraceService.class);

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);

        when(aiObservabilityTraceService.getTrace(123L)).thenReturn(trace);
        when(workspaceAiEvalRuleService
            .getEnabledEvalRulesByWorkspaceAndTarget(eq(1L), eq(AiEvalRuleTarget.LIVE_TRACE)))
                .thenReturn(List.of());

        AiEvalExecutor executor = new AiEvalExecutor(
            staticGuardrailsAdvisorProvider(),
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

        verify(workspaceAiEvalRuleService, times(1))
            .getEnabledEvalRulesByWorkspaceAndTarget(1L, AiEvalRuleTarget.LIVE_TRACE);
        verify(workspaceAiEvalRuleService, never())
            .getEnabledEvalRulesByWorkspaceAndTarget(eq(1L), eq(AiEvalRuleTarget.EXPERIMENT_TRACE));
    }

    @Test
    void testRecordTopLevelFailureBreadcrumbsDoesNotMintRowsForAlreadyEvaluatedRules() throws Exception {
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
            staticGuardrailsAdvisorProvider(),
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

        verify(aiEvalExecutionService, times(1)).create(any(AiEvalExecution.class));
    }

    @Test
    void testEvaluateTraceForRulePreservesInterruptFlagWhenJdbcWrapsInterruption() throws Exception {
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
            staticGuardrailsAdvisorProvider(),
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
            executor.evaluateTraceForRule(123L, 7L);

            boolean interruptedAfter = Thread.interrupted();

            assertThat(interruptedAfter)
                .as("evaluateTraceForRule must restore the interrupt flag when the cause chain contains "
                    + "InterruptedException — otherwise the @Async pool loses the cancellation signal")
                .isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void testEvaluateTraceForRuleDoesNotSetInterruptFlagWhenCauseIsUnrelated() throws Exception {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        AiEvalRuleService ruleService = mock(AiEvalRuleService.class);
        AiEvalExecutionService executionService = mock(AiEvalExecutionService.class);

        DataAccessResourceFailureException nonInterruptFailure = new DataAccessResourceFailureException(
            "connection refused", new RuntimeException("DNS down"));

        when(traceService.getTrace(456L)).thenThrow(nonInterruptFailure);

        AiEvalExecutor executor = new AiEvalExecutor(
            staticGuardrailsAdvisorProvider(),
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
        AiEvalRuleService aiEvalRuleService = mock(AiEvalRuleService.class);
        AiEvalExecutionService aiEvalExecutionService = mock(AiEvalExecutionService.class);

        AiEvalRule failingRule = aiEvalRule(11L, 1L, "failing");

        when(aiEvalExecutionService.create(any(AiEvalExecution.class)))
            .thenThrow(new RuntimeException("simulated DB outage on create"));

        AiEvalExecutor executor = new AiEvalExecutor(
            staticGuardrailsAdvisorProvider(),
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
            assertThat(invocationTargetException.getCause()).isInstanceOf(RuntimeException.class);
        }

        assertThat(evaluatedRuleIds).doesNotContain(failingRule.getId());
    }

    private static AiEvalExecutor newExecutor() {
        return new AiEvalExecutor(
            staticGuardrailsAdvisorProvider(),
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

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AiGuardrailsAdvisorProvider> staticGuardrailsAdvisorProvider() {
        return mock(ObjectProvider.class);
    }

    @Test
    void testEvaluationModelCallPassesThroughTheTraceWorkspaceGuardrailsAdvisor() throws Exception {
        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = mock(AiGuardrailsAdvisorProvider.class);
        RecordingAdvisor recordingAdvisor = new RecordingAdvisor();

        @SuppressWarnings("unchecked")
        ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderProvider = mock(ObjectProvider.class);

        when(aiGuardrailsAdvisorProviderProvider.getIfAvailable()).thenReturn(aiGuardrailsAdvisorProvider);
        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(42L, GuardrailSurface.AI_EVAL))
            .thenReturn(Optional.of(recordingAdvisor));

        AiGatewayProvider provider = mock(AiGatewayProvider.class);

        when(provider.getName()).thenReturn("openai");

        ChatModel chatModel = mock(ChatModel.class);

        when(chatModel.getOptions()).thenReturn(ChatOptions.builder()
            .build());
        when(chatModel.call(any(Prompt.class))).thenReturn(ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage("0.9"))))
            .build());

        AiGatewayProviderService aiGatewayProviderService = mock(AiGatewayProviderService.class);

        when(aiGatewayProviderService.getProviders()).thenReturn(List.of(provider));

        AiGatewayChatModelFactory aiGatewayChatModelFactory = mock(AiGatewayChatModelFactory.class);

        when(aiGatewayChatModelFactory.getChatModel(provider)).thenReturn(chatModel);

        AiObservabilitySpanService aiObservabilitySpanService = mock(AiObservabilitySpanService.class);

        when(aiObservabilitySpanService.getSpansByTrace(any())).thenReturn(List.of());

        AiEvalExecutor executor = new AiEvalExecutor(
            aiGuardrailsAdvisorProviderProvider,
            mock(AiEvalExecutionService.class),
            mock(AiEvalRuleService.class),
            mock(AiEvalScoreConfigService.class),
            mock(WorkspaceAiEvalScoreService.class),
            mock(WorkspaceAiEvalRuleService.class),
            aiGatewayChatModelFactory,
            aiGatewayProviderService,
            mock(AiObservabilityTraceService.class), mock(WorkspaceAiObservabilityTraceService.class),
            aiObservabilitySpanService,
            staticProvider(new SimpleMeterRegistry()));

        AiObservabilityTrace trace = traceWith("q", "a", "{}");

        trace.setWorkspaceId(42L);
        stamp(trace, "id", 7L);

        Method executeEvaluation = AiEvalExecutor.class.getDeclaredMethod(
            "executeEvaluation", AiEvalExecution.class, AiEvalRule.class, AiObservabilityTrace.class);

        executeEvaluation.setAccessible(true);
        executeEvaluation.invoke(executor, mock(AiEvalExecution.class), aiEvalRule(1L, 42L, "rule"), trace);

        assertThat(recordingAdvisor.advisedCall).isTrue();
    }

    @Test
    void testTheGuardrailsWorkspaceComesFromTheTraceRow() throws Exception {
        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = mock(AiGuardrailsAdvisorProvider.class);
        RecordingAdvisor recordingAdvisor = new RecordingAdvisor();

        @SuppressWarnings("unchecked")
        ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderProvider = mock(ObjectProvider.class);

        when(aiGuardrailsAdvisorProviderProvider.getIfAvailable()).thenReturn(aiGuardrailsAdvisorProvider);
        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(any(), eq(GuardrailSurface.AI_EVAL)))
            .thenReturn(Optional.empty());
        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(99L, GuardrailSurface.AI_EVAL))
            .thenReturn(Optional.of(recordingAdvisor));

        AiGatewayProvider provider = mock(AiGatewayProvider.class);

        when(provider.getName()).thenReturn("openai");

        ChatModel chatModel = mock(ChatModel.class);

        when(chatModel.getOptions()).thenReturn(ChatOptions.builder()
            .build());
        when(chatModel.call(any(Prompt.class))).thenReturn(ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage("0.9"))))
            .build());

        AiGatewayProviderService aiGatewayProviderService = mock(AiGatewayProviderService.class);

        when(aiGatewayProviderService.getProviders()).thenReturn(List.of(provider));

        AiGatewayChatModelFactory aiGatewayChatModelFactory = mock(AiGatewayChatModelFactory.class);

        when(aiGatewayChatModelFactory.getChatModel(provider)).thenReturn(chatModel);

        AiObservabilitySpanService aiObservabilitySpanService = mock(AiObservabilitySpanService.class);

        when(aiObservabilitySpanService.getSpansByTrace(any())).thenReturn(List.of());

        AiEvalExecutor executor = new AiEvalExecutor(
            aiGuardrailsAdvisorProviderProvider,
            mock(AiEvalExecutionService.class),
            mock(AiEvalRuleService.class),
            mock(AiEvalScoreConfigService.class),
            mock(WorkspaceAiEvalScoreService.class),
            mock(WorkspaceAiEvalRuleService.class),
            aiGatewayChatModelFactory,
            aiGatewayProviderService,
            mock(AiObservabilityTraceService.class), mock(WorkspaceAiObservabilityTraceService.class),
            aiObservabilitySpanService,
            staticProvider(new SimpleMeterRegistry()));

        AiObservabilityTrace trace = traceWith("q", "a", "{}");

        trace.setWorkspaceId(99L);
        stamp(trace, "id", 7L);

        Method executeEvaluation = AiEvalExecutor.class.getDeclaredMethod(
            "executeEvaluation", AiEvalExecution.class, AiEvalRule.class, AiObservabilityTrace.class);

        executeEvaluation.setAccessible(true);
        executeEvaluation.invoke(executor, mock(AiEvalExecution.class), aiEvalRule(1L, 42L, "rule"), trace);

        assertThat(recordingAdvisor.advisedCall).isTrue();
    }

    private static final class RecordingAdvisor implements CallAdvisor {

        private boolean advisedCall;

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
            advisedCall = true;

            return callAdvisorChain.nextCall(chatClientRequest);
        }

        @Override
        public String getName() {
            return "RecordingAdvisor";
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }
}
