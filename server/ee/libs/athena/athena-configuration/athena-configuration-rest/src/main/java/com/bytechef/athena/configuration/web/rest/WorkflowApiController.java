/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.web.rest;

import com.bytechef.athena.configuration.constant.IntegrationConstants;
import com.bytechef.athena.configuration.facade.IntegrationFacade;
import com.bytechef.athena.configuration.web.rest.model.WorkflowModel;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.athena.configuration.web.rest.workflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}")
public class WorkflowApiController implements WorkflowApi {

    private final IntegrationFacade integrationFacade;
    private final ConversionService conversionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public WorkflowApiController(
        IntegrationFacade integrationFacade, ConversionService conversionService, WorkflowService workflowService) {
        this.integrationFacade = integrationFacade;

        this.conversionService = conversionService;
        this.workflowService = workflowService;
    }

    @Override
    public ResponseEntity<Void> deleteIntegrationWorkflow(Long id, String workflowId) {
        integrationFacade.deleteWorkflow(id, workflowId);

        return ResponseEntity
            .noContent()
            .build();
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getIntegrationWorkflows(Long id) {
        return ResponseEntity.ok(
            integrationFacade.getIntegrationWorkflows(id)
                .stream()
                .map(workflow -> conversionService.convert(workflow, WorkflowModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<WorkflowModel> getWorkflow(String id) {
        return ResponseEntity.ok(
            Validate.notNull(
                conversionService.convert(workflowService.getWorkflow(id), WorkflowModel.class), "workflowModel")
                .definition(null));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getWorkflows() {
        List<WorkflowModel> workflowModels = new ArrayList<>();

        for (Workflow workflow : workflowService.getWorkflows(IntegrationConstants.INTEGRATION_TYPE)) {
            workflowModels.add(
                Validate.notNull(conversionService.convert(workflow, WorkflowModel.class), "workflowModel")
                    .definition(null));
        }

        return ResponseEntity.ok(workflowModels);
    }

    @Override
    public ResponseEntity<WorkflowModel> updateWorkflow(String id, WorkflowModel workflowModel) {
        return ResponseEntity.ok(
            conversionService.convert(workflowService.update(
                id, workflowModel.getDefinition()), WorkflowModel.class));
    }
}
