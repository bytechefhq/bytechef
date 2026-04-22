/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScore;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreConfig;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreDataType;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceSource;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayIntTestConfigurationSharedMocks
public class AiEvalScoreServiceIntTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final String SCORE_NAME = "relevance";

    @Autowired
    private AiEvalScoreService aiEvalScoreService;

    @Autowired
    private AiEvalScoreConfigService aiEvalScoreConfigService;

    @Autowired
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Test
    public void testCreateValidScoreAndRejectOutOfRange() {
        AiEvalScoreConfig scoreConfig = new AiEvalScoreConfig(WORKSPACE_ID, SCORE_NAME);

        scoreConfig.setDataType(AiEvalScoreDataType.NUMERIC);
        scoreConfig.setMinValue(BigDecimal.ZERO);
        scoreConfig.setMaxValue(BigDecimal.ONE);

        aiEvalScoreConfigService.create(scoreConfig);

        AiObservabilityTrace trace = new AiObservabilityTrace(WORKSPACE_ID, AiObservabilityTraceSource.API);

        trace.setName("scored-trace");

        aiObservabilityTraceService.create(trace);

        Long traceId = Validate.notNull(trace.getId(), "id");

        AiEvalScore validScore = AiEvalScore.numeric(
            WORKSPACE_ID, traceId, SCORE_NAME, AiEvalScoreSource.MANUAL, new BigDecimal("0.75"));

        AiEvalScore created = aiEvalScoreService.create(validScore);

        assertThat(created.getId()).isNotNull();

        AiEvalScore tooHigh = AiEvalScore.numeric(
            WORKSPACE_ID, traceId, SCORE_NAME, AiEvalScoreSource.MANUAL, new BigDecimal("1.5"));

        assertThatThrownBy(() -> aiEvalScoreService.create(tooHigh))
            .isInstanceOf(IllegalArgumentException.class);

        AiEvalScore tooLow = AiEvalScore.numeric(
            WORKSPACE_ID, traceId, SCORE_NAME, AiEvalScoreSource.MANUAL, new BigDecimal("-0.1"));

        assertThatThrownBy(() -> aiEvalScoreService.create(tooLow))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
