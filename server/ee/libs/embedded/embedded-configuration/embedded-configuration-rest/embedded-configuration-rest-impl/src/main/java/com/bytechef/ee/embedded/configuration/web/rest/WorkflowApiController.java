/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.WorkflowModel;
import com.bytechef.platform.configuration.web.rest.AbstractWorkflowApiController;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.web.rest.WorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
public class WorkflowApiController extends AbstractWorkflowApiController implements WorkflowApi {

    private final ConversionService conversionService;
    private final IntegrationFacade integrationFacade;

    @SuppressFBWarnings("EI2")
    public WorkflowApiController(
        ConversionService conversionService, IntegrationFacade integrationFacade, WorkflowService workflowService) {

        super(workflowService);

        this.conversionService = conversionService;
        this.integrationFacade = integrationFacade;
    }

    @Override
    public ResponseEntity<Void> deleteWorkflow(String workflowId) {
        integrationFacade.deleteWorkflow(workflowId);

        return ResponseEntity.noContent()
            .build();
    }

    @GetMapping("/workflows/{id}/export")
    @ResponseBody
    public ResponseEntity<Resource> exportWorkflow(@PathVariable("id") String id) {
        return doExportWorkflow(id);
    }

    @Override
    public ResponseEntity<WorkflowModel> getIntegrationWorkflow(Long integrationWorkflowId) {
        return ResponseEntity.ok(
            conversionService.convert(
                integrationFacade.getIntegrationWorkflow(integrationWorkflowId), WorkflowModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getIntegrationWorkflows(Long id) {
        return ResponseEntity.ok(
            CollectionUtils.map(
                integrationFacade.getIntegrationWorkflows(id),
                workflow -> conversionService.convert(workflow, WorkflowModel.class)));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getIntegrationVersionWorkflows(
        Long id, Integer integrationVersion, Boolean includeAllFields) {

        return ResponseEntity.ok(
            CollectionUtils.map(
                integrationFacade.getIntegrationVersionWorkflows(id, integrationVersion, includeAllFields),
                workflow -> conversionService.convert(workflow, WorkflowModel.class)));
    }

    @Override
    public ResponseEntity<WorkflowModel> getWorkflow(String id) {
        // TODO Add check regarding platform type

        return ResponseEntity.ok(
            conversionService.convert(integrationFacade.getIntegrationWorkflow(id), WorkflowModel.class));
    }

    @Override
    public ResponseEntity<Void> updateWorkflow(String id, WorkflowModel workflowModel) {
        // TODO Add check regarding platform type

        integrationFacade.updateWorkflow(id, workflowModel.getDefinition(),
            Objects.requireNonNull(workflowModel.getVersion()));

        return ResponseEntity.noContent()
            .build();
    }
}
