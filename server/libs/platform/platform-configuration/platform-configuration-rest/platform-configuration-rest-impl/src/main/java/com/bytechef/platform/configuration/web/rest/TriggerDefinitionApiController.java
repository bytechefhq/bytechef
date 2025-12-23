/*
 * Copyright 2025 ByteChef
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
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.web.rest.model.TriggerDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.TriggerDefinitionModel;
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
public class TriggerDefinitionApiController implements TriggerDefinitionApi {

    private final ConversionService conversionService;
    private final TriggerDefinitionService triggerDefinitionService;

    public TriggerDefinitionApiController(
        ConversionService conversionService, TriggerDefinitionService triggerDefinitionService) {

        this.conversionService = conversionService;
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Override
    public ResponseEntity<TriggerDefinitionModel> getComponentTriggerDefinition(
        String componentName, Integer componentVersion, String triggerName) {

        return ResponseEntity.ok(
            conversionService.convert(
                triggerDefinitionService.getTriggerDefinition(componentName, componentVersion, triggerName),
                TriggerDefinitionModel.class));
    }

    @Override
    public ResponseEntity<List<TriggerDefinitionBasicModel>> getComponentTriggerDefinitions(
        String componentName, Integer componentVersion) {

        return ResponseEntity.ok(
            triggerDefinitionService.getTriggerDefinitions(componentName, componentVersion)
                .stream()
                .map(triggerDefinitionDTO -> conversionService.convert(
                    triggerDefinitionDTO, TriggerDefinitionBasicModel.class))
                .toList());
    }
}
