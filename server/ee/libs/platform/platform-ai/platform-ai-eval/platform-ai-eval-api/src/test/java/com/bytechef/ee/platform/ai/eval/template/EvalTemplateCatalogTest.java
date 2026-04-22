/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.template;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class EvalTemplateCatalogTest {

    @Test
    void testCatalogHasEightUniqueTemplates() {
        List<EvalTemplate> templates = EvalTemplateCatalog.templates();

        assertThat(templates).hasSize(8);
        assertThat(templates.stream()
            .map(EvalTemplate::key)).doesNotHaveDuplicates();
    }

    @Test
    void testEveryTemplateDeclaresAValidPromptAndScoreShape() {
        for (EvalTemplate template : EvalTemplateCatalog.templates()) {
            assertThat(template.promptTemplate()).contains("{{");
            assertThat(template.dataType()).isNotNull();

            if (template.dataType() == AiEvalScoreDataType.NUMERIC) {
                assertThat(template.minValue()).isNotNull();
                assertThat(template.maxValue()).isNotNull();
            }
        }
    }

    @Test
    void testContextTemplatesReferenceContextVariable() {
        for (String key : List.of("hallucination", "context_relevance", "context_correctness")) {
            assertThat(EvalTemplateCatalog.byKey(key)).get()
                .extracting(EvalTemplate::promptTemplate, as(STRING))
                .contains("{{context}}");
        }
    }
}
