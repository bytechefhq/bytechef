/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class FilterExpressionCodecTest {

    private final FilterExpressionCodec filterExpressionCodec = new FilterExpressionCodec(
        JsonMapper.builder()
            .build());

    @Test
    void testRoundTripOfNestedExpressions() {
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        List<Filter.Expression> filterExpressions = List.of(
            filterExpressionBuilder.and(
                filterExpressionBuilder.eq("knowledge_base_id", 7L),
                filterExpressionBuilder.or(
                    filterExpressionBuilder.eq("tag_names_a", true),
                    filterExpressionBuilder.in("source", "x", "y")))
                .build(),
            filterExpressionBuilder.not(filterExpressionBuilder.group(filterExpressionBuilder.gte("score", 0.5)))
                .build(),
            filterExpressionBuilder.nin("k", 1, 2)
                .build());

        for (Filter.Expression filterExpression : filterExpressions) {
            assertThat(filterExpressionCodec.read(filterExpressionCodec.write(filterExpression)))
                .isEqualTo(filterExpression);
        }
    }

    @Test
    void testUnsupportedValueTypeIsRejected() {
        Filter.Expression filterExpression = new Filter.Expression(
            Filter.ExpressionType.EQ, new Filter.Key("created"), new Filter.Value(new Object()));

        assertThatThrownBy(() -> filterExpressionCodec.write(filterExpression))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
