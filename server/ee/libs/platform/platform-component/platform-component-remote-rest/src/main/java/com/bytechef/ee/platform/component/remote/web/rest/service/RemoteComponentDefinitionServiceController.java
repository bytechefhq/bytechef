/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.web.rest.service;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
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
        @PathVariable("name") String name, @PathVariable("version") int version) {

        return ResponseEntity.ok(componentDefinitionService.getComponentDefinition(name, version));
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
        @RequestParam(value = "triggerDefinitions", required = false) Boolean triggerDefinitions,
        @RequestParam(value = "include", required = false) List<String> include,
        @RequestParam(value = "platformType", required = false) PlatformType platformType) {

        return ResponseEntity.ok(
            actionDefinitions == null && connectionDefinitions == null && triggerDefinitions == null
                && include == null
                    ? componentDefinitionService.getComponentDefinitions()
                    : componentDefinitionService.getComponentDefinitions(
                        actionDefinitions, connectionDefinitions, triggerDefinitions, include, platformType));
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
