/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.ee.embedded.execution.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExecutionErrorModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.IntegrationReferenceModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.TaskExecutionModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.WorkflowExecutionBasicModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.WorkflowExecutionModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.WorkflowExecutionStatusModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.WorkflowReferenceModel;
import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.ee.embedded.workflow.execution.facade.IntegrationWorkflowExecutionFacade;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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
@RestController("com.bytechef.ee.embedded.execution.public_.web.rest.WorkflowExecutionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class WorkflowExecutionApiController implements WorkflowExecutionApi {

    private final EnvironmentService environmentService;
    private final IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionApiController(
        EnvironmentService environmentService, IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade) {

        this.environmentService = environmentService;
        this.integrationWorkflowExecutionFacade = integrationWorkflowExecutionFacade;
    }

    @Override
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public ResponseEntity<Page> getWorkflowExecutionsPage(
        String externalUserId, @Nullable EnvironmentModel xEnvironment, @Nullable WorkflowExecutionStatusModel status,
        @Nullable OffsetDateTime startDate, @Nullable OffsetDateTime endDate,
        @Nullable Long integrationInstanceConfigurationId, Integer pageNumber) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        Page<WorkflowExecutionDTO> workflowExecutionsPage =
            integrationWorkflowExecutionFacade.getConnectedUserWorkflowExecutions(
                externalUserId, getEnvironmentId(xEnvironment),
                status == null ? null : Job.Status.valueOf(status.name()), toInstant(startDate), toInstant(endDate),
                integrationInstanceConfigurationId, pageNumber == null ? 0 : pageNumber);

        return ResponseEntity.ok(workflowExecutionsPage.map(this::toWorkflowExecutionBasicModel));
    }

    @Override
    public ResponseEntity<WorkflowExecutionModel> getWorkflowExecution(String externalUserId, Long id) {
        SecurityUtils.checkCurrentUserLogin(externalUserId);

        return ResponseEntity.ok(
            toWorkflowExecutionModel(
                integrationWorkflowExecutionFacade.getConnectedUserWorkflowExecution(
                    externalUserId, getEnvironmentId(null), id)));
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private long getEnvironmentId(@Nullable EnvironmentModel xEnvironment) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        if (authentication instanceof AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken) {
            return apiKeyAuthenticationToken.getEnvironmentId();
        }

        Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());

        return environment.ordinal();
    }

    private WorkflowExecutionBasicModel toWorkflowExecutionBasicModel(WorkflowExecutionDTO workflowExecutionDTO) {
        JobDTO job = workflowExecutionDTO.job();

        WorkflowExecutionBasicModel workflowExecutionBasicModel = new WorkflowExecutionBasicModel()
            .id(workflowExecutionDTO.id())
            .integration(toIntegrationReferenceModel(workflowExecutionDTO))
            .workflow(toWorkflowReferenceModel(workflowExecutionDTO));

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
            .integration(toIntegrationReferenceModel(workflowExecutionDTO))
            .workflow(toWorkflowReferenceModel(workflowExecutionDTO));

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

    private @Nullable IntegrationReferenceModel toIntegrationReferenceModel(
        WorkflowExecutionDTO workflowExecutionDTO) {

        if (workflowExecutionDTO.integration() == null) {
            return null;
        }

        return new IntegrationReferenceModel()
            .id(
                workflowExecutionDTO.integration()
                    .getId())
            .componentName(
                workflowExecutionDTO.integration()
                    .getComponentName());
    }

    private @Nullable WorkflowReferenceModel toWorkflowReferenceModel(WorkflowExecutionDTO workflowExecutionDTO) {
        if (workflowExecutionDTO.workflow() == null) {
            return null;
        }

        return new WorkflowReferenceModel().label(
            workflowExecutionDTO.workflow()
                .getLabel());
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
