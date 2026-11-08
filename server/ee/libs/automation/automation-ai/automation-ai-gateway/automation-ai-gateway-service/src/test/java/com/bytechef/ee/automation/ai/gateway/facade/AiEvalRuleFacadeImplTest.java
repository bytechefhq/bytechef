/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalRuleService;
import com.bytechef.ee.automation.ai.gateway.evaluation.AiEvalExecutor;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRuleTarget;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import com.bytechef.ee.platform.ai.eval.service.AiEvalExecutionService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalRuleService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalScoreConfigService;
import com.bytechef.ee.platform.ai.eval.template.EvalTemplate;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiEvalRuleFacadeImplTest {

    @Mock
    private AiEvalExecutionService aiEvalExecutionService;

    @Mock
    private AiEvalExecutor aiEvalExecutor;

    @Mock
    private AiEvalRuleService aiEvalRuleService;

    @Mock
    private AiEvalScoreConfigService aiEvalScoreConfigService;

    @Mock
    private WorkspaceAiEvalRuleService workspaceAiEvalRuleService;

    @Mock
    private WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

    private AiEvalRuleFacadeImpl facade;

    @BeforeEach
    void beforeEach() {
        facade = createFacade(1_000);
    }

    @Test
    void testListTemplatesReturnsCatalog() {
        assertThat(facade.listTemplates()).hasSize(8);
    }

    @Test
    void testInstantiateTemplateCreatesConfigAndDisabledRule() {
        when(aiEvalScoreConfigService.create(any())).thenAnswer(invocation -> {
            AiEvalScoreConfig config = invocation.getArgument(0);

            ReflectionTestUtils.setField(config, "id", 42L);

            return config;
        });
        when(workspaceAiEvalRuleService.createInWorkspace(any(), eq(7L)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalRule rule = facade.instantiateTemplate(
            "toxicity", 7L, 3L, "gpt-4o", new BigDecimal("0.10"));

        assertThat(rule.getPromptTemplate()).contains("{{output}}");
        assertThat(rule.getScoreConfigId()).isEqualTo(42L);
        assertThat(rule.getModel()).isEqualTo("gpt-4o");
        assertThat(rule.getProjectId()).isEqualTo(3L);
        assertThat(rule.isEnabled()).isFalse();
        assertThat(rule.getTarget()).isEqualTo(AiEvalRuleTarget.LIVE_TRACE);
    }

    @Test
    void testInstantiateUnknownTemplateThrows() {
        assertThatThrownBy(() -> facade.instantiateTemplate("nope", 7L, 3L, "gpt-4o", BigDecimal.ONE))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testListTemplatesReturnsEvalTemplateInstances() {
        assertThat(facade.listTemplates()).allSatisfy(
            template -> assertThat(template).isInstanceOf(EvalTemplate.class));
    }

    @Test
    void testRunOnHistoricalTracesQueuesEveryTraceInTheRuleWorkspaceWindow() {
        AiEvalRule evalRule = new AiEvalRule("Toxicity", 1L, "prompt", "openai/gpt-4o", BigDecimal.ONE);

        ReflectionTestUtils.setField(evalRule, "id", 5L);

        when(aiEvalRuleService.getEvalRule(5L)).thenReturn(evalRule);
        when(workspaceAiEvalRuleService.getWorkspaceId(5L)).thenReturn(7L);
        when(workspaceAiObservabilityTraceService.getTracesByWorkspace(
            7L, Instant.ofEpochMilli(1_000L), Instant.ofEpochMilli(2_000L)))
                .thenReturn(List.of(createTrace(11L), createTrace(12L)));

        int queuedCount = facade.runOnHistoricalTraces(5L, 1_000L, 2_000L);

        assertThat(queuedCount).isEqualTo(2);

        verify(aiEvalExecutor).evaluateTraceForRule(11L, 5L);
        verify(aiEvalExecutor).evaluateTraceForRule(12L, 5L);
    }

    @Test
    void testRunOnHistoricalTracesStopsAtTheConfiguredMaximum() {
        AiEvalRuleFacadeImpl cappedFacade = createFacade(2);

        AiEvalRule evalRule = new AiEvalRule("Toxicity", 1L, "prompt", "openai/gpt-4o", BigDecimal.ONE);

        ReflectionTestUtils.setField(evalRule, "id", 5L);

        when(aiEvalRuleService.getEvalRule(5L)).thenReturn(evalRule);
        when(workspaceAiEvalRuleService.getWorkspaceId(5L)).thenReturn(7L);
        when(workspaceAiObservabilityTraceService.getTracesByWorkspace(
            7L, Instant.ofEpochMilli(1_000L), Instant.ofEpochMilli(2_000L)))
                .thenReturn(List.of(createTrace(11L), createTrace(12L), createTrace(13L), createTrace(14L)));

        int queuedCount = cappedFacade.runOnHistoricalTraces(5L, 1_000L, 2_000L);

        assertThat(queuedCount).isEqualTo(2);

        verify(aiEvalExecutor).evaluateTraceForRule(11L, 5L);
        verify(aiEvalExecutor).evaluateTraceForRule(12L, 5L);
        verify(aiEvalExecutor, times(2)).evaluateTraceForRule(anyLong(), anyLong());
    }

    private AiEvalRuleFacadeImpl createFacade(int runOnHistoryMaxTraces) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Ai ai = applicationProperties.getAi();
        ApplicationProperties.Ai.Eval eval = ai.getEval();
        ApplicationProperties.Ai.Eval.RunOnHistory runOnHistory = eval.getRunOnHistory();

        runOnHistory.setMaxTraces(runOnHistoryMaxTraces);

        return new AiEvalRuleFacadeImpl(
            aiEvalExecutionService, aiEvalExecutor, aiEvalRuleService, aiEvalScoreConfigService,
            applicationProperties, workspaceAiEvalRuleService, workspaceAiObservabilityTraceService);
    }

    private static AiObservabilityTrace createTrace(long traceId) {
        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        ReflectionTestUtils.setField(trace, "id", traceId);

        return trace;
    }
}
