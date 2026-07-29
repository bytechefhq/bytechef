/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProjectService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class WorkspaceAiGatewayProjectServiceImpl implements WorkspaceAiGatewayProjectService {

    private final AiGatewayProjectService aiGatewayProjectService;

    public WorkspaceAiGatewayProjectServiceImpl(AiGatewayProjectService aiGatewayProjectService) {
        this.aiGatewayProjectService = aiGatewayProjectService;
    }

    @Override
    public AiGatewayProject createInWorkspace(AiGatewayProject project, long workspaceId) {
        project.setWorkspaceId(workspaceId);

        return aiGatewayProjectService.create(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGatewayProject> fetchProjectByWorkspaceIdAndSlug(long workspaceId, String slug) {
        return aiGatewayProjectService.fetchProjectByWorkspaceIdAndSlug(workspaceId, slug);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getProjectWorkspaceMap() {
        Map<Long, Long> projectWorkspaceMap = new HashMap<>();

        for (AiGatewayProject project : aiGatewayProjectService.getProjects()) {
            Long workspaceId = project.getWorkspaceId();

            // A project with no workspace was previously represented by the absence of a membership row, so it stays
            // absent from the map rather than mapping to a null value.
            if (workspaceId != null) {
                projectWorkspaceMap.put(project.getId(), workspaceId);
            }
        }

        return projectWorkspaceMap;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayProject> getProjectsByWorkspaceId(long workspaceId) {
        return aiGatewayProjectService.getProjectsByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long projectId) {
        return aiGatewayProjectService.fetchProject(projectId)
            .map(AiGatewayProject::getWorkspaceId)
            .orElse(null);
    }
}
