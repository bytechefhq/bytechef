/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.ee.embedded.workflow.execution.facade.IntegrationWorkflowExecutionFacade;
import com.bytechef.ee.embedded.workflow.execution.web.rest.model.WorkflowExecutionBasicModel;
import com.bytechef.ee.embedded.workflow.execution.web.rest.model.WorkflowExecutionModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.workflow.execution.web.rest.model.TaskExecutionChildrenPageModel;
import com.bytechef.platform.workflow.execution.web.rest.model.TaskExecutionModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.workflow.execution.web.rest.WorkflowExecutionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class WorkflowExecutionApiController implements WorkflowExecutionApi {

    private final ConversionService conversionService;
    private final IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionApiController(
        ConversionService conversionService, IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade) {

        this.conversionService = conversionService;
        this.integrationWorkflowExecutionFacade = integrationWorkflowExecutionFacade;
    }

    @Override
    public ResponseEntity<WorkflowExecutionModel> getWorkflowExecution(Long id, Boolean lazy) {
        return ResponseEntity.ok(
            conversionService.convert(
                integrationWorkflowExecutionFacade.getWorkflowExecution(id, Boolean.TRUE.equals(lazy)),
                WorkflowExecutionModel.class));
    }

    @Override
    public ResponseEntity<TaskExecutionChildrenPageModel> getWorkflowExecutionTaskExecutionChildren(
        Long id, Long taskExecutionId, Integer pageNumber, Integer pageSize) {

        return ResponseEntity.ok(
            conversionService.convert(
                integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecutionChildren(
                    id, taskExecutionId, pageNumber, pageSize),
                TaskExecutionChildrenPageModel.class));
    }

    @Override
    public ResponseEntity<TaskExecutionModel> getWorkflowExecutionTaskExecution(Long id, Long taskExecutionId) {
        return ResponseEntity.ok(
            conversionService.convert(
                integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecution(id, taskExecutionId),
                TaskExecutionModel.class));
    }

    @Override
    public ResponseEntity<Page> getWorkflowExecutionsPage(
        Long environmentId, String jobStatus, OffsetDateTime jobStartDate, OffsetDateTime jobEndDate,
        Long integrationId, Long integrationInstanceConfigurationId, Long connectedUserId, String workflowId,
        Integer pageNumber) {

        return ResponseEntity.ok(
            integrationWorkflowExecutionFacade
                .getWorkflowExecutions(
                    environmentId, jobStatus == null ? null : Status.valueOf(jobStatus),
                    jobStartDate == null ? null : jobStartDate.toInstant(),
                    jobEndDate == null ? null : jobEndDate.toInstant(), integrationId,
                    integrationInstanceConfigurationId,
                    connectedUserId, workflowId, pageNumber)
                .map(workflowExecutionDTO -> conversionService.convert(
                    workflowExecutionDTO, WorkflowExecutionBasicModel.class)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(
        ConstraintViolationException constraintViolationException) {

        return ResponseEntity
            .of(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, constraintViolationException.getMessage()))
            .build();
    }
}
