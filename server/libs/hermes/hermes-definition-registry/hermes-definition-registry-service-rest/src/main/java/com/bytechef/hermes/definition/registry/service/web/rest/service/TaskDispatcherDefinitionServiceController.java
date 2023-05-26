
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

import com.bytechef.hermes.definition.registry.dto.TaskDispatcherDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.TaskDispatcherDefinitionService;
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
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "coordinator-service-app")
public class TaskDispatcherDefinitionServiceController {

    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    public TaskDispatcherDefinitionServiceController(TaskDispatcherDefinitionService taskDispatcherDefinitionService) {
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/task-dispatcher-definitions/{name}/{version}",
        produces = {
            "application/json"
        })
    public ResponseEntity<TaskDispatcherDefinitionDTO> getTaskDispatcherDefinition(
        @PathVariable("name") String name, @PathVariable("version") Integer version) {

        return ResponseEntity.ok(taskDispatcherDefinitionService.getTaskDispatcherDefinition(name, version));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/task-dispatcher-definitions",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<TaskDispatcherDefinitionDTO>> getTaskDispatcherDefinitions() {
        return ResponseEntity.ok(taskDispatcherDefinitionService.getTaskDispatcherDefinitions());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/task-dispatcher-definitions/{name}/versions",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<TaskDispatcherDefinitionDTO>> getTaskDispatcherDefinitionVersions(
        @PathVariable("name") String name) {

        return ResponseEntity.ok(taskDispatcherDefinitionService.getTaskDispatcherDefinitionVersions(name));
    }
}
