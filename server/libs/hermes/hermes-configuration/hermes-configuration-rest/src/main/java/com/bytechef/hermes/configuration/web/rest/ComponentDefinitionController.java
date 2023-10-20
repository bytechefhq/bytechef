
/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.hermes.configuration.web.rest;

import com.bytechef.hermes.component.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.configuration.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.hermes.configuration.web.rest.model.ComponentDefinitionModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.core:}")
@ConditionalOnProperty(prefix = "bytechef", name = "coordinator.enabled", matchIfMissing = true)
public class ComponentDefinitionController implements ComponentDefinitionApi {

    private final ConversionService conversionService;
    private final ComponentDefinitionFacade componentDefinitionFacade;
    private final ComponentDefinitionService componentDefinitionService;

    @SuppressFBWarnings("EI")
    public ComponentDefinitionController(
        ConversionService conversionService, ComponentDefinitionFacade componentDefinitionFacade,
        ComponentDefinitionService componentDefinitionService) {

        this.componentDefinitionFacade = componentDefinitionFacade;
        this.conversionService = conversionService;
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public ResponseEntity<ComponentDefinitionModel> getComponentDefinition(
        String componentName, Integer componentVersion) {

        return ResponseEntity.ok(
            conversionService.convert(
                componentDefinitionService.getComponentDefinition(componentName, componentVersion),
                ComponentDefinitionModel.class));
    }

    @Override
    public ResponseEntity<List<ComponentDefinitionBasicModel>> getComponentDefinitionVersions(
        String componentName) {

        return ResponseEntity.ok(
            componentDefinitionService.getComponentDefinitionVersions(componentName)
                .stream()
                .map(componentDefinition -> conversionService.convert(
                    componentDefinition, ComponentDefinitionBasicModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<ComponentDefinitionBasicModel>> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean connectionInstances,
        Boolean triggerDefinitions, List<String> include) {

        return ResponseEntity.ok(
            componentDefinitionFacade.getComponentDefinitions(
                actionDefinitions, connectionDefinitions, connectionInstances, triggerDefinitions, include)
                .stream()
                .map(componentDefinition -> conversionService.convert(
                    componentDefinition, ComponentDefinitionBasicModel.class))
                .toList());
    }
}
