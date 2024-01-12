/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.component.registry.remote.web.rest.service;

import com.bytechef.platform.component.registry.domain.ActionDefinition;
import com.bytechef.platform.component.registry.service.ActionDefinitionService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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
}
