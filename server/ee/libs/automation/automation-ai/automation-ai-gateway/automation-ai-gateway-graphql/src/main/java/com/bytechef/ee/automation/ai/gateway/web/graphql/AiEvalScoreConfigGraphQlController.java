/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreConfig;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreDataType;
import com.bytechef.ee.automation.ai.gateway.service.AiEvalScoreConfigService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiEvalScoreConfigGraphQlController {

    private final AiEvalScoreConfigService aiEvalScoreConfigService;

    @SuppressFBWarnings("EI")
    AiEvalScoreConfigGraphQlController(AiEvalScoreConfigService aiEvalScoreConfigService) {
        this.aiEvalScoreConfigService = aiEvalScoreConfigService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalScoreConfig aiEvalScoreConfig(@Argument long id) {
        return aiEvalScoreConfigService.getScoreConfig(id);
    }

    @QueryMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")
    public List<AiEvalScoreConfig> aiEvalScoreConfigs(@Argument Long workspaceId) {
        return aiEvalScoreConfigService.getScoreConfigsByWorkspace(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalScoreConfig createAiEvalScoreConfig(
        @Argument Long workspaceId, @Argument String name, @Argument AiEvalScoreDataType dataType,
        @Argument Double minValue, @Argument Double maxValue, @Argument String categories,
        @Argument String description) {

        AiEvalScoreConfig scoreConfig = new AiEvalScoreConfig(workspaceId, name);

        scoreConfig.setDataType(dataType);

        if (minValue != null) {
            scoreConfig.setMinValue(BigDecimal.valueOf(minValue));
        }

        if (maxValue != null) {
            scoreConfig.setMaxValue(BigDecimal.valueOf(maxValue));
        }

        scoreConfig.setCategories(categories);
        scoreConfig.setDescription(description);

        return aiEvalScoreConfigService.create(scoreConfig);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiEvalScoreConfig(@Argument long id) {
        aiEvalScoreConfigService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalScoreConfig updateAiEvalScoreConfig(
        @Argument long id, @Argument String name, @Argument AiEvalScoreDataType dataType,
        @Argument Double minValue, @Argument Double maxValue, @Argument String categories,
        @Argument String description) {

        AiEvalScoreConfig scoreConfig = aiEvalScoreConfigService.getScoreConfig(id);

        scoreConfig.setName(name);
        scoreConfig.setDataType(dataType);
        scoreConfig.setMinValue(minValue != null ? BigDecimal.valueOf(minValue) : null);
        scoreConfig.setMaxValue(maxValue != null ? BigDecimal.valueOf(maxValue) : null);
        scoreConfig.setCategories(categories);
        scoreConfig.setDescription(description);

        return aiEvalScoreConfigService.update(scoreConfig);
    }
}
