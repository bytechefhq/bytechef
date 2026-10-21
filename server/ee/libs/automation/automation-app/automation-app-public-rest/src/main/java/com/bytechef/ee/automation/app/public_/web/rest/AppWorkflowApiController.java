/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.dto.AppWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowRunDTO;
import com.bytechef.ee.automation.app.facade.AppWorkflowExecutionFacade;
import com.bytechef.ee.automation.app.public_.web.rest.model.AppWorkflowModel;
import com.bytechef.ee.automation.app.public_.web.rest.model.ExecuteWorkflowRequestModel;
import com.bytechef.ee.automation.app.public_.web.rest.model.WorkflowRunErrorModel;
import com.bytechef.ee.automation.app.public_.web.rest.model.WorkflowRunModel;
import com.bytechef.ee.automation.app.public_.web.rest.model.WorkflowRunStatusModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The workflow endpoints an app calls with its own secret key. {@link AppWorkflowExecutionFacade} owns the run and the
 * wait; this controller maps its results to the response shape and answers 200 for a finished run and 202 for one still
 * going or waiting on an approval.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppWorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.app:/api/app}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppWorkflowApiController extends AbstractAppApiController implements AppWorkflowApi {

    private final AppWorkflowExecutionFacade appWorkflowExecutionFacade;

    @SuppressFBWarnings("EI2")
    public AppWorkflowApiController(AppWorkflowExecutionFacade appWorkflowExecutionFacade) {
        this.appWorkflowExecutionFacade = appWorkflowExecutionFacade;
    }

    @Override
    public ResponseEntity<WorkflowRunModel> executeWorkflow(
        String workflowKey, ExecuteWorkflowRequestModel executeWorkflowRequestModel) {

        AppWorkflowRunDTO appWorkflowRunDTO = appWorkflowExecutionFacade.executeWorkflow(
            workflowKey, executeWorkflowRequestModel.getInput(), executeWorkflowRequestModel.getWaitSeconds());

        return ResponseEntity.status(appWorkflowRunDTO.isFinished() ? HttpStatus.OK : HttpStatus.ACCEPTED)
            .body(toModel(appWorkflowRunDTO));
    }

    @Override
    public ResponseEntity<WorkflowRunModel> getWorkflowRun(String runId) {
        return ResponseEntity.ok(toModel(appWorkflowExecutionFacade.getWorkflowRun(runId)));
    }

    @Override
    public ResponseEntity<List<AppWorkflowModel>> getWorkflows() {
        return ResponseEntity.ok(
            appWorkflowExecutionFacade.getWorkflows()
                .stream()
                .map(AppWorkflowApiController::toModel)
                .toList());
    }

    private static AppWorkflowModel toModel(AppWorkflowDTO appWorkflowDTO) {
        AppWorkflowModel appWorkflowModel = new AppWorkflowModel();

        appWorkflowModel.setAvailable(appWorkflowDTO.available());
        appWorkflowModel.setDescription(JsonNullable.of(appWorkflowDTO.description()));
        appWorkflowModel.setInputSchema(JsonNullable.of(appWorkflowDTO.inputSchema()));
        appWorkflowModel.setName(appWorkflowDTO.name());
        appWorkflowModel.setOutputSchema(JsonNullable.of(appWorkflowDTO.outputSchema()));
        appWorkflowModel.setWorkflowKey(appWorkflowDTO.workflowKey());

        return appWorkflowModel;
    }

    private static WorkflowRunModel toModel(AppWorkflowRunDTO appWorkflowRunDTO) {
        WorkflowRunModel workflowRunModel = new WorkflowRunModel();

        workflowRunModel.setApprovalFormUrl(JsonNullable.of(appWorkflowRunDTO.approvalFormUrl()));
        workflowRunModel.setResult(JsonNullable.of(appWorkflowRunDTO.result()));
        workflowRunModel.setRunId(appWorkflowRunDTO.runId());
        workflowRunModel.setStatus(WorkflowRunStatusModel.fromValue(appWorkflowRunDTO.status()
            .name()));
        workflowRunModel.setWorkflowKey(appWorkflowRunDTO.workflowKey());

        WorkflowRunErrorModel workflowRunErrorModel = null;

        if (appWorkflowRunDTO.errorMessage() != null) {
            workflowRunErrorModel = new WorkflowRunErrorModel();

            workflowRunErrorModel.setMessage(appWorkflowRunDTO.errorMessage());
        }

        workflowRunModel.setError(JsonNullable.of(workflowRunErrorModel));

        return workflowRunModel;
    }
}
