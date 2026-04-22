/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.facade;

import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreConfigService;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import com.bytechef.ee.platform.ai.eval.service.AiEvalScoreConfigService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiEvalScoreConfigFacade}. Delegates to the shared {@code AiEvalScoreConfigService} and
 * {@code WorkspaceAiEvalScoreConfigService} and carries the authorization guards so they are enforced for every caller
 * of the facade rather than only the GraphQL entry point.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiEvalScoreConfigFacadeImpl implements AiEvalScoreConfigFacade {

    private final AiEvalScoreConfigService aiEvalScoreConfigService;
    private final WorkspaceAiEvalScoreConfigService workspaceAiEvalScoreConfigService;

    @SuppressFBWarnings("EI")
    AiEvalScoreConfigFacadeImpl(
        AiEvalScoreConfigService aiEvalScoreConfigService,
        WorkspaceAiEvalScoreConfigService workspaceAiEvalScoreConfigService) {

        this.aiEvalScoreConfigService = aiEvalScoreConfigService;
        this.workspaceAiEvalScoreConfigService = workspaceAiEvalScoreConfigService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalScoreConfig getScoreConfig(long id) {
        return aiEvalScoreConfigService.getScoreConfig(id);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public List<AiEvalScoreConfig> getScoreConfigsByWorkspace(Long workspaceId) {
        return workspaceAiEvalScoreConfigService.getScoreConfigsByWorkspace(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalScoreConfig createInWorkspace(AiEvalScoreConfig scoreConfig, Long workspaceId) {
        return workspaceAiEvalScoreConfigService.createInWorkspace(scoreConfig, workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteInWorkspace(long id) {
        workspaceAiEvalScoreConfigService.deleteInWorkspace(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalScoreConfig update(AiEvalScoreConfig scoreConfig) {
        return aiEvalScoreConfigService.update(scoreConfig);
    }
}
