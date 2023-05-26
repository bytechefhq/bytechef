
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

package com.bytechef.hermes.definition.registry.service.web.rest.facade;

import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class ComponentDefinitionFacadeController {

    private final ComponentDefinitionFacade componentDefinitionFacade;

    public ComponentDefinitionFacadeController(ComponentDefinitionFacade componentDefinitionFacade) {
        this.componentDefinitionFacade = componentDefinitionFacade;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/search",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ComponentDefinitionDTO>> searchComponentDefinitions(
        @RequestParam(value = "actionDefinitions", required = false) Boolean actionDefinitions,
        @RequestParam(value = "connectionDefinitions", required = false) Boolean connectionDefinitions,
        @RequestParam(value = "connectionInstances", required = false) Boolean connectionInstances,
        @RequestParam(value = "actionDefinitions", required = false) Boolean triggerDefinitions) {

        return ResponseEntity.ok(
            componentDefinitionFacade.searchComponentDefinitions(
                actionDefinitions, connectionDefinitions, connectionInstances, triggerDefinitions));
    }
}
