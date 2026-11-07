/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.tool.usage;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.tool.usage.config.AiToolUsageIntTestConfiguration;
import com.bytechef.ee.platform.ai.tool.usage.repository.AiToolUsageRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiToolUsageIntTestConfiguration.class)
class AiToolUsageRecorderIntTest {

    @Autowired
    private AiToolUsageRepository aiToolUsageRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ToolUsageRecorder toolUsageRecorder;

    @AfterEach
    void afterEach() {
        aiToolUsageRepository.deleteAll();
    }

    @Test
    void testRecorderIsTransactionalProxy() {
        assertThat(AopUtils.isAopProxy(toolUsageRecorder)).isTrue();
    }

    @Test
    void testRecordToolPersistsRow() {
        toolUsageRecorder.recordTool(
            new ToolUsageContext(3L, 7L, 1, 42L), "scrape", 2, 150L, Map.of("url", "https://example.com"));

        List<AiToolUsage> aiToolUsages = (List<AiToolUsage>) aiToolUsageRepository.findAll();

        assertThat(aiToolUsages).hasSize(1);

        AiToolUsage aiToolUsage = aiToolUsages.getFirst();

        assertThat(aiToolUsage.getId()).isNotNull();
        assertThat(aiToolUsage.getWorkspaceId()).isEqualTo(3L);
        assertThat(aiToolUsage.getUserId()).isEqualTo(7L);
        assertThat(aiToolUsage.getOwnerId()).isEqualTo(42L);
        assertThat(aiToolUsage.getEnvironment()).isEqualTo(1);
        assertThat(aiToolUsage.getToolName()).isEqualTo("scrape");
        assertThat(aiToolUsage.getUnitCount()).isEqualTo(2);
        assertThat(aiToolUsage.getDurationMs()).isEqualTo(150);
        assertThat(aiToolUsage.getEstimatedCostUsd()).isEqualByComparingTo(
            AiToolUsageIntTestConfiguration.SCRAPE_RATE.multiply(BigDecimal.TWO));
        assertThat(aiToolUsage.getMetadataJson()).isEqualTo("{\"url\":\"https://example.com\"}");
        assertThat(aiToolUsage.getCreatedAt()).isNotNull();
    }

    @Test
    void testRecordToolSurvivesCallerRollback() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            toolUsageRecorder.recordTool(ToolUsageContext.ofWorkspaceUser(3L, 7L, 0), "scrape", 1, 10L, null);

            transactionStatus.setRollbackOnly();
        });

        List<AiToolUsage> aiToolUsages = (List<AiToolUsage>) aiToolUsageRepository.findAll();

        assertThat(aiToolUsages).hasSize(1);

        AiToolUsage aiToolUsage = aiToolUsages.getFirst();

        assertThat(aiToolUsage.getOwnerId()).isNull();
        assertThat(aiToolUsage.getMetadataJson()).isNull();
    }
}
