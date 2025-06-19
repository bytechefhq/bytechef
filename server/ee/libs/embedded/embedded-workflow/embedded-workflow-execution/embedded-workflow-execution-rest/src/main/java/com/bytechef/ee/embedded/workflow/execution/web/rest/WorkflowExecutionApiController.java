/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.ee.embedded.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.workflow.execution.facade.IntegrationWorkflowExecutionFacade;
import com.bytechef.ee.embedded.workflow.execution.web.rest.model.WorkflowExecutionBasicModel;
import com.bytechef.ee.embedded.workflow.execution.web.rest.model.WorkflowExecutionModel;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.OffsetDateTime;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<WorkflowExecutionModel> getWorkflowExecution(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(
                integrationWorkflowExecutionFacade.getWorkflowExecution(id), WorkflowExecutionModel.class));
    }

    @Override
    public ResponseEntity<Page> getWorkflowExecutionsPage(
        EnvironmentModel environment, String jobStatus, OffsetDateTime jobStartDate, OffsetDateTime jobEndDate,
        Long projectId, Long integrationInstanceConfigurationId, String workflowId, Integer pageNumber) {

        return ResponseEntity.ok(
            integrationWorkflowExecutionFacade
                .getWorkflowExecutions(
                    environment == null ? null : Environment.valueOf(environment.name()),
                    jobStatus == null ? null : Status.valueOf(jobStatus),
                    jobStartDate == null ? null : jobStartDate.toInstant(),
                    jobEndDate == null ? null : jobEndDate.toInstant(), projectId, integrationInstanceConfigurationId,
                    workflowId, pageNumber)
                .map(workflowExecutionDTO -> conversionService.convert(
                    workflowExecutionDTO, WorkflowExecutionBasicModel.class)));
    }
}
