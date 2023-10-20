
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.component.registry.remote.web.rest.facade;

import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.component.registry.facade.ComponentDefinitionFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/remote/component-definition-facade")
public class RemoteComponentDefinitionFacadeController {

    private final ComponentDefinitionFacade componentDefinitionFacade;

    public RemoteComponentDefinitionFacadeController(ComponentDefinitionFacade componentDefinitionFacade) {
        this.componentDefinitionFacade = componentDefinitionFacade;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-component-definitions",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ComponentDefinition>> getComponentDefinitions(
        @RequestParam(value = "actionDefinitions", required = false) Boolean actionDefinitions,
        @RequestParam(value = "connectionDefinitions", required = false) Boolean connectionDefinitions,
        @RequestParam(value = "connectionInstances", required = false) Boolean connectionInstances,
        @RequestParam(value = "triggerDefinitions", required = false) Boolean triggerDefinitions,
        @RequestParam(value = "include", required = false) List<String> include) {

        return ResponseEntity.ok(
            componentDefinitionFacade.getComponentDefinitions(
                actionDefinitions, connectionDefinitions, connectionInstances, triggerDefinitions, include));
    }
}
