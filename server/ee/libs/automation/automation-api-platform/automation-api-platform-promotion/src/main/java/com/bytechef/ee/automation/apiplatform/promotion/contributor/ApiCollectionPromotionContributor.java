/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.promotion.contributor;

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
import com.bytechef.ee.automation.configuration.promotion.connection.PromotionConnectionScope;
import com.bytechef.ee.automation.configuration.promotion.connection.WorkspaceConnectionEnvironmentMapper;
import com.bytechef.ee.automation.configuration.promotion.deployment.ProjectDeploymentPromoter;
import com.bytechef.ee.automation.configuration.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.configuration.promotion.deployment.ProjectDeploymentPromoter.SyncResult;
import com.bytechef.ee.automation.configuration.promotion.deployment.PromotionPreviews;
import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionChange.Kind;
import com.bytechef.ee.automation.promotion.dto.PromotionProjectPreview;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.util.PromotionChangeHashes;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ApiCollectionPromotionContributor implements EnvironmentPromotionContributor {

    public static final String RESOURCE_TYPE = "API_COLLECTION";

    private static final String ENDPOINT_ASSET_TYPE = "ENDPOINT";
    private static final boolean ENABLED_NOT_CARRIED_BY_THIS_DTO = false;

    private record EndpointKey(HttpMethod httpMethod, String path) {
    }

    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ApiCollectionFacade apiCollectionFacade;
    private final ApiCollectionService apiCollectionService;
    private final WorkspaceConnectionEnvironmentMapper workspaceConnectionEnvironmentMapper;
    private final ConnectionService connectionService;
    private final ProjectDeploymentPromoter projectDeploymentPromoter;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public ApiCollectionPromotionContributor(
        ApiCollectionEndpointService apiCollectionEndpointService, ApiCollectionFacade apiCollectionFacade,
        ApiCollectionService apiCollectionService,
        WorkspaceConnectionEnvironmentMapper workspaceConnectionEnvironmentMapper,
        ConnectionService connectionService, ProjectDeploymentPromoter projectDeploymentPromoter,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService) {

        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.apiCollectionFacade = apiCollectionFacade;
        this.apiCollectionService = apiCollectionService;
        this.workspaceConnectionEnvironmentMapper = workspaceConnectionEnvironmentMapper;
        this.connectionService = connectionService;
        this.projectDeploymentPromoter = projectDeploymentPromoter;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.AUTOMATION;
    }

    @Override
    @PreAuthorize("hasPermission(@apiCollectionPromotionAuthorizer.projectIdOfApiCollection(#sourceId), 'Project', 'DEPLOYMENT_PUSH')")
    @Transactional(readOnly = true)
    public EnvironmentPromotionPreview preview(
        long sourceId, Environment sourceEnvironment, Environment targetEnvironment) {

        ApiCollectionDTO source = loadSource(sourceId, sourceEnvironment, targetEnvironment);
        Project project = projectService.getProject(source.projectId());
        ProjectDeployment sourceProjectDeployment = source.projectDeployment();

        projectDeploymentPromoter.validatePromotable(source.projectId(), source.projectVersion());

        Optional<ApiCollection> existingApiCollection = fetchTargetApiCollection(source, targetEnvironment);
        List<PromotionChange> changes = predictChanges(source, project, existingApiCollection.orElse(null));
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

        return new EnvironmentPromotionPreview(
            RESOURCE_TYPE, sourceId, source.environment(), targetEnvironment,
            existingApiCollection.map(ApiCollection::getId)
                .orElse(null),
            existingApiCollection.map(ApiCollection::getName)
                .orElse(null),
            List.of(
                new PromotionProjectPreview(
                    source.projectId(), project.getName(), source.projectVersion(), targetProjectVersion)),
            PromotionPreviews.connectionMappings(
                sourceBindings, existingTargetBindings, connectionService,
                workspaceConnectionEnvironmentMapper.candidatesLookup(project.getWorkspaceId(), targetEnvironment)),
            warnings, List.of(), changes, PromotionChangeHashes.destructiveChangesHash(changes));
    }

    @Override
    @PreAuthorize("hasPermission(@apiCollectionPromotionAuthorizer.projectIdOfApiCollection(#sourceId), 'Project', 'DEPLOYMENT_PUSH')")
    @Transactional
    public EnvironmentPromotionResult promote(
        long sourceId, Environment sourceEnvironment, Environment targetEnvironment,
        Map<Long, Long> connectionMappings, @Nullable String expectedDestructiveChangesHash) {

        ApiCollectionDTO source = loadSource(sourceId, sourceEnvironment, targetEnvironment);
        Project project = projectService.getProject(source.projectId());

        long workspaceId = project.getWorkspaceId();

        projectDeploymentPromoter.validatePromotable(source.projectId(), source.projectVersion());

        ProjectDeployment sourceProjectDeployment = source.projectDeployment();

        List<SourceBinding> sourceBindings = projectDeploymentPromoter.collectSourceBindings(sourceProjectDeployment);
        Set<Long> sourceConnectionIds = PromotionConnectionScope.sourceConnectionIds(sourceBindings);

        PromotionConnectionScope.checkMappedConnectionsBelongToSource(sourceConnectionIds, connectionMappings);

        Map<Long, Long> requestedMappings = workspaceConnectionEnvironmentMapper.validate(
            workspaceId, targetEnvironment, connectionMappings);
        Map<Long, Long> suggestedMappings = workspaceConnectionEnvironmentMapper.suggest(
            workspaceId, sourceConnectionIds, targetEnvironment);

        apiCollectionService.lockLineage(lineageUuid(source));

        Optional<ApiCollection> existingApiCollection = fetchTargetApiCollection(source, targetEnvironment);
        List<PromotionChange> changes = predictChanges(source, project, existingApiCollection.orElse(null));

        PromotionChangeHashes.checkNotStale(
            expectedDestructiveChangesHash, PromotionChangeHashes.destructiveChangesHash(changes));

        boolean created = existingApiCollection.isEmpty();
        ApiCollection targetApiCollection = existingApiCollection.orElseGet(
            () -> createTargetApiCollection(source, workspaceId, targetEnvironment));

        SyncResult syncResult = projectDeploymentPromoter.sync(
            sourceProjectDeployment, getProjectDeployment(targetApiCollection), requestedMappings, suggestedMappings,
            created);

        if (!created) {
            targetApiCollection.setContextPath(source.contextPath());
            targetApiCollection.setCollectionVersion(source.collectionVersion());

            apiCollectionService.update(targetApiCollection);
        }

        long targetId = Objects.requireNonNull(targetApiCollection.getId(), "id");

        reconcileEndpoints(source, targetId, created);

        return new EnvironmentPromotionResult(targetId, created, null, syncResult.unresolvedConnectionIds());
    }

    private List<PromotionChange> predictChanges(
        ApiCollectionDTO source, Project project, @Nullable ApiCollection targetApiCollection) {

        List<PromotionChange> changes = new ArrayList<>();
        String collectionKey = Objects.requireNonNull(source.uuid(), "uuid");
        String collectionName = source.name();

        if (targetApiCollection == null) {
            changes.add(new PromotionChange(Kind.CREATE, RESOURCE_TYPE, collectionKey, null, collectionName, false));

            for (ApiCollectionEndpointDTO sourceEndpoint : source.endpoints()) {
                changes.add(endpointChange(Kind.CREATE, sourceEndpoint, collectionName, false));
            }

            changes.addAll(
                projectDeploymentPromoter.predictWorkflowChanges(
                    source.projectDeployment(), null, project.getName()));

            return changes;
        }

        ProjectDeployment targetProjectDeployment = getProjectDeployment(targetApiCollection);

        if (!Objects.equals(targetApiCollection.getContextPath(), source.contextPath()) ||
            !Objects.equals(targetApiCollection.getCollectionVersion(), source.collectionVersion()) ||
            targetProjectDeployment.getProjectVersion() != source.projectVersion()) {

            changes.add(new PromotionChange(Kind.UPDATE, RESOURCE_TYPE, collectionKey, null, collectionName, false));
        }

        ApiCollectionDTO target = apiCollectionFacade.getApiCollection(
            Objects.requireNonNull(targetApiCollection.getId(), "id"));
        Map<EndpointKey, ApiCollectionEndpointDTO> targetEndpointsByKey = new LinkedHashMap<>();

        for (ApiCollectionEndpointDTO targetEndpoint : target.endpoints()) {
            targetEndpointsByKey.put(new EndpointKey(targetEndpoint.httpMethod(), targetEndpoint.path()),
                targetEndpoint);
        }

        for (ApiCollectionEndpointDTO sourceEndpoint : source.endpoints()) {
            ApiCollectionEndpointDTO targetEndpoint = targetEndpointsByKey.remove(
                new EndpointKey(sourceEndpoint.httpMethod(), sourceEndpoint.path()));

            if (targetEndpoint == null) {
                changes.add(endpointChange(Kind.CREATE, sourceEndpoint, collectionName, false));
            } else if (!Objects.equals(targetEndpoint.name(), sourceEndpoint.name()) ||
                !Objects.equals(targetEndpoint.workflowUuid(), sourceEndpoint.workflowUuid())) {

                changes.add(endpointChange(Kind.UPDATE, sourceEndpoint, collectionName, false));
            }
        }

        for (ApiCollectionEndpointDTO staleEndpoint : targetEndpointsByKey.values()) {
            changes.add(endpointChange(Kind.DELETE, staleEndpoint, collectionName, true));
        }

        changes.addAll(
            projectDeploymentPromoter.predictWorkflowChanges(
                source.projectDeployment(), targetProjectDeployment, project.getName()));

        return changes;
    }

    private static PromotionChange endpointChange(
        Kind kind, ApiCollectionEndpointDTO endpoint, String collectionName, boolean destructive) {

        return new PromotionChange(
            kind, ENDPOINT_ASSET_TYPE, endpoint.httpMethod() + " " + endpoint.path(), collectionName,
            endpoint.name(), destructive);
    }

    private ApiCollection createTargetApiCollection(
        ApiCollectionDTO source, long workspaceId, Environment targetEnvironment) {

        if (apiCollectionService.existsByNameAndEnvironment(source.name(), workspaceId, targetEnvironment, null)) {
            throw new ConfigurationException(
                "An API collection named '%s' already exists in %s".formatted(source.name(), targetEnvironment),
                EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT);
        }

        ApiCollectionDTO createdApiCollectionDTO = apiCollectionFacade.createApiCollection(
            new ApiCollectionDTO(
                source.collectionVersion(), source.contextPath(), null, null, source.description(), false, List.of(),
                targetEnvironment, null, null, null, source.name(), null, source.projectId(), null, 0,
                source.projectVersion(), source.tags(), 0, source.uuid()));

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
                apiCollectionFacade.createApiCollectionEndpoint(
                    new ApiCollectionEndpointDTO(
                        targetId, null, null, sourceEndpoint.enabled(), sourceEndpoint.httpMethod(), null, null, null,
                        sourceEndpoint.name(), sourceEndpoint.path(), 0, 0, sourceEndpoint.workflowUuid()));
            } else {
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
        return apiCollectionService.fetchApiCollection(lineageUuid(source), targetEnvironment);
    }

    private static UUID lineageUuid(ApiCollectionDTO source) {
        return UUID.fromString(Objects.requireNonNull(source.uuid(), "uuid"));
    }

    private ProjectDeployment getProjectDeployment(ApiCollection apiCollection) {
        return projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(apiCollection.getProjectDeploymentId(), "projectDeploymentId"));
    }

    private static void checkSourceEnvironment(
        long sourceId, Environment rowEnvironment, Environment sourceEnvironment) {

        if (rowEnvironment != sourceEnvironment) {
            throw new ConfigurationException(
                "API collection id=%s is in %s, not in %s".formatted(sourceId, rowEnvironment, sourceEnvironment),
                EnvironmentPromotionErrorType.SOURCE_ENVIRONMENT_MISMATCH);
        }
    }

    private ApiCollectionDTO loadSource(long sourceId, Environment sourceEnvironment, Environment targetEnvironment) {
        ApiCollectionDTO source = apiCollectionFacade.getApiCollection(sourceId);

        checkSourceEnvironment(sourceId, source.environment(), sourceEnvironment);

        if (source.environment() == targetEnvironment) {
            throw new ConfigurationException(
                "API collection id=%s is already in %s".formatted(sourceId, targetEnvironment),
                EnvironmentPromotionErrorType.SAME_ENVIRONMENT);
        }

        return source;
    }
}
