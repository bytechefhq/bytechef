/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.constant.ModeType;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.web.rest.ComponentDefinitionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
public class ComponentDefinitionApiController implements ComponentDefinitionApi {

    private final ComponentDefinitionService componentDefinitionService;
    private final ConversionService conversionService;

    public ComponentDefinitionApiController(
        ComponentDefinitionService componentDefinitionService, ConversionService conversionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<List<ComponentDefinitionBasicModel>> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions, List<String> include) {

        return ResponseEntity.ok(
            componentDefinitionService
                .getComponentDefinitions(
                    actionDefinitions, connectionDefinitions, triggerDefinitions, include, ModeType.EMBEDDED)
                .stream()
                .map(componentDefinition -> conversionService.convert(
                    componentDefinition, ComponentDefinitionBasicModel.class))
                .toList());
    }
}
