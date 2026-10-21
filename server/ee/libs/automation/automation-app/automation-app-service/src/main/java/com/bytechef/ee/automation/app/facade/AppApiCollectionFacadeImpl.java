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
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppApiCollection;
import com.bytechef.ee.automation.app.dto.AppApiCollectionBindingDTO;
import com.bytechef.ee.automation.app.dto.AppBindableApiCollectionDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.service.AppApiCollectionService;
import com.bytechef.ee.automation.app.service.AppApiCollectionService.BindResult;
import com.bytechef.ee.automation.app.service.AppService;
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
    private final AppService appService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final ResourceVisibilityResolver resourceVisibilityResolver;

    @SuppressFBWarnings("EI")
    public AppApiCollectionFacadeImpl(
        ApiCollectionService apiCollectionService, AppApiCollectionResolver appApiCollectionResolver,
        AppApiCollectionService appApiCollectionService, AppAuditPublisher appAuditPublisher, AppService appService,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        ResourceVisibilityResolver resourceVisibilityResolver) {

        this.apiCollectionService = apiCollectionService;
        this.appApiCollectionResolver = appApiCollectionResolver;
        this.appApiCollectionService = appApiCollectionService;
        this.appAuditPublisher = appAuditPublisher;
        this.appService = appService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.resourceVisibilityResolver = resourceVisibilityResolver;
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppApiCollectionBindingDTO> getApiCollectionBindings(long appId) {
        App app = appService.getApp(appId);

        return appApiCollectionService.getBindings(appId)
            .stream()
            .map(binding -> toBindingDTO(app, binding))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
    @Transactional(readOnly = true)
    public List<AppBindableApiCollectionDTO> getBindableApiCollections(long appId) {
        App app = appService.getApp(appId);

        return getVisibleBindableApiCollections(app)
            .stream()
            .map(AppApiCollectionFacadeImpl::toBindableDTO)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
    public AppApiCollectionBindingDTO bindApiCollection(long appId, long apiCollectionId) {
        App app = appService.getApp(appId);

        BindableApiCollection bindableApiCollection = getVisibleBindableApiCollections(app)
            .stream()
            .filter(candidate -> Objects.equals(candidate.apiCollection()
                .getId(), apiCollectionId))
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                "API collection %s cannot be bound to app %s".formatted(apiCollectionId, appId),
                AppErrorType.APP_API_COLLECTION_NOT_BINDABLE));

        BindResult bindResult = appApiCollectionService.bind(appId, apiCollectionId);

        if (bindResult.created()) {
            ApiCollection apiCollection = bindableApiCollection.apiCollection();

            appAuditPublisher.publish(
                AppAuditEvent.APP_API_COLLECTION_BOUND, appId,
                Map.of("apiCollectionId", apiCollectionId, "name", apiCollection.getName()));
        }

        return toBindingDTO(app, bindResult.appApiCollection());
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
    public void unbindApiCollection(long appId, long apiCollectionId) {
        if (appApiCollectionService.unbind(appId, apiCollectionId)) {
            appAuditPublisher.publish(
                AppAuditEvent.APP_API_COLLECTION_UNBOUND, appId, Map.of("apiCollectionId", apiCollectionId));
        }
    }

    private Optional<ProjectDeployment> fetchProjectDeployment(ApiCollection apiCollection) {
        Long projectDeploymentId = apiCollection.getProjectDeploymentId();

        if (projectDeploymentId == null) {
            return Optional.empty();
        }

        return projectDeploymentService.fetchProjectDeployment(projectDeploymentId);
    }

    private List<BindableApiCollection> getVisibleBindableApiCollections(App app) {
        long workspaceId = getWorkspaceId(app);

        List<BindableApiCollection> bindableApiCollections = new ArrayList<>();

        for (ApiCollection apiCollection : appApiCollectionResolver.getBindableApiCollections(
            workspaceId, app.getEnvironment())) {

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

    private static long getWorkspaceId(App app) {
        return Objects.requireNonNull(app.getWorkspaceId(), "workspaceId");
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

    private AppApiCollectionBindingDTO toBindingDTO(App app, AppApiCollection binding) {
        long apiCollectionId = binding.getApiCollectionId();

        boolean available = appApiCollectionResolver.resolve(getWorkspaceId(app), app.getEnvironment(), apiCollectionId)
            .isPresent();

        Optional<ApiCollection> fetchedApiCollection = apiCollectionService.fetchApiCollection(apiCollectionId);

        if (fetchedApiCollection.isEmpty()) {
            return new AppApiCollectionBindingDTO(
                Objects.requireNonNull(binding.getId()), binding.getAppId(), apiCollectionId, "#" + apiCollectionId,
                null, 0, 0L, null, false);
        }

        ApiCollection apiCollection = fetchedApiCollection.get();

        Optional<Project> project = fetchProjectDeployment(apiCollection).flatMap(
            projectDeployment -> projectService.fetchProject(projectDeployment.getProjectId()));

        return new AppApiCollectionBindingDTO(
            Objects.requireNonNull(binding.getId()), binding.getAppId(), apiCollectionId, apiCollection.getName(),
            apiCollection.getContextPath(), toCollectionVersion(apiCollection),
            project.map(Project::getId)
                .orElse(0L),
            project.map(Project::getName)
                .orElse(null),
            available);
    }

    private record BindableApiCollection(
        ApiCollection apiCollection, ProjectDeployment projectDeployment, Project project) {
    }
}
