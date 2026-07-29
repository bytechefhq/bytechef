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
import com.bytechef.ee.platform.ai.eval.dto.AiEvalScoreTrendPoint;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayIntTestConfigurationSharedMocks
public class AiEvalScoreServiceIntTest {

    private static final Long OTHER_WORKSPACE_ID = 2L;
    private static final String TREND_SCORE_NAME = "trend-relevance";
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

    /**
     * Exercises the day-bucketed trend aggregation against a real Postgres. It used to aggregate across a JOIN to the
     * workspace_ai_eval_score relation table and now filters ai_eval_score.workspace_id directly; nothing covered the
     * statement before, so a rewrite of its GROUP BY / projection would have gone unnoticed. Also pins that a score in
     * another workspace does not leak into the bucket.
     */
    @Test
    public void testScoreTrendAggregatesNumericScoresOfOneWorkspace() {
        long traceId = createTrace("trend-trace", WORKSPACE_ID);
        long otherTraceId = createTrace("other-trend-trace", OTHER_WORKSPACE_ID);

        createNumericScore(traceId, WORKSPACE_ID, new BigDecimal("0.20"));
        createNumericScore(traceId, WORKSPACE_ID, new BigDecimal("0.80"));
        createNumericScore(otherTraceId, OTHER_WORKSPACE_ID, new BigDecimal("1.00"));

        Instant now = Instant.now();

        List<AiEvalScoreTrendPoint> trendPoints = workspaceAiEvalScoreService.getScoreTrend(
            WORKSPACE_ID, TREND_SCORE_NAME, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)));

        assertThat(trendPoints).hasSize(1);

        AiEvalScoreTrendPoint trendPoint = trendPoints.getFirst();

        assertThat(trendPoint.count()).isEqualTo(2);
        assertThat(trendPoint.average()).isEqualTo(0.5);
    }

    /**
     * Pins uk_ai_eval_score_config_workspace_name. The table shipped with this constraint, lost it when workspace_id
     * moved onto a relation table, and regained it when the column came back — with nothing covering it in between.
     * fetchScoreConfigByWorkspaceIdAndName returns an Optional, so without the constraint a duplicate name does not
     * shadow the first config, it breaks every subsequent score write in that workspace on an incorrect-result-size
     * read. The second half pins that the uniqueness is workspace-scoped, not global.
     */
    @Test
    public void testDuplicateScoreConfigNameIsRejectedWithinAWorkspaceButNotAcrossWorkspaces() {
        String configName = "duplicate-name-config";

        workspaceAiEvalScoreConfigService.createInWorkspace(new AiEvalScoreConfig(configName), WORKSPACE_ID);

        assertThatThrownBy(
            () -> workspaceAiEvalScoreConfigService.createInWorkspace(
                new AiEvalScoreConfig(configName), WORKSPACE_ID))
                    .isInstanceOf(DataIntegrityViolationException.class);

        AiEvalScoreConfig otherWorkspaceConfig = workspaceAiEvalScoreConfigService.createInWorkspace(
            new AiEvalScoreConfig(configName), OTHER_WORKSPACE_ID);

        assertThat(otherWorkspaceConfig.getId()).isNotNull();
    }

    private void createNumericScore(long traceId, Long workspaceId, BigDecimal value) {
        AiEvalScore score = AiEvalScore.numeric(traceId, TREND_SCORE_NAME, AiEvalScoreSource.MANUAL, value);

        workspaceAiEvalScoreService.createInWorkspace(score, workspaceId);
    }

    private long createTrace(String name, Long workspaceId) {
        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        trace.setName(name);

        workspaceAiObservabilityTraceService.createInWorkspace(trace, workspaceId);

        return Validate.notNull(trace.getId(), "id");
    }
}
