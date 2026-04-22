/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.eval.facade.AiEvalScoreConfigFacade;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Authorization is enforced on {@link AiEvalScoreConfigFacade}, not here.
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiEvalScoreConfigGraphQlController {

    private final AiEvalScoreConfigFacade aiEvalScoreConfigFacade;

    @SuppressFBWarnings("EI")
    AiEvalScoreConfigGraphQlController(AiEvalScoreConfigFacade aiEvalScoreConfigFacade) {
        this.aiEvalScoreConfigFacade = aiEvalScoreConfigFacade;
    }

    @QueryMapping
    public AiEvalScoreConfig aiEvalScoreConfig(@Argument long id) {
        return aiEvalScoreConfigFacade.getScoreConfig(id);
    }

    @QueryMapping
    public List<AiEvalScoreConfig> aiEvalScoreConfigs(@Argument Long workspaceId) {
        return aiEvalScoreConfigFacade.getScoreConfigsByWorkspace(workspaceId);
    }

    @MutationMapping
    public AiEvalScoreConfig createAiEvalScoreConfig(
        @Argument Long workspaceId, @Argument String name, @Argument AiEvalScoreDataType dataType,
        @Argument Double minValue, @Argument Double maxValue, @Argument String categories,
        @Argument String description) {

        AiEvalScoreConfig scoreConfig = new AiEvalScoreConfig(name);

        scoreConfig.setDataType(dataType);

        if (minValue != null) {
            scoreConfig.setMinValue(BigDecimal.valueOf(minValue));
        }

        if (maxValue != null) {
            scoreConfig.setMaxValue(BigDecimal.valueOf(maxValue));
        }

        scoreConfig.setCategories(categories);
        scoreConfig.setDescription(description);

        return aiEvalScoreConfigFacade.createInWorkspace(scoreConfig, workspaceId);
    }

    @MutationMapping
    public boolean deleteAiEvalScoreConfig(@Argument long id) {
        aiEvalScoreConfigFacade.deleteInWorkspace(id);

        return true;
    }

    @MutationMapping
    public AiEvalScoreConfig updateAiEvalScoreConfig(
        @Argument long id, @Argument String name, @Argument AiEvalScoreDataType dataType,
        @Argument Double minValue, @Argument Double maxValue, @Argument String categories,
        @Argument String description) {

        AiEvalScoreConfig scoreConfig = aiEvalScoreConfigFacade.getScoreConfig(id);

        scoreConfig.setName(name);
        scoreConfig.setDataType(dataType);
        scoreConfig.setMinValue(minValue != null ? BigDecimal.valueOf(minValue) : null);
        scoreConfig.setMaxValue(maxValue != null ? BigDecimal.valueOf(maxValue) : null);
        scoreConfig.setCategories(categories);
        scoreConfig.setDescription(description);

        return aiEvalScoreConfigFacade.update(scoreConfig);
    }
}
