
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

package com.bytechef.hermes.definition.registry.remote.web.rest.service;

import com.bytechef.hermes.definition.registry.domain.ActionDefinition;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/internal/action-definition-service")
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class ActionDefinitionServiceController {

    private final ActionDefinitionService actionDefinitionService;

    public ActionDefinitionServiceController(ActionDefinitionService actionDefinitionService) {
        this.actionDefinitionService = actionDefinitionService;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-trigger",
        produces = {
            "application/json"
        })
    public ResponseEntity<Object> executePerform(@RequestBody PerformRequest performRequest) {
        return ResponseEntity.ok(
            actionDefinitionService.executePerform(
                performRequest.componentName, performRequest.componentVersion, performRequest.actionName,
                performRequest.taskExecutionId, performRequest.inputParameters, performRequest.connectionIdMap));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-action-definition/{componentName}/{componentVersion}/{actionName}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ActionDefinition> getActionDefinition(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion, @PathVariable("actionName") String actionName) {

        return ResponseEntity
            .ok(actionDefinitionService.getActionDefinition(componentName, componentVersion, actionName));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-action-definitions/{componentName}/{componentVersion}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ActionDefinition>> getActionDefinitions(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion) {

        return ResponseEntity.ok(actionDefinitionService.getActionDefinitions(componentName, componentVersion));
    }

    @SuppressFBWarnings("EI")
    public record PerformRequest(
        String componentName, int componentVersion, String actionName, long taskExecutionId,
        Map<String, ?> inputParameters, Map<String, Long> connectionIdMap) {
    }
}
