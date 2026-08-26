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
 * Re-binds a target-environment {@link ProjectDeployment} onto the project version its source-environment counterpart
 * runs, for {@link com.bytechef.ee.automation.promotion.handler.EnvironmentPromotionHandler} implementations.
 *
 * <p>
 * Cross-VERSION re-binding and cross-ENVIRONMENT re-binding are the same operation, so this class deliberately reuses
 * the machinery behind "Change Project Version" rather than copying deployment rows:
 * {@link ProjectDeploymentFacade#updateProjectDeployment(ProjectDeployment, List, List)} matches each supplied
 * {@link ProjectDeploymentWorkflow} against the target's existing rows by {@link ProjectWorkflow} lineage uuid and
 * updates them IN PLACE. That is what keeps promotion non-destructive: {@code api_collection_endpoint},
 * {@code mcp_project_workflow} and {@code a2a_project_workflow} all hold foreign keys to
 * {@code project_deployment_workflow} ids, so inserting fresh rows instead of updating the existing ones would leave
 * every mapping row of the target environment dangling.
 * </p>
 *
 * <p>
 * {@code targetIsNew} is what makes "the first promotion adopts the source's configuration, a re-promotion preserves
 * whatever the target environment was tuned to" a single code path: on a brand-new target there is nothing of the
 * target's own to preserve, so inputs, the enabled flag and connection bindings all come from the source; on an
 * existing target each of those three is taken from the target's own row whenever one exists for the same lineage uuid.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ProjectDeploymentPromoter {

    /**
     * One connection binding of a source deployment, identified by the coordinates that survive a version change: the
     * workflow's lineage uuid rather than its per-version workflow id.
     *
     * @param workflowUuid  the lineage uuid of the workflow the binding belongs to, or {@code null} when the source
     *                      deployment row points at a workflow that is absent from its own project version — the same
     *                      stale-row condition {@link #sync} reports as a warning. Consumers building a promotion
     *                      preview must not assume this is non-null.
     * @param workflowLabel the workflow's human-readable label, for the promotion dialog
     * @param nodeName      the workflow node the connection is wired to
     * @param key           the node's connection key
     * @param connectionId  the source-environment connection bound to that node and key
     */
    public record SourceBinding(
        @Nullable String workflowUuid, String workflowLabel, String nodeName, String key, long connectionId) {
    }

    /**
     * The outcome of a {@link #sync(ProjectDeployment, ProjectDeployment, Map, Map, boolean)}.
     *
     * @param workflowIdMapping       source {@code project_deployment_workflow} id to the target's counterpart id, for
     *                                callers re-pointing their own mapping rows. A source row whose target counterpart
     *                                could not be resolved is OMITTED from the map rather than mapped to a {@code null}
     *                                value, so the map never holds a null-valued key and a {@code get(sourceId)}
     *                                returning {@code null} always means "no counterpart" — never "counterpart is
     *                                null".
     * @param unresolvedConnectionIds source connection ids left unbound because no requested, existing or suggested
     *                                target-environment counterpart was available
     * @param warnings                human-readable warnings, e.g. "workflow &lt;label&gt; no longer exists in version
     *                                N" for source rows the sync dropped
     */
    public record SyncResult(
        Map<Long, Long> workflowIdMapping, List<Long> unresolvedConnectionIds, List<String> warnings) {

        public SyncResult {
            workflowIdMapping = Map.copyOf(workflowIdMapping);
            unresolvedConnectionIds = List.copyOf(unresolvedConnectionIds);
            warnings = List.copyOf(warnings);
        }
    }

    /**
     * The {@code (workflowUuid, nodeName, key)} triple that identifies one connection binding stably across both a
     * version change and an environment change — the same coordinates {@link SourceBinding} carries.
     *
     * <p>
     * <b>Internal by design: this must never appear in a public signature.</b> {@link SourceBinding},
     * {@link SyncResult} and the four public methods are a contract later tasks are dispatched against.
     * </p>
     *
     * <p>
     * It is a record rather than a joined string because {@code workflowNodeName} and {@code workflowConnectionKey} are
     * unconstrained user-facing text — the only node-name validation in the platform is
     * {@code WorkflowValidatorFacade.validateNoReservedNodeNames}, which rejects a {@code __} prefix and nothing else —
     * so any delimiter could occur inside a component and make two distinct bindings collide on one key. Since
     * {@link #sync} resolves connections through this key, such a collision would silently bind a node to another
     * node's connection. A record's generated {@code equals}/{@code hashCode} compare component-wise, so the collision
     * class does not exist rather than being made unlikely.
     * </p>
     *
     * @param workflowUuid the workflow's lineage uuid, {@code null} for a source row pointing at a workflow absent from
     *                     its own project version — records handle a null component correctly, and such a key simply
     *                     matches nothing on the target side
     */
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

    /**
     * Rejects a project version that cannot back a deployment in any environment. The two rejected states mirror the
     * ones {@link ProjectDeploymentFacade#createProjectDeployment} already enforces, and reuse its error keys so a
     * promotion fails the same way — and reads the same way to a client — as a hand-made deployment would.
     *
     * @throws ConfigurationException if the project has never been published, or {@code projectVersion} is the
     *                                project's current DRAFT version
     */
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

    /**
     * @return every connection binding of {@code source}, keyed by lineage uuid rather than workflow id so the caller
     *         can correlate them against a target environment running a different project version
     */
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

    /**
     * Correlates the two deployments' connection bindings on the (lineage uuid, node name, connection key) triple — the
     * coordinates that are stable across both a version change and an environment change. What the target environment
     * already resolved a binding to outranks any suggestion the caller computed for it, so a re-promotion never
     * silently re-points a connection an operator deliberately wired.
     *
     * <p>
     * <b>This is a source-connection-level SUMMARY, for the promotion preview — it is deliberately NOT what
     * {@link #sync} resolves with.</b> The preview dialog lists one row per source connection, so collapsing every node
     * that binds a given source connection into a single suggested target is exactly the shape it needs. But the
     * collapse is lossy: if one source connection is bound at two nodes and the target wired those two nodes to
     * <em>different</em> connections, only one of them survives here (first wins, and which one is unspecified because
     * the underlying connection collection is unordered). {@code sync} therefore does its own per-node lookup on the
     * full triple rather than consulting this map — see the note on
     * {@link #sync(ProjectDeployment, ProjectDeployment, Map, Map, boolean)}. Do not "simplify" one into the other.
     * </p>
     *
     * @return source connection id to target connection id, for every source binding whose triple is also bound in
     *         {@code target}; where one source connection is bound at several nodes with differing target connections,
     *         one of them is reported and the rest are dropped
     */
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

    /**
     * Moves {@code target} onto {@code source}'s project version and re-binds its workflows, in place, through
     * {@link ProjectDeploymentFacade#updateProjectDeployment(ProjectDeployment, List, List)}.
     *
     * <p>
     * {@code target} is passed to the facade unchanged except for its project version, because
     * {@code ProjectDeploymentService#update} copies name, description, enabled, project version, tag ids and the
     * optimistic-locking version straight off the object it is given — so anything not carried over from the loaded
     * target would be silently blanked in the target environment.
     * </p>
     *
     * <p>
     * Connection resolution runs <b>per NODE</b>, not per source connection, and is a strict precedence chain: an
     * explicitly requested mapping for the source connection first, then whatever the target environment already had
     * bound <em>at that exact node</em>, then a suggested mapping for the source connection. A node none of the three
     * resolves has its source connection reported in {@link SyncResult#unresolvedConnectionIds()} and is left unbound
     * rather than promoted pointing at a connection of the wrong environment.
     * </p>
     *
     * <p>
     * The middle step is keyed on the same {@code (workflowUuid, nodeName, key)} triple {@link SourceBinding} carries.
     * Keying it on the source connection id alone — as {@link #existingTargetBindings} does for the preview — would
     * mean that a source connection bound at two nodes, whose target wired those nodes to two different connections,
     * has one of its nodes silently re-pointed at the other's connection, nondeterministically.
     * </p>
     *
     * <p>
     * <b>Precondition:</b> {@code target} must be the deployment entity as LOADED from the store, not a detached or
     * hand-built one. It is mutated in place (its project version is set to the source's) and then handed to the
     * facade, and {@code ProjectDeploymentService#update} copies name, description, enabled, tag ids and the
     * optimistic-locking version straight off it — so a hand-built stand-in blanks those columns in the target
     * environment and fails the optimistic-lock check.
     * </p>
     *
     * <p>
     * <b>Note for handler tests that mock {@link ProjectDeploymentFacade}:</b> {@link SyncResult#workflowIdMapping()}
     * is computed by RE-READING the target's {@code project_deployment_workflow} rows AFTER the facade call, because
     * only then do they carry the source version's workflow ids. Against the real facade that holds —
     * {@code checkProjectDeploymentWorkflows} re-points each row in place. Against a mocked facade it does not: the
     * mock performs no update, so the post-sync read keeps returning the pre-sync rows, their old-version workflow ids
     * resolve to no lineage uuid, and the mapping comes back EMPTY.
     * </p>
     *
     * <p>
     * Stub accordingly. This method reads {@code getProjectDeploymentWorkflows(targetId)} <b>exactly twice, on both the
     * create and the update path</b>, in this order:
     * </p>
     *
     * <ol>
     * <li><b>Read 1, before the facade call</b> — builds the lineage-uuid index of the target's existing rows AND the
     * per-node binding map the resolution chain consults. Must return the target's PRE-sync rows. On the update path
     * this read is what preserves the target's own inputs, enabled flag and connection bindings, so returning post-sync
     * rows here corrupts those three, not just the mapping.</li>
     * <li><b>Read 2, after the facade call</b> — resolves {@code workflowIdMapping}. Must return the target's POST-sync
     * rows, i.e. carrying the source version's workflow ids.</li>
     * </ol>
     *
     * <p>
     * So: two consecutive stub values, pre-sync then post-sync, on both paths. {@code getProjectDeploymentWorkflows}
     * for the SOURCE deployment is read exactly once, on both paths. (An earlier revision of this note said the update
     * path also read the target inside {@code existingTargetBindings}; that call was removed when resolution moved
     * per-node, and the counts above are the current ones.)
     * </p>
     *
     * @param requestedMappings source connection id to target connection id, as explicitly chosen by the caller
     * @param suggestedMappings source connection id to target connection id, as guessed for the caller
     * @param targetIsNew       {@code true} when {@code target} was just created for this promotion and therefore has
     *                          no configuration of its own worth preserving
     */
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

        // A brand-new target has no bindings of its own to preserve, so the per-node step is skipped wholesale
        // rather than consulted and found empty.
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

        // Every component is passed through raw: SyncResult's compact constructor is the single defensive-copy site.
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

    /**
     * Requested mapping (by source connection) → the target's existing binding AT THIS NODE (by {@link NodeBindingKey})
     * → suggested mapping (by source connection). Only the middle step is node-scoped, because only it reads state the
     * target environment owns per node; the other two are the caller's opinion about a connection as a whole.
     */
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

    /**
     * Re-reads the target's rows AFTER the facade updated them in place, so the returned ids are the ones the mapping
     * rows of {@code api_collection_endpoint}, {@code mcp_project_workflow} and {@code a2a_project_workflow} must be
     * re-pointed at.
     */
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

        // Plain HashMap on purpose: SyncResult copies this with Map.copyOf, which is unordered, so building it as a
        // LinkedHashMap would promise an iteration order the record cannot keep. Consumers look ids up by key.
        Map<Long, Long> workflowIdMapping = new HashMap<>();

        for (ProjectDeploymentWorkflow sourceProjectDeploymentWorkflow : sourceProjectDeploymentWorkflows) {
            Long targetId =
                targetIdsByUuid.get(sourceUuidsByWorkflowId.get(sourceProjectDeploymentWorkflow.getWorkflowId()));

            // A source row with no counterpart is omitted rather than mapped to null: callers use this map to
            // re-point their own rows, and a null target id is not something any of them could act on.
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

    /**
     * Shared by {@link #existingTargetBindings} and {@link #sync} so the preview and the actual resolution can never
     * disagree about what "the same binding" means.
     */
    private static NodeBindingKey nodeBindingKey(
        @Nullable String workflowUuid, ProjectDeploymentWorkflowConnection connection) {

        return new NodeBindingKey(
            workflowUuid, connection.getWorkflowNodeName(), connection.getWorkflowConnectionKey());
    }
}
