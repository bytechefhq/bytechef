/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.connection.ConnectionEnvironmentMapper;
import com.bytechef.ee.automation.promotion.connection.PromotionConnectionScope;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SyncResult;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionProjectPreview;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
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
 * Promotes a plain (non-synthetic) project deployment from one environment to its counterpart in another, matched by
 * the deployment's cross-environment lineage {@code uuid}.
 *
 * <p>
 * This is the simplest of the four {@link EnvironmentPromotionHandler} implementations: a plain
 * {@link ProjectDeployment} has no {@code api_collection_endpoint}, {@code mcp_project_workflow} or
 * {@code a2a_project_workflow} child rows to reconcile afterwards, so — unlike the API collection, MCP server and A2A
 * server handlers — this one never touches {@link ProjectWorkflowMappingReconciler}, and the delete-before-sync
 * ordering constraint that reconciler exists to satisfy simply does not arise here. Promotion is nothing more than
 * create-or-find the target deployment, then hand both sides to {@link ProjectDeploymentPromoter#sync}.
 * </p>
 *
 * <p>
 * <b>Synthetic deployments are refused, not merely hidden.</b> A deployment whose name starts with
 * {@link SystemProjects#API_COLLECTION_DEPLOYMENT_NAME_PREFIX},
 * {@link SystemProjects#MCP_SERVER_DEPLOYMENT_NAME_PREFIX} or {@link SystemProjects#A2A_SERVER_DEPLOYMENT_NAME_PREFIX}
 * backs an API collection, MCP server or A2A server rather than being a deployment a user created directly. The listing
 * that feeds the "Promote…" menu item already filters these out (spec §15.4), but that is a UI convenience, not an
 * authorization boundary — a direct GraphQL call could still name one of these ids, and promoting it directly would
 * create a second, orphaned deployment shell that the owning surface knows nothing about. This handler rejects such a
 * source independently, in both {@link #preview} and {@link #promote}, naming the surface the caller should promote
 * instead.
 * </p>
 *
 * <p>
 * What the target environment owns is deliberately not overwritten (spec §4 ⚑3): the target keeps its own name,
 * description, tags and every per-workflow {@code enabled} flag. This falls out of how
 * {@link ProjectDeploymentPromoter#sync} is built, rather than anything this handler does explicitly — {@code sync}
 * mutates only the project version of the {@code target} entity it is handed and passes that SAME loaded entity to
 * {@link ProjectDeploymentFacade#updateProjectDeployment(ProjectDeployment, List, List)}, which in turn copies name,
 * description, enabled and tag ids straight off the object it is given. So long as this handler never sets those fields
 * on an update-path {@code target} before calling {@code sync}, they survive untouched; this handler does not, and
 * there is accordingly no separate deployment write on the update path.
 * </p>
 *
 * <p>
 * <b>Name conflicts.</b> {@code project_deployment} carries no database constraint on {@code (name, project_id,
 * environment)} — verified against both the init changelog and every later one, and the closest thing,
 * {@code uk_project_deployment_uuid_environment} (spec §15.3), constrains lineage, not name. A create-path collision is
 * therefore checked here: before minting a target, the handler looks for another deployment already occupying
 * {@code (name, projectId, targetEnvironment)}. Since the create branch only runs when no same-uuid counterpart exists
 * yet, any row found this way is necessarily a different lineage — the embedded automation bridge's per-connected-user
 * catalog deployments being the notable example of another feature that can produce one.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("projectDeploymentPromotionHandler")
@ConditionalOnEEVersion
public class ProjectDeploymentPromotionHandler implements EnvironmentPromotionHandler {

    private final ConnectionEnvironmentMapper connectionEnvironmentMapper;
    private final ConnectionService connectionService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentPromoter projectDeploymentPromoter;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentPromotionHandler(
        ConnectionEnvironmentMapper connectionEnvironmentMapper, ConnectionService connectionService,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentPromoter projectDeploymentPromoter,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService) {

        this.connectionEnvironmentMapper = connectionEnvironmentMapper;
        this.connectionService = connectionService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentPromoter = projectDeploymentPromoter;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    @Override
    public PromotionResourceType getResourceType() {
        return PromotionResourceType.PROJECT_DEPLOYMENT;
    }

    @Override
    @PreAuthorize("hasPermission(@promotionAuthorizer.projectIdOfProjectDeployment(#sourceId), 'Project', 'DEPLOYMENT_PUSH')")
    @Transactional(readOnly = true)
    public EnvironmentPromotionPreview preview(long sourceId, Environment targetEnvironment) {
        ProjectDeployment source = loadSource(sourceId, targetEnvironment);
        long projectId = getProjectId(source);
        Project project = projectService.getProject(projectId);

        Optional<ProjectDeployment> existingTarget = fetchTarget(source, targetEnvironment);
        List<SourceBinding> sourceBindings = projectDeploymentPromoter.collectSourceBindings(source);

        Map<Long, Long> existingTargetBindings = Map.of();
        Integer targetProjectVersion = null;
        List<String> warnings = new ArrayList<>();

        if (existingTarget.isPresent()) {
            ProjectDeployment target = existingTarget.get();

            existingTargetBindings = projectDeploymentPromoter.existingTargetBindings(source, target);
            targetProjectVersion = target.getProjectVersion();
        } else {
            warnings.add("The promoted deployment is created disabled; enable it after reviewing its connections.");
        }

        Map<Long, Long> suggestedMappings = connectionEnvironmentMapper.suggest(
            project.getWorkspaceId(), PromotionConnectionScope.sourceConnectionIds(sourceBindings), targetEnvironment);

        return new EnvironmentPromotionPreview(
            PromotionResourceType.PROJECT_DEPLOYMENT, sourceId, source.getEnvironment(), targetEnvironment,
            existingTarget.map(ProjectDeployment::getId)
                .orElse(null),
            existingTarget.map(ProjectDeployment::getName)
                .orElse(null),
            List.of(new PromotionProjectPreview(projectId, project.getName(), source.getProjectVersion(),
                targetProjectVersion)),
            PromotionPreviews.connectionMappings(sourceBindings, existingTargetBindings, suggestedMappings,
                connectionService),
            warnings);
    }

    @Override
    @PreAuthorize("hasPermission(@promotionAuthorizer.projectIdOfProjectDeployment(#sourceId), 'Project', 'DEPLOYMENT_PUSH')")
    @Transactional
    public EnvironmentPromotionResult promote(
        long sourceId, Environment targetEnvironment, Map<Long, Long> connectionMappings) {

        ProjectDeployment source = loadSource(sourceId, targetEnvironment);
        long projectId = getProjectId(source);
        Project project = projectService.getProject(projectId);

        long workspaceId = project.getWorkspaceId();

        // Before anything is written: an unpublished project, or one whose requested version is still the DRAFT,
        // cannot back a deployment in any environment.
        projectDeploymentPromoter.validatePromotable(projectId, source.getProjectVersion());

        List<SourceBinding> sourceBindings = projectDeploymentPromoter.collectSourceBindings(source);
        Set<Long> sourceConnectionIds = PromotionConnectionScope.sourceConnectionIds(sourceBindings);

        // Source-side scoping, before the mapper's target-side validation — see PromotionConnectionScope.
        PromotionConnectionScope.checkMappedConnectionsBelongToSource(sourceConnectionIds, connectionMappings);

        Map<Long, Long> requestedMappings = connectionEnvironmentMapper.validate(
            workspaceId, targetEnvironment, connectionMappings);
        Map<Long, Long> suggestedMappings = connectionEnvironmentMapper.suggest(
            workspaceId, sourceConnectionIds, targetEnvironment);

        Optional<ProjectDeployment> existingTarget = fetchTarget(source, targetEnvironment);
        boolean created = existingTarget.isEmpty();
        ProjectDeployment target =
            existingTarget.orElseGet(() -> createTarget(source, projectId, targetEnvironment));

        SyncResult syncResult =
            projectDeploymentPromoter.sync(source, target, requestedMappings, suggestedMappings, created);

        long targetId = Objects.requireNonNull(target.getId(), "id");

        return new EnvironmentPromotionResult(targetId, created, null, syncResult.unresolvedConnectionIds());
    }

    /**
     * Mints an empty target deployment shell — no workflows, no tags — which {@link ProjectDeploymentPromoter#sync}
     * then populates in place. Mirrors how the API collection, MCP server and A2A server handlers create their own
     * synthetic deployment before handing it to the same {@code sync} call.
     */
    private ProjectDeployment createTarget(ProjectDeployment source, long projectId, Environment targetEnvironment) {
        checkNoNameConflict(source, projectId, targetEnvironment);

        ProjectDeployment target = new ProjectDeployment();

        target.setDescription(source.getDescription());
        target.setEnabled(false);
        target.setEnvironment(targetEnvironment);
        target.setName(source.getName());
        target.setProjectId(projectId);
        target.setProjectVersion(source.getProjectVersion());
        target.setUuid(source.getUuid());

        long targetId = projectDeploymentFacade.createProjectDeployment(target, List.of(), List.of());

        // Re-read as an entity: sync() needs the persisted row (with its own id and optimistic-locking version), not
        // the transient one this method built.
        return projectDeploymentService.getProjectDeployment(targetId);
    }

    /**
     * {@code project_deployment} carries no database constraint on {@code (name, project_id, environment)} — verified
     * against the init changelog and every changelog added since (the only later addition,
     * {@code uk_project_deployment_uuid_environment}, constrains lineage, not name) — so a collision is checked here.
     * Only deployments in the exact target environment are considered; {@code getProjectDeployments} already excludes
     * the synthetic API collection / MCP server / A2A server deployments, so a hit here always names another
     * user-facing (or embedded-bridge) deployment.
     */
    private void checkNoNameConflict(ProjectDeployment source, long projectId, Environment targetEnvironment) {
        List<ProjectDeployment> targetEnvironmentProjectDeployments =
            projectDeploymentService.getProjectDeployments(null, targetEnvironment, projectId, null, null);

        for (ProjectDeployment targetEnvironmentProjectDeployment : targetEnvironmentProjectDeployments) {
            if (Objects.equals(targetEnvironmentProjectDeployment.getName(), source.getName())) {
                throw new ConfigurationException(
                    "A project deployment named '%s' already exists in %s".formatted(
                        source.getName(), targetEnvironment),
                    EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT);
            }
        }
    }

    private Optional<ProjectDeployment> fetchTarget(ProjectDeployment source, Environment targetEnvironment) {
        return projectDeploymentService.fetchProjectDeployment(
            Objects.requireNonNull(source.getUuid(), "uuid"), targetEnvironment);
    }

    private ProjectDeployment loadSource(long sourceId, Environment targetEnvironment) {
        ProjectDeployment source = projectDeploymentService.getProjectDeployment(sourceId);

        checkNotSynthetic(source);

        if (source.getEnvironment() == targetEnvironment) {
            throw new ConfigurationException(
                "Project deployment id=%s is already in %s".formatted(sourceId, targetEnvironment),
                EnvironmentPromotionErrorType.SAME_ENVIRONMENT);
        }

        return source;
    }

    /**
     * Rejects a source deployment backing an API collection, MCP server or A2A server (spec §15.5). These are refused
     * independently of the listing filter that already hides them, since that filter is a UI concern, not an
     * authorization boundary.
     */
    private static void checkNotSynthetic(ProjectDeployment source) {
        String owningSurface = owningSurfaceName(source.getName());

        if (owningSurface != null) {
            throw new ConfigurationException(
                "Project deployment id=%s backs an %s; promote the %s itself instead".formatted(
                    source.getId(), owningSurface, owningSurface),
                EnvironmentPromotionErrorType.SYNTHETIC_DEPLOYMENT_NOT_PROMOTABLE);
        }
    }

    @Nullable
    private static String owningSurfaceName(@Nullable String name) {
        if (name == null) {
            return null;
        }

        if (name.startsWith(SystemProjects.API_COLLECTION_DEPLOYMENT_NAME_PREFIX)) {
            return "API collection";
        }

        if (name.startsWith(SystemProjects.MCP_SERVER_DEPLOYMENT_NAME_PREFIX)) {
            return "MCP server";
        }

        if (name.startsWith(SystemProjects.A2A_SERVER_DEPLOYMENT_NAME_PREFIX)) {
            return "A2A server";
        }

        return null;
    }

    private static long getProjectId(ProjectDeployment projectDeployment) {
        return Objects.requireNonNull(projectDeployment.getProjectId(), "projectId");
    }
}
