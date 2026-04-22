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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link AiEvalRuleFacadeImpl#listTemplates()} and {@link AiEvalRuleFacadeImpl#instantiateTemplate}.
 * Calling the impl's methods directly bypasses Spring's method-security proxy, so {@code @PreAuthorize} enforcement is
 * NOT exercised here; see {@link AiEvalRuleFacadeAuthorizationTest} for the annotation pin and
 * {@code PreAuthorizeProxyEnforcementIntTest} for runtime enforcement.
 *
 * @version ee
 *
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
        facade = new AiEvalRuleFacadeImpl(
            aiEvalExecutionService, aiEvalExecutor, aiEvalRuleService, aiEvalScoreConfigService,
            workspaceAiEvalRuleService, workspaceAiObservabilityTraceService);
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
}
