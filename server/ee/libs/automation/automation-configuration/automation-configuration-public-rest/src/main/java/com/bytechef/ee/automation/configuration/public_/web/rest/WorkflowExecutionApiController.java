/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.WorkspaceApiKeyService;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.ee.automation.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.ExecutionErrorModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.ProjectDeploymentReferenceModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.ProjectReferenceModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.TaskExecutionModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowExecutionBasicModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowExecutionModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowExecutionStatusModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowReferenceModel;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.configuration.public_.web.rest.WorkflowExecutionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class WorkflowExecutionApiController implements WorkflowExecutionApi {

    private static final String ACCESS_DENIED_MESSAGE = "Access is denied";

    private final ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;
    private final WorkspaceApiKeyService workspaceApiKeyService;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionApiController(
        ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade, WorkspaceApiKeyService workspaceApiKeyService) {

        this.projectWorkflowExecutionFacade = projectWorkflowExecutionFacade;
        this.workspaceApiKeyService = workspaceApiKeyService;
    }

    @Override
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public ResponseEntity<Page> getWorkflowExecutionsPage(
        Long workspaceId, @Nullable EnvironmentModel xEnvironment, @Nullable WorkflowExecutionStatusModel status,
        @Nullable OffsetDateTime startDate, @Nullable OffsetDateTime endDate, @Nullable Long projectId,
        @Nullable Long projectDeploymentId, @Nullable String workflowId, Integer pageNumber) {

        AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken = getApiKeyAuthenticationToken();

        if (!Objects.equals(getApiKeyWorkspaceId(apiKeyAuthenticationToken), workspaceId)) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }

        Page<WorkflowExecutionDTO> workflowExecutionsPage = projectWorkflowExecutionFacade
            .getWorkspaceWorkflowExecutions(
                workspaceId, apiKeyAuthenticationToken.getEnvironmentId(),
                status == null ? null : Job.Status.valueOf(status.name()), toInstant(startDate), toInstant(endDate),
                projectId, projectDeploymentId, workflowId, pageNumber == null ? 0 : pageNumber);

        return ResponseEntity.ok(workflowExecutionsPage.map(this::toWorkflowExecutionBasicModel));
    }

    @Override
    public ResponseEntity<WorkflowExecutionModel> getWorkflowExecution(Long id) {
        AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken = getApiKeyAuthenticationToken();

        return ResponseEntity.ok(
            toWorkflowExecutionModel(
                projectWorkflowExecutionFacade.getWorkspaceWorkflowExecution(
                    getApiKeyWorkspaceId(apiKeyAuthenticationToken), apiKeyAuthenticationToken.getEnvironmentId(),
                    id)));
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private static AbstractApiKeyAuthenticationToken getApiKeyAuthenticationToken() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        if (authentication instanceof AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken &&
            apiKeyAuthenticationToken.getPlatformType() == PlatformType.AUTOMATION &&
            apiKeyAuthenticationToken.getApiKeyId() != null) {

            return apiKeyAuthenticationToken;
        }

        throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
    }

    private long getApiKeyWorkspaceId(AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken) {
        return workspaceApiKeyService
            .fetchApiKeyWorkspaceId(Objects.requireNonNull(apiKeyAuthenticationToken.getApiKeyId()))
            .orElseThrow(() -> new AccessDeniedException(ACCESS_DENIED_MESSAGE));
    }

    private WorkflowExecutionBasicModel toWorkflowExecutionBasicModel(WorkflowExecutionDTO workflowExecutionDTO) {
        JobDTO job = workflowExecutionDTO.job();

        WorkflowExecutionBasicModel workflowExecutionBasicModel = new WorkflowExecutionBasicModel()
            .id(workflowExecutionDTO.id())
            .project(toProjectReferenceModel(workflowExecutionDTO.project()))
            .projectDeployment(toProjectDeploymentReferenceModel(workflowExecutionDTO.projectDeployment()))
            .workflow(toWorkflowReferenceModel(workflowExecutionDTO.workflow()));

        if (job != null) {
            workflowExecutionBasicModel
                .status(toWorkflowExecutionStatusModel(job.status()))
                .startDate(toOffsetDateTime(job.startDate()))
                .endDate(toOffsetDateTime(job.endDate()))
                .error(toExecutionErrorModel(job.error()));
        }

        return workflowExecutionBasicModel;
    }

    @SuppressWarnings("unchecked")
    private WorkflowExecutionModel toWorkflowExecutionModel(WorkflowExecutionDTO workflowExecutionDTO) {
        JobDTO job = workflowExecutionDTO.job();

        WorkflowExecutionModel workflowExecutionModel = new WorkflowExecutionModel()
            .id(workflowExecutionDTO.id())
            .project(toProjectReferenceModel(workflowExecutionDTO.project()))
            .projectDeployment(toProjectDeploymentReferenceModel(workflowExecutionDTO.projectDeployment()))
            .workflow(toWorkflowReferenceModel(workflowExecutionDTO.workflow()));

        if (job != null) {
            workflowExecutionModel
                .status(toWorkflowExecutionStatusModel(job.status()))
                .startDate(toOffsetDateTime(job.startDate()))
                .endDate(toOffsetDateTime(job.endDate()))
                .error(toExecutionErrorModel(job.error()))
                .inputs((Map<String, Object>) job.inputs())
                .outputs((Map<String, Object>) job.outputs());

            List<TaskExecutionDTO> taskExecutionDTOs = job.taskExecutions();

            if (taskExecutionDTOs != null) {
                workflowExecutionModel.taskExecutions(
                    taskExecutionDTOs.stream()
                        .map(this::toTaskExecutionModel)
                        .toList());
            }
        }

        return workflowExecutionModel;
    }

    @SuppressWarnings("unchecked")
    private TaskExecutionModel toTaskExecutionModel(TaskExecutionDTO taskExecutionDTO) {
        TaskExecutionModel taskExecutionModel = new TaskExecutionModel()
            .id(taskExecutionDTO.id() == null ? null : String.valueOf(taskExecutionDTO.id()))
            .title(taskExecutionDTO.title())
            .type(taskExecutionDTO.type())
            .startDate(toOffsetDateTime(taskExecutionDTO.startDate()))
            .endDate(toOffsetDateTime(taskExecutionDTO.endDate()))
            .error(toExecutionErrorModel(taskExecutionDTO.error()))
            .input((Map<String, Object>) taskExecutionDTO.input())
            .output(taskExecutionDTO.output());

        if (taskExecutionDTO.workflowTask() != null) {
            taskExecutionModel.name(
                taskExecutionDTO.workflowTask()
                    .getName());
        }

        if (taskExecutionDTO.status() != null) {
            taskExecutionModel.status(
                TaskExecutionModel.StatusEnum.valueOf(
                    taskExecutionDTO.status()
                        .name()));
        }

        return taskExecutionModel;
    }

    private static @Nullable ProjectReferenceModel toProjectReferenceModel(@Nullable Project project) {
        if (project == null) {
            return null;
        }

        return new ProjectReferenceModel()
            .id(project.getId())
            .name(project.getName());
    }

    private static @Nullable ProjectDeploymentReferenceModel toProjectDeploymentReferenceModel(
        @Nullable ProjectDeployment projectDeployment) {

        if (projectDeployment == null) {
            return null;
        }

        return new ProjectDeploymentReferenceModel()
            .id(projectDeployment.getId())
            .name(projectDeployment.getName())
            .projectVersion(projectDeployment.getProjectVersion())
            .environmentId(projectDeployment.getEnvironmentId())
            .enabled(projectDeployment.isEnabled());
    }

    private static @Nullable WorkflowReferenceModel toWorkflowReferenceModel(@Nullable Workflow workflow) {
        if (workflow == null) {
            return null;
        }

        return new WorkflowReferenceModel()
            .id(workflow.getId())
            .label(workflow.getLabel());
    }

    private static @Nullable ExecutionErrorModel toExecutionErrorModel(@Nullable ExecutionError executionError) {
        if (executionError == null) {
            return null;
        }

        return new ExecutionErrorModel()
            .message(executionError.getMessage())
            .stackTrace(executionError.getStackTrace());
    }

    private static @Nullable WorkflowExecutionStatusModel toWorkflowExecutionStatusModel(Job.@Nullable Status status) {
        return status == null ? null : WorkflowExecutionStatusModel.valueOf(status.name());
    }

    private static @Nullable Instant toInstant(@Nullable OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }

    private static @Nullable OffsetDateTime toOffsetDateTime(@Nullable Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
