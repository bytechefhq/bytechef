/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import com.bytechef.ee.embedded.configuration.exception.ConnectedUserProjectWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.exception.ProvidedValueLockedException;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserReferenceDeploymentService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SkipAutomationAuthorization
class ConnectedUserReferenceDeploymentManager {

    private final AutomationProjectConfigurationResolver automationProjectConfigurationResolver;
    private final AutomationProjectConfigurationService automationProjectConfigurationService;
    private final ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;
    private final ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService;
    private final ConnectedUserService connectedUserService;
    private final ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver;
    private final ConnectedUserWorkflowValidator connectedUserWorkflowValidator;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserReferenceDeploymentManager(
        AutomationProjectConfigurationResolver automationProjectConfigurationResolver,
        AutomationProjectConfigurationService automationProjectConfigurationService,
        ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService,
        ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService,
        ConnectedUserService connectedUserService,
        ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver,
        ConnectedUserWorkflowValidator connectedUserWorkflowValidator, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.automationProjectConfigurationResolver = automationProjectConfigurationResolver;
        this.automationProjectConfigurationService = automationProjectConfigurationService;
        this.connectedUserProjectConfigurationService = connectedUserProjectConfigurationService;
        this.connectedUserReferenceDeploymentService = connectedUserReferenceDeploymentService;
        this.connectedUserService = connectedUserService;
        this.connectedUserWorkflowConnectionResolver = connectedUserWorkflowConnectionResolver;
        this.connectedUserWorkflowValidator = connectedUserWorkflowValidator;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    public void deleteDeployment(long projectDeploymentId) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        projectDeploymentFacade.updateProjectDeployment(projectDeployment, List.of(), List.of());

        projectDeploymentFacade.deleteProjectDeployment(projectDeploymentId);
    }

    public Optional<ProjectDeploymentWorkflow> fetchRow(long projectDeploymentId, String automationUuid) {
        return projectDeploymentService.fetchProjectDeployment(projectDeploymentId)
            .flatMap(projectDeployment -> fetchRowAtCurrentVersion(projectDeployment, automationUuid));
    }

    public Optional<ProjectDeployment> fetchDeployment(long projectDeploymentId) {
        return projectDeploymentService.fetchProjectDeployment(projectDeploymentId);
    }

    public Optional<ProjectDeploymentWorkflow> fetchWorkflowRow(long projectDeploymentId, String workflowId) {
        return projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(projectDeploymentId, workflowId);
    }

    @Nullable
    public String findMissingRequiredInput(String workflowId, Map<String, ?> inputs) {
        return findMissingRequiredInput(workflowService.getWorkflow(workflowId), inputs);
    }

    public Map<String, Long> getConnectionIdsByComponentName(ProjectDeploymentWorkflow row) {
        return connectedUserWorkflowConnectionResolver.getConnectionIdsByComponentName(
            row.getWorkflowId(), row.getConnections());
    }

    public ProjectDeployment getDeployment(long projectDeploymentId) {
        return projectDeploymentService.getProjectDeployment(projectDeploymentId);
    }

    public Map<String, ?> getInputs(long projectDeploymentId, String automationUuid) {
        return fetchRow(projectDeploymentId, automationUuid)
            .<Map<String, ?>>map(ProjectDeploymentWorkflow::getInputs)
            .orElse(Map.of());
    }

    /**
     * Returns the connected user who owns the reference deployment, or empty when the deployment is not a reference
     * deployment.
     */
    public Optional<Long> fetchConnectedUserId(long projectDeploymentId) {
        return connectedUserReferenceDeploymentService.fetchByProjectDeploymentId(projectDeploymentId)
            .map(ConnectedUserReferenceDeployment::getConnectedUserId);
    }

    /**
     * Returns the project version of the configuration the connected user is assigned to, or empty when the user has no
     * assignment or the configuration is disabled.
     */
    public Optional<Integer> fetchReleasedVersion(long connectedUserId, long automationProjectId) {
        return fetchAssignedConfiguration(connectedUserId, automationProjectId)
            .filter(AutomationProjectConfiguration::isEnabled)
            .map(AutomationProjectConfiguration::getProjectVersion);
    }

    public int getReleasedVersion(long connectedUserId, long automationProjectId) {
        return fetchReleasedVersion(connectedUserId, automationProjectId)
            .orElseThrow(() -> newNotReleasedException(connectedUserId, automationProjectId));
    }

