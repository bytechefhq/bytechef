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
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppWorkflow;
import com.bytechef.ee.automation.app.dto.AppBindableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppCallableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowBindingDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.service.AppInstanceService;
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
    private final AppInstanceService appInstanceService;
    private final AppWorkflowService appWorkflowService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final ResourceVisibilityResolver resourceVisibilityResolver;

    @SuppressFBWarnings("EI")
    public AppWorkflowFacadeImpl(
        AppAuditPublisher appAuditPublisher, AppCallableWorkflowResolver appCallableWorkflowResolver,
        AppInstanceService appInstanceService, AppWorkflowService appWorkflowService,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        ResourceVisibilityResolver resourceVisibilityResolver) {

        this.appAuditPublisher = appAuditPublisher;
        this.appCallableWorkflowResolver = appCallableWorkflowResolver;
        this.appInstanceService = appInstanceService;
        this.appWorkflowService = appWorkflowService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.resourceVisibilityResolver = resourceVisibilityResolver;
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppWorkflowBindingDTO> getWorkflowBindings(long appInstanceId) {
        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        return appWorkflowService.getBindings(appInstanceId)
            .stream()
            .map(binding -> toBindingDTO(appInstance, binding))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')")
    @Transactional(readOnly = true)
    public List<AppBindableWorkflowDTO> getBindableWorkflows(long appInstanceId) {
        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        List<BindableWorkflow> bindableWorkflows = appCallableWorkflowResolver.getBindableWorkflows(
            appInstance.getWorkspaceId(), appInstance.getEnvironment());

        Set<Long> visibleProjectIds = getVisibleProjectIds(
            appInstance.getWorkspaceId(), bindableWorkflows.stream()
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
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')")
    public AppWorkflowBindingDTO bindWorkflow(
        long appInstanceId, String workflowKey, long projectDeploymentId, String workflowUuid) {

        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        AppInstanceStatusChecks.checkNotTerminated(appInstance);
        checkProjectVisible(appInstance, projectDeploymentId);

        Optional<AppWorkflow> existingBinding = appWorkflowService.fetchBinding(appInstanceId, workflowKey);

        AppWorkflow appWorkflow = appWorkflowService.bind(appInstance, workflowKey, projectDeploymentId, workflowUuid);

        Map<String, Object> data = new HashMap<>();

        data.put("appInstanceId", appInstanceId);
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

        appAuditPublisher.publish(AppAuditEvent.APP_WORKFLOW_BOUND, appInstance.getAppId(), data);

        return toBindingDTO(appInstance, appWorkflow);
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')")
    public void unbindWorkflow(long appInstanceId, String workflowKey) {
        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        if (appWorkflowService.unbind(appInstanceId, workflowKey)) {
            appAuditPublisher.publish(
                AppAuditEvent.APP_WORKFLOW_UNBOUND, appInstance.getAppId(),
                Map.of("appInstanceId", appInstanceId, "workflowKey", workflowKey));
        }
    }

    private void checkProjectVisible(AppInstance appInstance, long projectDeploymentId) {
        Optional<Project> project = projectDeploymentService.fetchProjectDeployment(projectDeploymentId)
            .flatMap(projectDeployment -> projectService.fetchProject(projectDeployment.getProjectId()));

        if (project.isEmpty()
            || !getVisibleProjectIds(appInstance.getWorkspaceId(), List.of(project.get())).contains(project.get()
                .getId())) {

            throw new ConfigurationException(
                "Project deployment %s cannot be bound to app instance %s".formatted(
                    projectDeploymentId, appInstance.getId()),
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

    private AppWorkflowBindingDTO toBindingDTO(AppInstance appInstance, AppWorkflow binding) {
        Optional<CallableWorkflow> callableWorkflow = appCallableWorkflowResolver.resolve(
            appInstance.getWorkspaceId(), appInstance.getEnvironment(), binding.getProjectDeploymentId(),
            binding.getWorkflowUuid());
        String projectDeploymentName = projectDeploymentService.fetchProjectDeployment(
            binding.getProjectDeploymentId())
            .map(ProjectDeployment::getName)
            .orElse(null);

        return new AppWorkflowBindingDTO(
            Objects.requireNonNull(binding.getId()), appInstance.getId(), binding.getWorkflowKey(),
            binding.getProjectDeploymentId(), binding.getWorkflowUuid(), projectDeploymentName,
            callableWorkflow.map(CallableWorkflow::label)
                .orElse(null),
            callableWorkflow.isPresent());
    }
}
