/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promotes an API collection from one environment to its counterpart in another, matched by the collection's
 * cross-environment lineage {@code uuid}.
 *
 * <p>
 * The collection row itself is thin — name, description, context path, collection version, tags — and everything that
 * makes it run lives on the synthetic {@code __API_COLLECTION__} {@link ProjectDeployment} behind it and on the
 * {@code api_collection_endpoint} rows pointing into that deployment's {@code project_deployment_workflow} rows.
 * Promotion therefore has three parts, in this order: create-or-find the target collection, hand its deployment to
 * {@link ProjectDeploymentPromoter#sync} so the project version and connection bindings move through the one shared
 * path, then reconcile the endpoints.
 * </p>
 *
 * <p>
 * Endpoints are reconciled by {@code (httpMethod, path)} rather than by id, since a target-environment endpoint has an
 * id of its own that no source id corresponds to. Reconciliation runs AFTER the sync: on the create path the deployment
 * must already carry its {@code project_deployment_workflow} rows before
 * {@link ApiCollectionFacade#createApiCollectionEndpoint} resolves one by workflow uuid.
 * </p>
 *
 * <p>
 * What the target environment owns is deliberately not overwritten (spec §6.2): the target keeps its own name,
 * description, tags and per-endpoint {@code enabled} flag, and a created collection's deployment stays disabled so an
 * operator reviews the connection wiring before traffic flows.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ApiCollectionPromotionHandler implements EnvironmentPromotionHandler {

    /**
     * {@code enabled} is not a column of {@code api_collection_endpoint} — it is a projection of the endpoint's
     * {@code project_deployment_workflow} row, which {@link ProjectDeploymentPromoter#sync} owns. Spec §6.2's "from S
     * only when the endpoint is created in T" is therefore delivered by the promoter's {@code keepTargetConfiguration}
     * branch, not here, and NEITHER value this handler puts in an {@link ApiCollectionEndpointDTO} reaches a row. This
     * constant names the inert value passed on the update path so a reader is not left inferring a semantic from it.
     */
    private static final boolean ENABLED_NOT_CARRIED_BY_THIS_DTO = false;

    /**
     * Identifies an endpoint across environments. A record rather than a joined string because {@code path} is
     * unconstrained user-facing text, so any delimiter could occur inside one and make two distinct endpoints collide
     * on a single key — which here would silently update one endpoint from another's source and delete the loser.
     */
    private record EndpointKey(HttpMethod httpMethod, String path) {
    }

    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ApiCollectionFacade apiCollectionFacade;
    private final ApiCollectionService apiCollectionService;
    private final ConnectionEnvironmentMapper connectionEnvironmentMapper;
    private final ConnectionService connectionService;
    private final ProjectDeploymentPromoter projectDeploymentPromoter;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public ApiCollectionPromotionHandler(
        ApiCollectionEndpointService apiCollectionEndpointService, ApiCollectionFacade apiCollectionFacade,
        ApiCollectionService apiCollectionService, ConnectionEnvironmentMapper connectionEnvironmentMapper,
        ConnectionService connectionService, ProjectDeploymentPromoter projectDeploymentPromoter,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService) {

        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.apiCollectionFacade = apiCollectionFacade;
        this.apiCollectionService = apiCollectionService;
        this.connectionEnvironmentMapper = connectionEnvironmentMapper;
        this.connectionService = connectionService;
        this.projectDeploymentPromoter = projectDeploymentPromoter;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    @Override
    public PromotionResourceType getResourceType() {
        return PromotionResourceType.API_COLLECTION;
    }

    @Override
    @PreAuthorize("hasPermission(@promotionAuthorizer.projectIdOfApiCollection(#sourceId), 'Project', 'DEPLOYMENT_PUSH')")
    @Transactional(readOnly = true)
    public EnvironmentPromotionPreview preview(long sourceId, Environment targetEnvironment) {
        ApiCollectionDTO source = loadSource(sourceId, targetEnvironment);
        Project project = projectService.getProject(source.projectId());
        ProjectDeployment sourceProjectDeployment = source.projectDeployment();

        Optional<ApiCollection> existingApiCollection = fetchTargetApiCollection(source, targetEnvironment);
        List<SourceBinding> sourceBindings = projectDeploymentPromoter.collectSourceBindings(sourceProjectDeployment);

        Map<Long, Long> existingTargetBindings = Map.of();
        Integer targetProjectVersion = null;
        List<String> warnings = new ArrayList<>();

        if (existingApiCollection.isPresent()) {
            ApiCollection targetApiCollection = existingApiCollection.get();

            ProjectDeployment targetProjectDeployment = getProjectDeployment(targetApiCollection);

            existingTargetBindings = projectDeploymentPromoter.existingTargetBindings(
                sourceProjectDeployment, targetProjectDeployment);
            targetProjectVersion = targetProjectDeployment.getProjectVersion();

            addOverwriteWarnings(warnings, source, targetApiCollection);
        } else {
            warnings.add("The promoted collection is created disabled; enable it after reviewing its connections.");
        }

        Map<Long, Long> suggestedMappings = connectionEnvironmentMapper.suggest(
            project.getWorkspaceId(), PromotionConnectionScope.sourceConnectionIds(sourceBindings), targetEnvironment);

        return new EnvironmentPromotionPreview(
            PromotionResourceType.API_COLLECTION, sourceId, source.environment(), targetEnvironment,
            existingApiCollection.map(ApiCollection::getId)
                .orElse(null),
            existingApiCollection.map(ApiCollection::getName)
                .orElse(null),
            List.of(
                new PromotionProjectPreview(
                    source.projectId(), project.getName(), source.projectVersion(), targetProjectVersion)),
            PromotionPreviews.connectionMappings(
                sourceBindings, existingTargetBindings, suggestedMappings, connectionService),
            warnings);
    }

    @Override
    @PreAuthorize("hasPermission(@promotionAuthorizer.projectIdOfApiCollection(#sourceId), 'Project', 'DEPLOYMENT_PUSH')")
    @Transactional
    public EnvironmentPromotionResult promote(
        long sourceId, Environment targetEnvironment, Map<Long, Long> connectionMappings) {

        ApiCollectionDTO source = loadSource(sourceId, targetEnvironment);
        Project project = projectService.getProject(source.projectId());

        long workspaceId = project.getWorkspaceId();

        // Before anything is written: an unpublished project, or one whose requested version is still the DRAFT,
        // cannot back a deployment in any environment.
        projectDeploymentPromoter.validatePromotable(source.projectId(), source.projectVersion());

        ProjectDeployment sourceProjectDeployment = source.projectDeployment();

        List<SourceBinding> sourceBindings = projectDeploymentPromoter.collectSourceBindings(sourceProjectDeployment);
        Set<Long> sourceConnectionIds = PromotionConnectionScope.sourceConnectionIds(sourceBindings);

        // Source-side scoping, before the mapper's target-side validation — see PromotionConnectionScope.
        PromotionConnectionScope.checkMappedConnectionsBelongToSource(sourceConnectionIds, connectionMappings);

        Map<Long, Long> requestedMappings = connectionEnvironmentMapper.validate(
            workspaceId, targetEnvironment, connectionMappings);
        Map<Long, Long> suggestedMappings = connectionEnvironmentMapper.suggest(
            workspaceId, sourceConnectionIds, targetEnvironment);

        Optional<ApiCollection> existingApiCollection = fetchTargetApiCollection(source, targetEnvironment);
        boolean created = existingApiCollection.isEmpty();
        ApiCollection targetApiCollection = existingApiCollection.orElseGet(
            () -> createTargetApiCollection(source, workspaceId, targetEnvironment));

        SyncResult syncResult = projectDeploymentPromoter.sync(
            sourceProjectDeployment, getProjectDeployment(targetApiCollection), requestedMappings, suggestedMappings,
            created);

        if (!created) {
            // Only the two fields that define the collection's public route move; name, description and tags stay
            // whatever the target environment named them.
            targetApiCollection.setContextPath(source.contextPath());
            targetApiCollection.setCollectionVersion(source.collectionVersion());

            apiCollectionService.update(targetApiCollection);
        }

        long targetId = Objects.requireNonNull(targetApiCollection.getId(), "id");

        reconcileEndpoints(source, targetId, created);

        return new EnvironmentPromotionResult(targetId, created, null, syncResult.unresolvedConnectionIds());
    }

    private ApiCollection createTargetApiCollection(
        ApiCollectionDTO source, long workspaceId, Environment targetEnvironment) {

        if (apiCollectionService.existsByNameAndEnvironment(source.name(), workspaceId, targetEnvironment, null)) {
            throw new ConfigurationException(
                "An API collection named '%s' already exists in %s".formatted(source.name(), targetEnvironment),
                EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT);
        }

        // The facade mints the synthetic __API_COLLECTION__ deployment; it never enables it, which is the
        // created-disabled contract of spec §6.3.
        ApiCollectionDTO createdApiCollectionDTO = apiCollectionFacade.createApiCollection(
            new ApiCollectionDTO(
                source.collectionVersion(), source.contextPath(), null, null, source.description(), false, List.of(),
                targetEnvironment, null, null, null, source.name(), null, source.projectId(), null, 0,
                source.projectVersion(), source.tags(), 0, source.uuid()));

        // Re-read as an entity: sync() and update() both need the persisted row, not a DTO projection of it.
        return apiCollectionService.getApiCollection(Objects.requireNonNull(createdApiCollectionDTO.id(), "id"));
    }

    private void reconcileEndpoints(ApiCollectionDTO source, long targetId, boolean created) {
        Map<EndpointKey, ApiCollectionEndpoint> targetEndpointsByKey = new LinkedHashMap<>();

        if (!created) {
            for (ApiCollectionEndpoint targetEndpoint : apiCollectionEndpointService.getApiEndpoints(targetId)) {
                targetEndpointsByKey.put(
                    new EndpointKey(targetEndpoint.getHttpMethod(), targetEndpoint.getPath()), targetEndpoint);
            }
        }

        for (ApiCollectionEndpointDTO sourceEndpoint : source.endpoints()) {
            ApiCollectionEndpoint targetEndpoint = targetEndpointsByKey.remove(
                new EndpointKey(sourceEndpoint.httpMethod(), sourceEndpoint.path()));

            if (targetEndpoint == null) {
                // sourceEndpoint.enabled() is inert here for the same reason as on the update branch: the flag lives
                // on the project_deployment_workflow row the sync already wrote, not on the endpoint.
                apiCollectionFacade.createApiCollectionEndpoint(
                    new ApiCollectionEndpointDTO(
                        targetId, null, null, sourceEndpoint.enabled(), sourceEndpoint.httpMethod(), null, null, null,
                        sourceEndpoint.name(), sourceEndpoint.path(), 0, 0, sourceEndpoint.workflowUuid()));
            } else {
                // workflowUuid is what makes the facade re-resolve the project_deployment_workflow pointer, so a
                // source endpoint moved onto another workflow re-points its target counterpart instead of keeping
                // the stale one.
                apiCollectionFacade.updateApiCollectionEndpoint(
                    new ApiCollectionEndpointDTO(
                        targetId, null, null, ENABLED_NOT_CARRIED_BY_THIS_DTO, sourceEndpoint.httpMethod(),
                        targetEndpoint.getId(), null, null, sourceEndpoint.name(), sourceEndpoint.path(),
                        Objects.requireNonNullElse(targetEndpoint.getProjectDeploymentWorkflowId(), 0L),
                        targetEndpoint.getVersion(), sourceEndpoint.workflowUuid()));
            }
        }

        for (ApiCollectionEndpoint staleEndpoint : targetEndpointsByKey.values()) {
            apiCollectionEndpointService.delete(Objects.requireNonNull(staleEndpoint.getId(), "id"));
        }
    }

    private static void addOverwriteWarnings(
        List<String> warnings, ApiCollectionDTO source, ApiCollection targetApiCollection) {

        if (!Objects.equals(targetApiCollection.getContextPath(), source.contextPath())) {
            warnings.add(
                "Context path will change from '%s' to '%s'".formatted(
                    targetApiCollection.getContextPath(), source.contextPath()));
        }

        if (!Objects.equals(targetApiCollection.getCollectionVersion(), source.collectionVersion())) {
            warnings.add(
                "Collection version will change from v%s to v%s".formatted(
                    targetApiCollection.getCollectionVersion(), source.collectionVersion()));
        }
    }

    private Optional<ApiCollection> fetchTargetApiCollection(ApiCollectionDTO source, Environment targetEnvironment) {
        return apiCollectionService.fetchApiCollection(
            UUID.fromString(Objects.requireNonNull(source.uuid(), "uuid")), targetEnvironment);
    }

    private ProjectDeployment getProjectDeployment(ApiCollection apiCollection) {
        return projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(apiCollection.getProjectDeploymentId(), "projectDeploymentId"));
    }

    private ApiCollectionDTO loadSource(long sourceId, Environment targetEnvironment) {
        ApiCollectionDTO source = apiCollectionFacade.getApiCollection(sourceId);

        if (source.environment() == targetEnvironment) {
            throw new ConfigurationException(
                "API collection id=%s is already in %s".formatted(sourceId, targetEnvironment),
                EnvironmentPromotionErrorType.SAME_ENVIRONMENT);
        }

        return source;
    }
}
