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
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("mcpServerPromotionHandler")
@ConditionalOnEEVersion
public class McpServerPromotionHandler implements EnvironmentPromotionHandler {

    private static final String SERVER_KIND = "MCP server";

    private record ComponentKey(String componentName, int componentVersion) {
    }

    private record SourceProject(
        ProjectDeployment projectDeployment, List<McpProjectWorkflow> mcpProjectWorkflows) {
    }

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

                Map<String, Boolean> previousEnabledByName =
                    enabledByName(mcpToolService.getMcpComponentMcpTools(targetMcpComponentId));

                McpComponent updatedMcpComponent = new McpComponent(
                    sourceMcpComponent.getComponentName(), sourceMcpComponent.getComponentVersion(), targetId,
                    targetConnectionId, targetMcpComponent.getVersion());

                updatedMcpComponent.setId(targetMcpComponentId);

                applyToolEnabled(
                    mcpServerFacade.update(updatedMcpComponent, null, toMcpTools(sourceMcpTools)),
                    previousEnabledByName);
            }
        }

        for (McpComponent staleMcpComponent : targetMcpComponentsByKey.values()) {
            mcpServerFacade.deleteMcpComponent(Objects.requireNonNull(staleMcpComponent.getId(), "id"));
        }

        return unresolvedConnectionIds;
    }

    private void applyToolEnabled(McpComponent mcpComponent, Map<String, Boolean> enabledByName) {
        List<McpTool> mcpTools = mcpToolService.getMcpComponentMcpTools(
            Objects.requireNonNull(mcpComponent.getId(), "id"));

        for (McpTool mcpTool : mcpTools) {
            mcpToolService.updateEnabled(
                Objects.requireNonNull(mcpTool.getId(), "id"), enabledByName.getOrDefault(mcpTool.getName(), true));
        }
    }

    private McpServer createTargetServer(McpServer source, long workspaceId, Environment targetEnvironment) {
        if (mcpServerService.existsByNameAndEnvironment(source.getName(), targetEnvironment, source.getUuid())) {
            throw new ConfigurationException(
                "An MCP server named '%s' already exists in %s".formatted(source.getName(), targetEnvironment),
                EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT);
        }

        McpServer target = workspaceMcpServerFacade.createWorkspaceMcpServer(
            source.getName(), PlatformType.AUTOMATION, targetEnvironment, false, source.isAuthenticationRequired(),
            source.isEnforceToolAuthorization(), workspaceId, source.getUuid());

        Map<McpServer, List<Tag>> tagsByMcpServer = mcpServerFacade.getMcpServerTags(List.of(source));

        List<Tag> tags = tagsByMcpServer.getOrDefault(source, List.of());

        if (!tags.isEmpty()) {
            mcpServerFacade.updateMcpServerTags(Objects.requireNonNull(target.getId(), "id"), tags);
        }

        return target;
    }

    private Optional<McpServer> findTarget(McpServer source, long workspaceId, Environment targetEnvironment) {
        Optional<McpServer> candidate = mcpServerService.fetchMcpServer(
            Objects.requireNonNull(source.getUuid(), "uuid"), targetEnvironment);

        if (candidate.isEmpty()) {
            return candidate;
        }

        McpServer candidateMcpServer = candidate.get();

        Optional<Long> candidateWorkspaceId =
            workspaceMcpServerService.fetchMcpServerWorkspaceId(candidateMcpServer.getId());

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
        return workspaceMcpServerService.fetchMcpServerWorkspaceId(mcpServerId)
            .orElseThrow(
                () -> new ConfigurationException(
                    "MCP server id=%s is not assigned to a workspace".formatted(mcpServerId),
                    EnvironmentPromotionErrorType.SOURCE_NOT_FOUND));
    }

    private SourceProjectView sourceProjectView(SourceProject sourceProject) {
        return new SourceProjectView(
            sourceProject.projectDeployment(), sourceParameters(sourceProject), () -> workflowIds(sourceProject));
    }

    private ServerProjectStore serverProjectStore() {
        return new ServerProjectStore() {

            @Override
            public TargetProject createTargetProject(
                long targetServerId, long projectId, int projectVersion, List<String> workflowIds) {

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

    private List<McpProject> sortedMcpProjects(long mcpServerId) {
        List<McpProject> mcpProjects = new ArrayList<>(mcpProjectService.getMcpServerMcpProjects(mcpServerId));

        mcpProjects.sort(Comparator.comparing(McpProject::getId));

        return mcpProjects;
    }

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
