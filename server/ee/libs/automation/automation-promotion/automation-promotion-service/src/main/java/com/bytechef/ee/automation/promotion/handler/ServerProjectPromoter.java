/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SyncResult;
import com.bytechef.ee.automation.promotion.handler.ProjectWorkflowMappingReconciler.Mapping;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class ServerProjectPromoter {

    record TargetProject(long id, ProjectDeployment projectDeployment) {
    }

    record SourceProjectView(
        ProjectDeployment projectDeployment, Map<Long, Map<String, ?>> parameters,
        Supplier<List<String>> workflowIds) {
    }

    interface ServerProjectStore {

        TargetProject createTargetProject(
            long targetServerId, long projectId, int projectVersion, List<String> workflowIds);

        List<Mapping> mappings(long serverProjectId);

        long createMapping(long serverProjectId, long projectDeploymentWorkflowId);

        void deleteMapping(long mappingId);

        void updateMappingParameters(long mappingId, Map<String, ?> parameters);
    }

    private final ProjectDeploymentPromoter projectDeploymentPromoter;
    private final ProjectWorkflowMappingReconciler projectWorkflowMappingReconciler;

    @SuppressFBWarnings("EI")
    ServerProjectPromoter(
        ProjectDeploymentPromoter projectDeploymentPromoter,
        ProjectWorkflowMappingReconciler projectWorkflowMappingReconciler) {

        this.projectDeploymentPromoter = projectDeploymentPromoter;
        this.projectWorkflowMappingReconciler = projectWorkflowMappingReconciler;
    }

    List<Long> promoteProject(
        SourceProjectView sourceProject, long targetServerId,
        Map<Long, Deque<TargetProject>> targetProjectsByProjectId, Map<Long, Long> requestedMappings,
        Map<Long, Long> suggestedMappings, ServerProjectStore serverProjectStore) {

        ProjectDeployment sourceProjectDeployment = sourceProject.projectDeployment();

        long projectId = getProjectId(sourceProjectDeployment);

        TargetProject existingTargetProject = pollTargetProject(targetProjectsByProjectId, projectId);
        boolean projectIsNew = existingTargetProject == null;
        TargetProject targetProject;

        if (existingTargetProject == null) {
            targetProject = serverProjectStore.createTargetProject(
                targetServerId, projectId, sourceProjectDeployment.getProjectVersion(),
                sourceProject.workflowIds()
                    .get());
        } else {
            targetProject = existingTargetProject;
        }

        long serverProjectId = targetProject.id();
        ProjectDeployment targetProjectDeployment = targetProject.projectDeployment();

        projectWorkflowMappingReconciler.deleteStaleMappings(
            sourceProjectDeployment, targetProjectDeployment, serverProjectStore.mappings(serverProjectId),
            serverProjectStore::deleteMapping);

        SyncResult syncResult = projectDeploymentPromoter.sync(
            sourceProjectDeployment, targetProjectDeployment, requestedMappings, suggestedMappings, projectIsNew);

        projectWorkflowMappingReconciler.syncMappings(
            serverProjectStore.mappings(serverProjectId), syncResult.workflowIdMapping(), sourceProject.parameters(),
            projectDeploymentWorkflowId -> serverProjectStore.createMapping(
                serverProjectId, projectDeploymentWorkflowId),
            serverProjectStore::updateMappingParameters);

        return syncResult.unresolvedConnectionIds();
    }

    static Map<Long, Deque<TargetProject>> groupByProjectId(List<TargetProject> targetProjects) {
        Map<Long, Deque<TargetProject>> targetProjectsByProjectId = new LinkedHashMap<>();

        for (TargetProject targetProject : targetProjects) {
            Deque<TargetProject> deque = targetProjectsByProjectId.computeIfAbsent(
                getProjectId(targetProject.projectDeployment()), id -> new ArrayDeque<>());

            deque.addLast(targetProject);
        }

        return targetProjectsByProjectId;
    }

    static long getProjectId(ProjectDeployment projectDeployment) {
        return Objects.requireNonNull(projectDeployment.getProjectId(), "projectId");
    }

    @Nullable
    static TargetProject pollTargetProject(
        Map<Long, Deque<TargetProject>> targetProjectsByProjectId, long projectId) {

        Deque<TargetProject> targetProjects = targetProjectsByProjectId.get(projectId);

        return targetProjects == null ? null : targetProjects.pollFirst();
    }

    static void putAllIfAbsent(Map<Long, Long> target, Map<Long, Long> source) {
        for (Map.Entry<Long, Long> entry : source.entrySet()) {
            target.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    static void addAmbiguousSourceProjectWarnings(
        List<String> warnings, List<ProjectDeployment> sourceProjectDeployments, String serverKind) {

        Set<Long> seenProjectIds = new LinkedHashSet<>();
        Set<Long> duplicateProjectIds = new LinkedHashSet<>();

        for (ProjectDeployment sourceProjectDeployment : sourceProjectDeployments) {
            if (!seenProjectIds.add(getProjectId(sourceProjectDeployment))) {
                duplicateProjectIds.add(getProjectId(sourceProjectDeployment));
            }
        }

        for (Long duplicateProjectId : duplicateProjectIds) {
            warnings.add(ambiguousProjectWarning("source", serverKind, duplicateProjectId));
        }
    }

    static void addAmbiguousTargetProjectWarnings(
        List<String> warnings, Map<Long, Deque<TargetProject>> targetProjectsByProjectId, String serverKind) {

        for (Map.Entry<Long, Deque<TargetProject>> entry : targetProjectsByProjectId.entrySet()) {
            Deque<TargetProject> targetProjects = entry.getValue();

            if (targetProjects.size() > 1) {
                warnings.add(ambiguousProjectWarning("target", serverKind, entry.getKey()));
            }
        }
    }

    private static String ambiguousProjectWarning(String side, String serverKind, long projectId) {
        return "The %s %s exposes project %s more than once; entries are matched in ascending id order"
            .formatted(side, serverKind, projectId);
    }
}
