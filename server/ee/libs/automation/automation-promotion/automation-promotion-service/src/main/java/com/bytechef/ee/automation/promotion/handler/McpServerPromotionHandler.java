/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.connection.ConnectionEnvironmentMapper;
import com.bytechef.ee.automation.promotion.connection.PromotionConnectionScope;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionProjectPreview;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.handler.ProjectWorkflowMappingReconciler.Mapping;
import com.bytechef.ee.automation.promotion.handler.ServerProjectPromoter.ServerProjectStore;
import com.bytechef.ee.automation.promotion.handler.ServerProjectPromoter.SourceProjectView;
import com.bytechef.ee.automation.promotion.handler.ServerProjectPromoter.TargetProject;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promotes an MCP server from one environment to its counterpart in another, matched by the server's cross-environment
 * lineage {@code uuid} within the same workspace.
 *
 * <p>
 * An MCP server has two independent halves and the promotion reconciles both. Its {@code mcp_project} rows each own a
 * synthetic {@link ProjectDeployment} whose workflows are exposed as tools, so those go through the shared
 * {@link ProjectDeploymentPromoter#sync} exactly as an API collection's single deployment does. Its
 * {@code mcp_component} rows are a flat catalogue of component actions with their own {@code connection_id}, unrelated
 * to any deployment. The union of both halves' connections is what the caller maps across environments — which is why
 * {@link PromotionConnectionScope#checkMappedConnectionsBelongToSource} takes a bare set rather than deriving one.
 * </p>
 *
 * <p>
 * <b>Mapping rows are reconciled around the sync, never through {@code updateMcpProject}.</b>
 * {@code mcp_project_workflow} holds a foreign key to {@code project_deployment_workflow}, and
 * {@code ProjectDeploymentFacade#updateProjectDeployment} deletes deployment-workflow rows absent from the list the
 * sync passes it, so children have to go before their parents can. The order — delete stale mapping rows, sync, create
 * the missing ones from {@link ProjectDeploymentPromoter.SyncResult#workflowIdMapping()}, overwrite parameters — is
 * owned by {@link ProjectWorkflowMappingReconciler}, shared with the A2A handler. See that class for why the
 * selection-level facade method is deliberately not used.
 * </p>
 *
 * <p>
 * What the target environment owns is not overwritten (spec §6.2): on a re-promotion the target keeps its name, tags,
 * enabled flag, authentication settings, secret key, per-tool {@code enabled} flags, and any component connection the
 * caller did not explicitly re-map. A created counterpart is created disabled with a secret key and URL of its own
 * (spec §6.3, §6.6). {@code McpComponentAuthority} rows are never copied — the security posture of a component is
 * environment-local.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("mcpServerPromotionHandler")
@ConditionalOnEEVersion
public class McpServerPromotionHandler implements EnvironmentPromotionHandler {

    private static final String SERVER_KIND = "MCP server";

    /**
     * Identifies a component across environments. A record rather than a joined string because {@code componentName} is
     * free text, so any delimiter could occur inside one and make two distinct components collide on a single key —
     * which here would update one component from another's source and delete the loser.
     */
    private record ComponentKey(String componentName, int componentVersion) {
    }

    /**
     * One {@code mcp_project} of the source server, reduced to what the promotion reads off it. The {@code mcp_project}
     * row itself is deliberately absent: nothing of the source's own identity crosses environments — the target's
     * projects are matched by {@code deployment.projectId}, never by the source project's id.
     */
    private record SourceProject(
        ProjectDeployment projectDeployment, List<McpProjectWorkflow> mcpProjectWorkflows) {
    }

    /**
     * One {@code mcp_project} of the target server. Its mapping rows are deliberately NOT captured here: the
     * reconciliation reads them twice, once either side of the sync, and a snapshot taken before would be stale for the
     * second read.
     */
    private final ConnectionEnvironmentMapper connectionEnvironmentMapper;
    private final ConnectionService connectionService;
    private final McpComponentService mcpComponentService;
    private final McpProjectFacade mcpProjectFacade;
    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final McpServerFacade mcpServerFacade;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;
    private final ProjectDeploymentPromoter projectDeploymentPromoter;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ServerProjectPromoter serverProjectPromoter;
    private final String publicUrl;
    private final WorkspaceMcpServerFacade workspaceMcpServerFacade;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public McpServerPromotionHandler(
        ApplicationProperties applicationProperties, ConnectionEnvironmentMapper connectionEnvironmentMapper,
        ConnectionService connectionService, McpComponentService mcpComponentService, McpProjectFacade mcpProjectFacade,
        McpProjectService mcpProjectService, McpProjectWorkflowService mcpProjectWorkflowService,
        McpServerFacade mcpServerFacade, McpServerService mcpServerService, McpToolService mcpToolService,
        ProjectDeploymentPromoter projectDeploymentPromoter, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ServerProjectPromoter serverProjectPromoter,
        WorkspaceMcpServerFacade workspaceMcpServerFacade, WorkspaceMcpServerService workspaceMcpServerService) {

        this.connectionEnvironmentMapper = connectionEnvironmentMapper;
        this.connectionService = connectionService;
        this.mcpComponentService = mcpComponentService;
        this.mcpProjectFacade = mcpProjectFacade;
        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.mcpServerFacade = mcpServerFacade;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
        this.projectDeploymentPromoter = projectDeploymentPromoter;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.serverProjectPromoter = serverProjectPromoter;
        this.publicUrl = applicationProperties.getPublicUrl();
        this.workspaceMcpServerFacade = workspaceMcpServerFacade;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    public PromotionResourceType getResourceType() {
        return PromotionResourceType.MCP_SERVER;
    }

    @Override
    @PreAuthorize("hasPermission(@promotionAuthorizer.workspaceIdOfMcpServer(#sourceId), 'Workspace', 'MCP_CREATE')")
    @Transactional(readOnly = true)
    public EnvironmentPromotionPreview preview(long sourceId, Environment targetEnvironment) {
        McpServer source = loadSource(sourceId, targetEnvironment);

        long workspaceId = getWorkspaceId(sourceId);

        List<SourceProject> sourceProjects = sourceProjects(source);
        Optional<McpServer> existingMcpServer = findTarget(source, workspaceId, targetEnvironment);
        List<String> warnings = new ArrayList<>();

        ServerProjectPromoter.addAmbiguousSourceProjectWarnings(
            warnings, sourceProjectDeployments(sourceProjects), SERVER_KIND);

        Map<Long, Deque<TargetProject>> targetProjectsByProjectId;

        if (existingMcpServer.isPresent()) {
            targetProjectsByProjectId = targetProjects(existingMcpServer.get());

            ServerProjectPromoter.addAmbiguousTargetProjectWarnings(
                warnings, targetProjectsByProjectId, SERVER_KIND);
        } else {
            targetProjectsByProjectId = new LinkedHashMap<>();

            warnings.add("The promoted server is created disabled; enable it after reviewing its connections.");
            warnings.add("A new server URL and secret key will be generated for the promoted server.");
        }

        List<PromotionProjectPreview> projects = new ArrayList<>();
        List<SourceBinding> sourceBindings = new ArrayList<>();
        Map<Long, Long> existingTargetBindings = new LinkedHashMap<>();

        for (SourceProject sourceProject : sourceProjects) {
            ProjectDeployment sourceProjectDeployment = sourceProject.projectDeployment();

            long projectId = ServerProjectPromoter.getProjectId(sourceProjectDeployment);

            sourceBindings.addAll(projectDeploymentPromoter.collectSourceBindings(sourceProjectDeployment));

            TargetProject targetProject =
                ServerProjectPromoter.pollTargetProject(targetProjectsByProjectId, projectId);

            Integer targetProjectVersion = null;

            if (targetProject != null) {
                ProjectDeployment targetProjectDeployment = targetProject.projectDeployment();

                targetProjectVersion = targetProjectDeployment.getProjectVersion();

                ServerProjectPromoter.putAllIfAbsent(
                    existingTargetBindings,
                    projectDeploymentPromoter.existingTargetBindings(
                        sourceProjectDeployment, targetProjectDeployment));
            }

            Project project = projectService.getProject(projectId);

            projects.add(
                new PromotionProjectPreview(
                    projectId, project.getName(), sourceProjectDeployment.getProjectVersion(), targetProjectVersion));
        }

        List<McpComponent> sourceMcpComponents = mcpComponentService.getMcpServerMcpComponents(sourceId);
        Map<ComponentKey, McpComponent> targetMcpComponentsByKey = existingMcpServer
            .map(targetMcpServer -> mcpComponentsByKey(Objects.requireNonNull(targetMcpServer.getId(), "id")))
            .orElseGet(LinkedHashMap::new);

        Map<Long, List<String>> componentUsages = new LinkedHashMap<>();

        for (McpComponent sourceMcpComponent : sourceMcpComponents) {
            Long sourceConnectionId = sourceMcpComponent.getConnectionId();

            if (sourceConnectionId == null) {
                continue;
            }

            List<String> usages = componentUsages.computeIfAbsent(sourceConnectionId, id -> new ArrayList<>());

            usages.add("component:" + sourceMcpComponent.getComponentName());

            McpComponent targetMcpComponent = targetMcpComponentsByKey.get(componentKey(sourceMcpComponent));

            if (targetMcpComponent != null && targetMcpComponent.getConnectionId() != null) {
                existingTargetBindings.putIfAbsent(sourceConnectionId, targetMcpComponent.getConnectionId());
            }
        }

        Map<Long, Long> suggestedMappings = connectionEnvironmentMapper.suggest(
            workspaceId, sourceConnectionIds(sourceBindings, sourceMcpComponents), targetEnvironment);

        return new EnvironmentPromotionPreview(
            PromotionResourceType.MCP_SERVER, sourceId, source.getEnvironment(), targetEnvironment,
            existingMcpServer.map(McpServer::getId)
                .orElse(null),
            existingMcpServer.map(McpServer::getName)
                .orElse(null),
            projects,
            PromotionPreviews.connectionMappings(
                sourceBindings, existingTargetBindings, suggestedMappings, componentUsages, connectionService),
            warnings);
    }

    @Override
    @PreAuthorize("hasPermission(@promotionAuthorizer.workspaceIdOfMcpServer(#sourceId), 'Workspace', 'MCP_CREATE')")
    @Transactional
    public EnvironmentPromotionResult promote(
        long sourceId, Environment targetEnvironment, Map<Long, Long> connectionMappings) {

        McpServer source = loadSource(sourceId, targetEnvironment);

        long workspaceId = getWorkspaceId(sourceId);

        List<SourceProject> sourceProjects = sourceProjects(source);

        // Before anything is written: an unpublished project, or one whose requested version is still the DRAFT,
        // cannot back a deployment in any environment.
        for (SourceProject sourceProject : sourceProjects) {
            ProjectDeployment sourceProjectDeployment = sourceProject.projectDeployment();

            projectDeploymentPromoter.validatePromotable(
                ServerProjectPromoter.getProjectId(sourceProjectDeployment),
                sourceProjectDeployment.getProjectVersion());
        }

        List<SourceBinding> sourceBindings = new ArrayList<>();

        for (SourceProject sourceProject : sourceProjects) {
            sourceBindings.addAll(projectDeploymentPromoter.collectSourceBindings(sourceProject.projectDeployment()));
        }

        List<McpComponent> sourceMcpComponents = mcpComponentService.getMcpServerMcpComponents(sourceId);
        Set<Long> sourceConnectionIds = sourceConnectionIds(sourceBindings, sourceMcpComponents);

        // Source-side scoping, before the mapper's target-side validation — see PromotionConnectionScope. The set
        // spans BOTH halves of the server, so a component-only connection is mappable and a foreign one is not.
        PromotionConnectionScope.checkMappedConnectionsBelongToSource(sourceConnectionIds, connectionMappings);

        Map<Long, Long> requestedMappings = connectionEnvironmentMapper.validate(
            workspaceId, targetEnvironment, connectionMappings);
        Map<Long, Long> suggestedMappings = connectionEnvironmentMapper.suggest(
            workspaceId, sourceConnectionIds, targetEnvironment);

        Optional<McpServer> existingMcpServer = findTarget(source, workspaceId, targetEnvironment);
        boolean created = existingMcpServer.isEmpty();
        McpServer target =
            existingMcpServer.orElseGet(() -> createTargetServer(source, workspaceId, targetEnvironment));

        long targetId = Objects.requireNonNull(target.getId(), "id");

        Map<Long, Deque<TargetProject>> targetProjectsByProjectId =
            created ? new LinkedHashMap<>() : targetProjects(target);
        Set<Long> unresolvedConnectionIds = new LinkedHashSet<>();

        for (SourceProject sourceProject : sourceProjects) {
            unresolvedConnectionIds.addAll(
                serverProjectPromoter.promoteProject(
                    sourceProjectView(sourceProject), targetId, targetProjectsByProjectId, requestedMappings,
                    suggestedMappings, serverProjectStore()));
        }

        // Whatever is left in the deques is a target project the source no longer exposes. The facade's own cascade
        // drops its deployment, deployment workflows and mapping rows.
        for (Deque<TargetProject> staleTargetProjects : targetProjectsByProjectId.values()) {
            for (TargetProject staleTargetProject : staleTargetProjects) {
                mcpProjectFacade.deleteMcpProject(staleTargetProject.id());
            }
        }

        unresolvedConnectionIds.addAll(
            reconcileComponents(sourceMcpComponents, targetId, created, requestedMappings, suggestedMappings));

        String targetUrl = publicUrl + "/api/automation/" + mcpServerService.getMcpServerSecretKey(targetId) + "/mcp";

        return new EnvironmentPromotionResult(targetId, created, targetUrl, List.copyOf(unresolvedConnectionIds));
    }

    private Set<Long> reconcileComponents(
        List<McpComponent> sourceMcpComponents, long targetId, boolean created, Map<Long, Long> requestedMappings,
        Map<Long, Long> suggestedMappings) {

        Map<ComponentKey, McpComponent> targetMcpComponentsByKey =
            created ? new LinkedHashMap<>() : mcpComponentsByKey(targetId);
        Set<Long> unresolvedConnectionIds = new LinkedHashSet<>();

        for (McpComponent sourceMcpComponent : sourceMcpComponents) {
            McpComponent targetMcpComponent = targetMcpComponentsByKey.remove(componentKey(sourceMcpComponent));

            Long sourceConnectionId = sourceMcpComponent.getConnectionId();
            Long targetConnectionId = resolveComponentConnectionId(
                sourceConnectionId, targetMcpComponent, requestedMappings, suggestedMappings);

            if (sourceConnectionId != null && targetConnectionId == null) {
                unresolvedConnectionIds.add(sourceConnectionId);
            }

            List<McpTool> sourceMcpTools = mcpToolService.getMcpComponentMcpTools(
                Objects.requireNonNull(sourceMcpComponent.getId(), "id"));

            if (targetMcpComponent == null) {
                McpComponent createdMcpComponent = mcpServerFacade.create(
                    new McpComponent(
                        sourceMcpComponent.getComponentName(), sourceMcpComponent.getComponentVersion(), targetId,
                        targetConnectionId),
                    toMcpTools(sourceMcpTools));

                applyToolEnabled(createdMcpComponent, enabledByName(sourceMcpTools));
            } else {
                long targetMcpComponentId = Objects.requireNonNull(targetMcpComponent.getId(), "id");

                // Read the target's tools BEFORE the update: McpServerFacade#update deletes and recreates every tool
                // of the component, and a recreated tool is always enabled, so the flags have to be captured first.
                Map<String, Boolean> previousEnabledByName =
                    enabledByName(mcpToolService.getMcpComponentMcpTools(targetMcpComponentId));

                McpComponent updatedMcpComponent = new McpComponent(
                    sourceMcpComponent.getComponentName(), sourceMcpComponent.getComponentVersion(), targetId,
                    targetConnectionId, targetMcpComponent.getVersion());

                updatedMcpComponent.setId(targetMcpComponentId);

                applyToolEnabled(
                    mcpServerFacade.update(updatedMcpComponent, toMcpTools(sourceMcpTools)), previousEnabledByName);
            }
        }

        for (McpComponent staleMcpComponent : targetMcpComponentsByKey.values()) {
            mcpServerFacade.deleteMcpComponent(Objects.requireNonNull(staleMcpComponent.getId(), "id"));
        }

        return unresolvedConnectionIds;
    }

    /**
     * Re-applies {@code enabled} on the tools of a component that {@link McpServerFacade} has just written, since both
     * its create and its update path recreate every tool row enabled.
     */
    private void applyToolEnabled(McpComponent mcpComponent, Map<String, Boolean> enabledByName) {
        List<McpTool> mcpTools = mcpToolService.getMcpComponentMcpTools(
            Objects.requireNonNull(mcpComponent.getId(), "id"));

        for (McpTool mcpTool : mcpTools) {
            mcpToolService.updateEnabled(
                Objects.requireNonNull(mcpTool.getId(), "id"), enabledByName.getOrDefault(mcpTool.getName(), true));
        }
    }

    /**
     * Creates the target counterpart, refusing first if a DIFFERENT lineage already holds the source's name in the
     * target environment.
     *
     * <p>
     * The check excludes the source's own lineage uuid, so a server that is our own counterpart never reads as a
     * conflict. It is scoped exactly to the {@code (name, environment)} unique constraint on {@code mcp_server}, which
     * spans every workspace and both platform types — see
     * {@link McpServerService#existsByNameAndEnvironment(String, Environment, java.util.UUID)}. That is wider than the
     * caller's own workspace on purpose: a workspace-scoped check would pass and the insert would then fail on the
     * constraint anyway, which is the 500 this exists to prevent.
     * </p>
     *
     * <p>
     * <b>One collision remains outside its scope.</b> A same-uuid counterpart living in ANOTHER workspace is
     * deliberately not adopted by {@link #findTarget}, so promotion takes this create path and the insert collides on
     * {@code uk_mcp_server_uuid_environment} rather than on the name. That is a lineage collision, not a name one, and
     * is not something this check can or should report.
     * </p>
     */
    private McpServer createTargetServer(McpServer source, long workspaceId, Environment targetEnvironment) {
        if (mcpServerService.existsByNameAndEnvironment(source.getName(), targetEnvironment, source.getUuid())) {
            throw new ConfigurationException(
                "An MCP server named '%s' already exists in %s".formatted(source.getName(), targetEnvironment),
                EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT);
        }

        // enabled = false: a created counterpart may still have unresolved connections, so an operator reviews it
        // before traffic flows (spec §6.3). The facade mints a secret key of its own (spec §6.6).
        McpServer target = workspaceMcpServerFacade.createWorkspaceMcpServer(
            source.getName(), PlatformType.AUTOMATION, targetEnvironment, false, source.isAuthenticationRequired(),
            source.isEnforceToolAuthorization(), workspaceId, source.getUuid());

        Map<McpServer, List<Tag>> tagsByMcpServer = mcpServerFacade.getMcpServerTags(List.of(source));

        List<Tag> tags = tagsByMcpServer.getOrDefault(source, List.of());

        // Tags are copied on create only; on a re-promotion the target keeps whatever the target environment tagged
        // it with, the same way its name and description are left alone.
        if (!tags.isEmpty()) {
            mcpServerFacade.updateMcpServerTags(Objects.requireNonNull(target.getId(), "id"), tags);
        }

        return target;
    }

    /**
     * The target counterpart is the same-uuid server in {@code targetEnvironment} AND in the same workspace. A
     * same-uuid server in another workspace is not this promotion's target: writing into it would cross a workspace
     * boundary the caller was never authorized against, so it is treated as absent and a counterpart is created.
     */
    private Optional<McpServer> findTarget(McpServer source, long workspaceId, Environment targetEnvironment) {
        Optional<McpServer> candidate = mcpServerService.fetchMcpServer(
            Objects.requireNonNull(source.getUuid(), "uuid"), targetEnvironment);

        if (candidate.isEmpty()) {
            return candidate;
        }

        McpServer candidateMcpServer = candidate.get();

        Optional<Long> candidateWorkspaceId =
            workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(candidateMcpServer.getId());

        return candidateWorkspaceId.filter(id -> Objects.equals(id, workspaceId))
            .isPresent() ? candidate : Optional.empty();
    }

    private McpServer loadSource(long sourceId, Environment targetEnvironment) {
        McpServer source = mcpServerService.getMcpServer(sourceId);

        if (source.getEnvironment() == targetEnvironment) {
            throw new ConfigurationException(
                "MCP server id=%s is already in %s".formatted(sourceId, targetEnvironment),
                EnvironmentPromotionErrorType.SAME_ENVIRONMENT);
        }

        return source;
    }

    private long getWorkspaceId(long mcpServerId) {
        return workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(mcpServerId)
            .orElseThrow(
                () -> new ConfigurationException(
                    "MCP server id=%s is not assigned to a workspace".formatted(mcpServerId),
                    EnvironmentPromotionErrorType.SOURCE_NOT_FOUND));
    }

    /**
     * Adapts one source project to the shared promoter's view of it. {@code workflowIds} stays a supplier: only the
     * create path needs them, and resolving them costs a deployment-workflow read.
     */
    private SourceProjectView sourceProjectView(SourceProject sourceProject) {
        return new SourceProjectView(
            sourceProject.projectDeployment(), sourceParameters(sourceProject), () -> workflowIds(sourceProject));
    }

    /**
     * The MCP-specific writes the shared promoter drives: minting a counterpart {@code mcp_project} through the facade,
     * and reading and writing its {@code mcp_project_workflow} rows.
     */
    private ServerProjectStore serverProjectStore() {
        return new ServerProjectStore() {

            @Override
            public TargetProject createTargetProject(
                long targetServerId, long projectId, int projectVersion, List<String> workflowIds) {

                // The facade mints the target deployment in the target server's environment, its deployment-workflow
                // rows and the matching mapping rows; the sync then moves connections and inputs onto them.
                McpProject createdMcpProject =
                    mcpProjectFacade.createMcpProject(targetServerId, projectId, projectVersion, workflowIds);

                return new TargetProject(
                    Objects.requireNonNull(createdMcpProject.getId(), "id"),
                    getProjectDeployment(createdMcpProject));
            }

            @Override
            public List<Mapping> mappings(long serverProjectId) {
                return McpServerPromotionHandler.this.mappings(serverProjectId);
            }

            @Override
            public long createMapping(long serverProjectId, long projectDeploymentWorkflowId) {
                return McpServerPromotionHandler.this.createMapping(serverProjectId, projectDeploymentWorkflowId);
            }

            @Override
            public void deleteMapping(long mappingId) {
                mcpProjectWorkflowService.delete(mappingId);
            }

            @Override
            public void updateMappingParameters(long mappingId, Map<String, ?> parameters) {
                mcpProjectWorkflowService.updateParameters(mappingId, parameters);
            }
        };
    }

    private static List<ProjectDeployment> sourceProjectDeployments(List<SourceProject> sourceProjects) {
        List<ProjectDeployment> sourceProjectDeployments = new ArrayList<>();

        for (SourceProject sourceProject : sourceProjects) {
            sourceProjectDeployments.add(sourceProject.projectDeployment());
        }

        return sourceProjectDeployments;
    }

    private Map<Long, Deque<TargetProject>> targetProjects(McpServer target) {
        List<TargetProject> targetProjects = new ArrayList<>();

        for (McpProject mcpProject : sortedMcpProjects(Objects.requireNonNull(target.getId(), "id"))) {
            targetProjects.add(
                new TargetProject(
                    Objects.requireNonNull(mcpProject.getId(), "id"), getProjectDeployment(mcpProject)));
        }

        return ServerProjectPromoter.groupByProjectId(targetProjects);
    }

    private List<SourceProject> sourceProjects(McpServer source) {
        List<SourceProject> sourceProjects = new ArrayList<>();

        for (McpProject mcpProject : sortedMcpProjects(Objects.requireNonNull(source.getId(), "id"))) {
            long mcpProjectId = Objects.requireNonNull(mcpProject.getId(), "id");

            sourceProjects.add(
                new SourceProject(
                    getProjectDeployment(mcpProject),
                    mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProjectId)));
        }

        return sourceProjects;
    }

    /**
     * Groups the target's projects by {@code deployment.projectId} in ascending {@code mcp_project} id order (spec
     * §6.5). A deque rather than a single value: the schema permits several projects for one {@code projectId}, and
     * pairing the i-th source with the i-th target is what keeps a re-promotion from creating a fresh duplicate on
     * every run while never deleting the one it skipped.
     */
    private List<McpProject> sortedMcpProjects(long mcpServerId) {
        List<McpProject> mcpProjects = new ArrayList<>(mcpProjectService.getMcpServerMcpProjects(mcpServerId));

        mcpProjects.sort(Comparator.comparing(McpProject::getId));

        return mcpProjects;
    }

    /**
     * The workflow ids of the source's selection, for the create path: {@code createMcpProject} takes per-version
     * workflow ids, and the target project is minted at the source's own project version, so the source's ids are the
     * right ones to hand it.
     */
    private List<String> workflowIds(SourceProject sourceProject) {
        Map<Long, String> workflowIdsByProjectDeploymentWorkflowId = new LinkedHashMap<>();

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
                Objects.requireNonNull(sourceProject.projectDeployment()
                    .getId(), "id"));

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            workflowIdsByProjectDeploymentWorkflowId.put(
                projectDeploymentWorkflow.getId(), projectDeploymentWorkflow.getWorkflowId());
        }

        List<String> workflowIds = new ArrayList<>();

        for (McpProjectWorkflow mcpProjectWorkflow : sourceProject.mcpProjectWorkflows()) {
            String workflowId =
                workflowIdsByProjectDeploymentWorkflowId.get(mcpProjectWorkflow.getProjectDeploymentWorkflowId());

            if (workflowId != null && !workflowIds.contains(workflowId)) {
                workflowIds.add(workflowId);
            }
        }

        return workflowIds;
    }

    private long createMapping(long mcpProjectId, long projectDeploymentWorkflowId) {
        McpProjectWorkflow mcpProjectWorkflow =
            mcpProjectWorkflowService.create(mcpProjectId, projectDeploymentWorkflowId);

        return Objects.requireNonNull(mcpProjectWorkflow.getId(), "id");
    }

    private List<Mapping> mappings(long mcpProjectId) {
        List<Mapping> mappings = new ArrayList<>();

        for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflowService
            .getMcpProjectMcpProjectWorkflows(mcpProjectId)) {

            Long projectDeploymentWorkflowId = mcpProjectWorkflow.getProjectDeploymentWorkflowId();

            if (projectDeploymentWorkflowId != null) {
                mappings.add(
                    new Mapping(
                        Objects.requireNonNull(mcpProjectWorkflow.getId(), "id"), projectDeploymentWorkflowId));
            }
        }

        return mappings;
    }

    private Map<ComponentKey, McpComponent> mcpComponentsByKey(long mcpServerId) {
        Map<ComponentKey, McpComponent> mcpComponentsByKey = new LinkedHashMap<>();

        for (McpComponent mcpComponent : mcpComponentService.getMcpServerMcpComponents(mcpServerId)) {
            mcpComponentsByKey.putIfAbsent(componentKey(mcpComponent), mcpComponent);
        }

        return mcpComponentsByKey;
    }

    private ProjectDeployment getProjectDeployment(McpProject mcpProject) {
        return projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(mcpProject.getProjectDeploymentId(), "projectDeploymentId"));
    }

    /**
     * Spec §6.5: several projects for one {@code projectId} are schema-legal but not something the UI can produce, and
     * the pairing that resolves them — ascending {@code mcp_project} id, i-th source to i-th target — is arbitrary
     * enough that the operator is told about it rather than left to discover which entry moved.
     */
    private static ComponentKey componentKey(McpComponent mcpComponent) {
        return new ComponentKey(mcpComponent.getComponentName(), mcpComponent.getComponentVersion());
    }

    private static Map<String, Boolean> enabledByName(List<McpTool> mcpTools) {
        Map<String, Boolean> enabledByName = new LinkedHashMap<>();

        for (McpTool mcpTool : mcpTools) {
            enabledByName.put(mcpTool.getName(), mcpTool.isEnabled());
        }

        return enabledByName;
    }

    /**
     * Requested mapping → the connection the target component already holds → suggested mapping. A source component
     * with no connection of its own leaves the target's untouched, which is spec §6.2's "T {@code connection_id} kept
     * unless the mapping supplies one" — and also why the requested map is never consulted with a null key, which an
     * immutable {@code Map} would reject outright.
     */
    @Nullable
    private static Long resolveComponentConnectionId(
        @Nullable Long sourceConnectionId, @Nullable McpComponent targetMcpComponent, Map<Long, Long> requestedMappings,
        Map<Long, Long> suggestedMappings) {

        if (sourceConnectionId == null) {
            return targetMcpComponent == null ? null : targetMcpComponent.getConnectionId();
        }

        Long targetConnectionId = requestedMappings.get(sourceConnectionId);

        if (targetConnectionId == null && targetMcpComponent != null) {
            targetConnectionId = targetMcpComponent.getConnectionId();
        }

        if (targetConnectionId == null) {
            targetConnectionId = suggestedMappings.get(sourceConnectionId);
        }

        return targetConnectionId;
    }

    /**
     * MCP is the one surface whose connection scope is wider than its deployment's bindings: an MCP server can also
     * bind a connection directly on an {@code mcp_component} row, with no workflow involved. So this unions the shared
     * binding-derived set with those component ids rather than deriving both itself.
     */
    private static Set<Long> sourceConnectionIds(
        List<SourceBinding> sourceBindings, List<McpComponent> sourceMcpComponents) {

        Set<Long> sourceConnectionIds = new LinkedHashSet<>(
            PromotionConnectionScope.sourceConnectionIds(sourceBindings));

        for (McpComponent sourceMcpComponent : sourceMcpComponents) {
            Long connectionId = sourceMcpComponent.getConnectionId();

            if (connectionId != null) {
                sourceConnectionIds.add(connectionId);
            }
        }

        return sourceConnectionIds;
    }

    private static Map<Long, Map<String, ?>> sourceParameters(SourceProject sourceProject) {
        Map<Long, Map<String, ?>> sourceParameters = new LinkedHashMap<>();

        for (McpProjectWorkflow mcpProjectWorkflow : sourceProject.mcpProjectWorkflows()) {
            Long projectDeploymentWorkflowId = mcpProjectWorkflow.getProjectDeploymentWorkflowId();

            if (projectDeploymentWorkflowId != null) {
                sourceParameters.put(projectDeploymentWorkflowId, mcpProjectWorkflow.getParameters());
            }
        }

        return sourceParameters;
    }

    private static List<McpTool> toMcpTools(List<McpTool> sourceMcpTools) {
        List<McpTool> mcpTools = new ArrayList<>();

        for (McpTool sourceMcpTool : sourceMcpTools) {
            mcpTools.add(new McpTool(sourceMcpTool.getName(), sourceMcpTool.getParameters()));
        }

        return mcpTools;
    }
}
