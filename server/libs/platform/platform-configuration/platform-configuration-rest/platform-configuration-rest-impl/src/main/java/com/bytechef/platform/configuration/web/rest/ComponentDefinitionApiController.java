/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.platform.component.definition.DataStreamComponentDefinition.ComponentType;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.ComponentDefinitionModel;
import com.bytechef.platform.configuration.web.rest.model.UnifiedApiCategoryModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class ComponentDefinitionApiController implements ComponentDefinitionApi {

    private final ConversionService conversionService;
    private final ComponentDefinitionService componentDefinitionService;

    @SuppressFBWarnings("EI")
    public ComponentDefinitionApiController(
        ConversionService conversionService, ComponentDefinitionService componentDefinitionService) {

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
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions, List<String> include) {

        return ResponseEntity.ok(
            componentDefinitionService.getComponentDefinitions(
                actionDefinitions, connectionDefinitions, triggerDefinitions, include)
                .stream()
                .map(componentDefinition -> conversionService.convert(
                    componentDefinition, ComponentDefinitionBasicModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<ComponentDefinitionBasicModel>> getDataStreamComponentDefinitions(String componentType) {
        return ResponseEntity.ok(
            componentDefinitionService
                .getDataStreamComponentDefinitions(ComponentType.valueOf(componentType))
                .stream()
                .map(componentDefinition -> conversionService.convert(
                    componentDefinition, ComponentDefinitionBasicModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<ComponentDefinitionBasicModel>> getUnifiedApiComponentDefinitions(
        UnifiedApiCategoryModel category) {

        return ResponseEntity.ok(
            componentDefinitionService
                .getUnifiedApiComponentDefinitions(UnifiedApiDefinition.Category.valueOf(category.name()))
                .stream()
                .map(componentDefinition -> conversionService.convert(
                    componentDefinition, ComponentDefinitionBasicModel.class))
                .toList());
    }
}
