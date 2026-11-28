/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.deployment;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.ee.automation.configuration.promotion.deployment.ProjectDeploymentPromoter.SyncResult;
import com.bytechef.ee.automation.configuration.promotion.deployment.ProjectWorkflowMappingReconciler.Mapping;
import com.bytechef.ee.automation.configuration.promotion.deployment.ProjectWorkflowMappingReconciler.MappingPlan;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionChange.Kind;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongFunction;
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
public class ServerProjectPromoter {

    @SuppressFBWarnings("EI")
    public record TargetProject(long id, ProjectDeployment projectDeployment) {
    }

    @SuppressFBWarnings("EI")
    public record SourceProjectView(
        ProjectDeployment projectDeployment, Map<Long, Map<String, ?>> parameters,
        Supplier<List<String>> workflowIds) {
    }

    public interface ServerProjectStore {

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
    public ServerProjectPromoter(
        ProjectDeploymentPromoter projectDeploymentPromoter,
        ProjectWorkflowMappingReconciler projectWorkflowMappingReconciler) {

        this.projectDeploymentPromoter = projectDeploymentPromoter;
        this.projectWorkflowMappingReconciler = projectWorkflowMappingReconciler;
    }

    public List<Long> promoteProject(
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

        MappingPlan mappingPlan = projectWorkflowMappingReconciler.planMappings(
            sourceProjectDeployment, targetProjectDeployment, serverProjectStore.mappings(serverProjectId),
            sourceProject.parameters());

        projectWorkflowMappingReconciler.deleteStaleMappings(mappingPlan, serverProjectStore::deleteMapping);

        SyncResult syncResult = projectDeploymentPromoter.sync(
            sourceProjectDeployment, targetProjectDeployment, requestedMappings, suggestedMappings, projectIsNew);

        projectWorkflowMappingReconciler.syncMappings(
            mappingPlan, syncResult.workflowIdMapping(),
            projectDeploymentWorkflowId -> serverProjectStore.createMapping(
                serverProjectId, projectDeploymentWorkflowId),
            serverProjectStore::updateMappingParameters);

        return syncResult.unresolvedConnectionIds();
    }

    public List<PromotionChange> predictProjectChanges(
        List<SourceProjectView> sourceProjects, Map<Long, Deque<TargetProject>> targetProjectsByProjectId,
        String projectAssetType, LongFunction<String> projectNames, ServerProjectStore serverProjectStore) {

        Map<Long, Deque<TargetProject>> remainingTargetProjectsByProjectId = new LinkedHashMap<>();

        for (Map.Entry<Long, Deque<TargetProject>> entry : targetProjectsByProjectId.entrySet()) {
            remainingTargetProjectsByProjectId.put(entry.getKey(), new ArrayDeque<>(entry.getValue()));
        }

        List<PromotionChange> changes = new ArrayList<>();

        for (SourceProjectView sourceProject : sourceProjects) {
            ProjectDeployment sourceProjectDeployment = sourceProject.projectDeployment();

            long projectId = getProjectId(sourceProjectDeployment);

            String projectName = projectNames.apply(projectId);
            TargetProject targetProject = pollTargetProject(remainingTargetProjectsByProjectId, projectId);

            if (targetProject == null) {
                MappingPlan newProjectMappingPlan = projectWorkflowMappingReconciler.planMappings(
                    sourceProjectDeployment, null, List.of(), sourceProject.parameters());

                changes.add(
                    projectChange(Kind.CREATE, projectAssetType, projectId, sourceProjectDeployment, projectName));
                changes.addAll(
                    keyByDeployment(
                        projectDeploymentPromoter.predictWorkflowChanges(sourceProjectDeployment, null, projectName),
                        sourceProjectDeployment, null));
                changes.addAll(
                    keyByDeployment(
                        projectWorkflowMappingReconciler.predictChanges(newProjectMappingPlan, projectName),
                        sourceProjectDeployment, null));

                continue;
            }

            ProjectDeployment targetProjectDeployment = targetProject.projectDeployment();

            if (targetProjectDeployment.getProjectVersion() != sourceProjectDeployment.getProjectVersion()) {
                changes.add(
                    projectChange(Kind.UPDATE, projectAssetType, projectId, targetProjectDeployment, projectName));
            }

            changes.addAll(
                keyByDeployment(
                    projectDeploymentPromoter.predictWorkflowChanges(
                        sourceProjectDeployment, targetProjectDeployment, projectName),
                    sourceProjectDeployment, targetProjectDeployment));

            MappingPlan mappingPlan = projectWorkflowMappingReconciler.planMappings(
                sourceProjectDeployment, targetProjectDeployment, serverProjectStore.mappings(targetProject.id()),
                sourceProject.parameters());

            changes.addAll(
                keyByDeployment(
                    projectWorkflowMappingReconciler.predictChanges(mappingPlan, projectName), sourceProjectDeployment,
                    targetProjectDeployment));
        }

        for (Deque<TargetProject> staleTargetProjects : remainingTargetProjectsByProjectId.values()) {
            for (TargetProject staleTargetProject : staleTargetProjects) {
                ProjectDeployment staleProjectDeployment = staleTargetProject.projectDeployment();

                long projectId = getProjectId(staleProjectDeployment);

                changes.add(
                    projectChange(
                        Kind.DELETE, projectAssetType, projectId, staleProjectDeployment,
                        projectNames.apply(projectId)));
            }
        }

        return changes;
    }

