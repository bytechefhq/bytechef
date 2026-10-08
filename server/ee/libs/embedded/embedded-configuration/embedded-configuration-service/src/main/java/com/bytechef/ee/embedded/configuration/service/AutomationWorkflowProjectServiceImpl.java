/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProject;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectWorkflow;
import com.bytechef.ee.embedded.configuration.repository.AutomationWorkflowProjectRepository;
import com.bytechef.ee.embedded.configuration.repository.AutomationWorkflowProjectWorkflowRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AutomationWorkflowProjectServiceImpl implements AutomationWorkflowProjectService {

    private final AutomationWorkflowProjectRepository automationWorkflowProjectRepository;
    private final AutomationWorkflowProjectWorkflowRepository automationWorkflowProjectWorkflowRepository;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectServiceImpl(
        AutomationWorkflowProjectRepository automationWorkflowProjectRepository,
        AutomationWorkflowProjectWorkflowRepository automationWorkflowProjectWorkflowRepository) {

        this.automationWorkflowProjectRepository = automationWorkflowProjectRepository;
        this.automationWorkflowProjectWorkflowRepository = automationWorkflowProjectWorkflowRepository;
    }

    @Override
    public AutomationWorkflowProject create(
        long projectId, @Nullable String permissionExpression, boolean automationHubVisible) {

        AutomationWorkflowProject automationWorkflowProject = new AutomationWorkflowProject(projectId);

        automationWorkflowProject.setAutomationHubVisible(automationHubVisible);
        automationWorkflowProject.setPermissionExpression(permissionExpression);

        return automationWorkflowProjectRepository.save(automationWorkflowProject);
    }

    @Override
    public void delete(long projectId) {
        automationWorkflowProjectWorkflowRepository.deleteByProjectId(projectId);
        automationWorkflowProjectRepository.deleteByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AutomationWorkflowProject> fetchAutomationWorkflowProject(long projectId) {
        return automationWorkflowProjectRepository.findByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationWorkflowProject getAutomationWorkflowProject(long projectId) {
        return automationWorkflowProjectRepository.findByProjectId(projectId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Project with id " + projectId + " is not an automation workflow project"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationWorkflowProject> getAutomationWorkflowProjects() {
        return automationWorkflowProjectRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> getWorkflowPermissionExpressions(long projectId) {
        return automationWorkflowProjectWorkflowRepository.findAllByProjectId(projectId)
            .stream()
            .filter(automationWorkflowProjectWorkflow -> automationWorkflowProjectWorkflow
                .getPermissionExpression() != null)
            .collect(
                Collectors.toMap(
                    AutomationWorkflowProjectWorkflow::getWorkflowUuid,
                    AutomationWorkflowProjectWorkflow::getPermissionExpression));
    }

    @Override
    public void updateAutomationHubVisible(long projectId, boolean automationHubVisible) {
        AutomationWorkflowProject automationWorkflowProject = getAutomationWorkflowProject(projectId);

        automationWorkflowProject.setAutomationHubVisible(automationHubVisible);

        automationWorkflowProjectRepository.save(automationWorkflowProject);
    }

    @Override
    public void updatePermissionExpression(long projectId, @Nullable String permissionExpression) {
        AutomationWorkflowProject automationWorkflowProject = getAutomationWorkflowProject(projectId);

        automationWorkflowProject.setPermissionExpression(permissionExpression);

        automationWorkflowProjectRepository.save(automationWorkflowProject);
    }

    @Override
    public void updateWorkflowPermissionExpression(
        long projectId, UUID workflowUuid, @Nullable String permissionExpression) {

        Optional<AutomationWorkflowProjectWorkflow> automationWorkflowProjectWorkflowOptional =
            automationWorkflowProjectWorkflowRepository.findByWorkflowUuid(workflowUuid);

        if (permissionExpression == null) {
            automationWorkflowProjectWorkflowOptional.ifPresent(automationWorkflowProjectWorkflowRepository::delete);

            return;
        }

        AutomationWorkflowProjectWorkflow automationWorkflowProjectWorkflow = automationWorkflowProjectWorkflowOptional
            .orElseGet(() -> new AutomationWorkflowProjectWorkflow(projectId, workflowUuid));

        automationWorkflowProjectWorkflow.setPermissionExpression(permissionExpression);

        automationWorkflowProjectWorkflowRepository.save(automationWorkflowProjectWorkflow);
    }
}
