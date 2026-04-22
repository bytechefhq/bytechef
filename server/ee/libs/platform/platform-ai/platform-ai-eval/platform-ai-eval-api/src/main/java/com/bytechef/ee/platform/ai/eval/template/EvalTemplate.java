/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.template;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import java.math.BigDecimal;
import java.util.List;

/**
 * A static, built-in definition of an LLM-judge evaluator: a titled, described prompt template that produces either a
 * NUMERIC score (bounded by {@code minValue}/{@code maxValue}) or a BOOLEAN score.
 *
 * @version ee
 */
public record EvalTemplate(
    String key, String title, String description, String promptTemplate,
    AiEvalScoreDataType dataType, BigDecimal minValue, BigDecimal maxValue, List<String> categories) {

    public EvalTemplate {
        categories = List.copyOf(categories);
    }
}
