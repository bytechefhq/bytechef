
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

package com.bytechef.hermes.definition.registry.service.web.rest.service;

import com.bytechef.hermes.definition.registry.dto.ActionDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class ActionDefinitionServiceController {

    private final ActionDefinitionService actionDefinitionService;

    public ActionDefinitionServiceController(ActionDefinitionService actionDefinitionService) {
        this.actionDefinitionService = actionDefinitionService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/{componentVersion}/action-definitions/{actionName}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ActionDefinitionDTO> getComponentActionDefinition(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion,
        @PathVariable("actionName") String actionName) {

        return ResponseEntity.ok(
            actionDefinitionService.getComponentActionDefinition(actionName, componentName, componentVersion));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/{componentVersion}/action-definitions",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ActionDefinitionDTO>> getComponentActionDefinitions(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion) {

        return ResponseEntity.ok(
            actionDefinitionService.getComponentActionDefinitions(componentName, componentVersion));
    }
}
