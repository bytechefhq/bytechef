/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class ProjectWorkflowMappingReconciler {

    record Mapping(long id, long projectDeploymentWorkflowId) {
    }

    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    ProjectWorkflowMappingReconciler(
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService) {

        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
    }

    void deleteStaleMappings(
        ProjectDeployment source, ProjectDeployment target, List<Mapping> mappings, LongConsumer deleteMapping) {

        Set<String> sourceWorkflowUuids = workflowUuids(source);
        Map<Long, String> targetWorkflowUuids = workflowUuidsByProjectDeploymentWorkflowId(target);

        for (Mapping mapping : mappings) {
            String workflowUuid = targetWorkflowUuids.get(mapping.projectDeploymentWorkflowId());

            if (workflowUuid == null || !sourceWorkflowUuids.contains(workflowUuid)) {
                deleteMapping.accept(mapping.id());
            }
        }
    }

    void syncMappings(
        List<Mapping> mappings, Map<Long, Long> workflowIdMapping, Map<Long, Map<String, ?>> sourceParameters,
        LongFunction<Long> createMapping, BiConsumer<Long, Map<String, ?>> updateMappingParameters) {

        Map<Long, Long> mappingIdsByProjectDeploymentWorkflowId = new HashMap<>();

        for (Mapping mapping : mappings) {
            mappingIdsByProjectDeploymentWorkflowId.putIfAbsent(mapping.projectDeploymentWorkflowId(), mapping.id());
        }

        List<Long> sourceProjectDeploymentWorkflowIds = sourceParameters.keySet()
            .stream()
            .sorted()
            .toList();

        for (Long sourceProjectDeploymentWorkflowId : sourceProjectDeploymentWorkflowIds) {
            Long targetProjectDeploymentWorkflowId = workflowIdMapping.get(sourceProjectDeploymentWorkflowId);

            if (targetProjectDeploymentWorkflowId == null) {
                continue;
            }

            Long mappingId = mappingIdsByProjectDeploymentWorkflowId.get(targetProjectDeploymentWorkflowId);

            if (mappingId == null) {
                mappingId = createMapping.apply(targetProjectDeploymentWorkflowId);
            }

            updateMappingParameters.accept(mappingId, sourceParameters.get(sourceProjectDeploymentWorkflowId));
        }
    }

    private Set<String> workflowUuids(ProjectDeployment projectDeployment) {
        Map<String, String> uuidsByWorkflowId = uuidsByWorkflowId(projectDeployment);
        Set<String> workflowUuids = new HashSet<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : getProjectDeploymentWorkflows(projectDeployment)) {
            String workflowUuid = uuidsByWorkflowId.get(projectDeploymentWorkflow.getWorkflowId());

            if (workflowUuid != null) {
                workflowUuids.add(workflowUuid);
            }
        }

        return workflowUuids;
    }

    private Map<Long, String> workflowUuidsByProjectDeploymentWorkflowId(ProjectDeployment projectDeployment) {
        Map<String, String> uuidsByWorkflowId = uuidsByWorkflowId(projectDeployment);
        Map<Long, String> workflowUuids = new HashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : getProjectDeploymentWorkflows(projectDeployment)) {
            String workflowUuid = uuidsByWorkflowId.get(projectDeploymentWorkflow.getWorkflowId());

            if (workflowUuid != null) {
                workflowUuids.put(projectDeploymentWorkflow.getId(), workflowUuid);
            }
        }

        return workflowUuids;
    }

    private List<ProjectDeploymentWorkflow> getProjectDeploymentWorkflows(ProjectDeployment projectDeployment) {
        return new ArrayList<>(
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
                Objects.requireNonNull(projectDeployment.getId(), "id")));
    }

    private Map<String, String> uuidsByWorkflowId(ProjectDeployment projectDeployment) {
        Map<String, String> uuidsByWorkflowId = new HashMap<>();

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            Objects.requireNonNull(projectDeployment.getProjectId(), "projectId"),
            projectDeployment.getProjectVersion());

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            uuidsByWorkflowId.put(projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString());
        }

        return uuidsByWorkflowId;
    }
}
