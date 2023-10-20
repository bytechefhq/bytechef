
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

import com.bytechef.autoconfigure.annotation.ConditionalOnEnabled;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.configuration.web.rest.model.TriggerDefinitionModel;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
@ConditionalOnEnabled("coordinator")
public class TriggerDefinitionController implements TriggerDefinitionsApi {

    private final ConversionService conversionService;
    private final TriggerDefinitionService triggerDefinitionService;

    public TriggerDefinitionController(
        ConversionService conversionService, TriggerDefinitionService triggerDefinitionService) {

        this.conversionService = conversionService;
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Override
    public ResponseEntity<List<TriggerDefinitionModel>> getTriggerDefinitions(List<String> triggerTypes) {
        return ResponseEntity.ok(
            CollectionUtils.map(
                triggerDefinitionService.getTriggerDefinitions(
                    triggerTypes == null ? List.of() : CollectionUtils.map(triggerTypes, ComponentOperation::ofType)),
                triggerDefinition -> conversionService.convert(triggerDefinition, TriggerDefinitionModel.class)));
    }
}
