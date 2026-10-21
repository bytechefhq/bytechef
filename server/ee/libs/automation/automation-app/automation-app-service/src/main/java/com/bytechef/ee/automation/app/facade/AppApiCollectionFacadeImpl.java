/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.AppApiCollection;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.dto.AppApiCollectionBindingDTO;
import com.bytechef.ee.automation.app.dto.AppBindableApiCollectionDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.service.AppApiCollectionService;
import com.bytechef.ee.automation.app.service.AppApiCollectionService.BindResult;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.workflow.AppApiCollectionResolver;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AppApiCollectionFacadeImpl implements AppApiCollectionFacade {

    private static final String PROJECT = "Project";

    private final ApiCollectionService apiCollectionService;
    private final AppApiCollectionResolver appApiCollectionResolver;
    private final AppApiCollectionService appApiCollectionService;
    private final AppAuditPublisher appAuditPublisher;
    private final AppInstanceService appInstanceService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final ResourceVisibilityResolver resourceVisibilityResolver;

    @SuppressFBWarnings("EI")
    public AppApiCollectionFacadeImpl(
        ApiCollectionService apiCollectionService, AppApiCollectionResolver appApiCollectionResolver,
        AppApiCollectionService appApiCollectionService, AppAuditPublisher appAuditPublisher,
        AppInstanceService appInstanceService,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        ResourceVisibilityResolver resourceVisibilityResolver) {

        this.apiCollectionService = apiCollectionService;
        this.appApiCollectionResolver = appApiCollectionResolver;
        this.appApiCollectionService = appApiCollectionService;
        this.appAuditPublisher = appAuditPublisher;
        this.appInstanceService = appInstanceService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.resourceVisibilityResolver = resourceVisibilityResolver;
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppApiCollectionBindingDTO> getApiCollectionBindings(long appInstanceId) {
        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        return appApiCollectionService.getBindings(appInstanceId)
            .stream()
            .map(binding -> toBindingDTO(appInstance, binding))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')")
    @Transactional(readOnly = true)
    public List<AppBindableApiCollectionDTO> getBindableApiCollections(long appInstanceId) {
        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        return getVisibleBindableApiCollections(appInstance)
            .stream()
            .map(AppApiCollectionFacadeImpl::toBindableDTO)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')")
    public AppApiCollectionBindingDTO bindApiCollection(long appInstanceId, long apiCollectionId) {
        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        AppInstanceStatusChecks.checkNotTerminated(appInstance);

        BindableApiCollection bindableApiCollection = getVisibleBindableApiCollections(appInstance)
            .stream()
            .filter(candidate -> Objects.equals(candidate.apiCollection()
                .getId(), apiCollectionId))
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                "API collection %s cannot be bound to app instance %s".formatted(apiCollectionId, appInstanceId),
                AppErrorType.APP_API_COLLECTION_NOT_BINDABLE));

        BindResult bindResult = appApiCollectionService.bind(appInstanceId, apiCollectionId);

        if (bindResult.created()) {
            ApiCollection apiCollection = bindableApiCollection.apiCollection();

            appAuditPublisher.publish(
                AppAuditEvent.APP_API_COLLECTION_BOUND, appInstance.getAppId(),
                Map.of(
                    "appInstanceId", appInstanceId, "apiCollectionId", apiCollectionId, "name",
                    apiCollection.getName()));
        }

        return toBindingDTO(appInstance, bindResult.appApiCollection());
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')")
    public void unbindApiCollection(long appInstanceId, long apiCollectionId) {
        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        if (appApiCollectionService.unbind(appInstanceId, apiCollectionId)) {
            appAuditPublisher.publish(
                AppAuditEvent.APP_API_COLLECTION_UNBOUND, appInstance.getAppId(),
                Map.of("appInstanceId", appInstanceId, "apiCollectionId", apiCollectionId));
        }
    }

    private Optional<ProjectDeployment> fetchProjectDeployment(ApiCollection apiCollection) {
        Long projectDeploymentId = apiCollection.getProjectDeploymentId();

        if (projectDeploymentId == null) {
            return Optional.empty();
        }

        return projectDeploymentService.fetchProjectDeployment(projectDeploymentId);
    }

    private List<BindableApiCollection> getVisibleBindableApiCollections(AppInstance appInstance) {
        long workspaceId = appInstance.getWorkspaceId();

        List<BindableApiCollection> bindableApiCollections = new ArrayList<>();

        for (ApiCollection apiCollection : appApiCollectionResolver.getBindableApiCollections(
            workspaceId, appInstance.getEnvironment())) {

            Optional<ProjectDeployment> projectDeployment = fetchProjectDeployment(apiCollection);

            Optional<Project> project = projectDeployment.flatMap(
                curProjectDeployment -> projectService.fetchProject(curProjectDeployment.getProjectId()));

            if (projectDeployment.isPresent() && project.isPresent()) {
                bindableApiCollections.add(
                    new BindableApiCollection(apiCollection, projectDeployment.get(), project.get()));
            }
        }

        if (bindableApiCollections.isEmpty()) {
            return List.of();
        }

        Set<Long> visibleProjectIds = resourceVisibilityResolver.filterVisibleIds(
            PROJECT, workspaceId,
            bindableApiCollections.stream()
                .map(BindableApiCollection::project)
                .map(project -> new VisibilityRecord(project.getId(), project.getVisibility(), project.getCreatedBy()))
                .distinct()
                .toList());

        return bindableApiCollections.stream()
            .filter(bindableApiCollection -> visibleProjectIds.contains(bindableApiCollection.project()
                .getId()))
            .toList();
    }

    private static int toCollectionVersion(ApiCollection apiCollection) {
        Integer collectionVersion = apiCollection.getCollectionVersion();

        return collectionVersion == null ? 0 : collectionVersion;
    }

    private static AppBindableApiCollectionDTO toBindableDTO(BindableApiCollection bindableApiCollection) {
        ApiCollection apiCollection = bindableApiCollection.apiCollection();
        Project project = bindableApiCollection.project();

        return new AppBindableApiCollectionDTO(
            Objects.requireNonNull(apiCollection.getId(), "id"), apiCollection.getName(),
            apiCollection.getContextPath(), toCollectionVersion(apiCollection),
            Objects.requireNonNull(project.getId(), "id"), project.getName(),
            bindableApiCollection.projectDeployment()
                .isEnabled());
    }

    private AppApiCollectionBindingDTO toBindingDTO(AppInstance appInstance, AppApiCollection binding) {
        long apiCollectionId = binding.getApiCollectionId();

        boolean available = appApiCollectionResolver.resolve(
            appInstance.getWorkspaceId(), appInstance.getEnvironment(), apiCollectionId)
            .isPresent();

        Optional<ApiCollection> fetchedApiCollection = apiCollectionService.fetchApiCollection(apiCollectionId);

        if (fetchedApiCollection.isEmpty()) {
            return new AppApiCollectionBindingDTO(
                Objects.requireNonNull(binding.getId()), appInstance.getId(), apiCollectionId, "#" + apiCollectionId,
                null, 0, 0L, null, false, null, null);
        }

        ApiCollection apiCollection = fetchedApiCollection.get();

        Optional<ProjectDeployment> projectDeployment = fetchProjectDeployment(apiCollection);

        Optional<Project> project = projectDeployment.flatMap(
            fetchedProjectDeployment -> projectService.fetchProject(fetchedProjectDeployment.getProjectId()));

        return new AppApiCollectionBindingDTO(
            Objects.requireNonNull(binding.getId()), appInstance.getId(), apiCollectionId, apiCollection.getName(),
            apiCollection.getContextPath(), toCollectionVersion(apiCollection),
            project.map(Project::getId)
                .orElse(0L),
            project.map(Project::getName)
                .orElse(null),
            available,
            projectDeployment.map(ProjectDeployment::getId)
                .orElse(null),
            projectDeployment.map(ProjectDeployment::getProjectVersion)
                .orElse(null));
    }

    private record BindableApiCollection(
        ApiCollection apiCollection, ProjectDeployment projectDeployment, Project project) {
    }
}
