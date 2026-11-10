/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.AutomationProject;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectWorkflow;
import com.bytechef.ee.embedded.configuration.repository.AutomationProjectRepository;
import com.bytechef.ee.embedded.configuration.repository.AutomationProjectWorkflowRepository;
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
public class AutomationProjectServiceImpl implements AutomationProjectService {

    private final AutomationProjectRepository automationProjectRepository;
    private final AutomationProjectWorkflowRepository automationProjectWorkflowRepository;

    @SuppressFBWarnings("EI")
    public AutomationProjectServiceImpl(
        AutomationProjectRepository automationProjectRepository,
        AutomationProjectWorkflowRepository automationProjectWorkflowRepository) {

        this.automationProjectRepository = automationProjectRepository;
        this.automationProjectWorkflowRepository = automationProjectWorkflowRepository;
    }

    @Override
    public AutomationProject create(long projectId, @Nullable String permissionExpression) {
        AutomationProject automationProject = new AutomationProject(projectId);

        automationProject.setPermissionExpression(permissionExpression);

        return automationProjectRepository.save(automationProject);
    }

    @Override
    public void delete(long projectId) {
        automationProjectWorkflowRepository.deleteByProjectId(projectId);
        automationProjectRepository.deleteByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AutomationProject> fetchAutomationProject(long projectId) {
        return automationProjectRepository.findByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationProject getAutomationProject(long projectId) {
        return automationProjectRepository.findByProjectId(projectId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Project with id " + projectId + " is not an automation project"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationProject> getAutomationProjects() {
        return automationProjectRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> getWorkflowPermissionExpressions(long projectId) {
        return automationProjectWorkflowRepository.findAllByProjectId(projectId)
            .stream()
            .filter(automationProjectWorkflow -> automationProjectWorkflow
                .getPermissionExpression() != null)
            .collect(
                Collectors.toMap(
                    AutomationProjectWorkflow::getWorkflowUuid,
                    AutomationProjectWorkflow::getPermissionExpression));
    }

    @Override
    public void updatePermissionExpression(long projectId, @Nullable String permissionExpression) {
        AutomationProject automationProject = getAutomationProject(projectId);

        automationProject.setPermissionExpression(permissionExpression);

        automationProjectRepository.save(automationProject);
    }

    @Override
    public void updateWorkflowPermissionExpression(
        long projectId, UUID workflowUuid, @Nullable String permissionExpression) {

        Optional<AutomationProjectWorkflow> automationProjectWorkflowOptional =
            automationProjectWorkflowRepository.findByWorkflowUuid(workflowUuid);

        if (permissionExpression == null) {
            automationProjectWorkflowOptional.ifPresent(automationProjectWorkflowRepository::delete);

            return;
        }

        AutomationProjectWorkflow automationProjectWorkflow = automationProjectWorkflowOptional
            .orElseGet(() -> new AutomationProjectWorkflow(projectId, workflowUuid));

        automationProjectWorkflow.setPermissionExpression(permissionExpression);

        automationProjectWorkflowRepository.save(automationProjectWorkflow);
    }
}
