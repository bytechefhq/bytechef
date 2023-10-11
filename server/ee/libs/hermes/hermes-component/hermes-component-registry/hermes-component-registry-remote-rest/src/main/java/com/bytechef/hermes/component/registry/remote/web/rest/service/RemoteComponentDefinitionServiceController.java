
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.component.registry.remote.web.rest.service;

import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/component-definition-service")
public class RemoteComponentDefinitionServiceController {

    private final ComponentDefinitionService componentDefinitionService;

    public RemoteComponentDefinitionServiceController(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-component-definition/{name}/{version}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ComponentDefinition> getComponentDefinition(
        @PathVariable("name") String name, @PathVariable("version") Integer version) {

        return ResponseEntity.ok(componentDefinitionService.getComponentDefinition(name, version));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-component-definitions",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ComponentDefinition>> getComponentDefinitions() {
        return ResponseEntity.ok(componentDefinitionService.getComponentDefinitions());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-component-definition-versions/{name}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ComponentDefinition>> getComponentDefinitionVersions(@PathVariable("name") String name) {
        return ResponseEntity.ok(componentDefinitionService.getComponentDefinitionVersions(name));
    }
}
