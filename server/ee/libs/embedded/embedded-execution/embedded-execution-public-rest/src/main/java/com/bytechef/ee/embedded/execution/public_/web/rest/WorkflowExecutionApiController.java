/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.execution.domain.Job;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read-only workflow-execution endpoints of the embedded API. List responses never carry execution data — the
 * single-execution endpoint returns the full detail including inputs, outputs, error and task executions.
 *
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

    @SuppressFBWarnings("EI2")
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

        Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());

        Page<WorkflowExecutionDTO> workflowExecutionsPage =
            integrationWorkflowExecutionFacade.getConnectedUserWorkflowExecutions(
                externalUserId, environment.ordinal(), status == null ? null : Job.Status.valueOf(status.name()),
                toInstant(startDate), toInstant(endDate), integrationInstanceConfigurationId, pageNumber);

        return ResponseEntity.ok(workflowExecutionsPage.map(this::toBasicModel));
    }

    @Override
    public ResponseEntity<WorkflowExecutionModel> getWorkflowExecution(String externalUserId, Long id) {
        SecurityUtils.checkCurrentUserLogin(externalUserId);

        return ResponseEntity.ok(
            toModel(integrationWorkflowExecutionFacade.getConnectedUserWorkflowExecution(externalUserId, id)));
    }

    private WorkflowExecutionBasicModel toBasicModel(WorkflowExecutionDTO workflowExecutionDTO) {
        JobDTO job = workflowExecutionDTO.job();

        WorkflowExecutionBasicModel workflowExecutionBasicModel = new WorkflowExecutionBasicModel()
            .id(workflowExecutionDTO.id())
            .integration(toIntegrationReferenceModel(workflowExecutionDTO))
            .workflow(toWorkflowReferenceModel(workflowExecutionDTO));

        if (job != null) {
            workflowExecutionBasicModel
                .status(toStatusModel(job.status()))
                .startDate(toOffsetDateTime(job.startDate()))
                .endDate(toOffsetDateTime(job.endDate()))
                .error(toErrorModel(job.error()));
        }

        return workflowExecutionBasicModel;
    }

    @SuppressWarnings("unchecked")
    private WorkflowExecutionModel toModel(WorkflowExecutionDTO workflowExecutionDTO) {
        JobDTO job = workflowExecutionDTO.job();

        WorkflowExecutionModel workflowExecutionModel = new WorkflowExecutionModel()
            .id(workflowExecutionDTO.id())
            .integration(toIntegrationReferenceModel(workflowExecutionDTO))
            .workflow(toWorkflowReferenceModel(workflowExecutionDTO));

        if (job != null) {
            workflowExecutionModel
                .status(toStatusModel(job.status()))
                .startDate(toOffsetDateTime(job.startDate()))
                .endDate(toOffsetDateTime(job.endDate()))
                .error(toErrorModel(job.error()))
                .inputs((Map<String, Object>) job.inputs())
                .outputs((Map<String, Object>) job.outputs());

            List<TaskExecutionDTO> taskExecutions = job.taskExecutions();

            if (taskExecutions != null) {
                workflowExecutionModel.taskExecutions(
                    taskExecutions.stream()
                        .map(this::toTaskExecutionModel)
                        .toList());
            }
        }

        return workflowExecutionModel;
    }

    @SuppressWarnings("unchecked")
    private TaskExecutionModel toTaskExecutionModel(TaskExecutionDTO taskExecutionDTO) {
        return new TaskExecutionModel()
            .id(taskExecutionDTO.id() == null ? null : String.valueOf(taskExecutionDTO.id()))
            .name(
                taskExecutionDTO.workflowTask() == null
                    ? null
                    : taskExecutionDTO.workflowTask()
                        .getName())
            .title(taskExecutionDTO.title())
            .type(taskExecutionDTO.type())
            .status(
                taskExecutionDTO.status() == null
                    ? null
                    : TaskExecutionModel.StatusEnum.valueOf(
                        taskExecutionDTO.status()
                            .name()))
            .startDate(toOffsetDateTime(taskExecutionDTO.startDate()))
            .endDate(toOffsetDateTime(taskExecutionDTO.endDate()))
            .error(toErrorModel(taskExecutionDTO.error()))
            .input((Map<String, Object>) taskExecutionDTO.input())
            .output(taskExecutionDTO.output());
    }

    private IntegrationReferenceModel toIntegrationReferenceModel(WorkflowExecutionDTO workflowExecutionDTO) {
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

    private WorkflowReferenceModel toWorkflowReferenceModel(WorkflowExecutionDTO workflowExecutionDTO) {
        if (workflowExecutionDTO.workflow() == null) {
            return null;
        }

        return new WorkflowReferenceModel()
            .label(
                workflowExecutionDTO.workflow()
                    .getLabel());
    }

    private ExecutionErrorModel toErrorModel(@Nullable ExecutionError executionError) {
        if (executionError == null) {
            return null;
        }

        return new ExecutionErrorModel()
            .message(executionError.getMessage())
            .stackTrace(executionError.getStackTrace());
    }

    private WorkflowExecutionStatusModel toStatusModel(Job.Status status) {
        return status == null ? null : WorkflowExecutionStatusModel.valueOf(status.name());
    }

    private @Nullable Instant toInstant(@Nullable OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }

    private @Nullable OffsetDateTime toOffsetDateTime(@Nullable Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
