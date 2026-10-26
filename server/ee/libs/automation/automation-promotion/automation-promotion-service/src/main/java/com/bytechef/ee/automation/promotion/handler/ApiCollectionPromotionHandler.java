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
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ApiCollectionPromotionHandler implements EnvironmentPromotionHandler {

    private static final boolean ENABLED_NOT_CARRIED_BY_THIS_DTO = false;

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

        projectDeploymentPromoter.validatePromotable(source.projectId(), source.projectVersion());

        ProjectDeployment sourceProjectDeployment = source.projectDeployment();

        List<SourceBinding> sourceBindings = projectDeploymentPromoter.collectSourceBindings(sourceProjectDeployment);
        Set<Long> sourceConnectionIds = PromotionConnectionScope.sourceConnectionIds(sourceBindings);

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
