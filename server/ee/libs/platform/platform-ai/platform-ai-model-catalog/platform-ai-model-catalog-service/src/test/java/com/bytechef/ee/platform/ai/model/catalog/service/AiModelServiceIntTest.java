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
import org.springframework.test.util.ReflectionTestUtils;

/**
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

    @Test
    void testUpdateChangesAndClearsDefaultRoutingPolicyId() {
        AiModel model = new AiModel(3L, "gpt-5-routing-policy-test");

        AiModel savedModel = aiModelService.create(model);

        AiModel updateWithPolicy = new AiModel(3L, "gpt-5-routing-policy-test");

        ReflectionTestUtils.setField(updateWithPolicy, "id", savedModel.getId());
        updateWithPolicy.setDefaultRoutingPolicyId(42L);

        AiModel updatedModel = aiModelService.update(updateWithPolicy);

        assertThat(updatedModel.getDefaultRoutingPolicyId()).isEqualTo(42L);

        AiModel refetchedModel = aiModelService.getModel(savedModel.getId());

        assertThat(refetchedModel.getDefaultRoutingPolicyId()).isEqualTo(42L);

        AiModel updateClearingPolicy = new AiModel(3L, "gpt-5-routing-policy-test");

        ReflectionTestUtils.setField(updateClearingPolicy, "id", savedModel.getId());
        updateClearingPolicy.setDefaultRoutingPolicyId(null);

        AiModel clearedModel = aiModelService.update(updateClearingPolicy);

        assertThat(clearedModel.getDefaultRoutingPolicyId()).isNull();

        AiModel refetchedClearedModel = aiModelService.getModel(savedModel.getId());

        assertThat(refetchedClearedModel.getDefaultRoutingPolicyId()).isNull();
    }
}
