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
import com.bytechef.ee.embedded.configuration.audit.AutomationProjectAuditEvents;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationDeleteImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationWorkflowProvidableValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.event.AutomationProjectConfigurationDeletedEvent;
import com.bytechef.ee.embedded.configuration.event.AutomationProjectConfigurationUpdatedEvent;
import com.bytechef.ee.embedded.configuration.exception.AutomationProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
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
public class AutomationProjectConfigurationAdminFacadeImpl
    implements AutomationProjectConfigurationAdminFacade {

    private static final Logger log = LoggerFactory.getLogger(
        AutomationProjectConfigurationAdminFacadeImpl.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final AutomationProjectConfigurationResolver automationProjectConfigurationResolver;
    private final AutomationProjectConfigurationService automationProjectConfigurationService;
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
    public AutomationProjectConfigurationAdminFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher,
        AutomationProjectConfigurationResolver automationProjectConfigurationResolver,
        AutomationProjectConfigurationService automationProjectConfigurationService,
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
        this.automationProjectConfigurationResolver = automationProjectConfigurationResolver;
        this.automationProjectConfigurationService = automationProjectConfigurationService;
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
    @Audited(event = AutomationProjectAuditEvents.AUTOMATION_PROJECT_CONFIGURATION_CREATED, data = {
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

        AutomationProjectConfiguration configuration;

        try {
            configuration = automationProjectConfigurationService.create(
                projectId, environmentId, configurationName, StringUtils.trimToNull(permissionExpression),
                projectVersion, enabled, automationHubVisible);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw newConfigurationNameExistsException(projectId, environmentId, configurationName);
        }

        publishUpdatedEvent(configuration);

        return configuration.getId();
    }

    @Override
    @Audited(event = AutomationProjectAuditEvents.AUTOMATION_PROJECT_CONFIGURATION_DELETED, data = {
        @AuditData(key = "configurationId", value = "#id")
    })
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void deleteConfiguration(long id) {
        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(id);

        Map<String, Set<String>> providedInputNames = new HashMap<>();

        for (AutomationProjectConfigurationWorkflow configurationWorkflow : configuration.getWorkflows()) {
            Set<String> workflowProvidedInputNames = getProvidedInputNames(configurationWorkflow.getProvidedInputs());

            if (!workflowProvidedInputNames.isEmpty()) {
                providedInputNames.put(
                    String.valueOf(configurationWorkflow.getWorkflowUuid()), workflowProvidedInputNames);
            }
        }

        requiresNewTransactionTemplate.executeWithoutResult(
            status -> automationProjectConfigurationService.updateEnabled(id, false));

        deleteAssignedReferenceDeployments(configuration);

        if (!deleteUnassignedConfiguration(id)) {
            deleteAssignedReferenceDeployments(configuration);

            if (!deleteUnassignedConfiguration(id)) {
                restoreEnabled(configuration);

                throw new ConfigurationException(
                    "Configuration is in use, try again",
                    AutomationProjectConfigurationErrorType.CONFIGURATION_IN_USE);
            }
        }

        applicationEventPublisher.publishEvent(
            new AutomationProjectConfigurationDeletedEvent(
                configuration.getProjectId(), configuration.getEnvironmentId(), id, providedInputNames));
    }

    @Override
    @Audited(
        event = AutomationProjectAuditEvents.AUTOMATION_PROJECT_CONFIGURATION_ENABLED_UPDATED, data = {
            @AuditData(key = "configurationId", value = "#id"),
            @AuditData(key = "enabled", value = "#enable")
        })
    public void enableConfiguration(long id, boolean enable) {
        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.updateEnabled(id, enable);

        publishUpdatedEvent(configuration);
    }

    @Override
    @Audited(
        event = AutomationProjectAuditEvents.AUTOMATION_PROJECT_CONFIGURATION_WORKFLOW_ENABLED_UPDATED,
        data = {
            @AuditData(key = "configurationId", value = "#id"),
            @AuditData(key = "workflowUuid", value = "#workflowUuid"),
            @AuditData(key = "enabled", value = "#enable")
        })
    public void enableConfigurationWorkflow(long id, String workflowUuid, boolean enable) {
        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.updateWorkflowEnabled(
                id, UUID.fromString(workflowUuid), enable);

        publishUpdatedEvent(configuration);
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationProjectConfigurationDTO getConfiguration(long id) {
        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(id);

        return toDTO(configuration, getAssignedConnectedUserIds(id).size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationProjectConfigurationConnectedUserDTO> getConfigurationConnectedUsers(long id) {
        automationProjectConfigurationService.getConfiguration(id);

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
            .map(connectedUser -> new AutomationProjectConfigurationConnectedUserDTO(
                connectedUser.getId(), connectedUser.getExternalId(), connectedUser.getName(),
                connectedUser.getEmail()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationProjectConfigurationDTO> getConfigurations(long projectId) {
        return toDTOs(automationProjectConfigurationService.getConfigurations(projectId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationProjectConfigurationDTO> getConfigurations(long projectId, long environmentId) {
        return toDTOs(automationProjectConfigurationService.getConfigurations(projectId, environmentId));
    }

    private List<AutomationProjectConfigurationDTO> toDTOs(
        List<AutomationProjectConfiguration> configurations) {

        Map<Long, Integer> connectedUserCounts = connectedUserProjectConfigurationService.countByConfigurationIds(
            configurations.stream()
                .map(AutomationProjectConfiguration::getId)
                .toList());

        return configurations.stream()
            .map(configuration -> toDTO(configuration, connectedUserCounts.getOrDefault(configuration.getId(), 0)))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationProjectConfigurationWorkflowProvidableValuesDTO getConfigurationWorkflowProvidableValues(
        long configurationId, String workflowUuid, @Nullable Integer projectVersion) {

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(configurationId);

        ProjectWorkflow projectWorkflow = getProjectWorkflow(configuration, workflowUuid, projectVersion);

        String workflowId = projectWorkflow.getWorkflowId();

        Optional<AutomationProjectConfigurationWorkflow> configurationWorkflowOptional =
            configuration.fetchWorkflow(projectWorkflow.getUuid());

        return new AutomationProjectConfigurationWorkflowProvidableValuesDTO(
            toConnectionSlotDTOs(configuration, workflowId),
            toInputDTOs(configuration, workflowService.getWorkflow(workflowId)),
            configurationWorkflowOptional
                .map(AutomationProjectConfigurationWorkflow::getProvidedConnections)
                .orElse(Map.of()),
            configurationWorkflowOptional
                .map(AutomationProjectConfigurationWorkflow::getProvidedInputs)
                .orElse(Map.of()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationProjectConfigurationWorkflowProvidableValuesDTO.InputOption>
        getConfigurationWorkflowInputOptions(
            long configurationId, String workflowUuid, @Nullable Integer projectVersion, String inputName,
            String propertyName, long connectionId, Map<String, ?> lookupDependsOnValues, @Nullable String searchText) {

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(configurationId);

        ProjectWorkflow projectWorkflow = getProjectWorkflow(configuration, workflowUuid, projectVersion);

        Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

        WorkflowInput.ComponentInputReference componentInputReference = WorkflowInput.of(workflow)
            .stream()
            .filter(workflowInput -> Objects.equals(workflowInput.getName(), inputName))
            .findFirst()
            .map(AutomationProjectConfigurationAdminFacadeImpl::getComponentInputReference)
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
            .map(option -> new AutomationProjectConfigurationWorkflowProvidableValuesDTO.InputOption(
                option.getLabel(), String.valueOf(option.getValue()), option.getDescription()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationProjectConfigurationDeleteImpactDTO getDeleteImpact(long id) {
        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(id);

        Set<Long> assignedConnectedUserIds = getAssignedConnectedUserIds(id);

        return new AutomationProjectConfigurationDeleteImpactDTO(
            assignedConnectedUserIds.size(),
            connectedUserReferenceRolloutManager.countReferences(
                configuration.getProjectId(), assignedConnectedUserIds));
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigurationMoveImpactDTO getMoveImpact(long sourceId, long targetId, List<Long> connectedUserIds) {
        AutomationProjectConfiguration sourceConfiguration =
            automationProjectConfigurationService.getConfiguration(sourceId);
        AutomationProjectConfiguration targetConfiguration =
            automationProjectConfigurationService.getConfiguration(targetId);

        validateSameProjectAndEnvironment(sourceConfiguration, targetConfiguration);

        return getMoveImpact(
            sourceConfiguration, targetConfiguration, getAssignedConnectedUserIds(sourceId),
            getSortedDistinct(connectedUserIds));
    }

    private ConfigurationMoveImpactDTO getMoveImpact(
        AutomationProjectConfiguration sourceConfiguration,
        AutomationProjectConfiguration targetConfiguration, Set<Long> assignedConnectedUserIds,
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

        AutomationProjectConfigurationVersionImpactDTO versionImpact = getVersionImpact(
            sourceConfiguration, targetConfiguration.getProjectVersion(), sourceConnectedUserIds);

        return new ConfigurationMoveImpactDTO(
            addRemovedProvidedInputs(
                versionImpact.workflows(), sourceConfiguration, targetConfiguration, sourceConnectedUserIds),
            skipped);
    }

    private List<AutomationProjectConfigurationVersionImpactDTO.Workflow> addRemovedProvidedInputs(
        List<AutomationProjectConfigurationVersionImpactDTO.Workflow> impactedWorkflows,
        AutomationProjectConfiguration sourceConfiguration,
        AutomationProjectConfiguration targetConfiguration, Set<Long> sourceConnectedUserIds) {

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
                .collect(Collectors.groupingBy(ConnectedUserWorkflowReferenceDTO::automationUuid));

        Map<String, AutomationProjectConfigurationVersionImpactDTO.Workflow> workflowsByUuid =
            new LinkedHashMap<>();

        for (AutomationProjectConfigurationVersionImpactDTO.Workflow impactedWorkflow : impactedWorkflows) {
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

            AutomationProjectConfigurationVersionImpactDTO.Workflow impactedWorkflow = workflowsByUuid.get(
                workflowUuid);

            if (impactedWorkflow == null) {
                Workflow workflow = workflowService.getWorkflow(targetProjectWorkflow.getWorkflowId());

                String label = StringUtils.isBlank(workflow.getLabel()) ? workflowUuid : workflow.getLabel();

                workflowsByUuid.put(
                    workflowUuid,
                    new AutomationProjectConfigurationVersionImpactDTO.Workflow(
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
                new AutomationProjectConfigurationVersionImpactDTO.Workflow(
                    workflowUuid, impactedWorkflow.label(), List.copyOf(newRequiredInputs),
                    impactedWorkflow.newRequiredConnections(),
                    Math.max(impactedWorkflow.affectedConnectedUserCount(), affectedConnectedUserCount)));
        }

        return workflowsByUuid.values()
            .stream()
            .sorted(Comparator.comparing(AutomationProjectConfigurationVersionImpactDTO.Workflow::label))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigurationMoveImpactDTO getUpgradeImpact(long targetId, List<Long> connectedUserIds) {
        AutomationProjectConfiguration targetConfiguration =
            automationProjectConfigurationService.getConfiguration(targetId);

        List<Long> sortedConnectedUserIds = getSortedDistinct(connectedUserIds);

        List<UpgradeSource> upgradeSources = getUpgradeSources(targetConfiguration, sortedConnectedUserIds);

        List<ConfigurationMoveResultDTO.Skipped> skipped = new ArrayList<>(
            getUpgradeUnmatchedSkipped(targetConfiguration, upgradeSources, sortedConnectedUserIds));
        Map<String, AutomationProjectConfigurationVersionImpactDTO.Workflow> workflowsByUuid =
            new LinkedHashMap<>();

        for (UpgradeSource upgradeSource : upgradeSources) {
            ConfigurationMoveImpactDTO moveImpact = getMoveImpact(
                upgradeSource.sourceConfiguration(), targetConfiguration,
                Set.copyOf(upgradeSource.connectedUserIds()), upgradeSource.connectedUserIds());

            for (AutomationProjectConfigurationVersionImpactDTO.Workflow workflow : moveImpact.workflows()) {
                workflowsByUuid.merge(
                    workflow.workflowUuid(), workflow,
                    AutomationProjectConfigurationAdminFacadeImpl::mergeImpactWorkflows);
            }

            skipped.addAll(moveImpact.skipped());
        }

        List<AutomationProjectConfigurationVersionImpactDTO.Workflow> workflows = workflowsByUuid.values()
            .stream()
            .sorted(Comparator.comparing(AutomationProjectConfigurationVersionImpactDTO.Workflow::label))
            .toList();

        return new ConfigurationMoveImpactDTO(workflows, sortSkipped(skipped));
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationProjectConfigurationVersionImpactDTO getVersionImpact(
        long configurationId, int projectVersion) {

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(configurationId);

        return getVersionImpact(configuration, projectVersion, getAssignedConnectedUserIds(configurationId));
    }

    @Override
    @Audited(
        event = AutomationProjectAuditEvents.AUTOMATION_PROJECT_CONFIGURATION_CONNECTED_USERS_MOVED,
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

        AutomationProjectConfiguration sourceConfiguration =
            automationProjectConfigurationService.getConfiguration(sourceId);
        AutomationProjectConfiguration targetConfiguration =
            automationProjectConfigurationService.getConfiguration(targetId);

        validateSameProjectAndEnvironment(sourceConfiguration, targetConfiguration);

        return moveConnectedUsers(sourceConfiguration, targetConfiguration, getSortedDistinct(connectedUserIds));
    }

    private ConfigurationMoveResultDTO moveConnectedUsers(
        AutomationProjectConfiguration sourceConfiguration,
        AutomationProjectConfiguration targetConfiguration, List<Long> sortedConnectedUserIds) {

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

    private AutomationProjectConfigurationVersionImpactDTO getVersionImpact(
        AutomationProjectConfiguration configuration, int projectVersion, Set<Long> connectedUserIds) {

        Map<String, ProjectWorkflow> currentProjectWorkflows = projectWorkflowService
            .getProjectWorkflows(configuration.getProjectId(), configuration.getProjectVersion())
            .stream()
            .collect(Collectors.toMap(ProjectWorkflow::getUuidAsString, projectWorkflow -> projectWorkflow));

        List<AutomationProjectConfigurationVersionImpactDTO.Workflow> impactedWorkflows = new ArrayList<>();
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
            return new AutomationProjectConfigurationVersionImpactDTO(impactedWorkflows);
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
                new AutomationProjectConfigurationVersionImpactDTO.Workflow(
                    workflowChange.workflowUuid(), workflowChange.label(), workflowChange.newRequiredInputs(),
                    workflowChange.newRequiredConnections(), affectedConnectedUserCount));
        }

        impactedWorkflows.sort(
            Comparator.comparing(AutomationProjectConfigurationVersionImpactDTO.Workflow::label));

        return new AutomationProjectConfigurationVersionImpactDTO(impactedWorkflows);
    }

    @Override
    @Audited(event = AutomationProjectAuditEvents.AUTOMATION_PROJECT_CONFIGURATION_UPDATED, data = {
        @AuditData(key = "configurationId", value = "#id"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "projectVersion", value = "#projectVersion"),
        @AuditData(key = "enabled", value = "#enabled"),
        @AuditData(key = "automationHubVisible", value = "#automationHubVisible")
    })
    public void updateConfiguration(
        long id, String name, @Nullable String permissionExpression, int projectVersion, boolean enabled,
        boolean automationHubVisible) {

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(id);

        validatePublishedProjectVersion(configuration.getProjectId(), projectVersion);

        String configurationName = getConfigurationName(name);

        validateConfigurationNameIsUnique(
            configuration.getProjectId(), configuration.getEnvironmentId(), configurationName, id);

        if (projectVersion < configuration.getProjectVersion()) {
            validateReferencedWorkflowsExistInVersion(configuration, projectVersion);
        }

        try {
            configuration = automationProjectConfigurationService.update(
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

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(id);

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

        AutomationProjectConfiguration updatedConfiguration =
            automationProjectConfigurationService.updateWorkflowProvidedValues(
                id, UUID.fromString(workflowUuid), validatedProvidedConnections, validatedProvidedInputs);

        applicationEventPublisher.publishEvent(
            new AutomationProjectConfigurationUpdatedEvent(
                updatedConfiguration.getProjectId(), updatedConfiguration.getEnvironmentId(),
                updatedConfiguration.getId(), removedProvidedInputNames.isEmpty()
                    ? Map.of() : Map.of(workflowUuid, removedProvidedInputNames)));
    }

    @Override
    @Audited(
        event = AutomationProjectAuditEvents.AUTOMATION_PROJECT_CONFIGURATION_CONNECTED_USERS_UPGRADED,
        data = {
            @AuditData(key = "targetConfigurationId", value = "#targetId"),
            @AuditData(key = "connectedUserIds", value = "#connectedUserIds")
        })
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ConfigurationMoveResultDTO upgradeConnectedUsers(long targetId, List<Long> connectedUserIds) {
        AutomationProjectConfiguration targetConfiguration =
            automationProjectConfigurationService.getConfiguration(targetId);

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
        AutomationProjectConfiguration targetConfiguration, List<Long> sortedConnectedUserIds) {

        List<AutomationProjectConfiguration> sourceConfigurations =
            automationProjectConfigurationService
                .getConfigurations(targetConfiguration.getProjectId(), targetConfiguration.getEnvironmentId())
                .stream()
                .filter(configuration -> !Objects.equals(configuration.getId(), targetConfiguration.getId()))
                .sorted(Comparator.comparing(AutomationProjectConfiguration::getId))
                .toList();

        List<UpgradeSource> upgradeSources = new ArrayList<>();

        for (AutomationProjectConfiguration sourceConfiguration : sourceConfigurations) {
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
        AutomationProjectConfiguration targetConfiguration, List<UpgradeSource> upgradeSources,
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

    private static AutomationProjectConfigurationVersionImpactDTO.Workflow mergeImpactWorkflows(
        AutomationProjectConfigurationVersionImpactDTO.Workflow workflow,
        AutomationProjectConfigurationVersionImpactDTO.Workflow otherWorkflow) {

        Set<String> newRequiredInputs = new LinkedHashSet<>(workflow.newRequiredInputs());

        newRequiredInputs.addAll(otherWorkflow.newRequiredInputs());

        Set<String> newRequiredConnections = new TreeSet<>(workflow.newRequiredConnections());

        newRequiredConnections.addAll(otherWorkflow.newRequiredConnections());

        return new AutomationProjectConfigurationVersionImpactDTO.Workflow(
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

    private List<AutomationProjectConfigurationWorkflowProvidableValuesDTO.ConnectionSlot> toConnectionSlotDTOs(
        AutomationProjectConfiguration configuration, String workflowId) {

        Map<String, List<ComponentConnection>> componentConnectionsByName = workflowConnectionSlot.getSlots(workflowId)
            .stream()
            .collect(Collectors.groupingBy(ComponentConnection::componentName, LinkedHashMap::new,
                Collectors.toList()));

        List<AutomationProjectConfigurationWorkflowProvidableValuesDTO.ConnectionSlot> connectionSlots =
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
                new AutomationProjectConfigurationWorkflowProvidableValuesDTO.ConnectionSlot(
                    componentName, title, required, getProvidableConnections(configuration, componentName)));
        }

        return connectionSlots;
    }

    private List<AutomationProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection>
        getProvidableConnections(
            AutomationProjectConfiguration configuration, String componentName) {

        List<Connection> connections = connectionService.getConnections(
            componentName, null, null, configuration.getEnvironmentId(), PlatformType.EMBEDDED);

        Set<Long> ownedConnectionIds = connectedUserConnectionMembership.getConnectedUserOwnedConnectionIds(
            connections.stream()
                .map(Connection::getId)
                .collect(Collectors.toSet()));

        return connections.stream()
            .filter(connection -> !ownedConnectionIds.contains(connection.getId()))
            .map(
                connection -> new AutomationProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection(
                    Objects.requireNonNull(connection.getId()), connection.getName()))
            .toList();
    }

    private List<AutomationProjectConfigurationWorkflowProvidableValuesDTO.Input> toInputDTOs(
        AutomationProjectConfiguration configuration, Workflow workflow) {

        Map<String, List<AutomationProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection>> optionConnectionsByComponentName =
            new HashMap<>();

        List<AutomationProjectConfigurationWorkflowProvidableValuesDTO.Input> inputs = new ArrayList<>();

        for (WorkflowInput workflowInput : WorkflowInput.of(workflow)) {
            WorkflowInput.ComponentInputReference componentInputReference = getComponentInputReference(workflowInput);

            if (componentInputReference == null) {
                inputs.add(
                    new AutomationProjectConfigurationWorkflowProvidableValuesDTO.Input(
                        workflowInput.getName(), workflowInput.getLabel(), workflowInput.getType(),
                        workflowInput.isRequired()));

                continue;
            }

            String componentName = componentInputReference.componentName();

            List<AutomationProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection> optionConnections =
                optionConnectionsByComponentName.computeIfAbsent(
                    componentName, curComponentName -> getProvidableConnections(configuration, curComponentName));

            inputs.add(
                new AutomationProjectConfigurationWorkflowProvidableValuesDTO.Input(
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
            message, AutomationProjectConfigurationErrorType.INVALID_PROVIDED_VALUE);
    }

    private static ConfigurationException newConfigurationNameExistsException(
        long projectId, long environmentId, String name) {

        return new ConfigurationException(
            "Project id=%s already has a configuration named '%s' in environment id=%s".formatted(
                projectId, name, environmentId),
            AutomationProjectConfigurationErrorType.CONFIGURATION_NAME_EXISTS);
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
    private void deleteAssignedReferenceDeployments(AutomationProjectConfiguration configuration) {
        requiresNewTransactionTemplate.executeWithoutResult(status -> {
            automationProjectConfigurationService.lockRollout(
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

                automationProjectConfigurationService.delete(id);

                return true;
            }));
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            log.debug("Configuration id={} gained an assignment while it was deleted", id);

            return false;
        }
    }

    private void restoreEnabled(AutomationProjectConfiguration configuration) {
        if (!configuration.isEnabled()) {
            return;
        }

        AutomationProjectConfiguration restoredConfiguration = requiresNewTransactionTemplate.execute(
            status -> automationProjectConfigurationService.updateEnabled(configuration.getId(), true));

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

        boolean nameExists = automationProjectConfigurationService.getConfigurations(projectId, environmentId)
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
                AutomationProjectConfigurationErrorType.UNKNOWN_ENVIRONMENT);
        }
    }

    private void publishUpdatedEvent(AutomationProjectConfiguration configuration) {
        applicationEventPublisher.publishEvent(
            new AutomationProjectConfigurationUpdatedEvent(
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
        AutomationProjectConfiguration configuration, Set<Long> connectedUserIds, Set<String> workflowUuids) {

        return getUsersReferences(configuration, connectedUserIds, workflowUuids)
            .stream()
            .filter(reference -> reference.enabled() && !reference.dangling())
            .collect(Collectors.groupingBy(ConnectedUserWorkflowReferenceDTO::automationUuid));
    }

    private List<ConnectedUserWorkflowReferenceDTO> getAssignedUsersReferences(
        AutomationProjectConfiguration configuration, Set<String> workflowUuids) {

        return getUsersReferences(configuration, getAssignedConnectedUserIds(configuration.getId()), workflowUuids);
    }

    private List<ConnectedUserWorkflowReferenceDTO> getUsersReferences(
        AutomationProjectConfiguration configuration, Set<Long> connectedUserIds, Set<String> workflowUuids) {

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
        AutomationProjectConfiguration configuration, List<ProjectWorkflow> projectWorkflows) {

        Set<String> workflowUuids = projectWorkflows.stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());

        return getAssignedUsersReferences(configuration, workflowUuids)
            .stream()
            .filter(reference -> reference.workflowId() != null)
            .collect(
                Collectors.groupingBy(
                    ConnectedUserWorkflowReferenceDTO::automationUuid,
                    Collectors.mapping(ConnectedUserWorkflowReferenceDTO::workflowId, Collectors.toList())));
    }

    private AutomationProjectConfigurationDTO toDTO(
        AutomationProjectConfiguration configuration, int connectedUserCount) {

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            configuration.getProjectId(), configuration.getProjectVersion());

        Map<String, List<String>> referenceWorkflowIds = getReferenceWorkflowIds(configuration, projectWorkflows);

        List<AutomationProjectConfigurationDTO.Workflow> workflows = projectWorkflows.stream()
            .map(projectWorkflow -> toWorkflowDTO(
                configuration, projectWorkflow,
                referenceWorkflowIds.getOrDefault(projectWorkflow.getUuidAsString(), List.of())))
            .toList();

        Instant lastExecutionDate = workflows.stream()
            .map(AutomationProjectConfigurationDTO.Workflow::lastExecutionDate)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);

        return new AutomationProjectConfigurationDTO(
            configuration.getId(), configuration.getProjectId(), configuration.getEnvironmentId(),
            configuration.getName(), configuration.getPermissionExpression(), configuration.getProjectVersion(),
            configuration.isEnabled(), configuration.isAutomationHubVisible(), connectedUserCount, workflows,
            configuration.getLastModifiedDate(), lastExecutionDate);
    }

    private AutomationProjectConfigurationDTO.Workflow toWorkflowDTO(
        AutomationProjectConfiguration configuration, ProjectWorkflow projectWorkflow,
        List<String> referenceWorkflowIds) {

        String workflowId = projectWorkflow.getWorkflowId();

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Instant lastExecutionDate = referenceWorkflowIds.isEmpty()
            ? null
            : OptionalUtils.mapOrElse(jobService.fetchLastWorkflowJob(referenceWorkflowIds), Job::getEndDate, null);

        Optional<AutomationProjectConfigurationWorkflow> configurationWorkflowOptional =
            configuration.fetchWorkflow(projectWorkflow.getUuid());

        return new AutomationProjectConfigurationDTO.Workflow(
            projectWorkflow.getUuidAsString(), workflowId, workflow.getLabel(),
            automationProjectConfigurationService.isWorkflowEnabled(configuration, projectWorkflow.getUuid()),
            lastExecutionDate,
            configurationWorkflowOptional
                .map(AutomationProjectConfigurationWorkflow::getProvidedConnections)
                .orElse(Map.of()),
            configurationWorkflowOptional
                .map(AutomationProjectConfigurationWorkflow::getProvidedInputs)
                .orElse(Map.of()));
    }

    private void validateReferencedWorkflowsExistInVersion(
        AutomationProjectConfiguration configuration, int targetProjectVersion) {

        List<String> workflowLabels = getMissingReferencedWorkflowLabels(
            configuration, getAssignedConnectedUserIds(configuration.getId()), targetProjectVersion);

        if (workflowLabels.isEmpty()) {
            return;
        }

        throw new ConfigurationException(
            "Version %d lacks workflows connected users still use: %s. Turn them off in this configuration or remove them from those users first."
                .formatted(targetProjectVersion, String.join(", ", workflowLabels)),
            AutomationProjectConfigurationErrorType.VERSION_LACKS_REFERENCED_WORKFLOWS);
    }

    private List<String> getMissingReferencedWorkflowLabels(
        AutomationProjectConfiguration configuration, Set<Long> connectedUserIds, int targetProjectVersion) {

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
            .map(ConnectedUserWorkflowReferenceDTO::automationUuid)
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
        long connectedUserId, AutomationProjectConfiguration sourceConfiguration,
        AutomationProjectConfiguration targetConfiguration,
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

        automationProjectConfigurationResolver.assign(
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
        AutomationProjectConfiguration sourceConfiguration,
        AutomationProjectConfiguration targetConfiguration) {

        Map<String, Set<String>> removedProvidedInputNames = new HashMap<>();

        for (AutomationProjectConfigurationWorkflow sourceConfigurationWorkflow : sourceConfiguration
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
        AutomationProjectConfiguration sourceConfiguration,
        AutomationProjectConfiguration targetConfiguration) {

        if (!Objects.equals(sourceConfiguration.getProjectId(), targetConfiguration.getProjectId()) ||
            sourceConfiguration.getEnvironmentId() != targetConfiguration.getEnvironmentId()) {

            throw new ConfigurationException(
                "Configuration id=%s does not belong to project id=%s in environment id=%s".formatted(
                    targetConfiguration.getId(), sourceConfiguration.getProjectId(),
                    sourceConfiguration.getEnvironmentId()),
                AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH);
        }
    }

    private void validateProvidedConnections(
        AutomationProjectConfiguration configuration, String workflowId,
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
        AutomationProjectConfiguration configuration, String workflowUuid, @Nullable Integer projectVersion) {

        int workflowProjectVersion = projectVersion == null ? configuration.getProjectVersion() : projectVersion;

        if (projectVersion != null) {
            validatePublishedProjectVersion(configuration.getProjectId(), workflowProjectVersion);
        }

        return projectWorkflowService
            .fetchProjectWorkflow(configuration.getProjectId(), workflowProjectVersion, workflowUuid)
            .orElseThrow(() -> newInvalidProvidedValueException(
                "Workflow uuid=%s is not part of version %d".formatted(workflowUuid, workflowProjectVersion)));
    }

    private void validateAdministratorConnection(
        AutomationProjectConfiguration configuration, String componentName, long connectionId) {

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
                AutomationProjectConfigurationErrorType.PROJECT_VERSION_NOT_PUBLISHED);
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
        AutomationProjectConfiguration sourceConfiguration, List<Long> connectedUserIds) {
    }

    private record WorkflowChange(
        String workflowUuid, String label, List<String> newRequiredInputs, List<String> newRequiredConnections) {
    }
}
