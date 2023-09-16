
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
import com.bytechef.hermes.configuration.web.rest.model.ActionDefinitionModel;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.service.RemoteActionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
public class ActionDefinitionController implements ActionDefinitionsApi {

    private final RemoteActionDefinitionService actionDefinitionService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ActionDefinitionController(
        RemoteActionDefinitionService actionDefinitionService, ConversionService conversionService) {

        this.actionDefinitionService = actionDefinitionService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<List<ActionDefinitionModel>> getActionDefinitions(List<String> taskTypes) {
        return ResponseEntity.ok(
            CollectionUtils.map(
                actionDefinitionService.getActionDefinitions(
                    taskTypes == null ? List.of() : CollectionUtils.map(taskTypes, ComponentOperation::ofType)),
                actionDefinition -> conversionService.convert(actionDefinition, ActionDefinitionModel.class)));
    }
}
