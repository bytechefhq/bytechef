/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationDeletedEvent;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationUpdatedEvent;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("isTenantAdmin()")
public class AutomationWorkflowProjectConfigurationAdminFacadeImpl
    implements AutomationWorkflowProjectConfigurationAdminFacade {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectConfigurationAdminFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher,
        AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService,
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        WorkflowService workflowService) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.automationWorkflowProjectConfigurationService = automationWorkflowProjectConfigurationService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    public long createConfiguration(
        long projectId, long environmentId, int projectVersion, boolean enabled, boolean automationHubVisible) {

        validateEnvironmentId(environmentId);
        validatePublishedProjectVersion(projectId, projectVersion);

        Optional<AutomationWorkflowProjectConfiguration> existingConfigurationOptional =
            automationWorkflowProjectConfigurationService.fetchConfiguration(projectId, environmentId);

        if (existingConfigurationOptional.isPresent()) {
            throw newConfigurationAlreadyExistsException(projectId, environmentId);
        }

        AutomationWorkflowProjectConfiguration configuration;

        try {
            configuration = automationWorkflowProjectConfigurationService.create(
                projectId, environmentId, projectVersion, enabled, automationHubVisible);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw newConfigurationAlreadyExistsException(projectId, environmentId);
        }

        publishUpdatedEvent(projectId, environmentId);

        return configuration.getId();
    }

    @Override
    public void deleteConfiguration(long id) {
        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(id);

        automationWorkflowProjectConfigurationService.delete(id);

        applicationEventPublisher.publishEvent(
            new AutomationWorkflowProjectConfigurationDeletedEvent(
                configuration.getProjectId(), configuration.getEnvironmentId()));
    }

    @Override
    public void enableConfiguration(long id, boolean enable) {
        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.updateEnabled(id, enable);

        publishUpdatedEvent(configuration.getProjectId(), configuration.getEnvironmentId());
    }

    @Override
    public void enableConfigurationWorkflow(long id, String workflowUuid, boolean enable) {
        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.updateWorkflowEnabled(
                id, UUID.fromString(workflowUuid), enable);

        publishUpdatedEvent(configuration.getProjectId(), configuration.getEnvironmentId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationWorkflowProjectConfigurationDTO> getConfigurations(long projectId) {
        return automationWorkflowProjectConfigurationService.getConfigurations(projectId)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    @Override
    public void updateConfiguration(long id, int projectVersion, boolean enabled, boolean automationHubVisible) {
        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(id);

        validatePublishedProjectVersion(configuration.getProjectId(), projectVersion);

        configuration = automationWorkflowProjectConfigurationService.update(
            id, projectVersion, enabled, automationHubVisible);

        publishUpdatedEvent(configuration.getProjectId(), configuration.getEnvironmentId());
    }

    private static ConfigurationException newConfigurationAlreadyExistsException(long projectId, long environmentId) {
        return new ConfigurationException(
            "Project id=%s already has a configuration for environment id=%s".formatted(projectId, environmentId),
            AutomationWorkflowProjectConfigurationErrorType.CONFIGURATION_ALREADY_EXISTS);
    }

    private static void validateEnvironmentId(long environmentId) {
        if (environmentId < 0 || environmentId >= Environment.values().length) {
            throw new ConfigurationException(
                "Environment id=%s is not a known environment".formatted(environmentId),
                AutomationWorkflowProjectConfigurationErrorType.UNKNOWN_ENVIRONMENT);
        }
    }

    private void publishUpdatedEvent(long projectId, long environmentId) {
        applicationEventPublisher.publishEvent(
            new AutomationWorkflowProjectConfigurationUpdatedEvent(projectId, environmentId));
    }

    private AutomationWorkflowProjectConfigurationDTO toDTO(AutomationWorkflowProjectConfiguration configuration) {
        List<AutomationWorkflowProjectConfigurationDTO.Workflow> workflows = projectWorkflowService
            .getProjectWorkflows(configuration.getProjectId(), configuration.getProjectVersion())
            .stream()
            .map(projectWorkflow -> toWorkflowDTO(configuration, projectWorkflow))
            .toList();

        return new AutomationWorkflowProjectConfigurationDTO(
            configuration.getId(), configuration.getProjectId(), configuration.getEnvironmentId(),
            configuration.getProjectVersion(), configuration.isEnabled(), configuration.isAutomationHubVisible(),
            workflows, configuration.getLastModifiedDate());
    }

    private AutomationWorkflowProjectConfigurationDTO.Workflow toWorkflowDTO(
        AutomationWorkflowProjectConfiguration configuration, ProjectWorkflow projectWorkflow) {

        String workflowId = projectWorkflow.getWorkflowId();

        Workflow workflow = workflowService.getWorkflow(workflowId);

        return new AutomationWorkflowProjectConfigurationDTO.Workflow(
            projectWorkflow.getUuidAsString(), workflowId, workflow.getLabel(),
            automationWorkflowProjectConfigurationService.isWorkflowEnabled(configuration, projectWorkflow.getUuid()));
    }

    private void validatePublishedProjectVersion(long projectId, int projectVersion) {
        Project project = projectService.getProject(projectId);

        boolean published = project.getProjectVersions()
            .stream()
            .anyMatch(
                curProjectVersion -> curProjectVersion.getVersion() == projectVersion &&
                    curProjectVersion.getStatus() == ProjectVersion.Status.PUBLISHED);

        if (!published) {
            throw new ConfigurationException(
                "Project id=%s version v=%s is not a published version".formatted(projectId, projectVersion),
                AutomationWorkflowProjectConfigurationErrorType.PROJECT_VERSION_NOT_PUBLISHED);
        }
    }
}
