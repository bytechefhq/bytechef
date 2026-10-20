/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.exception.WorkflowErrorType;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.ai.mcp.facade.McpAutomationRowFacade;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowCopyDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceValuesDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationTemplateNotVisibleException;
import com.bytechef.ee.embedded.configuration.exception.ConnectedUserProjectWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.exception.InvalidWorkflowException;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.ReferenceResolution;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.RowSpec;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.util.InternalOnlyInputUtils;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
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
public class ConnectedUserWorkflowReferenceFacadeImpl implements ConnectedUserWorkflowReferenceFacade {

    private static final String CONNECTED_USER_VIEW = "CONNECTED_USER_VIEW";

    private final AutomationProjectConfigurationResolver automationProjectConfigurationResolver;
    private final AutomationProjectConfigurationService automationProjectConfigurationService;
    private final AutomationProjectFacade automationProjectFacade;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;
    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;
    private final ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager;
    private final ConnectedUserService connectedUserService;
    private final ConnectedUserWorkflowReferenceReader connectedUserWorkflowReferenceReader;
    private final McpAutomationRowFacade mcpAutomationRowFacade;
    private final PermissionService permissionService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserWorkflowReferenceFacadeImpl(
        AutomationProjectConfigurationResolver automationProjectConfigurationResolver,
        AutomationProjectConfigurationService automationProjectConfigurationService,
        AutomationProjectFacade automationProjectFacade,
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager,
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager,
        ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager,
        ConnectedUserService connectedUserService,
        ConnectedUserWorkflowReferenceReader connectedUserWorkflowReferenceReader,
        McpAutomationRowFacade mcpAutomationRowFacade, @Lazy PermissionService permissionService,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService) {

        this.automationProjectConfigurationResolver = automationProjectConfigurationResolver;
        this.automationProjectConfigurationService = automationProjectConfigurationService;
        this.automationProjectFacade = automationProjectFacade;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowManager = connectedUserProjectWorkflowManager;
        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.connectedUserReferenceDeploymentManager = connectedUserReferenceDeploymentManager;
        this.connectedUserReferenceRolloutManager = connectedUserReferenceRolloutManager;
        this.connectedUserService = connectedUserService;
        this.connectedUserWorkflowReferenceReader = connectedUserWorkflowReferenceReader;
        this.mcpAutomationRowFacade = mcpAutomationRowFacade;
        this.permissionService = permissionService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public void deleteReference(String externalUserId, String automationUuid, Environment environment) {
        ConnectedUserProjectWorkflow reference = requireReference(externalUserId, automationUuid, environment);

        removeRow(reference);

        connectedUserProjectWorkflowRepository.deleteById(reference.getId());
    }

    @Override
    @Transactional(noRollbackFor = {
        DanglingReferenceException.class, InvalidWorkflowException.class, MissingConnectionException.class,
        MissingInputException.class
    })
    public ConnectedUserProjectWorkflow enableReference(
        String externalUserId, String automationUuid, boolean enable, Environment environment) {
        ConnectedUserProjectWorkflow reference = requireReference(externalUserId, automationUuid, environment);

        if (!enable) {
            if (reference.isDangling()) {
                removeRow(reference);

                return reference;
            }

            return applyWorkflow(reference, externalUserId, environment, false, ApplyMode.EXPLICIT, Map.of(), null);
        }

        if (reference.isDangling()) {
            throw new DanglingReferenceException(automationUuid);
        }

        reference = catchUp(reference);

        if (reference.isDangling()) {
            throw new DanglingReferenceException(automationUuid);
        }

        return applyWorkflow(reference, externalUserId, environment, true, ApplyMode.EXPLICIT, Map.of(), null);
    }

    @Override
    public List<ConnectedUserProjectWorkflow> getConnectedUserWorkflows(long connectedUserId) {
        return connectedUserProjectWorkflowRepository.findAllByConnectedUserId(connectedUserId);
    }

    @Override
    @Transactional(noRollbackFor = {
        MissingConnectionException.class, MissingInputException.class
    })
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationUuid, Environment environment) {
        return provisionReference(externalUserId, automationUuid, environment, Map.of(), null, null);
    }

