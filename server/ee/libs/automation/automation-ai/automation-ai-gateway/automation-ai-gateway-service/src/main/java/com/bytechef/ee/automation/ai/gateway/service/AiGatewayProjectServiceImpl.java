/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProject;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayProjectRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
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
class AiGatewayProjectServiceImpl implements AiGatewayProjectService {

    private final AiGatewayProjectRepository aiGatewayProjectRepository;

    public AiGatewayProjectServiceImpl(AiGatewayProjectRepository aiGatewayProjectRepository) {
        this.aiGatewayProjectRepository = aiGatewayProjectRepository;
    }

    @Override
    public AiGatewayProject create(AiGatewayProject project) {
        Validate.notNull(project, "'project' must not be null");
        Validate.isTrue(project.getId() == null, "'id' must be null");

        return aiGatewayProjectRepository.save(project);
    }

    @Override
    public void delete(long id) {
        aiGatewayProjectRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGatewayProject> fetchProjectByWorkspaceIdAndSlug(long workspaceId, String slug) {
        return aiGatewayProjectRepository.findByWorkspaceIdAndSlug(workspaceId, slug);
    }

    @Override
    @Transactional(readOnly = true)
    public AiGatewayProject getProject(long id) {
        return aiGatewayProjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayProject> getProjectsByWorkspaceId(long workspaceId) {
        return aiGatewayProjectRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    public AiGatewayProject update(AiGatewayProject project) {
        Validate.notNull(project, "'project' must not be null");

        AiGatewayProject existingProject = aiGatewayProjectRepository.findById(project.getId())
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + project.getId()));

        existingProject.setCachingEnabled(project.getCachingEnabled());
        existingProject.setCacheTtlMinutes(project.getCacheTtlMinutes());
        existingProject.setCompressionEnabled(project.getCompressionEnabled());
        existingProject.setDescription(project.getDescription());
        existingProject.setLogRetentionDays(project.getLogRetentionDays());
        existingProject.setName(project.getName());
        existingProject.setRetryMaxAttempts(project.getRetryMaxAttempts());
        existingProject.setRoutingPolicyId(project.getRoutingPolicyId());
        existingProject.setSlug(project.getSlug());
        existingProject.setTimeoutSeconds(project.getTimeoutSeconds());

        return aiGatewayProjectRepository.save(existingProject);
    }
}
