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
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

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

    private final ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserReferenceDeploymentManager(
        ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.connectedUserWorkflowConnectionResolver = connectedUserWorkflowConnectionResolver;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
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
        Workflow workflow = workflowService.getWorkflow(workflowId);

        for (Workflow.Input input : workflow.getInputs()) {
            Object value = inputs.get(input.name());

            if (input.required() && (value == null || StringUtils.isBlank(String.valueOf(value)))) {
                return input.name();
            }
        }

        return null;
    }

    public ProjectDeployment getDeployment(long projectDeploymentId) {
        return projectDeploymentService.getProjectDeployment(projectDeploymentId);
    }

    public String getDeploymentName(String externalUserId, Environment environment) {
        return MARKER + externalUserId + "__" + environment.name();
    }

    public Map<String, ?> getInputs(long projectDeploymentId, String automationWorkflowUuid) {
        return fetchRow(projectDeploymentId, automationWorkflowUuid)
            .<Map<String, ?>>map(ProjectDeploymentWorkflow::getInputs)
            .orElse(Map.of());
    }

    public int getLastPublishedVersion(long automationWorkflowProjectId) {
        Project project = projectService.getProject(automationWorkflowProjectId);

        ProjectVersion lastPublishedProjectVersion = project.getLastPublishedProjectVersion();

        if (lastPublishedProjectVersion == null) {
            throw new IllegalArgumentException(
                "Automation workflow project id=%s is not published".formatted(automationWorkflowProjectId));
        }

        return lastPublishedProjectVersion.getVersion();
    }

    public long
        getOrCreateDeployment(long automationWorkflowProjectId, String externalUserId, Environment environment) {
        String name = getDeploymentName(externalUserId, environment);

        return projectDeploymentService.fetchProjectDeploymentByName(automationWorkflowProjectId, name)
            .map(ProjectDeployment::getId)
            .orElseGet(() -> {
                ProjectDeployment projectDeployment = new ProjectDeployment();

                projectDeployment.setEnabled(true);
                projectDeployment.setEnvironment(environment);
                projectDeployment.setName(name);
                projectDeployment.setProjectId(automationWorkflowProjectId);
                projectDeployment.setProjectVersion(getLastPublishedVersion(automationWorkflowProjectId));

                long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(
                    projectDeployment, List.of(), List.of());

                projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);

                return projectDeploymentId;
            });
    }

    public static boolean isReferenceDeployment(ProjectDeployment projectDeployment) {
        String name = projectDeployment.getName();

        return name != null && name.startsWith(MARKER);
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

    public ReferenceResolution resolveReference(
        long connectedUserId, String workflowId, boolean enable, Map<String, Long> requestedConnectionIds,
        List<ProjectDeploymentWorkflowConnection> currentConnections, Map<String, ?> inputs) {
        ResolvedWorkflowConnections resolved = connectedUserWorkflowConnectionResolver.resolve(
            workflowId, connectedUserId, requestedConnectionIds, currentConnections);

        String missingInputName = findMissingRequiredInput(workflowId, inputs);

        boolean enabled = enable && resolved.isComplete() && missingInputName == null;

        return new ReferenceResolution(
            new RowSpec(resolved, enabled, null), resolved.firstMissingComponentName(), missingInputName);
    }

    public void updateInputs(long projectDeploymentId, String automationWorkflowUuid, Map<String, ?> inputs) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        ProjectDeploymentWorkflow row = fetchRowAtCurrentVersion(projectDeployment, automationWorkflowUuid)
            .orElseThrow(() -> new IllegalArgumentException(
                "Automation workflow %s is not deployed in deployment id=%s".formatted(
                    automationWorkflowUuid, projectDeploymentId)));

        if (row.isEnabled()) {
            String missingInputName = findMissingRequiredInput(row.getWorkflowId(), inputs);

            if (missingInputName != null) {
                throw new MissingInputException(missingInputName);
            }
        }

        RowSpec rowSpec = new RowSpec(
            new ResolvedWorkflowConnections(row.getConnections(), List.of()), row.isEnabled(), inputs);

        putWorkflow(projectDeployment, automationWorkflowUuid, rowSpec);
    }

    private Optional<ProjectDeploymentWorkflow> fetchRowAtCurrentVersion(
        ProjectDeployment projectDeployment, String automationWorkflowUuid) {
        return projectWorkflowService
            .fetchProjectWorkflow(
                projectDeployment.getProjectId(), projectDeployment.getProjectVersion(), automationWorkflowUuid)
            .flatMap(projectWorkflow -> projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(
                projectDeployment.getId(), projectWorkflow.getWorkflowId()));
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

            if (rowSpec.inputs() != null) {
                row.setInputs(rowSpec.inputs());
            }

            projectDeploymentWorkflowService.update(row);
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

    private static boolean isUnchanged(ProjectDeploymentWorkflow row, RowSpec rowSpec) {
        ResolvedWorkflowConnections resolved = rowSpec.resolved();

        return row.isEnabled() == rowSpec.enabled() &&
            Set.copyOf(row.getConnections())
                .equals(Set.copyOf(resolved.connections()))
            &&
            (rowSpec.inputs() == null || Objects.equals(row.getInputs(), rowSpec.inputs()));
    }

    public record ReferenceResolution(
        RowSpec rowSpec, @Nullable String missingComponentName, @Nullable String missingInputName) {
    }

    @SuppressFBWarnings("EI")
    public record RowSpec(ResolvedWorkflowConnections resolved, boolean enabled, @Nullable Map<String, ?> inputs) {
    }
}
