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
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppWorkflow;
import com.bytechef.ee.automation.app.dto.AppBindableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppCallableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowBindingDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.service.AppWorkflowService;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver.BindableWorkflow;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver.CallableWorkflow;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
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
public class AppWorkflowFacadeImpl implements AppWorkflowFacade {

    private static final String PROJECT = "Project";

    private final AppAuditPublisher appAuditPublisher;
    private final AppCallableWorkflowResolver appCallableWorkflowResolver;
    private final AppService appService;
    private final AppWorkflowService appWorkflowService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final ResourceVisibilityResolver resourceVisibilityResolver;

    @SuppressFBWarnings("EI")
    public AppWorkflowFacadeImpl(
        AppAuditPublisher appAuditPublisher, AppCallableWorkflowResolver appCallableWorkflowResolver,
        AppService appService, AppWorkflowService appWorkflowService,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        ResourceVisibilityResolver resourceVisibilityResolver) {

        this.appAuditPublisher = appAuditPublisher;
        this.appCallableWorkflowResolver = appCallableWorkflowResolver;
        this.appService = appService;
        this.appWorkflowService = appWorkflowService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.resourceVisibilityResolver = resourceVisibilityResolver;
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppWorkflowBindingDTO> getWorkflowBindings(long appId) {
        App app = appService.getApp(appId);

        return appWorkflowService.getBindings(appId)
            .stream()
            .map(binding -> toBindingDTO(app, binding))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
    @Transactional(readOnly = true)
    public List<AppBindableWorkflowDTO> getBindableWorkflows(long appId) {
        App app = appService.getApp(appId);

        List<BindableWorkflow> bindableWorkflows = appCallableWorkflowResolver.getBindableWorkflows(
            getWorkspaceId(app), app.getEnvironment());

        Set<Long> visibleProjectIds = getVisibleProjectIds(
            getWorkspaceId(app), bindableWorkflows.stream()
                .map(BindableWorkflow::project)
                .toList());

        return bindableWorkflows.stream()
            .filter(bindableWorkflow -> visibleProjectIds.contains(bindableWorkflow.project()
                .getId()))
            .map(bindableWorkflow -> new AppBindableWorkflowDTO(
                Objects.requireNonNull(bindableWorkflow.projectDeployment()
                    .getId()),
                bindableWorkflow.projectDeployment()
                    .getName(),
                bindableWorkflow.project()
                    .getName(),
                bindableWorkflow.workflowUuid(),
                bindableWorkflow.callableWorkflow()
                    .label(),
                bindableWorkflow.callableWorkflow()
                    .description()))
            .toList();
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'DEPLOYMENT_VIEW', " +
        "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT)")
    @Transactional(readOnly = true)
    public List<AppCallableWorkflowDTO> getCallableWorkflows(long workspaceId, @Nullable Long projectId) {
        List<BindableWorkflow> bindableWorkflows = appCallableWorkflowResolver
            .getBindableWorkflows(workspaceId, Environment.DEVELOPMENT)
            .stream()
            .filter(bindableWorkflow -> projectId == null || Objects.equals(getProjectId(bindableWorkflow), projectId))
            .toList();

        Set<Long> visibleProjectIds = getVisibleProjectIds(
            workspaceId, bindableWorkflows.stream()
                .map(BindableWorkflow::project)
                .toList());

        return bindableWorkflows.stream()
            .filter(bindableWorkflow -> visibleProjectIds.contains(getProjectId(bindableWorkflow)))
            .map(AppWorkflowFacadeImpl::toCallableWorkflowDTO)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
    public AppWorkflowBindingDTO bindWorkflow(
        long appId, String workflowKey, long projectDeploymentId, String workflowUuid) {

        App app = appService.getApp(appId);

        checkProjectVisible(app, projectDeploymentId);

        Optional<AppWorkflow> existingBinding = appWorkflowService.fetchBinding(appId, workflowKey);

        AppWorkflow appWorkflow = appWorkflowService.bind(appId, workflowKey, projectDeploymentId, workflowUuid);

        Map<String, Object> data = new HashMap<>();

        data.put("workflowKey", workflowKey);
        data.put("projectDeploymentId", projectDeploymentId);
        data.put("workflowUuid", workflowUuid);

        existingBinding
            .filter(previous -> !Objects.equals(previous.getProjectDeploymentId(), projectDeploymentId) ||
                !Objects.equals(previous.getWorkflowUuid(), workflowUuid))
            .ifPresent(previous -> {
                data.put("previousProjectDeploymentId", previous.getProjectDeploymentId());
                data.put("previousWorkflowUuid", previous.getWorkflowUuid());
            });

        appAuditPublisher.publish(AppAuditEvent.APP_WORKFLOW_BOUND, appId, data);

        return toBindingDTO(app, appWorkflow);
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
    public void unbindWorkflow(long appId, String workflowKey) {
        if (appWorkflowService.unbind(appId, workflowKey)) {
            appAuditPublisher.publish(AppAuditEvent.APP_WORKFLOW_UNBOUND, appId, Map.of("workflowKey", workflowKey));
        }
    }

    private void checkProjectVisible(App app, long projectDeploymentId) {
        Optional<Project> project = projectDeploymentService.fetchProjectDeployment(projectDeploymentId)
            .flatMap(projectDeployment -> projectService.fetchProject(projectDeployment.getProjectId()));

        if (project.isEmpty()
            || !getVisibleProjectIds(getWorkspaceId(app), List.of(project.get())).contains(project.get()
                .getId())) {

            throw new ConfigurationException(
                "Project deployment %s cannot be bound to app %s".formatted(projectDeploymentId, app.getId()),
                AppErrorType.APP_WORKFLOW_NOT_BINDABLE);
        }
    }

    private Set<Long> getVisibleProjectIds(long workspaceId, List<Project> projects) {
        if (projects.isEmpty()) {
            return Set.of();
        }

        return resourceVisibilityResolver.filterVisibleIds(
            PROJECT, workspaceId,
            projects.stream()
                .map(project -> new VisibilityRecord(project.getId(), project.getVisibility(), project.getCreatedBy()))
                .distinct()
                .toList());
    }

    private static Long getProjectId(BindableWorkflow bindableWorkflow) {
        Project project = bindableWorkflow.project();

        return project.getId();
    }

    private static AppCallableWorkflowDTO toCallableWorkflowDTO(BindableWorkflow bindableWorkflow) {
        ProjectDeployment projectDeployment = bindableWorkflow.projectDeployment();
        Project project = bindableWorkflow.project();
        CallableWorkflow callableWorkflow = bindableWorkflow.callableWorkflow();

        return new AppCallableWorkflowDTO(
            Objects.requireNonNull(projectDeployment.getId(), "id"), bindableWorkflow.workflowUuid(),
            callableWorkflow.label(), Objects.requireNonNull(project.getId(), "id"), project.getName(),
            callableWorkflow.inputSchema());
    }

    private static long getWorkspaceId(App app) {
        return Objects.requireNonNull(app.getWorkspaceId(), "workspaceId");
    }

    private AppWorkflowBindingDTO toBindingDTO(App app, AppWorkflow binding) {
        Optional<CallableWorkflow> callableWorkflow = appCallableWorkflowResolver.resolve(
            getWorkspaceId(app), app.getEnvironment(), binding.getProjectDeploymentId(), binding.getWorkflowUuid());
        String projectDeploymentName = projectDeploymentService.fetchProjectDeployment(
            binding.getProjectDeploymentId())
            .map(ProjectDeployment::getName)
            .orElse(null);

        return new AppWorkflowBindingDTO(
            Objects.requireNonNull(binding.getId()), binding.getAppId(), binding.getWorkflowKey(),
            binding.getProjectDeploymentId(), binding.getWorkflowUuid(), projectDeploymentName,
            callableWorkflow.map(CallableWorkflow::label)
                .orElse(null),
            callableWorkflow.isPresent());
    }
}
