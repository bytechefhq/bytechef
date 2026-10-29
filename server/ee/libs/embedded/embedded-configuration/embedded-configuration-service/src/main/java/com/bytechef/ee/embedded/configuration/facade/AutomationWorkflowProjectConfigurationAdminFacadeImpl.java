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
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationDeletedEvent;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationUpdatedEvent;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;
    private final ConnectedUserWorkflowReferenceAdminFacade connectedUserWorkflowReferenceAdminFacade;
    private final ConnectionService connectionService;
    private final JobService jobService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowConnectionSlot workflowConnectionSlot;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectConfigurationAdminFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher,
        AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService,
        ComponentDefinitionService componentDefinitionService,
        ConnectedUserConnectionMembership connectedUserConnectionMembership,
        ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager,
        ConnectedUserWorkflowReferenceAdminFacade connectedUserWorkflowReferenceAdminFacade,
        ConnectionService connectionService, JobService jobService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService,
        WorkflowConnectionSlot workflowConnectionSlot, WorkflowService workflowService) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.automationWorkflowProjectConfigurationService = automationWorkflowProjectConfigurationService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserReferenceDeploymentManager = connectedUserReferenceDeploymentManager;
        this.connectedUserWorkflowReferenceAdminFacade = connectedUserWorkflowReferenceAdminFacade;
        this.connectionService = connectionService;
        this.jobService = jobService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowConnectionSlot = workflowConnectionSlot;
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

        Map<String, Set<String>> providedInputNames = new HashMap<>();

        for (AutomationWorkflowProjectConfigurationWorkflow configurationWorkflow : configuration.getWorkflows()) {
            Set<String> workflowProvidedInputNames = getProvidedInputNames(configurationWorkflow.getProvidedInputs());

            if (!workflowProvidedInputNames.isEmpty()) {
                providedInputNames.put(
                    String.valueOf(configurationWorkflow.getWorkflowUuid()), workflowProvidedInputNames);
            }
        }

        automationWorkflowProjectConfigurationService.delete(id);

        applicationEventPublisher.publishEvent(
            new AutomationWorkflowProjectConfigurationDeletedEvent(
                configuration.getProjectId(), configuration.getEnvironmentId(), providedInputNames));
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
    @Transactional(readOnly = true)
    public AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO getConfigurationWorkflowProvidableValues(
        long configurationId, String workflowUuid) {

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(configurationId);

        ProjectWorkflow projectWorkflow = projectWorkflowService
            .fetchProjectWorkflow(configuration.getProjectId(), configuration.getProjectVersion(), workflowUuid)
            .orElseThrow(() -> newInvalidProvidedValueException(
                "Workflow uuid=%s is not part of version %d".formatted(
                    workflowUuid, configuration.getProjectVersion())));

        String workflowId = projectWorkflow.getWorkflowId();

        Optional<AutomationWorkflowProjectConfigurationWorkflow> configurationWorkflowOptional =
            configuration.fetchWorkflow(projectWorkflow.getUuid());

        return new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO(
            toConnectionSlotDTOs(configuration, workflowId), toInputDTOs(workflowService.getWorkflow(workflowId)),
            configurationWorkflowOptional
                .map(AutomationWorkflowProjectConfigurationWorkflow::getProvidedConnections)
                .orElse(Map.of()),
            configurationWorkflowOptional
                .map(AutomationWorkflowProjectConfigurationWorkflow::getProvidedInputs)
                .orElse(Map.of()));
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationWorkflowProjectConfigurationVersionImpactDTO getVersionImpact(
        long configurationId, int projectVersion) {

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(configurationId);

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
            configuration,
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
    public void updateConfiguration(long id, int projectVersion, boolean enabled, boolean automationHubVisible) {
        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(id);

        validatePublishedProjectVersion(configuration.getProjectId(), projectVersion);

        if (projectVersion < configuration.getProjectVersion()) {
            validateReferencedWorkflowsExistInVersion(configuration, projectVersion);
        }

        configuration = automationWorkflowProjectConfigurationService.update(
            id, projectVersion, enabled, automationHubVisible);

        publishUpdatedEvent(configuration.getProjectId(), configuration.getEnvironmentId());
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
                removedProvidedInputNames.isEmpty()
                    ? Map.of() : Map.of(workflowUuid, removedProvidedInputNames)));
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

    private static List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.Input>
        toInputDTOs(Workflow workflow) {
        return workflow.getInputs()
            .stream()
            .map(input -> new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.Input(
                input.name(), input.label(), input.type(), input.required()))
            .toList();
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
        AutomationWorkflowProjectConfiguration configuration, Set<String> workflowUuids) {

        Environment environment = Environment.values()[(int) configuration.getEnvironmentId()];

        return connectedUserWorkflowReferenceAdminFacade.getReferences(workflowUuids)
            .stream()
            .filter(reference -> reference.enabled() && !reference.dangling() &&
                Objects.equals(reference.environment(), environment.name()))
            .collect(Collectors.groupingBy(ConnectedUserWorkflowReferenceDTO::automationWorkflowUuid));
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

        Environment environment = Environment.values()[(int) configuration.getEnvironmentId()];

        Set<String> missingWorkflowUuids = connectedUserWorkflowReferenceAdminFacade
            .getReferences(currentWorkflowUuids)
            .stream()
            .filter(reference -> !reference.dangling() && Objects.equals(reference.environment(), environment.name()))
            .map(ConnectedUserWorkflowReferenceDTO::automationWorkflowUuid)
            .filter(workflowUuid -> !targetWorkflowUuids.contains(workflowUuid))
            .collect(Collectors.toSet());

        if (missingWorkflowUuids.isEmpty()) {
            return;
        }

        String workflowLabels = currentProjectWorkflows.stream()
            .filter(projectWorkflow -> missingWorkflowUuids.contains(projectWorkflow.getUuidAsString()))
            .map(projectWorkflow -> workflowService.getWorkflow(projectWorkflow.getWorkflowId())
                .getLabel())
            .sorted()
            .collect(Collectors.joining(", "));

        throw new ConfigurationException(
            "Version %d lacks workflows connected users still use: %s. Turn them off in this configuration or remove them from those users first."
                .formatted(targetProjectVersion, workflowLabels),
            AutomationWorkflowProjectConfigurationErrorType.VERSION_LACKS_REFERENCED_WORKFLOWS);
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

    private record WorkflowChange(
        String workflowUuid, String label, List<String> newRequiredInputs, List<String> newRequiredConnections) {
    }
}
