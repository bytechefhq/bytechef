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
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import com.bytechef.ee.embedded.configuration.exception.ConnectedUserProjectWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.exception.ProvidedValueLockedException;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
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

    private static final String MARKER = "__EMBEDDED__";

    private final AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;
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
        AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService,
        ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService,
        ConnectedUserService connectedUserService,
        ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver,
        ConnectedUserWorkflowValidator connectedUserWorkflowValidator, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.automationWorkflowProjectConfigurationService = automationWorkflowProjectConfigurationService;
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

    public Optional<ProjectDeploymentWorkflow> fetchRow(long projectDeploymentId, String automationWorkflowUuid) {
        return projectDeploymentService.fetchProjectDeployment(projectDeploymentId)
            .flatMap(projectDeployment -> fetchRowAtCurrentVersion(projectDeployment, automationWorkflowUuid));
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

    public ProjectDeployment getDeployment(long projectDeploymentId) {
        return projectDeploymentService.getProjectDeployment(projectDeploymentId);
    }

    public Map<String, ?> getInputs(long projectDeploymentId, String automationWorkflowUuid) {
        return fetchRow(projectDeploymentId, automationWorkflowUuid)
            .<Map<String, ?>>map(ProjectDeploymentWorkflow::getInputs)
            .orElse(Map.of());
    }

    public Optional<Integer> fetchReleasedVersion(long automationWorkflowProjectId, Environment environment) {
        return automationWorkflowProjectConfigurationService
            .fetchConfiguration(automationWorkflowProjectId, environment.ordinal())
            .filter(AutomationWorkflowProjectConfiguration::isEnabled)
            .map(AutomationWorkflowProjectConfiguration::getProjectVersion);
    }

    public int getReleasedVersion(long automationWorkflowProjectId, Environment environment) {
        return fetchReleasedVersion(automationWorkflowProjectId, environment)
            .orElseThrow(() -> new ConfigurationException(
                "Automation workflow project id=%s is not released in the %s environment".formatted(
                    automationWorkflowProjectId, environment),
                ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_NOT_RELEASED));
    }

    @Transactional
    public long
        getOrCreateDeployment(long automationWorkflowProjectId, String externalUserId, Environment environment) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return connectedUserReferenceDeploymentService
            .fetchConnectedUserReferenceDeployment(connectedUser.getId(), automationWorkflowProjectId)
            .map(ConnectedUserReferenceDeployment::getProjectDeploymentId)
            .orElseGet(() -> createDeployment(automationWorkflowProjectId, connectedUser));
    }

    public boolean isAdministrativelyEnabled(
        long automationWorkflowProjectId, Environment environment, String automationWorkflowUuid) {

        Optional<AutomationWorkflowProjectConfiguration> configurationOptional =
            automationWorkflowProjectConfigurationService.fetchConfiguration(
                automationWorkflowProjectId, environment.ordinal());

        if (configurationOptional.isEmpty()) {
            return false;
        }

        AutomationWorkflowProjectConfiguration configuration = configurationOptional.get();

        return configuration.isEnabled() && automationWorkflowProjectConfigurationService.isWorkflowEnabled(
            configuration, UUID.fromString(automationWorkflowUuid));
    }

    public boolean isReferenceDeployment(long projectDeploymentId) {
        return connectedUserReferenceDeploymentService.isReferenceDeployment(projectDeploymentId);
    }

    public Optional<String>
        fetchWorkflowId(long automationWorkflowProjectId, int projectVersion, String automationWorkflowUuid) {
        return projectWorkflowService
            .fetchProjectWorkflow(automationWorkflowProjectId, projectVersion, automationWorkflowUuid)
            .map(ProjectWorkflow::getWorkflowId);
    }

    public String getWorkflowId(long automationWorkflowProjectId, int projectVersion, String automationWorkflowUuid) {
        return fetchWorkflowId(automationWorkflowProjectId, projectVersion, automationWorkflowUuid)
            .orElseThrow(() -> new IllegalArgumentException(
                "Automation workflow %s is not in version %s of project id=%s".formatted(
                    automationWorkflowUuid, projectVersion, automationWorkflowProjectId)));
    }

    public void putWorkflows(
        long projectDeploymentId, int projectVersion, Map<String, RowSpec> rowSpecsByAutomationWorkflowUuid) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        if (projectDeployment.getProjectVersion() == projectVersion) {
            for (Map.Entry<String, RowSpec> entry : rowSpecsByAutomationWorkflowUuid.entrySet()) {
                putWorkflow(projectDeployment, entry.getKey(), entry.getValue());
            }

            return;
        }

        Map<String, ProjectDeploymentWorkflow> existingRowsByAutomationWorkflowUuid = getRowsByAutomationWorkflowUuid(
            projectDeploymentId);

        List<ProjectDeploymentWorkflow> rows = new ArrayList<>();

        for (Map.Entry<String, RowSpec> entry : rowSpecsByAutomationWorkflowUuid.entrySet()) {
            RowSpec rowSpec = entry.getValue();

            ProjectDeploymentWorkflow existingRow = existingRowsByAutomationWorkflowUuid.get(entry.getKey());
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

    public void putWorkflow(long projectDeploymentId, String automationWorkflowUuid, RowSpec rowSpec) {
        putWorkflow(projectDeploymentService.getProjectDeployment(projectDeploymentId), automationWorkflowUuid,
            rowSpec);
    }

    public void setRowEnabled(long projectDeploymentId, ProjectDeploymentWorkflow row, boolean enabled) {
        if (row.isEnabled() != enabled) {
            projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, row.getWorkflowId(), enabled);
        }
    }

    public void removeWorkflow(long projectDeploymentId, String automationWorkflowUuid) {
        Optional<ProjectDeployment> fetchedProjectDeployment = projectDeploymentService.fetchProjectDeployment(
            projectDeploymentId);

        if (fetchedProjectDeployment.isEmpty()) {
            return;
        }

        Optional<ProjectDeploymentWorkflow> row = fetchRowAtCurrentVersion(
            fetchedProjectDeployment.get(), automationWorkflowUuid);

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

    /**
     * Removes the named inputs from a connected user's stored inputs of the workflow, e.g. the administrator's values
     * of inputs the configuration no longer provides. A row without any of them is left untouched; a turned-on row is
     * redeployed with the remaining inputs.
     */
    public void removeInputs(long projectDeploymentId, String automationWorkflowUuid, Set<String> inputNames) {
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
            projectDeployment, automationWorkflowUuid);

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

        putWorkflow(projectDeployment, automationWorkflowUuid, rowSpec);
    }

    /**
     * Returns the names of the components whose connection, and of the inputs whose value, the configuration of the
     * automation workflow project in the environment provides for the workflow.
     */
    public ProvidedNames getProvidedNames(
        long automationWorkflowProjectId, Environment environment, String automationWorkflowUuid) {

        return toProvidedNames(getProvidedValues(automationWorkflowProjectId, environment, automationWorkflowUuid));
    }

    /**
     * Returns the names of the components whose connection, and of the inputs whose value, the configuration provides
     * for the workflow.
     */
    public ProvidedNames getProvidedNames(
        AutomationWorkflowProjectConfiguration configuration, String automationWorkflowUuid) {

        return toProvidedNames(getProvidedValues(configuration, automationWorkflowUuid));
    }

    /**
     * Rejects a connected user's request that sets the connection of a component, or the value of an input, the
     * configuration of the automation workflow project in the environment provides for the workflow. A request that
     * leaves them out, or gives an input no value, passes.
     *
     * @throws ProvidedValueLockedException naming the first provided component or input the request sets
     */
    public void checkProvidedValuesNotRequested(
        long automationWorkflowProjectId, Environment environment, String automationWorkflowUuid,
        Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> userInputs) {

        if (requestedConnectionIds.isEmpty() && (userInputs == null || userInputs.isEmpty())) {
            return;
        }

        ProvidedNames providedNames = getProvidedNames(
            automationWorkflowProjectId, environment, automationWorkflowUuid);

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

    /**
     * Resolves a connected user's reference to the workflow {@code workflowId} of the automation workflow project in
     * the environment, applying the connections and inputs the project's configuration provides for the workflow: a
     * provided connection fills its component's slots, and the returned row inputs are {@code userInputs} overridden by
     * the provided inputs, so a provided input never counts as missing.
     */
    public ReferenceResolution resolveReference(
        long connectedUserId, long automationWorkflowProjectId, Environment environment, String automationWorkflowUuid,
        String workflowId, boolean enable, Map<String, Long> requestedConnectionIds,
        List<ProjectDeploymentWorkflowConnection> currentConnections, Map<String, ?> userInputs) {

        ProvidedValues providedValues = getProvidedValues(
            automationWorkflowProjectId, environment, automationWorkflowUuid);

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

    public void updateInputs(long projectDeploymentId, String automationWorkflowUuid, Map<String, ?> userInputs) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        ProjectDeploymentWorkflow row = fetchRowAtCurrentVersion(projectDeployment, automationWorkflowUuid)
            .orElseThrow(() -> new IllegalArgumentException(
                "Automation workflow %s is not deployed in deployment id=%s".formatted(
                    automationWorkflowUuid, projectDeploymentId)));

        checkProvidedValuesNotRequested(
            projectDeployment.getProjectId(), projectDeployment.getEnvironment(), automationWorkflowUuid, Map.of(),
            userInputs);

        ProvidedValues providedValues = getProvidedValues(
            projectDeployment.getProjectId(), projectDeployment.getEnvironment(), automationWorkflowUuid);
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

        putWorkflow(projectDeployment, automationWorkflowUuid, rowSpec);
    }

    private long createDeployment(long automationWorkflowProjectId, ConnectedUser connectedUser) {
        Environment environment = connectedUser.getEnvironment();

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(true);
        projectDeployment.setEnvironment(environment);
        projectDeployment.setName(getDeploymentName(connectedUser.getExternalId(), environment));
        projectDeployment.setProjectId(automationWorkflowProjectId);
        projectDeployment.setProjectVersion(getReleasedVersion(automationWorkflowProjectId, environment));

        long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(
            projectDeployment, List.of(), List.of());

        connectedUserReferenceDeploymentService.create(
            connectedUser.getId(), automationWorkflowProjectId, projectDeploymentId);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);

        return projectDeploymentId;
    }

    private Optional<ProjectDeploymentWorkflow> fetchRowAtCurrentVersion(
        ProjectDeployment projectDeployment, String automationWorkflowUuid) {
        return projectWorkflowService
            .fetchProjectWorkflow(
                projectDeployment.getProjectId(), projectDeployment.getProjectVersion(), automationWorkflowUuid)
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

    private static String getDeploymentName(String externalUserId, Environment environment) {
        return MARKER + externalUserId + "__" + environment.name();
    }

    /**
     * The single place every resolution path reads the connections and inputs the configuration of the automation
     * workflow project in the environment provides for the workflow. No configuration, or no configuration row for the
     * workflow, provides nothing.
     */
    private ProvidedValues getProvidedValues(
        long automationWorkflowProjectId, Environment environment, String automationWorkflowUuid) {

        return automationWorkflowProjectConfigurationService
            .fetchConfiguration(automationWorkflowProjectId, environment.ordinal())
            .map(configuration -> getProvidedValues(configuration, automationWorkflowUuid))
            .orElse(ProvidedValues.NONE);
    }

    private static ProvidedValues getProvidedValues(
        AutomationWorkflowProjectConfiguration configuration, String automationWorkflowUuid) {

        return configuration.fetchWorkflow(UUID.fromString(automationWorkflowUuid))
            .map(configurationWorkflow -> new ProvidedValues(
                configurationWorkflow.getProvidedConnections(), configurationWorkflow.getProvidedInputs()))
            .orElse(ProvidedValues.NONE);
    }

    private int getWorkflowErrorCount(long connectedUserId, String workflowId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        return connectedUserWorkflowValidator.getErrorCount(workflowId, connectedUser.getEnvironment());
    }

    private Map<String, ProjectDeploymentWorkflow> getRowsByAutomationWorkflowUuid(long projectDeploymentId) {
        Map<String, ProjectDeploymentWorkflow> rowsByAutomationWorkflowUuid = new LinkedHashMap<>();

        for (ProjectDeploymentWorkflow row : projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            projectDeploymentId)) {
            ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(row.getWorkflowId());

            rowsByAutomationWorkflowUuid.put(projectWorkflow.getUuidAsString(), row);
        }

        return rowsByAutomationWorkflowUuid;
    }

    private void putWorkflow(ProjectDeployment projectDeployment, String automationWorkflowUuid, RowSpec rowSpec) {
        long projectDeploymentId = projectDeployment.getId();

        String workflowId = getWorkflowId(
            projectDeployment.getProjectId(), projectDeployment.getProjectVersion(), automationWorkflowUuid);

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

    /**
     * Returns the user's inputs overridden by the provided inputs. Provided inputs that are not inputs of the workflow,
     * or that have no value, are ignored.
     */
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

    /**
     * The names of the components whose connection, and of the inputs whose value, a configuration provides for a
     * workflow.
     */
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
