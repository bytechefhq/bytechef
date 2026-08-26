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
 * Reconciles the rows that map a promotable resource's exposed workflows onto {@code project_deployment_workflow} rows,
 * around a {@link com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter#sync} call.
 *
 * <p>
 * {@code mcp_project_workflow} and {@code a2a_project_workflow} have the same shape — a
 * {@code project_deployment_workflow_id} pointer plus a {@code parameters} map — and therefore need the same
 * reconciliation, so both handlers drive this one routine through functional hooks rather than each owning a copy of
 * it. Nothing here names either table; a caller supplies its rows as {@link Mapping}s and its writes as hooks.
 * </p>
 *
 * <p>
 * <b>The order the two methods must be called in is the whole point of this class, and is not interchangeable:</b>
 * </p>
 *
 * <ol>
 * <li>{@link #deleteStaleMappings} — BEFORE the sync. {@code ProjectDeploymentFacade#updateProjectDeployment} DELETES
 * every {@code project_deployment_workflow} row absent from the list it is passed, and a mapping row holds a foreign
 * key to one, so a child pointing at a row the sync is about to remove has to go first or the delete fails.</li>
 * <li>{@link com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter#sync} — re-points the surviving
 * rows in place and reports the source-to-target {@code project_deployment_workflow} id mapping.</li>
 * <li>{@link #syncMappings} — AFTER the sync, creating the mapping rows the target is missing and overwriting their
 * parameters from the source.</li>
 * </ol>
 *
 * <p>
 * <b>Precondition every caller must satisfy: the source deployment's {@code project_deployment_workflow} rows must be
 * exactly the workflows the source resource exposes, one to one.</b> {@link #deleteStaleMappings} decides staleness
 * from the source DEPLOYMENT's rows, not from the source's mapping rows, so an un-exposed deployment-workflow row on
 * the source side reads as "still exposed" and its target counterpart survives — a workflow the operator deselected
 * would keep serving in the target environment. The MCP side satisfies this because
 * {@code McpProjectFacadeImpl#createMcpProject} and {@code #updateMcpProject} create and delete a
 * {@code project_deployment_workflow} row alongside every {@code mcp_project_workflow} row, never one without the
 * other. <b>A new caller must verify the same holds for its own facade before reusing this class</b>; if it does not,
 * staleness has to be computed from the caller's own mapping rows instead.
 * </p>
 *
 * <p>
 * The reconciliation is deliberately NOT delegated to a resource-level "set the selection" facade method
 * ({@code McpProjectFacade#updateMcpProject} and its A2A twin): those reconcile the selection by creating and deleting
 * {@code project_deployment_workflow} rows themselves, blind to the sync's {@code workflowIdMapping}, so running one
 * either side of a sync undoes the pointer fix-ups this class exists to make.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class ProjectWorkflowMappingReconciler {

    /**
     * One mapping row of the target resource, reduced to the two fields reconciliation turns on.
     *
     * @param id                          the mapping row's own id, which the delete and update hooks are called with
     * @param projectDeploymentWorkflowId the {@code project_deployment_workflow} row it points at
     */
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

    /**
     * Deletes every mapping row of the target whose {@code project_deployment_workflow} row the upcoming sync will
     * remove — that is, every one whose workflow lineage uuid the source no longer exposes.
     *
     * <p>
     * Staleness is decided on the lineage uuid rather than the per-version workflow id, because source and target may
     * be running different project versions at this point and the sync itself matches rows by uuid. A target row whose
     * workflow is absent from the target's own project version resolves to no uuid at all and is stale by the same
     * rule: the sync cannot match it either, so it will be deleted.
     * </p>
     *
     * <p>
     * This also covers the create path. A freshly minted target carries one {@code project_deployment_workflow} row per
     * workflow id of the source's selection, and a source row pointing at a workflow that has since left the source's
     * own project version is one the sync skips — so its brand-new counterpart is stale the moment it is created.
     * </p>
     *
     * @param source        the source deployment, as loaded from the store
     * @param target        the target deployment, as loaded from the store and BEFORE the sync moves its version
     * @param mappings      the target resource's current mapping rows
     * @param deleteMapping called with the id of each mapping row to delete
     */
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

    /**
     * Points the target's mapping rows at the {@code project_deployment_workflow} rows the sync produced, creating the
     * ones the target does not have yet, and overwrites their parameters from the source.
     *
     * <p>
     * Iteration is driven by the SOURCE's mapping rows, not by {@code workflowIdMapping}: the latter covers every
     * workflow of the deployment, while only the ones the source actually exposes should get a mapping row in the
     * target. A source mapping whose workflow has no target counterpart — the sync dropped it — is skipped rather than
     * created against a guessed id.
     * </p>
     *
     * @param mappings                the target resource's mapping rows, re-read AFTER the sync
     * @param workflowIdMapping       {@link com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SyncResult#workflowIdMapping()}
     * @param sourceParameters        source {@code project_deployment_workflow} id to the parameters of the source
     *                                mapping row pointing at it; its key set is what defines the target's selection
     * @param createMapping           called with a target {@code project_deployment_workflow} id, returning the id of
     *                                the mapping row it created
     * @param updateMappingParameters called with a mapping row id and the parameters to overwrite it with
     */
    void syncMappings(
        List<Mapping> mappings, Map<Long, Long> workflowIdMapping, Map<Long, Map<String, ?>> sourceParameters,
        LongFunction<Long> createMapping, BiConsumer<Long, Map<String, ?>> updateMappingParameters) {

        Map<Long, Long> mappingIdsByProjectDeploymentWorkflowId = new HashMap<>();

        for (Mapping mapping : mappings) {
            mappingIdsByProjectDeploymentWorkflowId.putIfAbsent(mapping.projectDeploymentWorkflowId(), mapping.id());
        }

        // Sorted so a promotion writes in a stable order whatever the source rows came back in, which keeps the
        // write sequence reproducible for both operators reading an audit trail and tests pinning it.
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

            // Absent rather than mapped to null, so "no uuid" and "no such row" are one case for the caller.
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
