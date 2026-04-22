/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreConfigService;
import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreService;
import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreSource;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
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
    private WorkspaceAiEvalScoreService workspaceAiEvalScoreService;

    @Autowired
    private WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

    @Autowired
    private WorkspaceAiEvalScoreConfigService workspaceAiEvalScoreConfigService;

    @Autowired
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Test
    public void testCreateValidScoreAndRejectOutOfRange() {
        AiEvalScoreConfig scoreConfig = new AiEvalScoreConfig(SCORE_NAME);

        scoreConfig.setDataType(AiEvalScoreDataType.NUMERIC);
        scoreConfig.setMinValue(BigDecimal.ZERO);
        scoreConfig.setMaxValue(BigDecimal.ONE);

        workspaceAiEvalScoreConfigService.createInWorkspace(scoreConfig, WORKSPACE_ID);

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        trace.setName("scored-trace");

        workspaceAiObservabilityTraceService.createInWorkspace(trace, WORKSPACE_ID);

        Long traceId = Validate.notNull(trace.getId(), "id");

        AiEvalScore validScore = AiEvalScore.numeric(
            traceId, SCORE_NAME, AiEvalScoreSource.MANUAL, new BigDecimal("0.75"));

        AiEvalScore created = workspaceAiEvalScoreService.createInWorkspace(validScore, WORKSPACE_ID);

        assertThat(created.getId()).isNotNull();

        AiEvalScore tooHigh = AiEvalScore.numeric(
            traceId, SCORE_NAME, AiEvalScoreSource.MANUAL, new BigDecimal("1.5"));

        assertThatThrownBy(() -> workspaceAiEvalScoreService.createInWorkspace(tooHigh, WORKSPACE_ID))
            .isInstanceOf(IllegalArgumentException.class);

        AiEvalScore tooLow = AiEvalScore.numeric(
            traceId, SCORE_NAME, AiEvalScoreSource.MANUAL, new BigDecimal("-0.1"));

        assertThatThrownBy(() -> workspaceAiEvalScoreService.createInWorkspace(tooLow, WORKSPACE_ID))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
