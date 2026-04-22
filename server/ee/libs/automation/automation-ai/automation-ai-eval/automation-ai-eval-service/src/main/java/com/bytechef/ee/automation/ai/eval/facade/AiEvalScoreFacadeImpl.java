/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.facade;

import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreService;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.dto.AiEvalScoreTrendPoint;
import com.bytechef.ee.platform.ai.eval.service.AiEvalScoreService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiEvalScoreFacade}. Delegates to the shared {@code AiEvalScoreService} and
 * {@code WorkspaceAiEvalScoreService} and carries the authorization guards so they are enforced for every caller of the
 * facade rather than only the GraphQL entry point.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiEvalScoreFacadeImpl implements AiEvalScoreFacade {

    private final AiEvalScoreService aiEvalScoreService;
    private final WorkspaceAiEvalScoreService workspaceAiEvalScoreService;

    @SuppressFBWarnings("EI")
    AiEvalScoreFacadeImpl(
        AiEvalScoreService aiEvalScoreService, WorkspaceAiEvalScoreService workspaceAiEvalScoreService) {

        this.aiEvalScoreService = aiEvalScoreService;
        this.workspaceAiEvalScoreService = workspaceAiEvalScoreService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalScore createInWorkspace(AiEvalScore score, long workspaceId) {
        return workspaceAiEvalScoreService.createInWorkspace(score, workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteInWorkspace(long id) {
        workspaceAiEvalScoreService.deleteInWorkspace(id);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public List<AiEvalScore> getScoresByWorkspace(Long workspaceId) {
        return workspaceAiEvalScoreService.getScoresByWorkspace(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiEvalScore> getScoresByWorkspaceForAnalytics(Long workspaceId) {
        return workspaceAiEvalScoreService.getScoresByWorkspace(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiEvalScore> getScoresByTrace(Long traceId) {
        return aiEvalScoreService.getScoresByTrace(traceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiEvalScoreTrendPoint> getScoreTrend(Long workspaceId, String name, Instant start, Instant end) {
        return workspaceAiEvalScoreService.getScoreTrend(workspaceId, name, start, end);
    }
}
