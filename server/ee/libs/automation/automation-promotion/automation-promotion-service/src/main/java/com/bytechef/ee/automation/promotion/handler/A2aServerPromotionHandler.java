/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.facade.A2aProjectFacade;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promotes an A2A server from one environment to its counterpart in another, matched by the server's cross-environment
 * lineage {@code uuid}.
 *
 * <p>
 * Unlike an MCP server, an A2A server has no component-level connections and no workspace-membership row of its own —
 * {@code a2a_server} is tenant-global, not {@code workspace}-scoped. What it shares with the MCP server is the same
 * project shape: its {@code a2a_project} rows each own a synthetic {@link ProjectDeployment} whose workflows are
 * exposed as A2A skills, so those go through the shared {@link ProjectDeploymentPromoter#sync} exactly as an MCP
 * server's projects do. The workspace {@link ConnectionEnvironmentMapper} needs — purely to decide which
 * target-environment connections are visible — is read off the FIRST source project's own project, since that is the
 * only workspace association an A2A server has access to; a source with no projects has no connections to map and the
 * value is never consulted.
 * </p>
 *
 * <p>
 * <b>Mapping rows are reconciled around the sync, never through {@code updateA2aProject}.</b>
 * {@code a2a_project_workflow} holds a foreign key to {@code project_deployment_workflow}, and
 * {@code ProjectDeploymentFacade#updateProjectDeployment} deletes deployment-workflow rows absent from the list the
 * sync passes it, so children have to go before their parents can. The order — delete stale mapping rows, sync, create
 * the missing ones from {@link ProjectDeploymentPromoter.SyncResult#workflowIdMapping()}, overwrite parameters — is
 * owned by {@link ProjectWorkflowMappingReconciler}, shared with the MCP handler. See that class for why the
 * selection-level facade method is deliberately not used, and for the precondition every caller must satisfy: it holds
 * here because {@code A2aProjectFacadeImpl#createA2aProject} and {@code #updateA2aProject} create and delete a
 * {@code project_deployment_workflow} row alongside every {@code a2a_project_workflow} row, never one without the
 * other.
 * </p>
 *
 * <p>
 * What the target environment owns is not overwritten (spec §6.2): on a re-promotion the target keeps its name,
 * description, enabled flag, authentication setting and secret key, and any per-skill parameters the caller did not
 * re-expose. A created counterpart is created disabled with a secret key and agent-card URL of its own (spec §6.3,
 * §6.6). {@code a2a_server} carries no unique constraint on {@code (name, environment)} — only on
 * {@code (uuid, environment)} and on {@code secret_key} — so unlike the API collection and MCP server handlers, there
 * is no name collision for this handler to guard against before creating a counterpart.
 * </p>
 *
 * <p>
 * Authorization is a flat {@code ROLE_ADMIN} check rather than a resolved project/workspace id, because an A2A server
 * has no owning project or workspace row for {@code PromotionAuthorizer} to resolve.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("a2aServerPromotionHandler")
@ConditionalOnEEVersion
public class A2aServerPromotionHandler implements EnvironmentPromotionHandler {

    private static final String SERVER_KIND = "A2A server";

    /**
     * One {@code a2a_project} of the source server, reduced to what the promotion reads off it. The {@code a2a_project}
     * row itself is deliberately absent: nothing of the source's own identity crosses environments — the target's
     * projects are matched by {@code deployment.projectId}, never by the source project's id.
     */
    private record SourceProject(
        ProjectDeployment projectDeployment, List<A2aProjectWorkflow> a2aProjectWorkflows) {
    }

    private final A2aProjectFacade a2aProjectFacade;
    private final A2aProjectService a2aProjectService;
    private final A2aProjectWorkflowService a2aProjectWorkflowService;
    private final A2aServerService a2aServerService;
    private final ConnectionEnvironmentMapper connectionEnvironmentMapper;
    private final ConnectionService connectionService;
    private final ProjectDeploymentPromoter projectDeploymentPromoter;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ServerProjectPromoter serverProjectPromoter;
    private final String publicUrl;

    @SuppressFBWarnings("EI")
    public A2aServerPromotionHandler(
        A2aProjectFacade a2aProjectFacade, A2aProjectService a2aProjectService,
        A2aProjectWorkflowService a2aProjectWorkflowService, A2aServerService a2aServerService,
        ApplicationProperties applicationProperties, ConnectionEnvironmentMapper connectionEnvironmentMapper,
        ConnectionService connectionService, ProjectDeploymentPromoter projectDeploymentPromoter,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ServerProjectPromoter serverProjectPromoter) {

        this.a2aProjectFacade = a2aProjectFacade;
        this.a2aProjectService = a2aProjectService;
        this.a2aProjectWorkflowService = a2aProjectWorkflowService;
        this.a2aServerService = a2aServerService;
        this.connectionEnvironmentMapper = connectionEnvironmentMapper;
        this.connectionService = connectionService;
        this.projectDeploymentPromoter = projectDeploymentPromoter;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.serverProjectPromoter = serverProjectPromoter;
        this.publicUrl = applicationProperties.getPublicUrl();
    }

    @Override
    public PromotionResourceType getResourceType() {
        return PromotionResourceType.A2A_SERVER;
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public EnvironmentPromotionPreview preview(long sourceId, Environment targetEnvironment) {
        A2aServer source = loadSource(sourceId, targetEnvironment);

        List<SourceProject> sourceProjects = sourceProjects(source);
        long workspaceId = workspaceId(sourceProjects);
        Optional<A2aServer> existingA2aServer = findTarget(source, targetEnvironment);
        List<String> warnings = new ArrayList<>();

        ServerProjectPromoter.addAmbiguousSourceProjectWarnings(
            warnings, sourceProjectDeployments(sourceProjects), SERVER_KIND);

        Map<Long, Deque<TargetProject>> targetProjectsByProjectId;

        if (existingA2aServer.isPresent()) {
            targetProjectsByProjectId = targetProjects(existingA2aServer.get());

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

        Set<Long> sourceConnectionIds = PromotionConnectionScope.sourceConnectionIds(sourceBindings);
        Map<Long, Long> suggestedMappings =
            connectionEnvironmentMapper.suggest(workspaceId, sourceConnectionIds, targetEnvironment);

        return new EnvironmentPromotionPreview(
            PromotionResourceType.A2A_SERVER, sourceId, source.getEnvironment(), targetEnvironment,
            existingA2aServer.map(A2aServer::getId)
                .orElse(null),
            existingA2aServer.map(A2aServer::getName)
                .orElse(null),
            projects,
            PromotionPreviews.connectionMappings(sourceBindings, existingTargetBindings, suggestedMappings,
                connectionService),
            warnings);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public EnvironmentPromotionResult promote(
        long sourceId, Environment targetEnvironment, Map<Long, Long> connectionMappings) {

        A2aServer source = loadSource(sourceId, targetEnvironment);

        List<SourceProject> sourceProjects = sourceProjects(source);
        long workspaceId = workspaceId(sourceProjects);

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

        Set<Long> sourceConnectionIds = PromotionConnectionScope.sourceConnectionIds(sourceBindings);

        // Source-side scoping, before the mapper's target-side validation — see PromotionConnectionScope.
        PromotionConnectionScope.checkMappedConnectionsBelongToSource(sourceConnectionIds, connectionMappings);

        Map<Long, Long> requestedMappings = connectionEnvironmentMapper.validate(
            workspaceId, targetEnvironment, connectionMappings);
        Map<Long, Long> suggestedMappings = connectionEnvironmentMapper.suggest(
            workspaceId, sourceConnectionIds, targetEnvironment);

        Optional<A2aServer> existingA2aServer = findTarget(source, targetEnvironment);
        boolean created = existingA2aServer.isEmpty();
        A2aServer target = existingA2aServer.orElseGet(() -> createTargetServer(source, targetEnvironment));

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
                a2aProjectFacade.deleteA2aProject(staleTargetProject.id());
            }
        }

        String targetUrl =
            publicUrl + "/api/automation/a2a/" + target.getSecretKey() + "/.well-known/agent-card.json";

        return new EnvironmentPromotionResult(targetId, created, targetUrl, List.copyOf(unresolvedConnectionIds));
    }

    /**
     * Creates the target counterpart. Unlike the API collection and MCP server handlers, no name-collision check runs
     * first: {@code a2a_server} carries no unique constraint on {@code (name, environment)} — only on
     * {@code (uuid, environment)}, which {@link #findTarget} already resolves, and on {@code secret_key}, which is
     * always freshly minted here (spec §6.6) and therefore never collides.
     */
    private A2aServer createTargetServer(A2aServer source, Environment targetEnvironment) {
        // enabled = false: a created counterpart may still have unresolved connections, so an operator reviews it
        // before traffic flows (spec §6.3). The constructor mints a secret key of its own (spec §6.6).
        A2aServer target =
            new A2aServer(source.getName(), source.getDescription(), PlatformType.AUTOMATION, targetEnvironment);

        target.setEnabled(false);
        target.setAuthenticationRequired(source.isAuthenticationRequired());
        target.setUuid(source.getUuid());

        return a2aServerService.create(target);
    }

    /**
     * The target counterpart is the same-uuid server in {@code targetEnvironment}. Unlike the MCP server handler, there
     * is no workspace ownership to additionally check: {@code a2a_server} has no workspace-membership row, so a
     * same-uuid match is unambiguously the counterpart.
     */
    private Optional<A2aServer> findTarget(A2aServer source, Environment targetEnvironment) {
        return a2aServerService.fetchA2aServer(Objects.requireNonNull(source.getUuid(), "uuid"), targetEnvironment);
    }

    private A2aServer loadSource(long sourceId, Environment targetEnvironment) {
        A2aServer source = a2aServerService.getA2aServer(sourceId);

        if (source.getEnvironment() == targetEnvironment) {
            throw new ConfigurationException(
                "A2A server id=%s is already in %s".formatted(sourceId, targetEnvironment),
                EnvironmentPromotionErrorType.SAME_ENVIRONMENT);
        }

        return source;
    }

    /**
     * An A2A server has no workspace row of its own, so the workspace {@link ConnectionEnvironmentMapper} needs is read
     * off the first source project's own project instead. A source with no projects has no connections to map, so the
     * placeholder {@code 0} this returns is never consulted: {@link ConnectionEnvironmentMapper#suggest} and
     * {@link ConnectionEnvironmentMapper#validate} both short-circuit on an empty connection set/mapping before using
     * their {@code workspaceId} argument, and {@link PromotionConnectionScope#checkMappedConnectionsBelongToSource}
     * rejects any non-empty mapping before either is called.
     */
    private long workspaceId(List<SourceProject> sourceProjects) {
        if (sourceProjects.isEmpty()) {
            return 0L;
        }

        Project project = projectService.getProject(ServerProjectPromoter.getProjectId(sourceProjects.getFirst()
            .projectDeployment()));

        return project.getWorkspaceId();
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
     * The A2A-specific writes the shared promoter drives: minting a counterpart {@code a2a_project} through the facade,
     * and reading and writing its {@code a2a_project_workflow} rows.
     */
    private ServerProjectStore serverProjectStore() {
        return new ServerProjectStore() {

            @Override
            public TargetProject createTargetProject(
                long targetServerId, long projectId, int projectVersion, List<String> workflowIds) {

                // The facade mints the target deployment in the target server's environment, its deployment-workflow
                // rows and the matching mapping rows; the sync then moves connections and inputs onto them.
                A2aProject createdA2aProject =
                    a2aProjectFacade.createA2aProject(targetServerId, projectId, projectVersion, workflowIds);

                return new TargetProject(
                    Objects.requireNonNull(createdA2aProject.getId(), "id"),
                    getProjectDeployment(createdA2aProject));
            }

            @Override
            public List<Mapping> mappings(long serverProjectId) {
                return A2aServerPromotionHandler.this.mappings(serverProjectId);
            }

            @Override
            public long createMapping(long serverProjectId, long projectDeploymentWorkflowId) {
                return A2aServerPromotionHandler.this.createMapping(serverProjectId, projectDeploymentWorkflowId);
            }

            @Override
            public void deleteMapping(long mappingId) {
                a2aProjectWorkflowService.delete(mappingId);
            }

            @Override
            public void updateMappingParameters(long mappingId, Map<String, ?> parameters) {
                a2aProjectWorkflowService.updateParameters(mappingId, parameters);
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

    private Map<Long, Deque<TargetProject>> targetProjects(A2aServer target) {
        List<TargetProject> targetProjects = new ArrayList<>();

        for (A2aProject a2aProject : sortedA2aProjects(Objects.requireNonNull(target.getId(), "id"))) {
            targetProjects.add(
                new TargetProject(
                    Objects.requireNonNull(a2aProject.getId(), "id"), getProjectDeployment(a2aProject)));
        }

        return ServerProjectPromoter.groupByProjectId(targetProjects);
    }

    private List<SourceProject> sourceProjects(A2aServer source) {
        List<SourceProject> sourceProjects = new ArrayList<>();

        for (A2aProject a2aProject : sortedA2aProjects(Objects.requireNonNull(source.getId(), "id"))) {
            long a2aProjectId = Objects.requireNonNull(a2aProject.getId(), "id");

            sourceProjects.add(
                new SourceProject(
                    getProjectDeployment(a2aProject),
                    a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(a2aProjectId)));
        }

        return sourceProjects;
    }

    private List<A2aProject> sortedA2aProjects(long a2aServerId) {
        List<A2aProject> a2aProjects = new ArrayList<>(a2aProjectService.getA2aServerA2aProjects(a2aServerId));

        a2aProjects.sort(Comparator.comparing(A2aProject::getId));

        return a2aProjects;
    }

    /**
     * The workflow ids of the source's selection, for the create path: {@code createA2aProject} takes per-version
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

        for (A2aProjectWorkflow a2aProjectWorkflow : sourceProject.a2aProjectWorkflows()) {
            String workflowId =
                workflowIdsByProjectDeploymentWorkflowId.get(a2aProjectWorkflow.getProjectDeploymentWorkflowId());

            if (workflowId != null && !workflowIds.contains(workflowId)) {
                workflowIds.add(workflowId);
            }
        }

        return workflowIds;
    }

    private long createMapping(long a2aProjectId, long projectDeploymentWorkflowId) {
        A2aProjectWorkflow a2aProjectWorkflow =
            a2aProjectWorkflowService.create(a2aProjectId, projectDeploymentWorkflowId);

        return Objects.requireNonNull(a2aProjectWorkflow.getId(), "id");
    }

    private List<Mapping> mappings(long a2aProjectId) {
        List<Mapping> mappings = new ArrayList<>();

        for (A2aProjectWorkflow a2aProjectWorkflow : a2aProjectWorkflowService
            .getA2aProjectA2aProjectWorkflows(a2aProjectId)) {

            Long projectDeploymentWorkflowId = a2aProjectWorkflow.getProjectDeploymentWorkflowId();

            if (projectDeploymentWorkflowId != null) {
                mappings.add(
                    new Mapping(
                        Objects.requireNonNull(a2aProjectWorkflow.getId(), "id"), projectDeploymentWorkflowId));
            }
        }

        return mappings;
    }

    private ProjectDeployment getProjectDeployment(A2aProject a2aProject) {
        return projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(a2aProject.getProjectDeploymentId(), "projectDeploymentId"));
    }

    private static Map<Long, Map<String, ?>> sourceParameters(SourceProject sourceProject) {
        Map<Long, Map<String, ?>> sourceParameters = new LinkedHashMap<>();

        for (A2aProjectWorkflow a2aProjectWorkflow : sourceProject.a2aProjectWorkflows()) {
            Long projectDeploymentWorkflowId = a2aProjectWorkflow.getProjectDeploymentWorkflowId();

            if (projectDeploymentWorkflowId != null) {
                sourceParameters.put(projectDeploymentWorkflowId, a2aProjectWorkflow.getParameters());
            }
        }

        return sourceParameters;
    }
}