    private static List<PromotionChange> keyByDeployment(
        List<PromotionChange> changes, ProjectDeployment sourceProjectDeployment,
        @Nullable ProjectDeployment targetProjectDeployment) {

        List<PromotionChange> keyedChanges = new ArrayList<>();

        for (PromotionChange change : changes) {
            ProjectDeployment keyProjectDeployment =
                change.kind() == Kind.CREATE || targetProjectDeployment == null
                    ? sourceProjectDeployment : targetProjectDeployment;

            keyedChanges.add(
                new PromotionChange(
                    change.kind(), change.assetType(), keyProjectDeployment.getUuid() + ":" + change.key(),
                    change.parentName(), change.name(), change.destructive()));
        }

        return keyedChanges;
    }

    private static PromotionChange projectChange(
        Kind kind, String projectAssetType, long projectId, ProjectDeployment projectDeployment, String projectName) {

        return new PromotionChange(
            kind, projectAssetType, projectId + ":" + projectDeployment.getUuid(), null, projectName,
            kind == Kind.DELETE);
    }

    public static Map<Long, Deque<TargetProject>> groupByProjectId(List<TargetProject> targetProjects) {
        Map<Long, Deque<TargetProject>> targetProjectsByProjectId = new LinkedHashMap<>();

        for (TargetProject targetProject : targetProjects) {
            Deque<TargetProject> deque = targetProjectsByProjectId.computeIfAbsent(
                getProjectId(targetProject.projectDeployment()), id -> new ArrayDeque<>());

            deque.addLast(targetProject);
        }

        return targetProjectsByProjectId;
    }

    public static long getProjectId(ProjectDeployment projectDeployment) {
        return Objects.requireNonNull(projectDeployment.getProjectId(), "projectId");
    }

    @Nullable
    public static TargetProject pollTargetProject(
        Map<Long, Deque<TargetProject>> targetProjectsByProjectId, long projectId) {

        Deque<TargetProject> targetProjects = targetProjectsByProjectId.get(projectId);

        return targetProjects == null ? null : targetProjects.pollFirst();
    }

    public static void putAllIfAbsent(Map<Long, Long> target, Map<Long, Long> source) {
        for (Map.Entry<Long, Long> entry : source.entrySet()) {
            target.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    public static void addAmbiguousSourceProjectWarnings(
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

    public static void addAmbiguousTargetProjectWarnings(
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
