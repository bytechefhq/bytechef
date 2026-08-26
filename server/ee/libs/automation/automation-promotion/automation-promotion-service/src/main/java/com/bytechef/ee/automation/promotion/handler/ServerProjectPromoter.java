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
 * The project-level half of promoting a server that exposes project workflows — everything an MCP server and an A2A
 * server do identically once their own entity types are factored out.
 *
 * <p>
 * The two surfaces have the same shape: a server owns rows ({@code mcp_project} / {@code a2a_project}) that each own a
 * synthetic {@link ProjectDeployment}, and each of those owns mapping rows pointing at
 * {@code project_deployment_workflow}. Promotion pairs source projects with target projects by
 * {@code deployment.projectId}, creates the missing counterparts, and reconciles the mapping rows around a
 * {@link ProjectDeploymentPromoter#sync}. None of that reads anything type-specific.
 * </p>
 *
 * <p>
 * <b>What made this shareable is that a target project's entity is only ever needed for its id.</b> Both handlers used
 * to carry a {@code TargetProject} record holding the whole {@code McpProject} / {@code A2aProject}, but every use of
 * that payload — the create path, the stale-project cleanup — immediately reduced it to {@code getId()}. Capturing the
 * id instead removes the type parameter that a generic superclass would otherwise have needed, and with it the reason
 * to make one.
 * </p>
 *
 * <p>
 * The type-specific remainder is supplied by the caller as a {@link ServerProjectStore}: creating the counterpart
 * project through the surface's own facade, and reading and writing its mapping rows. Handlers keep their own
 * {@code promote} and {@code preview} — those differ genuinely, and moving {@code promote} would relocate the
 * {@code PromotionConnectionScope} call that {@code PromotionHandlerAuthorizationTest} inspects each handler's bytecode
 * for.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class ServerProjectPromoter {

    /**
     * One project of the target server: its own row id, and the {@link ProjectDeployment} that row owns. The entity
     * itself is deliberately absent — see the class javadoc for why capturing the id is what let the two surfaces share
     * this at all.
     */
    record TargetProject(long id, ProjectDeployment projectDeployment) {
    }

    /**
     * One project of the source server, reduced to what the promotion reads off it.
     *
     * @param projectDeployment the source project's synthetic deployment
     * @param parameters        source {@code project_deployment_workflow} id to the parameters of the mapping row
     *                          pointing at it; its key set defines the target's selection
     * @param workflowIds       the workflow ids to hand the create path, resolved lazily because the update path never
     *                          needs them and resolving them costs a deployment-workflow read
     */
    record SourceProjectView(
        ProjectDeployment projectDeployment, Map<Long, Map<String, ?>> parameters,
        Supplier<List<String>> workflowIds) {
    }

    /**
     * The surface-specific writes: minting a target counterpart project, and reading and writing its mapping rows.
     *
     * <p>
     * Every mapping method is keyed by the SERVER PROJECT's id rather than being bound to one project at construction,
     * so a single store instance serves every project of a promotion. The mapping-row id and the server-project id are
     * both {@code long} and easy to transpose; the parameter names here and in
     * {@link ProjectWorkflowMappingReconciler.Mapping} are the only thing distinguishing them.
     * </p>
     */
    interface ServerProjectStore {

        /**
         * Creates the target counterpart through the surface's own facade, which mints the deployment in the target
         * server's environment, its deployment-workflow rows and the matching mapping rows.
         */
        TargetProject createTargetProject(
            long targetServerId, long projectId, int projectVersion, List<String> workflowIds);

        /**
         * @param serverProjectId the {@code mcp_project} / {@code a2a_project} id whose mapping rows to read
         */
        List<Mapping> mappings(long serverProjectId);

        /**
         * @return the id of the mapping row created
         */
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

    /**
     * Promotes one source project onto its target counterpart, creating the counterpart if the pairing has none left.
     *
     * <p>
     * The call order — delete stale mapping rows, sync, create the missing ones from
     * {@link SyncResult#workflowIdMapping()}, overwrite parameters — is {@link ProjectWorkflowMappingReconciler}'s
     * contract and is pinned by {@code InOrder} assertions in both handlers' tests. See that class for why it is not
     * interchangeable.
     * </p>
     *
     * @return the connection ids the sync could not resolve in the target environment
     */
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

    /**
     * Groups the target's projects by {@code deployment.projectId}, preserving the order given (spec §6.5 requires
     * ascending server-project id, which is the caller's sort). A deque rather than a single value: the schema permits
     * several projects for one {@code projectId}, and pairing the i-th source with the i-th target is what keeps a
     * re-promotion from creating a fresh duplicate on every run while never deleting the one it skipped.
     */
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

    /**
     * Spec §6.5: several projects for one {@code projectId} are schema-legal but not something the UI can produce, and
     * the pairing that resolves them — ascending server-project id, i-th source to i-th target — is arbitrary enough
     * that the operator is told about it rather than left to discover which entry moved.
     *
     * @param serverKind how to name the surface in the warning, e.g. {@code "MCP server"}
     */
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
