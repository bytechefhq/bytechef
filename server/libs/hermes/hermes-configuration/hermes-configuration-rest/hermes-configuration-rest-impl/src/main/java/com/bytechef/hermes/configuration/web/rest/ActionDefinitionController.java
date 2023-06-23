
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

import com.bytechef.hermes.configuration.web.rest.model.ActionDefinitionModel;
import com.bytechef.hermes.configuration.web.rest.model.GetActionDefinitionsTaskTypesParameterInnerModel;
import com.bytechef.hermes.definition.registry.dto.ComponentOperation;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.beans.PropertyEditorSupport;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
public class ActionDefinitionController implements ActionDefinitionsApi {

    private final ActionDefinitionService actionDefinitionService;
    private final ConversionService conversionService;
    private final ObjectMapper objectMapper;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(
            GetActionDefinitionsTaskTypesParameterInnerModel.class,
            new GetActionDefinitionsTaskTypesParameterInnerModelPropertyEditorSupport(objectMapper));
    }

    @SuppressFBWarnings("EI")
    public ActionDefinitionController(
        ActionDefinitionService actionDefinitionService, ConversionService conversionService,
        ObjectMapper objectMapper) {

        this.actionDefinitionService = actionDefinitionService;
        this.conversionService = conversionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<List<ActionDefinitionModel>> getActionDefinitions(
        List<GetActionDefinitionsTaskTypesParameterInnerModel> taskTypesParameterInnerModels) {

        return ResponseEntity.ok(
            actionDefinitionService.getActionDefinitions(
                taskTypesParameterInnerModels.stream()
                    .map(taskTypesParameterInnerModel -> new ComponentOperation(
                        taskTypesParameterInnerModel.getComponentName(),
                        taskTypesParameterInnerModel.getComponentVersion(),
                        taskTypesParameterInnerModel.getActionName()))
                    .toList())
                .stream()
                .map(action -> conversionService.convert(action, ActionDefinitionModel.class))
                .toList());
    }

    public static class GetActionDefinitionsTaskTypesParameterInnerModelPropertyEditorSupport
        extends PropertyEditorSupport {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI")
        public GetActionDefinitionsTaskTypesParameterInnerModelPropertyEditorSupport(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public void setAsText(String text) throws IllegalArgumentException {
            try {
                setValue(
                    objectMapper.readValue(
                        text, new TypeReference<GetActionDefinitionsTaskTypesParameterInnerModel>() {}));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
