/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.audit.AutomationProjectAuditEvents;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationReleaseDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableAutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.exception.ConnectedUserProjectWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
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

    private final AutomationProjectConfigurationResolver automationProjectConfigurationResolver;
    private final AutomationProjectConfigurationService automationProjectConfigurationService;
    private final AutomationProjectFacade automationProjectFacade;
    private final ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;
    private final ConnectedUserProjectFacade connectedUserProjectFacade;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;
    private final ConnectedUserService connectedUserService;
    private final ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;
    private final TransactionTemplate transactionTemplate;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectAdminFacadeImpl(
        AutomationProjectConfigurationResolver automationProjectConfigurationResolver,
        AutomationProjectConfigurationService automationProjectConfigurationService,
        AutomationProjectFacade automationProjectFacade,
        ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService,
        ConnectedUserProjectFacade connectedUserProjectFacade, ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager,
        ConnectedUserService connectedUserService,
        ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade,
        PlatformTransactionManager platformTransactionManager) {

        this.automationProjectConfigurationResolver = automationProjectConfigurationResolver;
        this.automationProjectConfigurationService = automationProjectConfigurationService;
        this.automationProjectFacade = automationProjectFacade;
        this.connectedUserProjectConfigurationService = connectedUserProjectConfigurationService;
        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowManager = connectedUserProjectWorkflowManager;
        this.connectedUserService = connectedUserService;
        this.connectedUserWorkflowReferenceFacade = connectedUserWorkflowReferenceFacade;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }

    @Override
    @Audited(event = AutomationProjectAuditEvents.CONNECTED_USER_PROJECT_WORKFLOW_DELETED, data = {
        @AuditData(key = "connectedUserProjectWorkflowId", value = "#connectedUserProjectWorkflowId")
    })
    public void deleteProjectWorkflow(long connectedUserProjectWorkflowId) {
        connectedUserProjectFacade.deleteProjectWorkflow(connectedUserProjectWorkflowId);
    }

    @Override
    @Audited(event = AutomationProjectAuditEvents.CONNECTED_USER_PROJECT_WORKFLOW_ENABLED_UPDATED, data = {
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
    public List<ProvisionableAutomationProjectDTO> getProvisionableProjects(long connectedUserId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Map<String, ConnectedUserProjectWorkflow> referencesByWorkflowUuid = connectedUserWorkflowReferenceFacade
            .getConnectedUserWorkflows(connectedUserId)
            .stream()
            .collect(
                Collectors.toMap(
                    ConnectedUserProjectWorkflow::getAutomationUuid, Function.identity(),
                    (firstReference, secondReference) -> firstReference));

        Map<Long, List<AutomationProjectConfigurationReleaseDTO>> releasesByProjectId =
            automationProjectFacade
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
                Comparator.comparing(ProvisionableAutomationProjectDTO::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Override
    @Audited(event = AutomationProjectAuditEvents.CONNECTED_USER_PROJECT_WORKFLOWS_PROVISIONED, data = {
        @AuditData(key = "connectedUserId", value = "#connectedUserId"),
        @AuditData(key = "configurationId", value = "#configurationId"),
        @AuditData(key = "workflowUuids", value = "#workflows.![workflowUuid]")
    })
    public void provisionProjectWorkflows(
        long connectedUserId, long configurationId, List<ProvisionConnectedUserProjectWorkflowDTO> workflows) {

        if (workflows.isEmpty()) {
            throw new ConfigurationException(
                "No workflows to provision for connected user %s on configuration %s".formatted(
                    connectedUserId, configurationId),
                ConnectedUserProjectWorkflowErrorType.WORKFLOW_NOT_PROVISIONABLE);
        }

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

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

    private void assignConfiguration(
        ConnectedUser connectedUser, long configurationId, List<ProvisionConnectedUserProjectWorkflowDTO> workflows) {

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(configurationId);

        long projectId = configuration.getProjectId();

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager
            .getOrCreateConnectedUserProject(connectedUser.getExternalId(), connectedUser.getEnvironment());

        connectedUserProjectService.lockConnectedUserProject(Objects.requireNonNull(connectedUserProject.getId()));

        ProvisionableAutomationProjectDTO.Configuration provisionableConfiguration =
            getProvisionableConfiguration(connectedUser, projectId, configurationId);

        Optional<ConnectedUserProjectConfiguration> assignmentOptional =
            connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId);

        if (assignmentOptional.isPresent()) {
            ConnectedUserProjectConfiguration assignment = assignmentOptional.get();

            if (!Objects.equals(assignment.getConfigurationId(), configurationId)) {
                throw new ConfigurationException(
                    ("Connected user id=%s is assigned to configuration id=%s of project id=%s; move the connected " +
                        "user to configuration id=%s instead").formatted(
                            connectedUser.getId(), assignment.getConfigurationId(), projectId, configurationId),
                    AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH);
            }
        } else {
            automationProjectConfigurationResolver.assign(connectedUser, projectId, configurationId);
        }

        Set<String> provisionableWorkflowUuids = provisionableConfiguration.workflows()
            .stream()
            .map(ProvisionableAutomationProjectDTO.Workflow::workflowUuid)
            .collect(Collectors.toSet());

        for (ProvisionConnectedUserProjectWorkflowDTO workflow : workflows) {
            if (!provisionableWorkflowUuids.contains(workflow.workflowUuid())) {
                throw new ConfigurationException(
                    "Workflow %s cannot be provisioned for connected user %s on configuration %s".formatted(
                        workflow.workflowUuid(), connectedUser.getId(), configurationId),
                    ConnectedUserProjectWorkflowErrorType.WORKFLOW_NOT_PROVISIONABLE);
            }
        }
    }

    private ProvisionableAutomationProjectDTO.Configuration getProvisionableConfiguration(
        ConnectedUser connectedUser, long projectId, long configurationId) {

        return getProvisionableProjects(connectedUser.getId()).stream()
            .filter(project -> project.projectId() == projectId)
            .flatMap(project -> project.configurations()
                .stream())
            .filter(configuration -> configuration.id() == configurationId)
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                ("Configuration id=%s of project id=%s cannot be provisioned for connected user id=%s: it is " +
                    "disabled, in another environment, or the project is not released to the connected user")
                        .formatted(configurationId, projectId, connectedUser.getId()),
                AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH));
    }

    private ConnectedUserAutomationProjectConfigurationDTO toConnectedUserAutomationProjectConfiguration(
        ConnectedUserProjectConfiguration assignment) {

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(assignment.getConfigurationId());

        return new ConnectedUserAutomationProjectConfigurationDTO(
            assignment.getProjectId(), configuration.getId(), configuration.getName(),
            configuration.getProjectVersion());
    }

    private ProvisionableAutomationProjectDTO toProvisionableProject(
        ConnectedUser connectedUser, List<AutomationProjectConfigurationReleaseDTO> releases,
        Map<String, ConnectedUserProjectWorkflow> referencesByWorkflowUuid) {

        AutomationProjectDTO project = releases.getFirst()
            .project();

        Long assignedConfigurationId = connectedUserProjectConfigurationService
            .fetchAssignment(connectedUser.getId(), project.id())
            .map(ConnectedUserProjectConfiguration::getConfigurationId)
            .orElse(null);

        Long selfServeConfigurationId = automationProjectConfigurationResolver
            .resolve(connectedUser, project.id())
            .map(AutomationProjectConfiguration::getId)
            .orElse(null);

        List<ProvisionableAutomationProjectDTO.Configuration> configurations = releases.stream()
            .map(release -> toProvisionableConfiguration(release, selfServeConfigurationId, referencesByWorkflowUuid))
            .toList();

        ProvisionableAutomationProjectDTO.Configuration preselectedConfiguration = findConfiguration(
            configurations, assignedConfigurationId)
                .or(() -> findConfiguration(configurations, selfServeConfigurationId))
                .orElse(configurations.getFirst());

        return new ProvisionableAutomationProjectDTO(
            project.id(), project.name(), preselectedConfiguration.projectVersion(),
            preselectedConfiguration.workflows(), configurations, assignedConfigurationId);
    }

    private static Optional<ProvisionableAutomationProjectDTO.Configuration> findConfiguration(
        List<ProvisionableAutomationProjectDTO.Configuration> configurations,
        @Nullable Long configurationId) {

        return configurations.stream()
            .filter(configuration -> Objects.equals(configuration.id(), configurationId))
            .findFirst();
    }

    private static ProvisionableAutomationProjectDTO.Configuration toProvisionableConfiguration(
        AutomationProjectConfigurationReleaseDTO release, @Nullable Long selfServeConfigurationId,
        Map<String, ConnectedUserProjectWorkflow> referencesByWorkflowUuid) {

        AutomationProjectDTO project = release.project();

        List<ProvisionableAutomationProjectDTO.Workflow> workflows = project.workflowTemplates()
            .stream()
            .map(template -> toProvisionableWorkflow(template, referencesByWorkflowUuid.get(template.workflowUuid())))
            .toList();

        return new ProvisionableAutomationProjectDTO.Configuration(
            release.configurationId(), release.configurationName(),
            Objects.requireNonNull(project.lastPublishedVersion()),
            Objects.equals(release.configurationId(), selfServeConfigurationId), workflows);
    }

    private static ProvisionableAutomationProjectDTO.Workflow toProvisionableWorkflow(
        ConnectedUserWorkflowTemplateDTO template, @Nullable ConnectedUserProjectWorkflow reference) {

        return new ProvisionableAutomationProjectDTO.Workflow(
            template.workflowUuid(), template.label(), template.inputs(), reference != null,
            reference != null && reference.isDangling());
    }
}
