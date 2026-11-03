/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.audit.AutomationWorkflowProjectAuditEvents;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationReleaseDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableAutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.exception.ConnectedUserProjectWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@PreAuthorize("isTenantAdmin()")
public class ConnectedUserProjectAdminFacadeImpl implements ConnectedUserProjectAdminFacade {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserProjectAdminFacadeImpl.class);

    private final AutomationWorkflowProjectConfigurationResolver automationWorkflowProjectConfigurationResolver;
    private final AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;
    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private final ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;
    private final ConnectedUserProjectFacade connectedUserProjectFacade;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;
    private final ConnectedUserService connectedUserService;
    private final ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;
    private final TransactionTemplate transactionTemplate;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectAdminFacadeImpl(
        AutomationWorkflowProjectConfigurationResolver automationWorkflowProjectConfigurationResolver,
        AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService,
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService,
        ConnectedUserProjectFacade connectedUserProjectFacade, ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager,
        ConnectedUserService connectedUserService,
        ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade,
        PlatformTransactionManager platformTransactionManager) {

        this.automationWorkflowProjectConfigurationResolver = automationWorkflowProjectConfigurationResolver;
        this.automationWorkflowProjectConfigurationService = automationWorkflowProjectConfigurationService;
        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
        this.connectedUserProjectConfigurationService = connectedUserProjectConfigurationService;
        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowManager = connectedUserProjectWorkflowManager;
        this.connectedUserService = connectedUserService;
        this.connectedUserWorkflowReferenceFacade = connectedUserWorkflowReferenceFacade;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.CONNECTED_USER_PROJECT_WORKFLOW_DELETED, data = {
        @AuditData(key = "connectedUserProjectWorkflowId", value = "#connectedUserProjectWorkflowId")
    })
    public void deleteProjectWorkflow(long connectedUserProjectWorkflowId) {
        connectedUserProjectFacade.deleteProjectWorkflow(connectedUserProjectWorkflowId);
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.CONNECTED_USER_PROJECT_WORKFLOW_ENABLED_UPDATED, data = {
        @AuditData(key = "connectedUserProjectWorkflowId", value = "#connectedUserProjectWorkflowId"),
        @AuditData(key = "enabled", value = "#enable")
    })
    public void enableProjectWorkflow(long connectedUserProjectWorkflowId, boolean enable) {
        connectedUserProjectFacade.enableProjectWorkflow(connectedUserProjectWorkflowId, enable);
    }

    @Override
    public List<ConnectedUserAutomationProjectConfigurationDTO> getConnectedUserProjectConfigurations(
        long connectedUserId) {

        return connectedUserProjectConfigurationService.getConnectedUserAssignments(connectedUserId)
            .stream()
            .map(this::toConnectedUserAutomationProjectConfiguration)
            .sorted(Comparator.comparingLong(ConnectedUserAutomationProjectConfigurationDTO::projectId))
            .toList();
    }

    @Override
    public List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment) {
        return connectedUserProjectFacade.getConnectedUserProjects(connectedUserId, environment);
    }

    @Override
    public List<ProvisionableAutomationWorkflowProjectDTO> getProvisionableProjects(long connectedUserId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Map<String, ConnectedUserProjectWorkflow> referencesByWorkflowUuid = connectedUserWorkflowReferenceFacade
            .getConnectedUserWorkflows(connectedUserId)
            .stream()
            .collect(
                Collectors.toMap(
                    ConnectedUserProjectWorkflow::getAutomationWorkflowUuid, Function.identity(),
                    (firstReference, secondReference) -> firstReference));

        Map<Long, List<AutomationWorkflowProjectConfigurationReleaseDTO>> releasesByProjectId =
            automationWorkflowProjectFacade
                .getPublishedProjectConfigurations(connectedUser.getExternalId(), connectedUser.getEnvironment())
                .stream()
                .collect(
                    Collectors.groupingBy(
                        release -> release.project()
                            .id(),
                        LinkedHashMap::new, Collectors.toList()));

        return releasesByProjectId.values()
            .stream()
            .map(releases -> toProvisionableProject(connectedUser, releases, referencesByWorkflowUuid))
            .sorted(
                Comparator.comparing(ProvisionableAutomationWorkflowProjectDTO::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.CONNECTED_USER_PROJECT_WORKFLOWS_PROVISIONED, data = {
        @AuditData(key = "connectedUserId", value = "#connectedUserId"),
        @AuditData(key = "configurationId", value = "#configurationId"),
        @AuditData(key = "workflowUuids", value = "#workflows.![workflowUuid]")
    })
    public void provisionProjectWorkflows(
        long connectedUserId, long configurationId, List<ProvisionConnectedUserProjectWorkflowDTO> workflows) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        // The assignment commits before the references, which keep committing one by one with their triggers.
        transactionTemplate.executeWithoutResult(
            transactionStatus -> assignConfiguration(connectedUser, configurationId, workflows));

        for (ProvisionConnectedUserProjectWorkflowDTO workflow : workflows) {
            Map<String, ?> inputs = workflow.inputs();

            try {
                connectedUserWorkflowReferenceFacade.getOrCreateReference(
                    connectedUser.getExternalId(), workflow.workflowUuid(), connectedUser.getEnvironment(), Map.of(),
                    inputs == null || inputs.isEmpty() ? null : inputs);
            } catch (MissingConnectionException missingConnectionException) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "Workflow {} provisioned for connected user {} without the {} connection",
                        workflow.workflowUuid(), connectedUserId, missingConnectionException.getComponentName());
                }
            }
        }
    }

    /**
     * Locks the connected user's project, then assigns the connected user to the configuration (which locks the
     * connected user), so the lock order matches the reference paths: connected user project, then connected user.
     * Rejects a configuration other than the one the connected user is already assigned to, and workflows the
     * configuration does not release; a rejection rolls the assignment back.
     */
    private void assignConfiguration(
        ConnectedUser connectedUser, long configurationId, List<ProvisionConnectedUserProjectWorkflowDTO> workflows) {

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(configurationId);

        long projectId = configuration.getProjectId();

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager
            .getOrCreateConnectedUserProject(connectedUser.getExternalId(), connectedUser.getEnvironment());

        connectedUserProjectService.lockConnectedUserProject(Objects.requireNonNull(connectedUserProject.getId()));

        Optional<ConnectedUserProjectConfiguration> assignmentOptional =
            connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId);

        if (assignmentOptional.isPresent()) {
            ConnectedUserProjectConfiguration assignment = assignmentOptional.get();

            if (!Objects.equals(assignment.getConfigurationId(), configurationId)) {
                throw new ConfigurationException(
                    ("Connected user id=%s is assigned to configuration id=%s of project id=%s; move the connected " +
                        "user to configuration id=%s instead").formatted(
                            connectedUser.getId(), assignment.getConfigurationId(), projectId, configurationId),
                    AutomationWorkflowProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH);
            }
        } else {
            automationWorkflowProjectConfigurationResolver.assign(connectedUser, projectId, configurationId);
        }

        Set<String> provisionableWorkflowUuids = getProvisionableWorkflowUuids(
            connectedUser.getId(), projectId, configurationId);

        for (ProvisionConnectedUserProjectWorkflowDTO workflow : workflows) {
            if (!provisionableWorkflowUuids.contains(workflow.workflowUuid())) {
                throw new ConfigurationException(
                    "Workflow %s cannot be provisioned for connected user %s on configuration %s".formatted(
                        workflow.workflowUuid(), connectedUser.getId(), configurationId),
                    ConnectedUserProjectWorkflowErrorType.WORKFLOW_NOT_PROVISIONABLE);
            }
        }
    }

    private Set<String> getProvisionableWorkflowUuids(long connectedUserId, long projectId, long configurationId) {
        return getProvisionableProjects(connectedUserId).stream()
            .filter(project -> project.projectId() == projectId)
            .flatMap(project -> project.configurations()
                .stream())
            .filter(configuration -> configuration.id() == configurationId)
            .flatMap(configuration -> configuration.workflows()
                .stream())
            .map(ProvisionableAutomationWorkflowProjectDTO.Workflow::workflowUuid)
            .collect(Collectors.toSet());
    }

    private ConnectedUserAutomationProjectConfigurationDTO toConnectedUserAutomationProjectConfiguration(
        ConnectedUserProjectConfiguration assignment) {

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(assignment.getConfigurationId());

        return new ConnectedUserAutomationProjectConfigurationDTO(
            assignment.getProjectId(), configuration.getId(), configuration.getName(),
            configuration.getProjectVersion());
    }

    private ProvisionableAutomationWorkflowProjectDTO toProvisionableProject(
        ConnectedUser connectedUser, List<AutomationWorkflowProjectConfigurationReleaseDTO> releases,
        Map<String, ConnectedUserProjectWorkflow> referencesByWorkflowUuid) {

        AutomationWorkflowProjectDTO project = releases.getFirst()
            .project();

        Long assignedConfigurationId = connectedUserProjectConfigurationService
            .fetchAssignment(connectedUser.getId(), project.id())
            .map(ConnectedUserProjectConfiguration::getConfigurationId)
            .orElse(null);

        Long selfServeConfigurationId = automationWorkflowProjectConfigurationResolver
            .resolve(connectedUser, project.id())
            .map(AutomationWorkflowProjectConfiguration::getId)
            .orElse(null);

        List<ProvisionableAutomationWorkflowProjectDTO.Configuration> configurations = releases.stream()
            .map(release -> toProvisionableConfiguration(release, selfServeConfigurationId, referencesByWorkflowUuid))
            .toList();

        ProvisionableAutomationWorkflowProjectDTO.Configuration preselectedConfiguration = findConfiguration(
            configurations, assignedConfigurationId)
                .or(() -> findConfiguration(configurations, selfServeConfigurationId))
                .orElse(configurations.getFirst());

        return new ProvisionableAutomationWorkflowProjectDTO(
            project.id(), project.name(), preselectedConfiguration.projectVersion(),
            preselectedConfiguration.workflows(), configurations, assignedConfigurationId);
    }

    private static Optional<ProvisionableAutomationWorkflowProjectDTO.Configuration> findConfiguration(
        List<ProvisionableAutomationWorkflowProjectDTO.Configuration> configurations,
        @Nullable Long configurationId) {

        return configurations.stream()
            .filter(configuration -> Objects.equals(configuration.id(), configurationId))
            .findFirst();
    }

    private static ProvisionableAutomationWorkflowProjectDTO.Configuration toProvisionableConfiguration(
        AutomationWorkflowProjectConfigurationReleaseDTO release, @Nullable Long selfServeConfigurationId,
        Map<String, ConnectedUserProjectWorkflow> referencesByWorkflowUuid) {

        AutomationWorkflowProjectDTO project = release.project();

        List<ProvisionableAutomationWorkflowProjectDTO.Workflow> workflows = project.workflowTemplates()
            .stream()
            .map(template -> toProvisionableWorkflow(template, referencesByWorkflowUuid.get(template.workflowUuid())))
            .toList();

        return new ProvisionableAutomationWorkflowProjectDTO.Configuration(
            release.configurationId(), release.configurationName(),
            Objects.requireNonNull(project.lastPublishedVersion()),
            Objects.equals(release.configurationId(), selfServeConfigurationId), workflows);
    }

    private static ProvisionableAutomationWorkflowProjectDTO.Workflow toProvisionableWorkflow(
        ConnectedUserWorkflowTemplateDTO template, @Nullable ConnectedUserProjectWorkflow reference) {

        return new ProvisionableAutomationWorkflowProjectDTO.Workflow(
            template.workflowUuid(), template.label(), template.inputs(), reference != null,
            reference != null && reference.isDangling());
    }
}
