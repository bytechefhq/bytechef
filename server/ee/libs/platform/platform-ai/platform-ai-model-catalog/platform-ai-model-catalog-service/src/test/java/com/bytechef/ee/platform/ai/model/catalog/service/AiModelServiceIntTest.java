/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.model.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.model.catalog.config.AiModelCatalogIntTestConfiguration;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Proves the renamed {@code ai_model} table is created from scratch by the module's own changelog against a real
 * PostgreSQL via Testcontainers — the {@code liquibase} Spring profile applies nothing via {@code bootRun}, so a
 * fresh-schema integration test is the real evidence. Deliberately runs WITHOUT {@code bytechef.ai.gateway.enabled},
 * pinning that the catalog registers independently of the gateway toggle.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiModelCatalogIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class AiModelServiceIntTest {

    @Autowired
    private AiModelService aiModelService;

    @Test
    void testCreateAndFetchModelAgainstFreshSchema() {
        AiModel model = new AiModel(1L, "gpt-5");

        model.setContextWindow(400000);
        model.setInputCostPerMTokens(new BigDecimal("1.2500"));

        AiModel savedModel = aiModelService.create(model);

        assertThat(savedModel.getId()).isNotNull();

        AiModel fetchedModel = aiModelService.getModel(1L, "gpt-5");

        assertThat(fetchedModel.getContextWindow()).isEqualTo(400000);
        assertThat(aiModelService.findByModelIdentifier("gpt-5")).isPresent();
    }

    @Test
    void testDeleteRemovesRow() {
        AiModel savedModel = aiModelService.create(new AiModel(2L, "claude-fable-5"));

        aiModelService.delete(savedModel.getId());

        assertThat(aiModelService.findByModelIdentifier("claude-fable-5")).isEmpty();
    }
}
