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
import com.bytechef.ee.embedded.configuration.audit.AutomationWorkflowProjectAuditEvents;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDeleteImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationDeletedEvent;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationUpdatedEvent;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserReferenceDeploymentService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowInput;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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

    private static final Logger log = LoggerFactory.getLogger(
        AutomationWorkflowProjectConfigurationAdminFacadeImpl.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final AutomationWorkflowProjectConfigurationResolver automationWorkflowProjectConfigurationResolver;
    private final AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;
    private final ComponentDefinitionFacade componentDefinitionFacade;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserReferenceAttentionResolver connectedUserReferenceAttentionResolver;
    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;
    private final ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService;
    private final ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager;
    private final ConnectedUserService connectedUserService;
    private final ConnectedUserWorkflowReferenceAdminFacade connectedUserWorkflowReferenceAdminFacade;
    private final ConnectionService connectionService;
    private final JobService jobService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final WorkflowConnectionSlot workflowConnectionSlot;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectConfigurationAdminFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher,
        AutomationWorkflowProjectConfigurationResolver automationWorkflowProjectConfigurationResolver,
        AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService,
        ComponentDefinitionFacade componentDefinitionFacade, ComponentDefinitionService componentDefinitionService,
        ConnectedUserConnectionMembership connectedUserConnectionMembership,
        ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService,
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserReferenceAttentionResolver connectedUserReferenceAttentionResolver,
        ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager,
        ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService,
        ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager,
        ConnectedUserService connectedUserService,
        ConnectedUserWorkflowReferenceAdminFacade connectedUserWorkflowReferenceAdminFacade,
        ConnectionService connectionService, JobService jobService,
        PlatformTransactionManager platformTransactionManager, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService,
        WorkflowConnectionSlot workflowConnectionSlot, WorkflowService workflowService) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.automationWorkflowProjectConfigurationResolver = automationWorkflowProjectConfigurationResolver;
        this.automationWorkflowProjectConfigurationService = automationWorkflowProjectConfigurationService;
        this.componentDefinitionFacade = componentDefinitionFacade;
        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserProjectConfigurationService = connectedUserProjectConfigurationService;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserReferenceAttentionResolver = connectedUserReferenceAttentionResolver;
        this.connectedUserReferenceDeploymentManager = connectedUserReferenceDeploymentManager;
        this.connectedUserReferenceDeploymentService = connectedUserReferenceDeploymentService;
        this.connectedUserReferenceRolloutManager = connectedUserReferenceRolloutManager;
        this.connectedUserService = connectedUserService;
        this.connectedUserWorkflowReferenceAdminFacade = connectedUserWorkflowReferenceAdminFacade;
        this.connectionService = connectionService;
        this.jobService = jobService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowConnectionSlot = workflowConnectionSlot;
        this.workflowService = workflowService;

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        this.requiresNewTransactionTemplate = transactionTemplate;
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CONFIGURATION_CREATED, data = {
        @AuditData(key = "configurationId", value = "#result"),
        @AuditData(key = "projectId", value = "#projectId"),
        @AuditData(key = "environmentId", value = "#environmentId"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "projectVersion", value = "#projectVersion"),
        @AuditData(key = "enabled", value = "#enabled"),
        @AuditData(key = "automationHubVisible", value = "#automationHubVisible")
    })
    public long createConfiguration(
        long projectId, long environmentId, String name, @Nullable String permissionExpression, int projectVersion,
        boolean enabled, boolean automationHubVisible) {

        validateEnvironmentId(environmentId);
        validatePublishedProjectVersion(projectId, projectVersion);

        String configurationName = getConfigurationName(name);

        validateConfigurationNameIsUnique(projectId, environmentId, configurationName, null);

        AutomationWorkflowProjectConfiguration configuration;

        try {
            configuration = automationWorkflowProjectConfigurationService.create(
                projectId, environmentId, configurationName, StringUtils.trimToNull(permissionExpression),
                projectVersion, enabled, automationHubVisible);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw newConfigurationNameExistsException(projectId, environmentId, configurationName);
        }

        publishUpdatedEvent(configuration);

        return configuration.getId();
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CONFIGURATION_DELETED, data = {
        @AuditData(key = "configurationId", value = "#id")
    })
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void deleteConfiguration(long id) {
        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(id);

        Map<String, Set<String>> providedInputNames = new HashMap<>();

        for (AutomationWorkflowProjectConfigurationWorkflow configurationWorkflow : configuration.getWorkflows()) {
            Set<String> workflowProvidedInputNames = getProvidedInputNames(configurationWorkflow.getProvidedInputs());

            if (!workflowProvidedInputNames.isEmpty()) {
                providedInputNames.put(
                    String.valueOf(configurationWorkflow.getWorkflowUuid()), workflowProvidedInputNames);
            }
        }

        requiresNewTransactionTemplate.executeWithoutResult(
            status -> automationWorkflowProjectConfigurationService.updateEnabled(id, false));

        deleteAssignedReferenceDeployments(configuration);

        if (!deleteUnassignedConfiguration(id)) {
            deleteAssignedReferenceDeployments(configuration);

            if (!deleteUnassignedConfiguration(id)) {
                restoreEnabled(configuration);

                throw new ConfigurationException(
                    "Configuration is in use, try again",
                    AutomationWorkflowProjectConfigurationErrorType.CONFIGURATION_IN_USE);
            }
        }

        applicationEventPublisher.publishEvent(
            new AutomationWorkflowProjectConfigurationDeletedEvent(
                configuration.getProjectId(), configuration.getEnvironmentId(), id, providedInputNames));
    }

    @Override
    @Audited(
        event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CONFIGURATION_ENABLED_UPDATED, data = {
            @AuditData(key = "configurationId", value = "#id"),
            @AuditData(key = "enabled", value = "#enable")
        })
    public void enableConfiguration(long id, boolean enable) {
        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.updateEnabled(id, enable);

        publishUpdatedEvent(configuration);
    }

    @Override
    @Audited(
        event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CONFIGURATION_WORKFLOW_ENABLED_UPDATED,
        data = {
            @AuditData(key = "configurationId", value = "#id"),
            @AuditData(key = "workflowUuid", value = "#workflowUuid"),
            @AuditData(key = "enabled", value = "#enable")
        })
    public void enableConfigurationWorkflow(long id, String workflowUuid, boolean enable) {
        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.updateWorkflowEnabled(
                id, UUID.fromString(workflowUuid), enable);

        publishUpdatedEvent(configuration);
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationWorkflowProjectConfigurationDTO getConfiguration(long id) {
        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(id);

        return toDTO(configuration, getAssignedConnectedUserIds(id).size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationWorkflowProjectConfigurationConnectedUserDTO> getConfigurationConnectedUsers(long id) {
        automationWorkflowProjectConfigurationService.getConfiguration(id);

        List<Long> connectedUserIds = getAssignedConnectedUserIds(id)
            .stream()
            .sorted()
            .toList();

        if (connectedUserIds.isEmpty()) {
            return List.of();
        }

        return connectedUserService.getConnectedUsers(connectedUserIds)
            .stream()
            .sorted(Comparator.comparing(ConnectedUser::getId))
            .map(connectedUser -> new AutomationWorkflowProjectConfigurationConnectedUserDTO(
                connectedUser.getId(), connectedUser.getExternalId(), connectedUser.getName(),
                connectedUser.getEmail()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationWorkflowProjectConfigurationDTO> getConfigurations(long projectId) {
        return toDTOs(automationWorkflowProjectConfigurationService.getConfigurations(projectId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationWorkflowProjectConfigurationDTO> getConfigurations(long projectId, long environmentId) {
        return toDTOs(automationWorkflowProjectConfigurationService.getConfigurations(projectId, environmentId));
    }

    private List<AutomationWorkflowProjectConfigurationDTO> toDTOs(
        List<AutomationWorkflowProjectConfiguration> configurations) {

        Map<Long, Integer> connectedUserCounts = connectedUserProjectConfigurationService.countByConfigurationIds(
            configurations.stream()
                .map(AutomationWorkflowProjectConfiguration::getId)
                .toList());

        return configurations.stream()
            .map(configuration -> toDTO(configuration, connectedUserCounts.getOrDefault(configuration.getId(), 0)))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO getConfigurationWorkflowProvidableValues(
        long configurationId, String workflowUuid) {

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(configurationId);

        ProjectWorkflow projectWorkflow = getProjectWorkflow(configuration, workflowUuid);

        String workflowId = projectWorkflow.getWorkflowId();

        Optional<AutomationWorkflowProjectConfigurationWorkflow> configurationWorkflowOptional =
            configuration.fetchWorkflow(projectWorkflow.getUuid());

        return new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO(
            toConnectionSlotDTOs(configuration, workflowId),
            toInputDTOs(configuration, workflowService.getWorkflow(workflowId)),
            configurationWorkflowOptional
                .map(AutomationWorkflowProjectConfigurationWorkflow::getProvidedConnections)
                .orElse(Map.of()),
            configurationWorkflowOptional
                .map(AutomationWorkflowProjectConfigurationWorkflow::getProvidedInputs)
                .orElse(Map.of()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.InputOption>
        getConfigurationWorkflowInputOptions(
            long configurationId, String workflowUuid, String inputName, String propertyName, long connectionId,
            Map<String, ?> lookupDependsOnValues, @Nullable String searchText) {

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(configurationId);

        ProjectWorkflow projectWorkflow = getProjectWorkflow(configuration, workflowUuid);

        Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

        WorkflowInput.ComponentInputReference componentInputReference = WorkflowInput.of(workflow)
            .stream()
            .filter(workflowInput -> Objects.equals(workflowInput.getName(), inputName))
            .findFirst()
            .map(AutomationWorkflowProjectConfigurationAdminFacadeImpl::getComponentInputReference)
            .orElseThrow(() -> newInvalidProvidedValueException(
                "Input %s of workflow uuid=%s does not reference a component input".formatted(
                    inputName, workflowUuid)));

        validateAdministratorConnection(configuration, componentInputReference.componentName(), connectionId);

        Map<String, ?> dependsOnValues = lookupDependsOnValues == null ? Map.of() : lookupDependsOnValues;

        List<Option> options = componentDefinitionFacade.executeWorkflowInputOptions(
            componentInputReference.componentName(), componentInputReference.componentVersion(),
            componentInputReference.groupName(), propertyName, dependsOnValues,
            List.copyOf(dependsOnValues.keySet()), searchText, connectionId);

        return options.stream()
            .map(option -> new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.InputOption(
                option.getLabel(), String.valueOf(option.getValue()), option.getDescription()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationWorkflowProjectConfigurationDeleteImpactDTO getDeleteImpact(long id) {
        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(id);

        Set<Long> assignedConnectedUserIds = getAssignedConnectedUserIds(id);

        return new AutomationWorkflowProjectConfigurationDeleteImpactDTO(
            assignedConnectedUserIds.size(),
            connectedUserReferenceRolloutManager.countReferences(
                configuration.getProjectId(), assignedConnectedUserIds));
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigurationMoveImpactDTO getMoveImpact(long sourceId, long targetId, List<Long> connectedUserIds) {
        AutomationWorkflowProjectConfiguration sourceConfiguration =
            automationWorkflowProjectConfigurationService.getConfiguration(sourceId);
        AutomationWorkflowProjectConfiguration targetConfiguration =
            automationWorkflowProjectConfigurationService.getConfiguration(targetId);

        validateSameProjectAndEnvironment(sourceConfiguration, targetConfiguration);

        return getMoveImpact(
            sourceConfiguration, targetConfiguration, getAssignedConnectedUserIds(sourceId),
            getSortedDistinct(connectedUserIds));
    }

    private ConfigurationMoveImpactDTO getMoveImpact(
        AutomationWorkflowProjectConfiguration sourceConfiguration,
        AutomationWorkflowProjectConfiguration targetConfiguration, Set<Long> assignedConnectedUserIds,
        List<Long> sortedConnectedUserIds) {

        Set<Long> sourceConnectedUserIds = new HashSet<>();
        List<ConfigurationMoveResultDTO.Skipped> skipped = new ArrayList<>();

        for (long connectedUserId : sortedConnectedUserIds) {
            if (!assignedConnectedUserIds.contains(connectedUserId)) {
                skipped.add(
                    new ConfigurationMoveResultDTO.Skipped(
                        connectedUserId, ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()));

                continue;
            }

            sourceConnectedUserIds.add(connectedUserId);

            List<String> missingWorkflowLabels = getMissingReferencedWorkflowLabels(
                sourceConfiguration, Set.of(connectedUserId), targetConfiguration.getProjectVersion());

            if (!missingWorkflowLabels.isEmpty()) {
                skipped.add(
                    new ConfigurationMoveResultDTO.Skipped(
                        connectedUserId, ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_REFERENCED_WORKFLOWS,
                        missingWorkflowLabels));
            }
        }

        AutomationWorkflowProjectConfigurationVersionImpactDTO versionImpact = getVersionImpact(
            sourceConfiguration, targetConfiguration.getProjectVersion(), sourceConnectedUserIds);

        return new ConfigurationMoveImpactDTO(
            addRemovedProvidedInputs(
                versionImpact.workflows(), sourceConfiguration, targetConfiguration, sourceConnectedUserIds),
            skipped);
    }

    private List<AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow> addRemovedProvidedInputs(
        List<AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow> impactedWorkflows,
        AutomationWorkflowProjectConfiguration sourceConfiguration,
        AutomationWorkflowProjectConfiguration targetConfiguration, Set<Long> sourceConnectedUserIds) {

        Map<String, Set<String>> removedProvidedInputNames = getRemovedProvidedInputNames(
            sourceConfiguration, targetConfiguration);

        if (removedProvidedInputNames.isEmpty()) {
            return impactedWorkflows;
        }

        Map<String, ProjectWorkflow> targetProjectWorkflows = projectWorkflowService
            .getProjectWorkflows(targetConfiguration.getProjectId(), targetConfiguration.getProjectVersion())
            .stream()
            .collect(Collectors.toMap(ProjectWorkflow::getUuidAsString, projectWorkflow -> projectWorkflow));

        Map<String, List<ConnectedUserWorkflowReferenceDTO>> referencesByWorkflowUuid = getUsersReferences(
            sourceConfiguration, sourceConnectedUserIds, removedProvidedInputNames.keySet())
                .stream()
                .filter(reference -> !reference.dangling())
                .collect(Collectors.groupingBy(ConnectedUserWorkflowReferenceDTO::automationWorkflowUuid));

        Map<String, AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow> workflowsByUuid =
            new LinkedHashMap<>();

        for (AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow impactedWorkflow : impactedWorkflows) {
            workflowsByUuid.put(impactedWorkflow.workflowUuid(), impactedWorkflow);
        }

        for (Map.Entry<String, Set<String>> entry : removedProvidedInputNames.entrySet()) {
            String workflowUuid = entry.getKey();
            ProjectWorkflow targetProjectWorkflow = targetProjectWorkflows.get(workflowUuid);

            if (targetProjectWorkflow == null) {
                continue;
            }

            int affectedConnectedUserCount = (int) referencesByWorkflowUuid.getOrDefault(workflowUuid, List.of())
                .stream()
                .map(ConnectedUserWorkflowReferenceDTO::connectedUserId)
                .distinct()
                .count();

            AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow impactedWorkflow = workflowsByUuid.get(
                workflowUuid);

            if (impactedWorkflow == null) {
                Workflow workflow = workflowService.getWorkflow(targetProjectWorkflow.getWorkflowId());

                String label = StringUtils.isBlank(workflow.getLabel()) ? workflowUuid : workflow.getLabel();

                workflowsByUuid.put(
                    workflowUuid,
                    new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
                        workflowUuid, label, entry.getValue()
                            .stream()
                            .sorted()
                            .toList(),
                        List.of(), affectedConnectedUserCount));

                continue;
            }

            Set<String> newRequiredInputs = new LinkedHashSet<>(impactedWorkflow.newRequiredInputs());

            newRequiredInputs.addAll(
                entry.getValue()
                    .stream()
                    .sorted()
                    .toList());

            workflowsByUuid.put(
                workflowUuid,
                new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
                    workflowUuid, impactedWorkflow.label(), List.copyOf(newRequiredInputs),
                    impactedWorkflow.newRequiredConnections(),
                    Math.max(impactedWorkflow.affectedConnectedUserCount(), affectedConnectedUserCount)));
        }

        return workflowsByUuid.values()
            .stream()
            .sorted(Comparator.comparing(AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow::label))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigurationMoveImpactDTO getUpgradeImpact(long targetId, List<Long> connectedUserIds) {
        AutomationWorkflowProjectConfiguration targetConfiguration =
            automationWorkflowProjectConfigurationService.getConfiguration(targetId);

        List<Long> sortedConnectedUserIds = getSortedDistinct(connectedUserIds);

        List<UpgradeSource> upgradeSources = getUpgradeSources(targetConfiguration, sortedConnectedUserIds);

        List<ConfigurationMoveResultDTO.Skipped> skipped = new ArrayList<>(
            getUpgradeUnmatchedSkipped(targetConfiguration, upgradeSources, sortedConnectedUserIds));
        Map<String, AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow> workflowsByUuid =
            new LinkedHashMap<>();

        for (UpgradeSource upgradeSource : upgradeSources) {
            ConfigurationMoveImpactDTO moveImpact = getMoveImpact(
                upgradeSource.sourceConfiguration(), targetConfiguration,
                Set.copyOf(upgradeSource.connectedUserIds()), upgradeSource.connectedUserIds());

            for (AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow workflow : moveImpact.workflows()) {
                workflowsByUuid.merge(
                    workflow.workflowUuid(), workflow,
                    AutomationWorkflowProjectConfigurationAdminFacadeImpl::mergeImpactWorkflows);
            }

            skipped.addAll(moveImpact.skipped());
        }

        List<AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow> workflows = workflowsByUuid.values()
            .stream()
            .sorted(Comparator.comparing(AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow::label))
            .toList();

        return new ConfigurationMoveImpactDTO(workflows, sortSkipped(skipped));
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationWorkflowProjectConfigurationVersionImpactDTO getVersionImpact(
        long configurationId, int projectVersion) {

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(configurationId);

        return getVersionImpact(configuration, projectVersion, getAssignedConnectedUserIds(configurationId));
    }

    @Override
    @Audited(
        event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CONFIGURATION_CONNECTED_USERS_MOVED,
        data = {
            @AuditData(key = "sourceConfigurationId", value = "#sourceId"),
            @AuditData(key = "targetConfigurationId", value = "#targetId"),
            @AuditData(key = "connectedUserIds", value = "#connectedUserIds")
        })
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ConfigurationMoveResultDTO moveConnectedUsers(long sourceId, long targetId, List<Long> connectedUserIds) {
        if (sourceId == targetId) {
            throw new IllegalArgumentException("The source and target configurations must differ");
        }

        AutomationWorkflowProjectConfiguration sourceConfiguration =
            automationWorkflowProjectConfigurationService.getConfiguration(sourceId);
        AutomationWorkflowProjectConfiguration targetConfiguration =
            automationWorkflowProjectConfigurationService.getConfiguration(targetId);

        validateSameProjectAndEnvironment(sourceConfiguration, targetConfiguration);

        return moveConnectedUsers(sourceConfiguration, targetConfiguration, getSortedDistinct(connectedUserIds));
    }

    private ConfigurationMoveResultDTO moveConnectedUsers(
        AutomationWorkflowProjectConfiguration sourceConfiguration,
        AutomationWorkflowProjectConfiguration targetConfiguration, List<Long> sortedConnectedUserIds) {

        Map<String, Set<String>> removedProvidedInputNames = getRemovedProvidedInputNames(
            sourceConfiguration, targetConfiguration);

        List<Long> movedConnectedUserIds = new ArrayList<>();
        List<Long> needsAttentionConnectedUserIds = new ArrayList<>();
        List<ConfigurationMoveResultDTO.Skipped> skipped = new ArrayList<>();

        for (long connectedUserId : sortedConnectedUserIds) {
            MoveOutcome moveOutcome;

            try {
                moveOutcome = requiresNewTransactionTemplate.execute(
                    status -> moveConnectedUser(
                        connectedUserId, sourceConfiguration, targetConfiguration, removedProvidedInputNames));
            } catch (RuntimeException exception) {
                log.error(
                    "Moving connected user id={} from configuration id={} to id={} failed", connectedUserId,
                    sourceConfiguration.getId(), targetConfiguration.getId(), exception);

                moveOutcome = MoveOutcome.skipped(
                    connectedUserId, ConfigurationMoveResultDTO.Skipped.MOVE_FAILED, List.of());
            }

            Objects.requireNonNull(moveOutcome);

            if (moveOutcome.skipped() != null) {
                skipped.add(moveOutcome.skipped());

                continue;
            }

            movedConnectedUserIds.add(connectedUserId);

            if (moveOutcome.needsAttention()) {
                needsAttentionConnectedUserIds.add(connectedUserId);
            }
        }

        return new ConfigurationMoveResultDTO(movedConnectedUserIds, skipped, needsAttentionConnectedUserIds);
    }

    private AutomationWorkflowProjectConfigurationVersionImpactDTO getVersionImpact(
        AutomationWorkflowProjectConfiguration configuration, int projectVersion, Set<Long> connectedUserIds) {

        Map<String, ProjectWorkflow> currentProjectWorkflows = projectWorkflowService
            .getProjectWorkflows(configuration.getProjectId(), configuration.getProjectVersion())
            .stream()
            .collect(Collectors.toMap(ProjectWorkflow::getUuidAsString, projectWorkflow -> projectWorkflow));

        List<AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow> impactedWorkflows = new ArrayList<>();
        List<WorkflowChange> workflowChanges = new ArrayList<>();

        for (ProjectWorkflow requestedProjectWorkflow : projectWorkflowService.getProjectWorkflows(
            configuration.getProjectId(), projectVersion)) {

            ProjectWorkflow currentProjectWorkflow = currentProjectWorkflows.get(
                requestedProjectWorkflow.getUuidAsString());

            if (currentProjectWorkflow == null) {
                continue;
            }

            Workflow requestedWorkflow = workflowService.getWorkflow(requestedProjectWorkflow.getWorkflowId());

            String label = StringUtils.isBlank(requestedWorkflow.getLabel())
                ? requestedProjectWorkflow.getUuidAsString() : requestedWorkflow.getLabel();

            List<String> newRequiredInputs = getNewRequiredInputs(
                workflowService.getWorkflow(currentProjectWorkflow.getWorkflowId()), requestedWorkflow);
            List<String> newRequiredConnections = getNewRequiredConnections(
                currentProjectWorkflow.getWorkflowId(), requestedProjectWorkflow.getWorkflowId());

            if (!newRequiredInputs.isEmpty() || !newRequiredConnections.isEmpty()) {
                workflowChanges.add(
                    new WorkflowChange(
                        requestedProjectWorkflow.getUuidAsString(), label, newRequiredInputs, newRequiredConnections));
            }
        }

        if (workflowChanges.isEmpty()) {
            return new AutomationWorkflowProjectConfigurationVersionImpactDTO(impactedWorkflows);
        }

        Map<String, List<ConnectedUserWorkflowReferenceDTO>> referencesByWorkflowUuid = getTurnedOnReferences(
            configuration, connectedUserIds,
            workflowChanges.stream()
                .map(WorkflowChange::workflowUuid)
                .collect(Collectors.toSet()));

        for (WorkflowChange workflowChange : workflowChanges) {
            int affectedConnectedUserCount = getAffectedConnectedUserCount(
                workflowChange, referencesByWorkflowUuid.getOrDefault(workflowChange.workflowUuid(), List.of()));

            impactedWorkflows.add(
                new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
                    workflowChange.workflowUuid(), workflowChange.label(), workflowChange.newRequiredInputs(),
                    workflowChange.newRequiredConnections(), affectedConnectedUserCount));
        }

        impactedWorkflows.sort(
            Comparator.comparing(AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow::label));

        return new AutomationWorkflowProjectConfigurationVersionImpactDTO(impactedWorkflows);
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CONFIGURATION_UPDATED, data = {
        @AuditData(key = "configurationId", value = "#id"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "projectVersion", value = "#projectVersion"),
        @AuditData(key = "enabled", value = "#enabled"),
        @AuditData(key = "automationHubVisible", value = "#automationHubVisible")
    })
    public void updateConfiguration(
        long id, String name, @Nullable String permissionExpression, int projectVersion, boolean enabled,
        boolean automationHubVisible) {

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(id);

        validatePublishedProjectVersion(configuration.getProjectId(), projectVersion);

        String configurationName = getConfigurationName(name);

        validateConfigurationNameIsUnique(
            configuration.getProjectId(), configuration.getEnvironmentId(), configurationName, id);

        if (projectVersion < configuration.getProjectVersion()) {
            validateReferencedWorkflowsExistInVersion(configuration, projectVersion);
        }

        try {
            configuration = automationWorkflowProjectConfigurationService.update(
                id, configurationName, StringUtils.trimToNull(permissionExpression), projectVersion, enabled,
                automationHubVisible);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw newConfigurationNameExistsException(
                configuration.getProjectId(), configuration.getEnvironmentId(), configurationName);
        }

        publishUpdatedEvent(configuration);
    }

    @Override
    public void updateConfigurationWorkflow(
        long id, String workflowUuid, Map<String, Long> providedConnections, Map<String, Object> providedInputs) {

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(id);

        Map<String, Long> validatedProvidedConnections = providedConnections == null ? Map.of() : providedConnections;
        Map<String, Object> validatedProvidedInputs = providedInputs == null ? Map.of() : providedInputs;

        ProjectWorkflow projectWorkflow = projectWorkflowService
            .fetchProjectWorkflow(configuration.getProjectId(), configuration.getProjectVersion(), workflowUuid)
            .orElseThrow(() -> newInvalidProvidedValueException(
                "Workflow uuid=%s is not part of version %d".formatted(
                    workflowUuid, configuration.getProjectVersion())));

        validateProvidedConnections(
            configuration, projectWorkflow.getWorkflowId(), validatedProvidedConnections);
        validateProvidedInputs(projectWorkflow.getWorkflowId(), validatedProvidedInputs);

        Set<String> previouslyProvidedInputNames = configuration.fetchWorkflow(projectWorkflow.getUuid())
            .map(configurationWorkflow -> getProvidedInputNames(configurationWorkflow.getProvidedInputs()))
            .orElse(Set.of());

        Set<String> removedProvidedInputNames = new HashSet<>(previouslyProvidedInputNames);

        removedProvidedInputNames.removeAll(getProvidedInputNames(validatedProvidedInputs));

        AutomationWorkflowProjectConfiguration updatedConfiguration =
            automationWorkflowProjectConfigurationService.updateWorkflowProvidedValues(
                id, UUID.fromString(workflowUuid), validatedProvidedConnections, validatedProvidedInputs);

        applicationEventPublisher.publishEvent(
            new AutomationWorkflowProjectConfigurationUpdatedEvent(
                updatedConfiguration.getProjectId(), updatedConfiguration.getEnvironmentId(),
                updatedConfiguration.getId(), removedProvidedInputNames.isEmpty()
                    ? Map.of() : Map.of(workflowUuid, removedProvidedInputNames)));
    }

    @Override
    @Audited(
        event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CONFIGURATION_CONNECTED_USERS_UPGRADED,
        data = {
            @AuditData(key = "targetConfigurationId", value = "#targetId"),
            @AuditData(key = "connectedUserIds", value = "#connectedUserIds")
        })
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ConfigurationMoveResultDTO upgradeConnectedUsers(long targetId, List<Long> connectedUserIds) {
        AutomationWorkflowProjectConfiguration targetConfiguration =
            automationWorkflowProjectConfigurationService.getConfiguration(targetId);

        List<Long> sortedConnectedUserIds = getSortedDistinct(connectedUserIds);

        List<UpgradeSource> upgradeSources = getUpgradeSources(targetConfiguration, sortedConnectedUserIds);

        List<ConfigurationMoveResultDTO.Skipped> skipped = new ArrayList<>(
            getUpgradeUnmatchedSkipped(targetConfiguration, upgradeSources, sortedConnectedUserIds));
        Set<Long> movedConnectedUserIds = new TreeSet<>();
        Set<Long> needsAttentionConnectedUserIds = new TreeSet<>();

        for (UpgradeSource upgradeSource : upgradeSources) {
            ConfigurationMoveResultDTO moveResult = moveConnectedUsers(
                upgradeSource.sourceConfiguration(), targetConfiguration, upgradeSource.connectedUserIds());

            movedConnectedUserIds.addAll(moveResult.movedConnectedUserIds());
            needsAttentionConnectedUserIds.addAll(moveResult.needsAttentionConnectedUserIds());
            skipped.addAll(moveResult.skipped());
        }

        return new ConfigurationMoveResultDTO(
            List.copyOf(movedConnectedUserIds), sortSkipped(skipped), List.copyOf(needsAttentionConnectedUserIds));
    }

    private List<UpgradeSource> getUpgradeSources(
        AutomationWorkflowProjectConfiguration targetConfiguration, List<Long> sortedConnectedUserIds) {

        List<AutomationWorkflowProjectConfiguration> sourceConfigurations =
            automationWorkflowProjectConfigurationService
                .getConfigurations(targetConfiguration.getProjectId(), targetConfiguration.getEnvironmentId())
                .stream()
                .filter(configuration -> !Objects.equals(configuration.getId(), targetConfiguration.getId()))
                .sorted(Comparator.comparing(AutomationWorkflowProjectConfiguration::getId))
                .toList();

        List<UpgradeSource> upgradeSources = new ArrayList<>();

        for (AutomationWorkflowProjectConfiguration sourceConfiguration : sourceConfigurations) {
            Set<Long> assignedConnectedUserIds = getAssignedConnectedUserIds(sourceConfiguration.getId());

            List<Long> sourceConnectedUserIds = sortedConnectedUserIds.stream()
                .filter(assignedConnectedUserIds::contains)
                .toList();

            if (sourceConnectedUserIds.isEmpty()) {
                continue;
            }

            upgradeSources.add(new UpgradeSource(sourceConfiguration, sourceConnectedUserIds));
        }

        return upgradeSources;
    }

    private List<ConfigurationMoveResultDTO.Skipped> getUpgradeUnmatchedSkipped(
        AutomationWorkflowProjectConfiguration targetConfiguration, List<UpgradeSource> upgradeSources,
        List<Long> sortedConnectedUserIds) {

        Set<Long> sourceConnectedUserIds = upgradeSources.stream()
            .flatMap(upgradeSource -> upgradeSource.connectedUserIds()
                .stream())
            .collect(Collectors.toSet());
        Set<Long> targetConnectedUserIds = getAssignedConnectedUserIds(targetConfiguration.getId());

        List<ConfigurationMoveResultDTO.Skipped> unmatchedSkipped = new ArrayList<>();

        for (long connectedUserId : sortedConnectedUserIds) {
            if (sourceConnectedUserIds.contains(connectedUserId)) {
                continue;
            }

            String reason = targetConnectedUserIds.contains(connectedUserId)
                ? ConfigurationMoveResultDTO.Skipped.ALREADY_ON_TARGET
                : ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE;

            unmatchedSkipped.add(new ConfigurationMoveResultDTO.Skipped(connectedUserId, reason, List.of()));
        }

        return unmatchedSkipped;
    }

    private static AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow mergeImpactWorkflows(
        AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow workflow,
        AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow otherWorkflow) {

        Set<String> newRequiredInputs = new LinkedHashSet<>(workflow.newRequiredInputs());

        newRequiredInputs.addAll(otherWorkflow.newRequiredInputs());

        Set<String> newRequiredConnections = new TreeSet<>(workflow.newRequiredConnections());

        newRequiredConnections.addAll(otherWorkflow.newRequiredConnections());

        return new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
            workflow.workflowUuid(), workflow.label(), List.copyOf(newRequiredInputs),
            List.copyOf(newRequiredConnections),
            workflow.affectedConnectedUserCount() + otherWorkflow.affectedConnectedUserCount());
    }

    private static List<ConfigurationMoveResultDTO.Skipped> sortSkipped(
        List<ConfigurationMoveResultDTO.Skipped> skipped) {

        return skipped.stream()
            .distinct()
            .sorted(Comparator.comparingLong(ConfigurationMoveResultDTO.Skipped::connectedUserId))
            .toList();
    }

    private List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ConnectionSlot> toConnectionSlotDTOs(
        AutomationWorkflowProjectConfiguration configuration, String workflowId) {

        Map<String, List<ComponentConnection>> componentConnectionsByName = workflowConnectionSlot.getSlots(workflowId)
            .stream()
            .collect(Collectors.groupingBy(ComponentConnection::componentName, LinkedHashMap::new,
                Collectors.toList()));

        List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ConnectionSlot> connectionSlots =
            new ArrayList<>();

        for (Map.Entry<String, List<ComponentConnection>> entry : componentConnectionsByName.entrySet()) {
            String componentName = entry.getKey();
            ComponentConnection firstComponentConnection = entry.getValue()
                .getFirst();

            boolean required = entry.getValue()
                .stream()
                .anyMatch(ComponentConnection::required);

            String title = componentDefinitionService
                .fetchComponentDefinition(componentName, firstComponentConnection.componentVersion())
                .map(ComponentDefinition::getTitle)
                .filter(StringUtils::isNotBlank)
                .orElse(componentName);

            connectionSlots.add(
                new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ConnectionSlot(
                    componentName, title, required, getProvidableConnections(configuration, componentName)));
        }

        return connectionSlots;
    }

    private List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection>
        getProvidableConnections(
            AutomationWorkflowProjectConfiguration configuration, String componentName) {

        List<Connection> connections = connectionService.getConnections(
            componentName, null, null, configuration.getEnvironmentId(), PlatformType.EMBEDDED);

        Set<Long> ownedConnectionIds = connectedUserConnectionMembership.getConnectedUserOwnedConnectionIds(
            connections.stream()
                .map(Connection::getId)
                .collect(Collectors.toSet()));

        return connections.stream()
            .filter(connection -> !ownedConnectionIds.contains(connection.getId()))
            .map(
                connection -> new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection(
                    Objects.requireNonNull(connection.getId()), connection.getName()))
            .toList();
    }

    private List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.Input> toInputDTOs(
        AutomationWorkflowProjectConfiguration configuration, Workflow workflow) {

        Map<String, List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection>> optionConnectionsByComponentName =
            new HashMap<>();

        List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.Input> inputs = new ArrayList<>();

        for (WorkflowInput workflowInput : WorkflowInput.of(workflow)) {
            WorkflowInput.ComponentInputReference componentInputReference = getComponentInputReference(workflowInput);

            if (componentInputReference == null) {
                inputs.add(
                    new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.Input(
                        workflowInput.getName(), workflowInput.getLabel(), workflowInput.getType(),
                        workflowInput.isRequired()));

                continue;
            }

            String componentName = componentInputReference.componentName();

            List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection> optionConnections =
                optionConnectionsByComponentName.computeIfAbsent(
                    componentName, curComponentName -> getProvidableConnections(configuration, curComponentName));

            inputs.add(
                new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.Input(
                    workflowInput.getName(), workflowInput.getLabel(), workflowInput.getType(),
                    workflowInput.isRequired(), componentName, componentInputReference.componentVersion(),
                    componentInputReference.groupName(), optionConnections));
        }

        return inputs;
    }

    private static WorkflowInput.@Nullable ComponentInputReference getComponentInputReference(
        WorkflowInput workflowInput) {

        try {
            return workflowInput.getComponentInputReference();
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private static Set<String> getProvidedInputNames(Map<String, ?> providedInputs) {
        return providedInputs.entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    private static ConfigurationException newInvalidProvidedValueException(String message) {
        return new ConfigurationException(
            message, AutomationWorkflowProjectConfigurationErrorType.INVALID_PROVIDED_VALUE);
    }

    private static ConfigurationException newConfigurationNameExistsException(
        long projectId, long environmentId, String name) {

        return new ConfigurationException(
            "Project id=%s already has a configuration named '%s' in environment id=%s".formatted(
                projectId, name, environmentId),
            AutomationWorkflowProjectConfigurationErrorType.CONFIGURATION_NAME_EXISTS);
    }

    private static String getConfigurationName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("A configuration name is required");
        }

        return name.trim();
    }

    /**
     * Deletes the references, the reference deployment and the assignment of every connected user still assigned to the
     * configuration. Takes the rollout lock first and then each user's connected user projects; no path takes the
     * rollout lock while it holds a connected user project lock.
     */
    private void deleteAssignedReferenceDeployments(AutomationWorkflowProjectConfiguration configuration) {
        requiresNewTransactionTemplate.executeWithoutResult(status -> {
            automationWorkflowProjectConfigurationService.lockRollout(
                configuration.getProjectId(), configuration.getEnvironmentId());

            connectedUserReferenceRolloutManager.deleteReferenceDeployments(
                configuration.getProjectId(), configuration.getId(),
                getAssignedConnectedUserIds(configuration.getId()));
        });
    }

    private boolean deleteUnassignedConfiguration(long id) {
        try {
            return Boolean.TRUE.equals(requiresNewTransactionTemplate.execute(status -> {
                if (!connectedUserProjectConfigurationService.getAssignments(id)
                    .isEmpty()) {

                    return false;
                }

                automationWorkflowProjectConfigurationService.delete(id);

                return true;
            }));
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            log.debug("Configuration id={} gained an assignment while it was deleted", id);

            return false;
        }
    }

    private void restoreEnabled(AutomationWorkflowProjectConfiguration configuration) {
        if (!configuration.isEnabled()) {
            return;
        }

        AutomationWorkflowProjectConfiguration restoredConfiguration = requiresNewTransactionTemplate.execute(
            status -> automationWorkflowProjectConfigurationService.updateEnabled(configuration.getId(), true));

        if (restoredConfiguration != null) {
            publishUpdatedEvent(restoredConfiguration);
        }
    }

    private Set<Long> getAssignedConnectedUserIds(long configurationId) {
        return connectedUserProjectConfigurationService.getAssignments(configurationId)
            .stream()
            .map(ConnectedUserProjectConfiguration::getConnectedUserId)
            .collect(Collectors.toSet());
    }

    private void validateConfigurationNameIsUnique(
        long projectId, long environmentId, String name, @Nullable Long excludedConfigurationId) {

        boolean nameExists = automationWorkflowProjectConfigurationService.getConfigurations(projectId, environmentId)
            .stream()
            .filter(configuration -> !Objects.equals(configuration.getId(), excludedConfigurationId))
            .anyMatch(configuration -> StringUtils.equalsIgnoreCase(StringUtils.trim(configuration.getName()), name));

        if (nameExists) {
            throw newConfigurationNameExistsException(projectId, environmentId, name);
        }
    }

    private static void validateEnvironmentId(long environmentId) {
        if (environmentId < 0 || environmentId >= Environment.values().length) {
            throw new ConfigurationException(
                "Environment id=%s is not a known environment".formatted(environmentId),
                AutomationWorkflowProjectConfigurationErrorType.UNKNOWN_ENVIRONMENT);
        }
    }

    private void publishUpdatedEvent(AutomationWorkflowProjectConfiguration configuration) {
        applicationEventPublisher.publishEvent(
            new AutomationWorkflowProjectConfigurationUpdatedEvent(
                configuration.getProjectId(), configuration.getEnvironmentId(), configuration.getId()));
    }

    private int getAffectedConnectedUserCount(
        WorkflowChange workflowChange, List<ConnectedUserWorkflowReferenceDTO> references) {

        if (!workflowChange.newRequiredConnections()
            .isEmpty()) {

            return references.size();
        }

        int affectedConnectedUserCount = 0;

        for (ConnectedUserWorkflowReferenceDTO reference : references) {
            Long projectDeploymentId = reference.projectDeploymentId();

            Map<String, ?> storedInputs = projectDeploymentId == null
                ? Map.of()
                : connectedUserReferenceDeploymentManager.getInputs(projectDeploymentId, workflowChange.workflowUuid());

            boolean lacksNewRequiredInput = workflowChange.newRequiredInputs()
                .stream()
                .anyMatch(inputName -> {
                    Object value = storedInputs.get(inputName);

                    return value == null || StringUtils.isBlank(String.valueOf(value));
                });

            if (lacksNewRequiredInput) {
                affectedConnectedUserCount++;
            }
        }

        return affectedConnectedUserCount;
    }

    private Map<String, List<ConnectedUserWorkflowReferenceDTO>> getTurnedOnReferences(
        AutomationWorkflowProjectConfiguration configuration, Set<Long> connectedUserIds, Set<String> workflowUuids) {

        return getUsersReferences(configuration, connectedUserIds, workflowUuids)
            .stream()
            .filter(reference -> reference.enabled() && !reference.dangling())
            .collect(Collectors.groupingBy(ConnectedUserWorkflowReferenceDTO::automationWorkflowUuid));
    }

    private List<ConnectedUserWorkflowReferenceDTO> getAssignedUsersReferences(
        AutomationWorkflowProjectConfiguration configuration, Set<String> workflowUuids) {

        return getUsersReferences(configuration, getAssignedConnectedUserIds(configuration.getId()), workflowUuids);
    }

    private List<ConnectedUserWorkflowReferenceDTO> getUsersReferences(
        AutomationWorkflowProjectConfiguration configuration, Set<Long> connectedUserIds, Set<String> workflowUuids) {

        if (connectedUserIds.isEmpty() || workflowUuids.isEmpty()) {
            return List.of();
        }

        Environment environment = Environment.values()[(int) configuration.getEnvironmentId()];

        return connectedUserWorkflowReferenceAdminFacade.getReferences(workflowUuids, environment)
            .stream()
            .filter(reference -> connectedUserIds.contains(reference.connectedUserId()) &&
                Objects.equals(reference.environment(), environment.name()))
            .toList();
    }

    private List<String> getNewRequiredConnections(String currentWorkflowId, String requestedWorkflowId) {
        Set<String> currentComponentNames = workflowConnectionSlot.getSlots(currentWorkflowId)
            .stream()
            .map(ComponentConnection::componentName)
            .collect(Collectors.toSet());

        return workflowConnectionSlot.getSlots(requestedWorkflowId)
            .stream()
            .filter(ComponentConnection::required)
            .map(ComponentConnection::componentName)
            .filter(componentName -> !currentComponentNames.contains(componentName))
            .distinct()
            .sorted()
            .toList();
    }

    private static List<String> getNewRequiredInputs(Workflow currentWorkflow, Workflow requestedWorkflow) {
        Set<String> currentRequiredInputNames = currentWorkflow.getInputs()
            .stream()
            .filter(Workflow.Input::required)
            .map(Workflow.Input::name)
            .collect(Collectors.toSet());

        return requestedWorkflow.getInputs()
            .stream()
            .filter(Workflow.Input::required)
            .map(Workflow.Input::name)
            .filter(inputName -> !currentRequiredInputNames.contains(inputName))
            .toList();
    }

    private Map<String, List<String>> getReferenceWorkflowIds(
        AutomationWorkflowProjectConfiguration configuration, List<ProjectWorkflow> projectWorkflows) {

        Set<String> workflowUuids = projectWorkflows.stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());

        return getAssignedUsersReferences(configuration, workflowUuids)
            .stream()
            .filter(reference -> reference.workflowId() != null)
            .collect(
                Collectors.groupingBy(
                    ConnectedUserWorkflowReferenceDTO::automationWorkflowUuid,
                    Collectors.mapping(ConnectedUserWorkflowReferenceDTO::workflowId, Collectors.toList())));
    }

    private AutomationWorkflowProjectConfigurationDTO toDTO(
        AutomationWorkflowProjectConfiguration configuration, int connectedUserCount) {

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
            configuration.getName(), configuration.getPermissionExpression(), configuration.getProjectVersion(),
            configuration.isEnabled(), configuration.isAutomationHubVisible(), connectedUserCount, workflows,
            configuration.getLastModifiedDate(), lastExecutionDate);
    }

    private AutomationWorkflowProjectConfigurationDTO.Workflow toWorkflowDTO(
        AutomationWorkflowProjectConfiguration configuration, ProjectWorkflow projectWorkflow,
        List<String> referenceWorkflowIds) {

        String workflowId = projectWorkflow.getWorkflowId();

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Instant lastExecutionDate = referenceWorkflowIds.isEmpty()
            ? null
            : OptionalUtils.mapOrElse(jobService.fetchLastWorkflowJob(referenceWorkflowIds), Job::getEndDate, null);

        Optional<AutomationWorkflowProjectConfigurationWorkflow> configurationWorkflowOptional =
            configuration.fetchWorkflow(projectWorkflow.getUuid());

        return new AutomationWorkflowProjectConfigurationDTO.Workflow(
            projectWorkflow.getUuidAsString(), workflowId, workflow.getLabel(),
            automationWorkflowProjectConfigurationService.isWorkflowEnabled(configuration, projectWorkflow.getUuid()),
            lastExecutionDate,
            configurationWorkflowOptional
                .map(AutomationWorkflowProjectConfigurationWorkflow::getProvidedConnections)
                .orElse(Map.of()),
            configurationWorkflowOptional
                .map(AutomationWorkflowProjectConfigurationWorkflow::getProvidedInputs)
                .orElse(Map.of()));
    }

    private void validateReferencedWorkflowsExistInVersion(
        AutomationWorkflowProjectConfiguration configuration, int targetProjectVersion) {

        List<String> workflowLabels = getMissingReferencedWorkflowLabels(
            configuration, getAssignedConnectedUserIds(configuration.getId()), targetProjectVersion);

        if (workflowLabels.isEmpty()) {
            return;
        }

        throw new ConfigurationException(
            "Version %d lacks workflows connected users still use: %s. Turn them off in this configuration or remove them from those users first."
                .formatted(targetProjectVersion, String.join(", ", workflowLabels)),
            AutomationWorkflowProjectConfigurationErrorType.VERSION_LACKS_REFERENCED_WORKFLOWS);
    }

    private List<String> getMissingReferencedWorkflowLabels(
        AutomationWorkflowProjectConfiguration configuration, Set<Long> connectedUserIds, int targetProjectVersion) {

        List<ProjectWorkflow> currentProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            configuration.getProjectId(), configuration.getProjectVersion());

        Set<String> targetWorkflowUuids = projectWorkflowService
            .getProjectWorkflows(configuration.getProjectId(), targetProjectVersion)
            .stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());

        Set<String> currentWorkflowUuids = currentProjectWorkflows.stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());

        Set<String> missingWorkflowUuids = getUsersReferences(configuration, connectedUserIds, currentWorkflowUuids)
            .stream()
            .filter(reference -> !reference.dangling())
            .map(ConnectedUserWorkflowReferenceDTO::automationWorkflowUuid)
            .filter(workflowUuid -> !targetWorkflowUuids.contains(workflowUuid))
            .collect(Collectors.toSet());

        if (missingWorkflowUuids.isEmpty()) {
            return List.of();
        }

        return currentProjectWorkflows.stream()
            .filter(projectWorkflow -> missingWorkflowUuids.contains(projectWorkflow.getUuidAsString()))
            .map(projectWorkflow -> workflowService.getWorkflow(projectWorkflow.getWorkflowId())
                .getLabel())
            .sorted()
            .toList();
    }

    private MoveOutcome moveConnectedUser(
        long connectedUserId, AutomationWorkflowProjectConfiguration sourceConfiguration,
        AutomationWorkflowProjectConfiguration targetConfiguration,
        Map<String, Set<String>> removedProvidedInputNames) {

        long projectId = sourceConfiguration.getProjectId();

        Optional<ConnectedUser> connectedUserOptional = connectedUserService.fetchConnectedUser(connectedUserId);

        if (connectedUserOptional.isEmpty()) {
            return MoveOutcome.skipped(connectedUserId, ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of());
        }

        lockConnectedUserProjects(connectedUserId);

        boolean onSource = connectedUserProjectConfigurationService.fetchAssignment(connectedUserId, projectId)
            .map(assignment -> Objects.equals(assignment.getConfigurationId(), sourceConfiguration.getId()))
            .orElse(false);

        if (!onSource) {
            return MoveOutcome.skipped(connectedUserId, ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of());
        }

        List<String> missingWorkflowLabels = getMissingReferencedWorkflowLabels(
            sourceConfiguration, Set.of(connectedUserId), targetConfiguration.getProjectVersion());

        if (!missingWorkflowLabels.isEmpty()) {
            return MoveOutcome.skipped(
                connectedUserId, ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_REFERENCED_WORKFLOWS,
                missingWorkflowLabels);
        }

        automationWorkflowProjectConfigurationResolver.assign(
            connectedUserOptional.get(), projectId, targetConfiguration.getId());

        Optional<ConnectedUserReferenceDeployment> referenceDeploymentOptional =
            connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(connectedUserId, projectId);

        if (referenceDeploymentOptional.isEmpty()) {
            return MoveOutcome.moved(connectedUserId, false);
        }

        ConnectedUserReferenceDeployment referenceDeployment = referenceDeploymentOptional.get();

        long projectDeploymentId = referenceDeployment.getProjectDeploymentId();

        connectedUserReferenceRolloutManager.rollOutDeploymentIfBehind(projectDeploymentId);

        List<ConnectedUserProjectWorkflow> references =
            connectedUserReferenceRolloutManager.applyDeploymentConfigurationState(
                projectDeploymentId, removedProvidedInputNames);

        return MoveOutcome.moved(connectedUserId, needsAttention(references));
    }

    private boolean needsAttention(List<ConnectedUserProjectWorkflow> references) {
        return references.stream()
            .filter(ConnectedUserProjectWorkflow::isEnabled)
            .map(connectedUserReferenceAttentionResolver::resolve)
            .map(ConnectedUserReferenceAttentionResolver.ReferenceState::attentionReason)
            .anyMatch(attentionReason -> attentionReason != null &&
                !Objects.equals(
                    attentionReason, ConnectedUserReferenceAttentionResolver.TURNED_OFF_BY_ADMINISTRATOR));
    }

    private void lockConnectedUserProjects(long connectedUserId) {
        List<Long> connectedUserProjectIds = connectedUserProjectService
            .getConnectedUserConnectedUserProjects(connectedUserId)
            .stream()
            .map(ConnectedUserProject::getId)
            .sorted()
            .toList();

        for (long connectedUserProjectId : connectedUserProjectIds) {
            connectedUserProjectService.lockConnectedUserProject(connectedUserProjectId);
        }
    }

    private static Map<String, Set<String>> getRemovedProvidedInputNames(
        AutomationWorkflowProjectConfiguration sourceConfiguration,
        AutomationWorkflowProjectConfiguration targetConfiguration) {

        Map<String, Set<String>> removedProvidedInputNames = new HashMap<>();

        for (AutomationWorkflowProjectConfigurationWorkflow sourceConfigurationWorkflow : sourceConfiguration
            .getWorkflows()) {

            Set<String> inputNames = new HashSet<>(
                getProvidedInputNames(sourceConfigurationWorkflow.getProvidedInputs()));

            targetConfiguration.fetchWorkflow(sourceConfigurationWorkflow.getWorkflowUuid())
                .ifPresent(targetConfigurationWorkflow -> inputNames.removeAll(
                    getProvidedInputNames(targetConfigurationWorkflow.getProvidedInputs())));

            if (!inputNames.isEmpty()) {
                removedProvidedInputNames.put(
                    String.valueOf(sourceConfigurationWorkflow.getWorkflowUuid()), Set.copyOf(inputNames));
            }
        }

        return removedProvidedInputNames;
    }

    private static List<Long> getSortedDistinct(List<Long> connectedUserIds) {
        return connectedUserIds.stream()
            .distinct()
            .sorted()
            .toList();
    }

    private static void validateSameProjectAndEnvironment(
        AutomationWorkflowProjectConfiguration sourceConfiguration,
        AutomationWorkflowProjectConfiguration targetConfiguration) {

        if (!Objects.equals(sourceConfiguration.getProjectId(), targetConfiguration.getProjectId()) ||
            sourceConfiguration.getEnvironmentId() != targetConfiguration.getEnvironmentId()) {

            throw new ConfigurationException(
                "Configuration id=%s does not belong to project id=%s in environment id=%s".formatted(
                    targetConfiguration.getId(), sourceConfiguration.getProjectId(),
                    sourceConfiguration.getEnvironmentId()),
                AutomationWorkflowProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH);
        }
    }

    private void validateProvidedConnections(
        AutomationWorkflowProjectConfiguration configuration, String workflowId,
        Map<String, Long> providedConnections) {

        if (providedConnections.isEmpty()) {
            return;
        }

        Set<String> slotComponentNames = workflowConnectionSlot.getSlots(workflowId)
            .stream()
            .map(ComponentConnection::componentName)
            .collect(Collectors.toSet());

        for (Map.Entry<String, Long> providedConnection : providedConnections.entrySet()) {
            String componentName = providedConnection.getKey();
            Long connectionId = providedConnection.getValue();

            if (!slotComponentNames.contains(componentName)) {
                throw newInvalidProvidedValueException(
                    "Component %s is not a connection of the workflow".formatted(componentName));
            }

            if (connectionId == null) {
                throw newInvalidProvidedValueException(
                    "Component %s has no connection id".formatted(componentName));
            }

            validateAdministratorConnection(configuration, componentName, connectionId);
        }
    }

    private ProjectWorkflow getProjectWorkflow(
        AutomationWorkflowProjectConfiguration configuration, String workflowUuid) {

        return projectWorkflowService
            .fetchProjectWorkflow(configuration.getProjectId(), configuration.getProjectVersion(), workflowUuid)
            .orElseThrow(() -> newInvalidProvidedValueException(
                "Workflow uuid=%s is not part of version %d".formatted(
                    workflowUuid, configuration.getProjectVersion())));
    }

    private void validateAdministratorConnection(
        AutomationWorkflowProjectConfiguration configuration, String componentName, long connectionId) {

        Connection connection = connectionService.fetchConnection(connectionId)
            .orElseThrow(() -> newInvalidProvidedValueException(
                "Connection id=%s for component %s does not exist".formatted(connectionId, componentName)));

        if (!Objects.equals(connection.getComponentName(), componentName)) {
            throw newInvalidProvidedValueException(
                "Connection id=%s belongs to component %s, not %s".formatted(
                    connectionId, connection.getComponentName(), componentName));
        }

        if (connection.getEnvironmentId() != configuration.getEnvironmentId()) {
            throw newInvalidProvidedValueException(
                "Connection id=%s for component %s belongs to environment id=%s, not id=%s".formatted(
                    connectionId, componentName, connection.getEnvironmentId(),
                    configuration.getEnvironmentId()));
        }

        if (connection.getType() != PlatformType.EMBEDDED) {
            throw newInvalidProvidedValueException(
                "Connection id=%s for component %s is not an embedded connection".formatted(
                    connectionId, componentName));
        }

        if (connectedUserConnectionMembership.isOwnedByConnectedUser(connectionId)) {
            throw newInvalidProvidedValueException(
                "Connection id=%s for component %s belongs to a connected user".formatted(
                    connectionId, componentName));
        }
    }

    private void validateProvidedInputs(String workflowId, Map<String, Object> providedInputs) {
        if (providedInputs.isEmpty()) {
            return;
        }

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Set<String> inputNames = workflow.getInputs()
            .stream()
            .map(Workflow.Input::name)
            .collect(Collectors.toSet());

        for (String inputName : providedInputs.keySet()) {
            if (!inputNames.contains(inputName)) {
                throw newInvalidProvidedValueException(
                    "Input %s is not an input of the workflow".formatted(inputName));
            }
        }
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

    private record MoveOutcome(
        long connectedUserId, ConfigurationMoveResultDTO.@Nullable Skipped skipped, boolean needsAttention) {

        static MoveOutcome moved(long connectedUserId, boolean needsAttention) {
            return new MoveOutcome(connectedUserId, null, needsAttention);
        }

        static MoveOutcome skipped(long connectedUserId, String reason, List<String> workflowLabels) {
            return new MoveOutcome(
                connectedUserId, new ConfigurationMoveResultDTO.Skipped(connectedUserId, reason, workflowLabels),
                false);
        }
    }

    private record UpgradeSource(
        AutomationWorkflowProjectConfiguration sourceConfiguration, List<Long> connectedUserIds) {
    }

    private record WorkflowChange(
        String workflowUuid, String label, List<String> newRequiredInputs, List<String> newRequiredConnections) {
    }
}