    @Transactional
    public long
        getOrCreateDeployment(long automationProjectId, String externalUserId, Environment environment) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return connectedUserReferenceDeploymentService
            .fetchConnectedUserReferenceDeployment(connectedUser.getId(), automationProjectId)
            .map(ConnectedUserReferenceDeployment::getProjectDeploymentId)
            .orElseGet(() -> createDeployment(automationProjectId, connectedUser));
    }

    public boolean isAdministrativelyEnabled(
        long connectedUserId, long automationProjectId, String automationUuid) {

        Optional<AutomationProjectConfiguration> configurationOptional = fetchAssignedConfiguration(
            connectedUserId, automationProjectId);

        if (configurationOptional.isEmpty()) {
            return false;
        }

        AutomationProjectConfiguration configuration = configurationOptional.get();

        return configuration.isEnabled() && automationProjectConfigurationService.isWorkflowEnabled(
            configuration, UUID.fromString(automationUuid));
    }

    public boolean isReferenceDeployment(long projectDeploymentId) {
        return connectedUserReferenceDeploymentService.isReferenceDeployment(projectDeploymentId);
    }

    public Optional<String>
        fetchWorkflowId(long automationProjectId, int projectVersion, String automationUuid) {
        return projectWorkflowService
            .fetchProjectWorkflow(automationProjectId, projectVersion, automationUuid)
            .map(ProjectWorkflow::getWorkflowId);
    }

    public String getWorkflowId(long automationProjectId, int projectVersion, String automationUuid) {
        return fetchWorkflowId(automationProjectId, projectVersion, automationUuid)
            .orElseThrow(() -> new IllegalArgumentException(
                "Automation workflow %s is not in version %s of project id=%s".formatted(
                    automationUuid, projectVersion, automationProjectId)));
    }

    public void putWorkflows(
        long projectDeploymentId, int projectVersion, Map<String, RowSpec> rowSpecsByAutomationUuid) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        if (projectDeployment.getProjectVersion() == projectVersion) {
            for (Map.Entry<String, RowSpec> entry : rowSpecsByAutomationUuid.entrySet()) {
                putWorkflow(projectDeployment, entry.getKey(), entry.getValue());
            }

            return;
        }

        Map<String, ProjectDeploymentWorkflow> existingRowsByAutomationUuid = getRowsByAutomationUuid(
            projectDeploymentId);

        List<ProjectDeploymentWorkflow> rows = new ArrayList<>();

        for (Map.Entry<String, RowSpec> entry : rowSpecsByAutomationUuid.entrySet()) {
            RowSpec rowSpec = entry.getValue();

            ProjectDeploymentWorkflow existingRow = existingRowsByAutomationUuid.get(entry.getKey());
            ResolvedWorkflowConnections resolved = rowSpec.resolved();

            ProjectDeploymentWorkflow row = new ProjectDeploymentWorkflow();

            row.setConnections(resolved.connections());
            row.setEnabled(rowSpec.enabled());
            row.setInputs(
                rowSpec.inputs() != null || existingRow == null ? rowSpec.inputs() : existingRow.getInputs());
            row.setProjectDeploymentId(projectDeploymentId);
            row.setWorkflowId(getWorkflowId(projectDeployment.getProjectId(), projectVersion, entry.getKey()));

            rows.add(row);
        }

        projectDeployment.setProjectVersion(projectVersion);

        projectDeploymentFacade.updateProjectDeployment(projectDeployment, rows, List.of());
    }

    public void putWorkflow(long projectDeploymentId, String automationUuid, RowSpec rowSpec) {
        putWorkflow(projectDeploymentService.getProjectDeployment(projectDeploymentId), automationUuid,
            rowSpec);
    }

    public void setRowEnabled(long projectDeploymentId, ProjectDeploymentWorkflow row, boolean enabled) {
        if (row.isEnabled() != enabled) {
            projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, row.getWorkflowId(), enabled);
        }
    }

    public void removeWorkflow(long projectDeploymentId, String automationUuid) {
        Optional<ProjectDeployment> fetchedProjectDeployment = projectDeploymentService.fetchProjectDeployment(
            projectDeploymentId);

        if (fetchedProjectDeployment.isEmpty()) {
            return;
        }

        Optional<ProjectDeploymentWorkflow> row = fetchRowAtCurrentVersion(
            fetchedProjectDeployment.get(), automationUuid);

        if (row.isPresent()) {
            ProjectDeploymentWorkflow existingRow = row.get();

            if (existingRow.isEnabled()) {
                projectDeploymentFacade.enableProjectDeploymentWorkflow(
                    projectDeploymentId, existingRow.getWorkflowId(), false);
            }

            projectDeploymentWorkflowService.delete(existingRow.getId());
        }

        List<ProjectDeploymentWorkflow> remainingRows = projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            projectDeploymentId);

        if (remainingRows.isEmpty()) {
            projectDeploymentFacade.deleteProjectDeployment(projectDeploymentId);
        }
    }

    public void removeInputs(long projectDeploymentId, String automationUuid, Set<String> inputNames) {
        if (inputNames.isEmpty()) {
            return;
        }

        Optional<ProjectDeployment> projectDeploymentOptional = projectDeploymentService.fetchProjectDeployment(
            projectDeploymentId);

        if (projectDeploymentOptional.isEmpty()) {
            return;
        }

        ProjectDeployment projectDeployment = projectDeploymentOptional.get();

        Optional<ProjectDeploymentWorkflow> rowOptional = fetchRowAtCurrentVersion(
            projectDeployment, automationUuid);

        if (rowOptional.isEmpty()) {
            return;
        }

        ProjectDeploymentWorkflow row = rowOptional.get();

        Map<String, Object> remainingInputs = new LinkedHashMap<>(row.getInputs());

        boolean removed = false;

        for (String inputName : inputNames) {
            if (remainingInputs.containsKey(inputName)) {
                remainingInputs.remove(inputName);

                removed = true;
            }
        }

        if (!removed) {
            return;
        }

        RowSpec rowSpec = new RowSpec(
            new ResolvedWorkflowConnections(row.getConnections(), List.of()), row.isEnabled(), remainingInputs);

        putWorkflow(projectDeployment, automationUuid, rowSpec);
    }

    public ProvidedNames getProvidedNames(
        long connectedUserId, long automationProjectId, String automationUuid) {

        return toProvidedNames(getProvidedValues(connectedUserId, automationProjectId, automationUuid));
    }

    public ProvidedNames getProvidedNames(
        AutomationProjectConfiguration configuration, String automationUuid) {

        return toProvidedNames(getProvidedValues(configuration, automationUuid));
    }

    public void checkProvidedValuesNotRequested(
        long connectedUserId, long automationProjectId, String automationUuid,
        Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> userInputs) {

        if (requestedConnectionIds.isEmpty() && (userInputs == null || userInputs.isEmpty())) {
            return;
        }

        checkProvidedValuesNotRequested(
            toProvidedNames(getProvidedValues(connectedUserId, automationProjectId, automationUuid)),
            requestedConnectionIds, userInputs);
    }

    private static void checkProvidedValuesNotRequested(
        ProvidedNames providedNames, Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> userInputs) {

        Set<String> providedComponentNames = providedNames.componentNames();

        for (String componentName : requestedConnectionIds.keySet()) {
            if (providedComponentNames.contains(componentName)) {
                throw new ProvidedValueLockedException(componentName, ProvidedValueLockedException.Kind.CONNECTION);
            }
        }

        if (userInputs == null) {
            return;
        }

        Set<String> providedInputNames = providedNames.inputNames();

        for (Map.Entry<String, ?> entry : userInputs.entrySet()) {
            if (entry.getValue() != null && providedInputNames.contains(entry.getKey())) {
                throw new ProvidedValueLockedException(entry.getKey(), ProvidedValueLockedException.Kind.INPUT);
            }
        }
    }

    public ReferenceResolution resolveReference(
        long connectedUserId, long automationProjectId, String automationUuid, String workflowId,
        boolean enable, Map<String, Long> requestedConnectionIds,
        List<ProjectDeploymentWorkflowConnection> currentConnections, Map<String, ?> userInputs) {

        ProvidedValues providedValues = getProvidedValues(
            connectedUserId, automationProjectId, automationUuid);

        ResolvedWorkflowConnections resolved = connectedUserWorkflowConnectionResolver.resolve(
            workflowId, connectedUserId, requestedConnectionIds, currentConnections, providedValues.connectionIds());

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, Object> inputs = mergeInputs(workflow, userInputs, providedValues.inputs());

        String missingInputName = findMissingRequiredInput(workflow, inputs);

        int workflowErrorCount = enable ? getWorkflowErrorCount(connectedUserId, workflowId) : 0;

        boolean enabled = enable && resolved.isComplete() && missingInputName == null && workflowErrorCount == 0;

        return new ReferenceResolution(
            new RowSpec(resolved, enabled, inputs), resolved.firstMissingComponentName(), missingInputName,
            workflowErrorCount);
    }

    public void updateInputs(long projectDeploymentId, String automationUuid, Map<String, ?> userInputs) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        ProjectDeploymentWorkflow row = fetchRowAtCurrentVersion(projectDeployment, automationUuid)
            .orElseThrow(() -> new IllegalArgumentException(
                "Automation workflow %s is not deployed in deployment id=%s".formatted(
                    automationUuid, projectDeploymentId)));

        ProvidedValues providedValues = fetchConnectedUserId(projectDeploymentId)
            .map(connectedUserId -> getProvidedValues(
                connectedUserId, projectDeployment.getProjectId(), automationUuid))
            .orElse(ProvidedValues.NONE);

        checkProvidedValuesNotRequested(toProvidedNames(providedValues), Map.of(), userInputs);

        Workflow workflow = workflowService.getWorkflow(row.getWorkflowId());

        Map<String, Object> inputs = mergeInputs(workflow, userInputs, providedValues.inputs());

        if (row.isEnabled()) {
            String missingInputName = findMissingRequiredInput(workflow, inputs);

            if (missingInputName != null) {
                throw new MissingInputException(missingInputName);
            }
        }

        RowSpec rowSpec = new RowSpec(
            new ResolvedWorkflowConnections(row.getConnections(), List.of()), row.isEnabled(), inputs);

        putWorkflow(projectDeployment, automationUuid, rowSpec);
    }

    private long createDeployment(long automationProjectId, ConnectedUser connectedUser) {
        Environment environment = connectedUser.getEnvironment();

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(true);
        projectDeployment.setEnvironment(environment);
        projectDeployment.setName(connectedUser.getExternalId());
        projectDeployment.setProjectId(automationProjectId);
        projectDeployment.setProjectVersion(
            automationProjectConfigurationResolver.resolveAndAssign(connectedUser, automationProjectId)
                .filter(AutomationProjectConfiguration::isEnabled)
                .map(AutomationProjectConfiguration::getProjectVersion)
                .orElseThrow(() -> newNotReleasedException(connectedUser.getId(), automationProjectId)));

        long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(
            projectDeployment, List.of(), List.of());

        connectedUserReferenceDeploymentService.create(
            connectedUser.getId(), automationProjectId, projectDeploymentId);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);

        return projectDeploymentId;
    }

    private Optional<ProjectDeploymentWorkflow> fetchRowAtCurrentVersion(
        ProjectDeployment projectDeployment, String automationUuid) {
        return projectWorkflowService
            .fetchProjectWorkflow(
                projectDeployment.getProjectId(), projectDeployment.getProjectVersion(), automationUuid)
            .flatMap(projectWorkflow -> projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(
                projectDeployment.getId(), projectWorkflow.getWorkflowId()));
    }

    @Nullable
    private static String findMissingRequiredInput(Workflow workflow, Map<String, ?> inputs) {
        for (Workflow.Input input : workflow.getInputs()) {
            Object value = inputs.get(input.name());

            if (input.required() && (value == null || StringUtils.isBlank(String.valueOf(value)))) {
                return input.name();
            }
        }

        return null;
    }

    private ProvidedValues getProvidedValues(
        long connectedUserId, long automationProjectId, String automationUuid) {

        return fetchAssignedConfiguration(connectedUserId, automationProjectId)
            .map(configuration -> getProvidedValues(configuration, automationUuid))
            .orElse(ProvidedValues.NONE);
    }

    private ConfigurationException newNotReleasedException(long connectedUserId, long automationProjectId) {
        return new ConfigurationException(
            "Automation project id=%s is not released to connected user id=%s".formatted(
                automationProjectId, connectedUserId),
            ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_NOT_RELEASED);
    }

    /**
     * Reads the configuration the connected user is assigned to. Every caller starts from an existing reference
     * deployment, so a missing assignment means the project is not released to the user; it never falls back to the
     * self-serve pick, which would move the user implicitly.
     */
    private Optional<AutomationProjectConfiguration> fetchAssignedConfiguration(
        long connectedUserId, long automationProjectId) {

        return connectedUserProjectConfigurationService.fetchAssignment(connectedUserId, automationProjectId)
            .map(assignment -> automationProjectConfigurationService.getConfiguration(
                assignment.getConfigurationId()));
    }

    private static ProvidedValues getProvidedValues(
        AutomationProjectConfiguration configuration, String automationUuid) {

        return configuration.fetchWorkflow(UUID.fromString(automationUuid))
            .map(configurationWorkflow -> new ProvidedValues(
                configurationWorkflow.getProvidedConnections(), configurationWorkflow.getProvidedInputs()))
            .orElse(ProvidedValues.NONE);
    }

    private int getWorkflowErrorCount(long connectedUserId, String workflowId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        return connectedUserWorkflowValidator.getErrorCount(workflowId, connectedUser.getEnvironment());
    }

    private Map<String, ProjectDeploymentWorkflow> getRowsByAutomationUuid(long projectDeploymentId) {
        Map<String, ProjectDeploymentWorkflow> rowsByAutomationUuid = new LinkedHashMap<>();

        for (ProjectDeploymentWorkflow row : projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            projectDeploymentId)) {
            ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(row.getWorkflowId());

            rowsByAutomationUuid.put(projectWorkflow.getUuidAsString(), row);
        }

        return rowsByAutomationUuid;
    }

    private void putWorkflow(ProjectDeployment projectDeployment, String automationUuid, RowSpec rowSpec) {
        long projectDeploymentId = projectDeployment.getId();

        String workflowId = getWorkflowId(
            projectDeployment.getProjectId(), projectDeployment.getProjectVersion(), automationUuid);

        Optional<ProjectDeploymentWorkflow> existingRow = projectDeploymentWorkflowService
            .fetchProjectDeploymentWorkflow(projectDeploymentId, workflowId);

        ResolvedWorkflowConnections resolved = rowSpec.resolved();

        if (existingRow.isPresent()) {
            ProjectDeploymentWorkflow row = existingRow.get();

            if (isUnchanged(row, rowSpec)) {
                return;
            }

            if (row.isEnabled()) {
                projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, false);

                row = projectDeploymentWorkflowService.getProjectDeploymentWorkflow(row.getId());
            }

            row.setConnections(resolved.connections());
            row.setEnabled(false);
            row.setWorkflowId(workflowId);

            Map<String, ?> inputs = rowSpec.inputs();

            if (inputs != null) {
                row.setInputs(inputs);
            }

            ProjectDeploymentWorkflow updatedRow = projectDeploymentWorkflowService.update(row);

            Map<String, ?> updatedInputs = updatedRow.getInputs();

            if (inputs != null && inputs.isEmpty() && !updatedInputs.isEmpty()) {
                projectDeploymentWorkflowService.clearInputs(updatedRow.getId());
            }
        } else {
            ProjectDeploymentWorkflow row = new ProjectDeploymentWorkflow();

            row.setConnections(resolved.connections());
            row.setEnabled(false);
            row.setInputs(rowSpec.inputs());
            row.setProjectDeploymentId(projectDeploymentId);
            row.setWorkflowId(workflowId);

            projectDeploymentWorkflowService.create(row);
        }

        if (rowSpec.enabled()) {
            projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, true);
        }
    }

    private static Map<String, Object> mergeInputs(
        Workflow workflow, Map<String, ?> userInputs, Map<String, Object> providedInputs) {

        Map<String, Object> inputs = new LinkedHashMap<>(userInputs);

        for (Workflow.Input input : workflow.getInputs()) {
            Object providedValue = providedInputs.get(input.name());

            if (providedValue != null) {
                inputs.put(input.name(), providedValue);
            }
        }

        return inputs;
    }

    private ProvidedNames toProvidedNames(ProvidedValues providedValues) {
        Set<String> providedInputNames = new LinkedHashSet<>();
        Map<String, Object> providedInputs = providedValues.inputs();

        for (Map.Entry<String, Object> entry : providedInputs.entrySet()) {
            if (entry.getValue() != null) {
                providedInputNames.add(entry.getKey());
            }
        }

        return new ProvidedNames(
            connectedUserWorkflowConnectionResolver.getProvidedComponentNames(providedValues.connectionIds()),
            providedInputNames);
    }

    private static boolean isUnchanged(ProjectDeploymentWorkflow row, RowSpec rowSpec) {
        ResolvedWorkflowConnections resolved = rowSpec.resolved();

        return row.isEnabled() == rowSpec.enabled() &&
            Set.copyOf(row.getConnections())
                .equals(Set.copyOf(resolved.connections()))
            &&
            (rowSpec.inputs() == null || Objects.equals(row.getInputs(), rowSpec.inputs()));
    }

    public record ReferenceResolution(
        RowSpec rowSpec, @Nullable String missingComponentName, @Nullable String missingInputName,
        int workflowErrorCount) {
    }

    @SuppressFBWarnings("EI")
    public record ProvidedNames(Set<String> componentNames, Set<String> inputNames) {
    }

    @SuppressFBWarnings("EI")
    private record ProvidedValues(Map<String, Long> connectionIds, Map<String, Object> inputs) {

        private static final ProvidedValues NONE = new ProvidedValues(Map.of(), Map.of());
    }

    @SuppressFBWarnings("EI")
    public record RowSpec(ResolvedWorkflowConnections resolved, boolean enabled, @Nullable Map<String, ?> inputs) {
    }
}
