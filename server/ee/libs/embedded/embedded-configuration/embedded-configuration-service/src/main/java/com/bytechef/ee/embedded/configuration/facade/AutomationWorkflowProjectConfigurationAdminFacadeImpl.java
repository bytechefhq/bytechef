/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationDeletedEvent;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationUpdatedEvent;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final ConnectedUserWorkflowReferenceAdminFacade connectedUserWorkflowReferenceAdminFacade;
    private final JobService jobService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectConfigurationAdminFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher,
        AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService,
        ConnectedUserWorkflowReferenceAdminFacade connectedUserWorkflowReferenceAdminFacade, JobService jobService,
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        WorkflowService workflowService) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.automationWorkflowProjectConfigurationService = automationWorkflowProjectConfigurationService;
        this.connectedUserWorkflowReferenceAdminFacade = connectedUserWorkflowReferenceAdminFacade;
        this.jobService = jobService;
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

    private Map<String, List<String>> getReferenceWorkflowIds(
        AutomationWorkflowProjectConfiguration configuration, List<ProjectWorkflow> projectWorkflows) {

        Environment environment = Environment.values()[(int) configuration.getEnvironmentId()];

        Set<String> workflowUuids = projectWorkflows.stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());

        return connectedUserWorkflowReferenceAdminFacade.getReferences(workflowUuids)
            .stream()
            .filter(reference -> Objects.equals(reference.environment(), environment.name()) &&
                reference.workflowId() != null)
            .collect(
                Collectors.groupingBy(
                    ConnectedUserWorkflowReferenceDTO::automationWorkflowUuid,
                    Collectors.mapping(ConnectedUserWorkflowReferenceDTO::workflowId, Collectors.toList())));
    }

    private AutomationWorkflowProjectConfigurationDTO toDTO(AutomationWorkflowProjectConfiguration configuration) {
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            configuration.getProjectId(), configuration.getProjectVersion());

        Map<String, List<String>> referenceWorkflowIds = getReferenceWorkflowIds(configuration, projectWorkflows);

        List<AutomationWorkflowProjectConfigurationDTO.Workflow> workflows = projectWorkflows.stream()
            .map(projectWorkflow -> toWorkflowDTO(
                configuration, projectWorkflow,
                referenceWorkflowIds.getOrDefault(projectWorkflow.getUuidAsString(), List.of())))
            .toList();

        Instant lastExecutionDate = workflows.stream()
            .map(AutomationWorkflowProjectConfigurationDTO.Workflow::lastExecutionDate)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);

        return new AutomationWorkflowProjectConfigurationDTO(
            configuration.getId(), configuration.getProjectId(), configuration.getEnvironmentId(),
            configuration.getProjectVersion(), configuration.isEnabled(), configuration.isAutomationHubVisible(),
            workflows, configuration.getLastModifiedDate(), lastExecutionDate);
    }

    private AutomationWorkflowProjectConfigurationDTO.Workflow toWorkflowDTO(
        AutomationWorkflowProjectConfiguration configuration, ProjectWorkflow projectWorkflow,
        List<String> referenceWorkflowIds) {

        String workflowId = projectWorkflow.getWorkflowId();

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Instant lastExecutionDate = referenceWorkflowIds.isEmpty()
            ? null
            : OptionalUtils.mapOrElse(jobService.fetchLastWorkflowJob(referenceWorkflowIds), Job::getEndDate, null);

        return new AutomationWorkflowProjectConfigurationDTO.Workflow(
            projectWorkflow.getUuidAsString(), workflowId, workflow.getLabel(),
            automationWorkflowProjectConfigurationService.isWorkflowEnabled(configuration, projectWorkflow.getUuid()),
            lastExecutionDate);
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
