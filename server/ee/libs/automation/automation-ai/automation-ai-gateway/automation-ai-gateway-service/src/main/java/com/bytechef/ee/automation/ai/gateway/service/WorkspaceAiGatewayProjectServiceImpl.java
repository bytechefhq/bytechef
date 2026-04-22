/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayProject;
import com.bytechef.ee.automation.ai.gateway.repository.WorkspaceAiGatewayProjectRepository;
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
    private final WorkspaceAiGatewayProjectRepository workspaceAiGatewayProjectRepository;

    public WorkspaceAiGatewayProjectServiceImpl(
        AiGatewayProjectService aiGatewayProjectService,
        WorkspaceAiGatewayProjectRepository workspaceAiGatewayProjectRepository) {

        this.aiGatewayProjectService = aiGatewayProjectService;
        this.workspaceAiGatewayProjectRepository = workspaceAiGatewayProjectRepository;
    }

    @Override
    public AiGatewayProject createInWorkspace(AiGatewayProject project, long workspaceId) {
        AiGatewayProject savedProject = aiGatewayProjectService.create(project);

        workspaceAiGatewayProjectRepository.save(new WorkspaceAiGatewayProject(savedProject.getId(), workspaceId));

        return savedProject;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGatewayProject> fetchProjectByWorkspaceIdAndSlug(long workspaceId, String slug) {
        return workspaceAiGatewayProjectRepository.findProjectByWorkspaceIdAndSlug(workspaceId, slug);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getProjectWorkspaceMap() {
        Map<Long, Long> projectWorkspaceMap = new HashMap<>();

        for (WorkspaceAiGatewayProject membership : workspaceAiGatewayProjectRepository.findAll()) {
            projectWorkspaceMap.put(membership.getAiGatewayProjectId(), membership.getWorkspaceId());
        }

        return projectWorkspaceMap;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayProject> getProjectsByWorkspaceId(long workspaceId) {
        return workspaceAiGatewayProjectRepository.findProjectsByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long projectId) {
        return workspaceAiGatewayProjectRepository.findByAiGatewayProjectId(projectId)
            .map(WorkspaceAiGatewayProject::getWorkspaceId)
            .orElse(null);
    }
}
