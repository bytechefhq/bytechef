/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.deployment;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.exception.ProjectDeploymentErrorType;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ProjectDeploymentPromoter {

    public record SourceBinding(
        @Nullable String workflowUuid, String workflowLabel, String nodeName, String key, long connectionId) {
    }

    public record SyncResult(
        Map<Long, Long> workflowIdMapping, List<Long> unresolvedConnectionIds, List<String> warnings) {

        public SyncResult {
            workflowIdMapping = Map.copyOf(workflowIdMapping);
            unresolvedConnectionIds = List.copyOf(unresolvedConnectionIds);
            warnings = List.copyOf(warnings);
        }
    }

    private record NodeBindingKey(@Nullable String workflowUuid, String nodeName, String key) {
    }

    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentPromoter(
        ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    public void validatePromotable(long projectId, int projectVersion) {
        Project project = projectService.getProject(projectId);

        if (!project.isPublished()) {
            throw new ConfigurationException(
                "Project id=%s is not published".formatted(projectId),
                ProjectDeploymentErrorType.PROJECT_NOT_PUBLISHED);
        }

        if (project.getLastProjectVersion() == projectVersion) {
            throw new ConfigurationException(
                "Project version v=%s cannot be in DRAFT".formatted(projectVersion),
                ProjectDeploymentErrorType.INVALID_PROJECT_VERSION);
        }
    }

    public List<SourceBinding> collectSourceBindings(ProjectDeployment source) {
        Map<String, String> uuidsByWorkflowId = uuidsByWorkflowId(getProjectId(source), source.getProjectVersion());
        List<SourceBinding> sourceBindings = new ArrayList<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : getProjectDeploymentWorkflows(source)) {
            Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

            for (ProjectDeploymentWorkflowConnection connection : projectDeploymentWorkflow.getConnections()) {
                sourceBindings.add(
                    new SourceBinding(
                        uuidsByWorkflowId.get(projectDeploymentWorkflow.getWorkflowId()), workflow.getLabel(),
                        connection.getWorkflowNodeName(), connection.getWorkflowConnectionKey(),
                        connection.getConnectionId()));
            }
        }

        return sourceBindings;
    }

    public Map<Long, Long> existingTargetBindings(ProjectDeployment source, ProjectDeployment target) {
        Map<NodeBindingKey, Long> targetConnectionIdsByTriple = new HashMap<>();
        Map<String, String> targetUuidsByWorkflowId =
            uuidsByWorkflowId(getProjectId(target), target.getProjectVersion());

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : getProjectDeploymentWorkflows(target)) {
            String workflowUuid = targetUuidsByWorkflowId.get(projectDeploymentWorkflow.getWorkflowId());

            for (ProjectDeploymentWorkflowConnection connection : projectDeploymentWorkflow.getConnections()) {
                targetConnectionIdsByTriple.put(
                    nodeBindingKey(workflowUuid, connection), connection.getConnectionId());
            }
        }

        Map<Long, Long> existingBindings = new HashMap<>();
        Map<String, String> sourceUuidsByWorkflowId =
            uuidsByWorkflowId(getProjectId(source), source.getProjectVersion());

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : getProjectDeploymentWorkflows(source)) {
            String workflowUuid = sourceUuidsByWorkflowId.get(projectDeploymentWorkflow.getWorkflowId());

            for (ProjectDeploymentWorkflowConnection connection : projectDeploymentWorkflow.getConnections()) {
                Long targetConnectionId = targetConnectionIdsByTriple.get(nodeBindingKey(workflowUuid, connection));

                if (targetConnectionId != null) {
                    existingBindings.putIfAbsent(connection.getConnectionId(), targetConnectionId);
                }
            }
        }

        return existingBindings;
    }

    public SyncResult sync(
        ProjectDeployment source, ProjectDeployment target, Map<Long, Long> requestedMappings,
        Map<Long, Long> suggestedMappings, boolean targetIsNew) {

        long sourceProjectId = getProjectId(source);
        long targetProjectId = getProjectId(target);
        int sourceProjectVersion = source.getProjectVersion();

        Map<String, ProjectWorkflow> sourceProjectWorkflowsByWorkflowId =
            projectWorkflowsByWorkflowId(sourceProjectId, sourceProjectVersion);
        Map<String, String> targetUuidsByWorkflowId = uuidsByWorkflowId(targetProjectId, target.getProjectVersion());

        Map<String, ProjectDeploymentWorkflow> targetProjectDeploymentWorkflowsByUuid = new HashMap<>();
        Map<NodeBindingKey, Long> targetConnectionIdsByNode = new HashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : getProjectDeploymentWorkflows(target)) {
            String workflowUuid = targetUuidsByWorkflowId.get(projectDeploymentWorkflow.getWorkflowId());

            targetProjectDeploymentWorkflowsByUuid.put(workflowUuid, projectDeploymentWorkflow);

            for (ProjectDeploymentWorkflowConnection connection : projectDeploymentWorkflow.getConnections()) {
                targetConnectionIdsByNode.put(nodeBindingKey(workflowUuid, connection), connection.getConnectionId());
            }
        }

        Map<NodeBindingKey, Long> existingNodeBindings = targetIsNew ? Map.of() : targetConnectionIdsByNode;
        Set<Long> unresolvedConnectionIds = new LinkedHashSet<>();
        List<String> warnings = new ArrayList<>();
        List<ProjectDeploymentWorkflow> syncedProjectDeploymentWorkflows = new ArrayList<>();
        List<ProjectDeploymentWorkflow> sourceProjectDeploymentWorkflows = getProjectDeploymentWorkflows(source);

        for (ProjectDeploymentWorkflow sourceProjectDeploymentWorkflow : sourceProjectDeploymentWorkflows) {
            ProjectWorkflow sourceProjectWorkflow =
                sourceProjectWorkflowsByWorkflowId.get(sourceProjectDeploymentWorkflow.getWorkflowId());

            if (sourceProjectWorkflow == null) {
                Workflow workflow = workflowService.getWorkflow(sourceProjectDeploymentWorkflow.getWorkflowId());

                warnings.add(
                    "Workflow %s no longer exists in version %s and was skipped".formatted(
                        workflow.getLabel(), sourceProjectVersion));

                continue;
            }

            syncedProjectDeploymentWorkflows.add(
                syncProjectDeploymentWorkflow(
                    target, sourceProjectDeploymentWorkflow, sourceProjectWorkflow,
                    targetProjectDeploymentWorkflowsByUuid.get(sourceProjectWorkflow.getUuidAsString()), targetIsNew,
                    requestedMappings, existingNodeBindings, suggestedMappings, unresolvedConnectionIds));
        }

        target.setProjectVersion(sourceProjectVersion);

        projectDeploymentFacade.updateProjectDeployment(target, syncedProjectDeploymentWorkflows, List.of());

        return new SyncResult(
            workflowIdMapping(source, target, sourceProjectDeploymentWorkflows),
            new ArrayList<>(unresolvedConnectionIds), warnings);
    }

    private ProjectDeploymentWorkflow syncProjectDeploymentWorkflow(
        ProjectDeployment target, ProjectDeploymentWorkflow sourceProjectDeploymentWorkflow,
        ProjectWorkflow sourceProjectWorkflow, @Nullable ProjectDeploymentWorkflow targetProjectDeploymentWorkflow,
        boolean targetIsNew, Map<Long, Long> requestedMappings, Map<NodeBindingKey, Long> existingNodeBindings,
        Map<Long, Long> suggestedMappings, Set<Long> unresolvedConnectionIds) {

        boolean keepTargetConfiguration = !targetIsNew && targetProjectDeploymentWorkflow != null;

        ProjectDeploymentWorkflow syncedProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        syncedProjectDeploymentWorkflow.setProjectDeploymentId(target.getId());
        syncedProjectDeploymentWorkflow.setWorkflowId(sourceProjectWorkflow.getWorkflowId());
        syncedProjectDeploymentWorkflow.setEnabled(
            keepTargetConfiguration ? targetProjectDeploymentWorkflow.isEnabled()
                : sourceProjectDeploymentWorkflow.isEnabled());
        syncedProjectDeploymentWorkflow.setInputs(
            keepTargetConfiguration ? targetProjectDeploymentWorkflow.getInputs()
                : sourceProjectDeploymentWorkflow.getInputs());

        List<ProjectDeploymentWorkflowConnection> connections = new ArrayList<>();

        String workflowUuid = sourceProjectWorkflow.getUuidAsString();

        for (ProjectDeploymentWorkflowConnection sourceConnection : sourceProjectDeploymentWorkflow.getConnections()) {
            Long targetConnectionId = resolveConnectionId(
                sourceConnection.getConnectionId(), nodeBindingKey(workflowUuid, sourceConnection), requestedMappings,
                existingNodeBindings, suggestedMappings);

            if (targetConnectionId == null) {
                unresolvedConnectionIds.add(sourceConnection.getConnectionId());
            } else {
                connections.add(
                    new ProjectDeploymentWorkflowConnection(
                        targetConnectionId, sourceConnection.getWorkflowConnectionKey(),
                        sourceConnection.getWorkflowNodeName()));
            }
        }

        syncedProjectDeploymentWorkflow.setConnections(connections);

        return syncedProjectDeploymentWorkflow;
    }

    @Nullable
    private static Long resolveConnectionId(
        long sourceConnectionId, NodeBindingKey nodeBindingKey, Map<Long, Long> requestedMappings,
        Map<NodeBindingKey, Long> existingNodeBindings, Map<Long, Long> suggestedMappings) {

        Long targetConnectionId = requestedMappings.get(sourceConnectionId);

        if (targetConnectionId == null) {
            targetConnectionId = existingNodeBindings.get(nodeBindingKey);
        }

        if (targetConnectionId == null) {
            targetConnectionId = suggestedMappings.get(sourceConnectionId);
        }

        return targetConnectionId;
    }

    private Map<Long, Long> workflowIdMapping(
        ProjectDeployment source, ProjectDeployment target,
        List<ProjectDeploymentWorkflow> sourceProjectDeploymentWorkflows) {

        int sourceProjectVersion = source.getProjectVersion();

        Map<String, String> sourceUuidsByWorkflowId = uuidsByWorkflowId(getProjectId(source), sourceProjectVersion);
        Map<String, String> syncedUuidsByWorkflowId = uuidsByWorkflowId(getProjectId(target), sourceProjectVersion);

        Map<String, Long> targetIdsByUuid = new HashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : getProjectDeploymentWorkflows(target)) {
            targetIdsByUuid.put(
                syncedUuidsByWorkflowId.get(projectDeploymentWorkflow.getWorkflowId()),
                projectDeploymentWorkflow.getId());
        }

        Map<Long, Long> workflowIdMapping = new HashMap<>();

        for (ProjectDeploymentWorkflow sourceProjectDeploymentWorkflow : sourceProjectDeploymentWorkflows) {
            Long targetId =
                targetIdsByUuid.get(sourceUuidsByWorkflowId.get(sourceProjectDeploymentWorkflow.getWorkflowId()));

            if (targetId != null) {
                workflowIdMapping.put(sourceProjectDeploymentWorkflow.getId(), targetId);
            }
        }

        return workflowIdMapping;
    }

    private List<ProjectDeploymentWorkflow> getProjectDeploymentWorkflows(ProjectDeployment projectDeployment) {
        return projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            Objects.requireNonNull(projectDeployment.getId(), "id"));
    }

    private Map<String, String> uuidsByWorkflowId(long projectId, int projectVersion) {
        Map<String, String> uuidsByWorkflowId = new HashMap<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(projectId, projectVersion)) {
            uuidsByWorkflowId.put(projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString());
        }

        return uuidsByWorkflowId;
    }

    private Map<String, ProjectWorkflow> projectWorkflowsByWorkflowId(long projectId, int projectVersion) {
        Map<String, ProjectWorkflow> projectWorkflowsByWorkflowId = new HashMap<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(projectId, projectVersion)) {
            projectWorkflowsByWorkflowId.put(projectWorkflow.getWorkflowId(), projectWorkflow);
        }

        return projectWorkflowsByWorkflowId;
    }

    private static long getProjectId(ProjectDeployment projectDeployment) {
        return Objects.requireNonNull(projectDeployment.getProjectId(), "projectId");
    }

    private static NodeBindingKey nodeBindingKey(
        @Nullable String workflowUuid, ProjectDeploymentWorkflowConnection connection) {

        return new NodeBindingKey(
            workflowUuid, connection.getWorkflowNodeName(), connection.getWorkflowConnectionKey());
    }
}
