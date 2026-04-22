/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.eval.facade.AiEvalRuleFacade;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import com.bytechef.ee.platform.ai.eval.template.EvalTemplate;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiEvalRuleGraphQlControllerTest {

    private final AiEvalRuleFacade aiEvalRuleFacade = mock(AiEvalRuleFacade.class);

    private final AiEvalRuleGraphQlController aiEvalRuleGraphQlController = new AiEvalRuleGraphQlController(
        aiEvalRuleFacade);

    @Test
    void testAiEvalTemplatesReturnsFacadeCatalog() {
        List<EvalTemplate> templates = List.of(
            new EvalTemplate(
                "toxicity", "Toxicity", "description", "prompt", AiEvalScoreDataType.BOOLEAN, null, null,
                List.of()));

        when(aiEvalRuleFacade.listTemplates()).thenReturn(templates);

        List<EvalTemplate> result = aiEvalRuleGraphQlController.aiEvalTemplates();

        assertThat(result).isSameAs(templates);
    }

    @Test
    void testInstantiateAiEvalTemplateDelegatesToFacade() {
        AiEvalRule evalRule = new AiEvalRule("Toxicity", 1L, "prompt", "gpt-4o", BigDecimal.valueOf(0.1));

        when(aiEvalRuleFacade.instantiateTemplate("toxicity", 7L, 3L, "gpt-4o", BigDecimal.valueOf(0.1)))
            .thenReturn(evalRule);

        AiEvalRule result = aiEvalRuleGraphQlController.instantiateAiEvalTemplate(
            "toxicity", 7L, 3L, "gpt-4o", 0.1);

        assertThat(result).isSameAs(evalRule);

        ArgumentCaptor<BigDecimal> samplingRateCaptor = ArgumentCaptor.forClass(BigDecimal.class);

        verify(aiEvalRuleFacade)
            .instantiateTemplate(eq("toxicity"), eq(7L), eq(3L), eq("gpt-4o"), samplingRateCaptor.capture());

        assertThat(samplingRateCaptor.getValue()).isEqualByComparingTo(BigDecimal.valueOf(0.1));
    }
}
