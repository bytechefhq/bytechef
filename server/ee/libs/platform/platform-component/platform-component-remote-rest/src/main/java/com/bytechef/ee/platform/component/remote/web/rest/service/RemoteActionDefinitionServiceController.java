/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.web.rest.service;

import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/action-definition-service")
public class RemoteActionDefinitionServiceController {

    private final ActionDefinitionService actionDefinitionService;

    public RemoteActionDefinitionServiceController(ActionDefinitionService actionDefinitionService) {
        this.actionDefinitionService = actionDefinitionService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-action-definition/{componentName}/{componentVersion}/{actionName}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ActionDefinition> getActionDefinition(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion, @PathVariable("actionName") String actionName) {

        return ResponseEntity
            .ok(actionDefinitionService.getActionDefinition(componentName, componentVersion, actionName));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-action-definitions/{componentName}/{componentVersion}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ActionDefinition>> getActionDefinitions(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion) {

        return ResponseEntity.ok(actionDefinitionService.getActionDefinitions(componentName, componentVersion));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-workflow/node-description",
        consumes = {
            "application/json"
        })
    public ResponseEntity<String> executeWorkflowNodeDescription(
        @Valid @RequestBody WorkflowNodeDescriptionRequest workflowNodeDescriptionRequest) {

        return ResponseEntity.ok(actionDefinitionService.executeWorkflowNodeDescription(
            workflowNodeDescriptionRequest.componentName, workflowNodeDescriptionRequest.componentVersion,
            workflowNodeDescriptionRequest.actionName, workflowNodeDescriptionRequest.inputParameters));
    }

    @SuppressFBWarnings("EI")
    public record WorkflowNodeDescriptionRequest(
        String componentName, int componentVersion, String actionName, Map<String, Object> inputParameters) {
    }
}
