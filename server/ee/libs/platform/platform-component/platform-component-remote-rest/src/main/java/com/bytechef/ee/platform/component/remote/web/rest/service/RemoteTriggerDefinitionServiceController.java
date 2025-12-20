/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.web.rest.service;

import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
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
@RequestMapping("/remote/trigger-definition-service")
public class RemoteTriggerDefinitionServiceController {

    private final TriggerDefinitionService triggerDefinitionService;

    public RemoteTriggerDefinitionServiceController(TriggerDefinitionService triggerDefinitionService) {
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-workflow-node-description",
        consumes = {
            "application/json"
        })
    public ResponseEntity<String> executeEditorDescription(
        @Valid @RequestBody WorkflowNodeDescriptionRequest workflowNodeDescriptionRequest) {

        return ResponseEntity.ok(
            triggerDefinitionService.executeWorkflowNodeDescription(
                workflowNodeDescriptionRequest.componentName, workflowNodeDescriptionRequest.componentVersion,
                workflowNodeDescriptionRequest.triggerName, workflowNodeDescriptionRequest.inputParameters));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-trigger-definition/{componentName}/{componentVersion}/{triggerName}",
        produces = {
            "application/json"
        })
    public ResponseEntity<TriggerDefinition> getTriggerDefinition(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion, @PathVariable("triggerName") String triggerName) {

        return ResponseEntity.ok(
            triggerDefinitionService.getTriggerDefinition(componentName, componentVersion, triggerName));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-trigger-definitions/{componentName}/{componentVersion}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<TriggerDefinition>> getTriggerDefinitions(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion) {

        return ResponseEntity.ok(triggerDefinitionService.getTriggerDefinitions(componentName, componentVersion));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-webhook-trigger-flags/{componentName}/{componentVersion}/{triggerName}",
        produces = {
            "application/json"
        })
    public ResponseEntity<WebhookTriggerFlags> getWebhookTriggerFlags(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion,
        @PathVariable("triggerName") String triggerName) {

        return ResponseEntity.ok(
            triggerDefinitionService.getWebhookTriggerFlags(componentName, componentVersion, triggerName));
    }

    @SuppressFBWarnings("EI")
    public record WorkflowNodeDescriptionRequest(
        String componentName, String triggerName, int componentVersion, Map<String, ?> inputParameters) {
    }
}