    @Override
    @Transactional(noRollbackFor = {
        MissingConnectionException.class, MissingInputException.class
    })
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationUuid, Environment environment,
        Map<String, Long> requestedConnectionIds) {
        return provisionReference(externalUserId, automationUuid, environment, requestedConnectionIds, null, null);
    }

    @Override
    @Transactional(noRollbackFor = {
        MissingConnectionException.class, MissingInputException.class
    })
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationUuid, Environment environment,
        Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> inputs) {
        return provisionReference(externalUserId, automationUuid, environment, requestedConnectionIds, inputs, null);
    }

    @Override
    @Transactional(noRollbackFor = {
        MissingConnectionException.class, MissingInputException.class
    })
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationUuid, Environment environment,
        Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> inputs, @Nullable String provisionedBy) {

        return provisionReference(
            externalUserId, automationUuid, environment, requestedConnectionIds, inputs, provisionedBy);
    }

    private ConnectedUserProjectWorkflow provisionReference(
        String externalUserId, String automationUuid, Environment environment,
        Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> inputs, @Nullable String provisionedBy) {
        ConnectedUserProject connectedUserProject = lockConnectedUserProject(externalUserId, environment);

        Optional<ConnectedUserProjectWorkflow> existingReference = connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndAutomationUuid(connectedUserProject.getId(),
                automationUuid);

        if (existingReference.isPresent()) {
            ConnectedUserProjectWorkflow reference = catchUp(existingReference.get());

            if ((!requestedConnectionIds.isEmpty() || inputs != null) && !reference.isDangling()) {
                return applyWorkflow(
                    reference, externalUserId, environment, reference.isEnabled(), ApplyMode.PROVISION,
                    requestedConnectionIds, inputs);
            }

            return reference;
        }

        checkWorkflowTemplateReleased(
            connectedUserService.getConnectedUser(externalUserId, environment), automationUuid);

        AutomationProjectDTO automationProject = getVisibleAutomationProject(
            externalUserId, automationUuid, environment);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProject.id(), externalUserId, environment);

        if (connectedUserReferenceRolloutManager.rollOutDeploymentIfBehind(projectDeploymentId)) {
            projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
                automationProject.id(), externalUserId, environment);
        }

        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setAutomationUuid(automationUuid);
        reference.setConnectedUserProjectId(connectedUserProject.getId());
        reference.setEnabled(false);
        reference.setProjectDeploymentId(projectDeploymentId);
        reference.setProvisionedBy(provisionedBy);

        ConnectedUserProjectWorkflow savedReference = connectedUserProjectWorkflowRepository.save(reference);

        mcpAutomationRowFacade.addMissingRowsForReference(savedReference.getId());

        return applyWorkflow(
            savedReference, externalUserId, environment, true, ApplyMode.PROVISION, requestedConnectionIds, inputs);
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectedUserWorkflowReferenceValuesDTO getReferenceValues(long referenceId) {
        ConnectedUserProjectWorkflow reference = connectedUserProjectWorkflowRepository.findById(referenceId)
            .orElseThrow(() -> new ConfigurationException(
                "No connected user workflow: %s".formatted(referenceId), WorkflowErrorType.WORKFLOW_NOT_FOUND));

        String automationUuid = reference.getAutomationUuid();
        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (automationUuid == null || projectDeploymentId == null) {
            throw new ConfigurationException(
                "Connected user workflow %s is not a reference".formatted(referenceId),
                WorkflowErrorType.WORKFLOW_NOT_FOUND);
        }

        Optional<ProjectDeploymentWorkflow> rowOptional = connectedUserReferenceDeploymentManager.fetchRow(
            projectDeploymentId, automationUuid);

        if (rowOptional.isEmpty()) {
            return new ConnectedUserWorkflowReferenceValuesDTO(Map.of(), Map.of());
        }

        ProjectDeploymentWorkflow row = rowOptional.get();

        ConnectedUserProject connectedUserProject = connectedUserProjectService.getConnectedUserProject(
            reference.getConnectedUserProjectId());
        ProjectDeployment projectDeployment = connectedUserReferenceDeploymentManager.getDeployment(
            projectDeploymentId);

        Set<String> providedComponentNames = connectedUserReferenceDeploymentManager.getProvidedComponentNames(
            connectedUserProject.getConnectedUserId(), projectDeployment.getProjectId(), automationUuid);

        Map<String, Long> connectionIds = new LinkedHashMap<>(
            connectedUserReferenceDeploymentManager.getConnectionIdsByComponentName(row));

        connectionIds.keySet()
            .removeAll(providedComponentNames);

        Map<String, Object> inputs = InternalOnlyInputUtils.removeInternalOnlyInputs(
            connectedUserReferenceDeploymentManager.getWorkflow(row.getWorkflowId()), row.getInputs());

        return new ConnectedUserWorkflowReferenceValuesDTO(connectionIds, inputs);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(readOnly = true)
    public List<ConnectedUserWorkflowCopyDTO> getCopies(Set<String> automationUuids, Environment environment) {
        if (automationUuids.isEmpty()) {
            return List.of();
        }

        List<ConnectedUserProjectWorkflow> copies =
            connectedUserProjectWorkflowService.getCopiedConnectedUserProjectWorkflows(automationUuids);

        List<Long> connectedUserProjectIds = copies.stream()
            .map(ConnectedUserProjectWorkflow::getConnectedUserProjectId)
            .distinct()
            .toList();

        Map<Long, ConnectedUserProject> connectedUserProjectMap =
            connectedUserProjectService.getConnectedUserProjects(connectedUserProjectIds)
                .stream()
                .collect(Collectors.toMap(ConnectedUserProject::getId, Function.identity()));

        List<Long> connectedUserIds = connectedUserProjectMap.values()
            .stream()
            .map(ConnectedUserProject::getConnectedUserId)
            .distinct()
            .toList();

        Map<Long, ConnectedUser> connectedUserMap = connectedUserService.getConnectedUsers(connectedUserIds)
            .stream()
            .collect(Collectors.toMap(ConnectedUser::getId, Function.identity()));

        return copies.stream()
            .filter(copy -> {
                ConnectedUser connectedUser = getCopyConnectedUser(copy, connectedUserProjectMap, connectedUserMap);

                return connectedUser.getEnvironment() == environment;
            })
            .map(copy -> toCopyDTO(
                copy, getCopyConnectedUser(copy, connectedUserProjectMap, connectedUserMap), environment))
            .toList();
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('CONNECTED_USER_VIEW')")
    @Transactional(readOnly = true)
    public List<ConnectedUserWorkflowReferenceDTO> getReferences(Set<String> automationUuids) {
        Map<String, Boolean> canViewByEnvironment = new HashMap<>();

        return connectedUserWorkflowReferenceReader.getReferences(automationUuids)
            .stream()
            .filter(reference -> canViewByEnvironment.computeIfAbsent(
                reference.environment(),
                environment -> permissionService.hasEmbeddedScope(
                    CONNECTED_USER_VIEW, Environment.valueOf(environment))))
            .toList();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(readOnly = true)
    public List<ConnectedUserWorkflowReferenceDTO> getReferences(
        Set<String> automationUuids, Environment environment) {

        List<ConnectedUserWorkflowReferenceDTO> references = getReferences(automationUuids);

        return references.stream()
            .filter(reference -> Objects.equals(reference.environment(), environment.name()))
            .toList();
    }

    @Override
    public void updateReferenceInputs(
        String externalUserId, String automationUuid, Map<String, ?> inputs, Environment environment) {
        ConnectedUserProjectWorkflow reference = requireReference(externalUserId, automationUuid, environment);

        if (reference.isDangling()) {
            throw new DanglingReferenceException(automationUuid);
        }

        connectedUserReferenceDeploymentManager.updateInputs(
            reference.getProjectDeploymentId(), automationUuid, inputs);

        if (reference.isEnabled() && !isRowEnabled(reference)) {
            applyWorkflow(reference, externalUserId, environment, true, ApplyMode.RESUME, Map.of(), null);
        }
    }

    private static ConnectedUser getCopyConnectedUser(
        ConnectedUserProjectWorkflow copy, Map<Long, ConnectedUserProject> connectedUserProjectMap,
        Map<Long, ConnectedUser> connectedUserMap) {

        ConnectedUserProject connectedUserProject = connectedUserProjectMap.get(copy.getConnectedUserProjectId());

        return connectedUserMap.get(connectedUserProject.getConnectedUserId());
    }

    private boolean isCopyEnabled(ProjectWorkflow projectWorkflow, Environment environment) {
        return projectDeploymentService.fetchProjectDeployment(projectWorkflow.getProjectId(), environment)
            .flatMap(projectDeployment -> projectWorkflowService
                .fetchProjectWorkflowWorkflowId(projectDeployment.getId(), projectWorkflow.getUuidAsString())
                .map(workflowId -> projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
                    projectDeployment.getId(), workflowId)))
            .orElse(false);
    }

    private ConnectedUserWorkflowCopyDTO toCopyDTO(
        ConnectedUserProjectWorkflow copy, ConnectedUser connectedUser, Environment environment) {

        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
            Objects.requireNonNull(copy.getProjectWorkflowId()));

        return new ConnectedUserWorkflowCopyDTO(
            copy.getCopiedFromWorkflowUuid(), projectWorkflow.getUuidAsString(), connectedUser.getExternalId(),
            connectedUser.getName(), connectedUser.getEmail(), isCopyEnabled(projectWorkflow, environment));
    }

    private ConnectedUserProjectWorkflow applyWorkflow(
        ConnectedUserProjectWorkflow reference, String externalUserId, Environment environment, boolean enable,
        ApplyMode applyMode, Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> inputs) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);
        ProjectDeployment projectDeployment = connectedUserReferenceDeploymentManager.getDeployment(
            reference.getProjectDeploymentId());

        int projectVersion = projectDeployment.getProjectVersion();
        String automationUuid = reference.getAutomationUuid();

        String workflowId = connectedUserReferenceDeploymentManager.getWorkflowId(
            projectDeployment.getProjectId(), projectVersion, automationUuid);

        connectedUserReferenceDeploymentManager.checkProvidedConnectionsNotRequested(
            connectedUser.getId(), projectDeployment.getProjectId(), automationUuid, requestedConnectionIds);

        Optional<ProjectDeploymentWorkflow> currentRow = connectedUserReferenceDeploymentManager.fetchWorkflowRow(
            reference.getProjectDeploymentId(), workflowId);

        List<ProjectDeploymentWorkflowConnection> currentConnections = currentRow
            .map(ProjectDeploymentWorkflow::getConnections)
            .orElse(List.of());
        Map<String, ?> currentInputs = inputs == null
            ? currentRow.<Map<String, ?>>map(ProjectDeploymentWorkflow::getInputs)
                .orElse(Map.of())
            : inputs;

        ReferenceResolution resolution = connectedUserReferenceDeploymentManager.resolveReference(
            connectedUser.getId(), projectDeployment.getProjectId(), automationUuid, workflowId, enable,
            requestedConnectionIds, currentConnections, currentInputs);

        RowSpec resolvedRowSpec = resolution.rowSpec();

        boolean requestedEnabled = resolvedRowSpec.enabled();
        boolean rowEnabled = requestedEnabled &&
            connectedUserReferenceDeploymentManager.isAdministrativelyEnabled(
                connectedUser.getId(), projectDeployment.getProjectId(), automationUuid);

        RowSpec rowSpec = new RowSpec(resolvedRowSpec.resolved(), rowEnabled, resolvedRowSpec.inputs());

        connectedUserReferenceDeploymentManager.putWorkflows(
            reference.getProjectDeploymentId(), projectVersion, Map.of(automationUuid, rowSpec));

        reference.setEnabled(
            enable && (reference.isEnabled() || requestedEnabled || resolution.awaitingConfiguration()));

        ConnectedUserProjectWorkflow savedReference = connectedUserProjectWorkflowRepository.save(reference);

        if (enable && applyMode.throwsOnMissingConnection() && resolution.missingComponentName() != null) {
            throw new MissingConnectionException(resolution.missingComponentName());
        }

        if (enable && applyMode.throwsOnIncompleteWorkflow() && resolution.missingInputName() != null) {
            throw new MissingInputException(resolution.missingInputName());
        }

        if (enable && applyMode.throwsOnIncompleteWorkflow() && resolution.workflowErrorCount() > 0) {
            throw new InvalidWorkflowException(automationUuid, resolution.workflowErrorCount());
        }

        return savedReference;
    }

    private ConnectedUserProjectWorkflow catchUp(ConnectedUserProjectWorkflow reference) {
        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (projectDeploymentId == null || isDanglingWithoutDeployment(reference, projectDeploymentId) ||
            !connectedUserReferenceRolloutManager.rollOutDeploymentIfBehind(projectDeploymentId)) {
            return reference;
        }

        return connectedUserProjectWorkflowRepository.findById(reference.getId())
            .orElseThrow();
    }

    private void checkWorkflowTemplateReleased(ConnectedUser connectedUser, String automationUuid) {
        Optional<AutomationProjectDTO> publishedProject = automationProjectFacade
            .getPublishedProjects()
            .stream()
            .filter(project -> CollectionUtils.stream(project.workflowTemplates())
                .anyMatch(workflowTemplate -> Objects.equals(workflowTemplate.workflowUuid(), automationUuid)))
            .findFirst();

        if (publishedProject.isEmpty()) {
            return;
        }

        AutomationProjectDTO project = publishedProject.get();

        Optional<AutomationProjectConfiguration> configurationOptional =
            automationProjectConfigurationResolver.resolve(connectedUser, project.id());

        boolean released = false;

        if (configurationOptional.isPresent() && configurationOptional.get()
            .isEnabled()) {

            AutomationProjectConfiguration configuration = configurationOptional.get();

            UUID workflowUuid = UUID.fromString(automationUuid);

            released = automationProjectConfigurationService.isWorkflowEnabled(configuration, workflowUuid) &&
                projectWorkflowService
                    .getProjectWorkflows(configuration.getProjectId(), configuration.getProjectVersion())
                    .stream()
                    .anyMatch(projectWorkflow -> workflowUuid.equals(projectWorkflow.getUuid()));
        }

        if (!released) {
            throw new ConfigurationException(
                "Automation workflow template %s is not released".formatted(automationUuid),
                ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_NOT_RELEASED);
        }
    }

    private AutomationProjectDTO getVisibleAutomationProject(
        String externalUserId, String automationUuid, Environment environment) {
        return automationProjectFacade.getPublishedProjects(externalUserId, environment)
            .stream()
            .filter(project -> CollectionUtils.stream(project.workflowTemplates())
                .anyMatch(workflowTemplate -> Objects.equals(workflowTemplate.workflowUuid(), automationUuid)))
            .findFirst()
            .orElseThrow(() -> new AutomationTemplateNotVisibleException(automationUuid));
    }

    private boolean isDanglingWithoutDeployment(ConnectedUserProjectWorkflow reference, long projectDeploymentId) {
        if (!reference.isDangling()) {
            return false;
        }

        Optional<ProjectDeployment> projectDeployment = connectedUserReferenceDeploymentManager.fetchDeployment(
            projectDeploymentId);

        return projectDeployment.isEmpty();
    }

    private ConnectedUserProject lockConnectedUserProject(String externalUserId, Environment environment) {
        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager
            .getOrCreateConnectedUserProject(externalUserId, environment);

        return connectedUserProjectService.lockConnectedUserProject(connectedUserProject.getId());
    }

    private boolean isRowEnabled(ConnectedUserProjectWorkflow reference) {
        Optional<ProjectDeploymentWorkflow> row = connectedUserReferenceDeploymentManager.fetchRow(
            reference.getProjectDeploymentId(), reference.getAutomationUuid());

        return row.map(ProjectDeploymentWorkflow::isEnabled)
            .orElse(false);
    }

    private void removeRow(ConnectedUserProjectWorkflow reference) {
        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (projectDeploymentId != null) {
            connectedUserReferenceDeploymentManager.removeWorkflow(
                projectDeploymentId, reference.getAutomationUuid());
        }
    }

    private ConnectedUserProjectWorkflow requireReference(
        String externalUserId, String automationUuid, Environment environment) {
        ConnectedUserProject connectedUserProject = lockConnectedUserProject(externalUserId, environment);

        return connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndAutomationUuid(connectedUserProject.getId(), automationUuid)
            .orElseThrow(() -> new ConfigurationException(
                "No reference to automation workflow: %s".formatted(automationUuid),
                WorkflowErrorType.WORKFLOW_NOT_FOUND));
    }

    private enum ApplyMode {

        EXPLICIT(true, true),
        PROVISION(true, false),
        RESUME(false, false);

        private final boolean throwsOnMissingConnection;
        private final boolean throwsOnIncompleteWorkflow;

        ApplyMode(boolean throwsOnMissingConnection, boolean throwsOnIncompleteWorkflow) {
            this.throwsOnMissingConnection = throwsOnMissingConnection;
            this.throwsOnIncompleteWorkflow = throwsOnIncompleteWorkflow;
        }

        boolean throwsOnIncompleteWorkflow() {
            return throwsOnIncompleteWorkflow;
        }

        boolean throwsOnMissingConnection() {
            return throwsOnMissingConnection;
        }
    }
}
