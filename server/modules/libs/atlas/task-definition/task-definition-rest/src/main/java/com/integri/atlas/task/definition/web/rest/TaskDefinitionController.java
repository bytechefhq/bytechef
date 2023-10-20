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

package com.integri.atlas.task.definition.web.rest;

import com.integri.atlas.engine.annotation.ConditionalOnCoordinator;
import com.integri.atlas.task.definition.handler.TaskDefinitionHandler;
import com.integri.atlas.task.definition.model.TaskAuthDefinition;
import com.integri.atlas.task.definition.model.TaskDefinition;
import com.integri.atlas.task.definition.service.TaskDefinitionHandlerService;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class TaskDefinitionController {

    private final TaskDefinitionHandlerService taskDefinitionHandlerService;

    public TaskDefinitionController(TaskDefinitionHandlerService taskDefinitionHandlerService) {
        this.taskDefinitionHandlerService = taskDefinitionHandlerService;
    }

    @GetMapping(value = "/task-auth-definitions/{name}/{authName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TaskAuthDefinition getTaskAuthDefinition(
        @PathVariable("name") String name,
        @PathVariable("authName") String authName
    ) {
        TaskDefinitionHandler taskDefinitionHandler = taskDefinitionHandlerService.getTaskDefinitionHandler(name);

        return taskDefinitionHandler
            .getTaskAuthDefinitions()
            .stream()
            .filter(taskAuthDefinition -> Objects.equals(taskAuthDefinition.getName(), authName))
            .findFirst()
            .orElse(null);
    }

    @GetMapping(value = "/task-auth-definitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TaskAuthDefinition> getTaskAuthDefinitions() {
        return taskDefinitionHandlerService
            .getTaskDefinitionHandlers()
            .stream()
            .flatMap(taskDefinitionHandler -> taskDefinitionHandler.getTaskAuthDefinitions().stream())
            .collect(Collectors.toList());
    }

    @GetMapping(value = "/task-definitions/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TaskDefinition getTaskDefinition(@PathVariable("name") String name) {
        TaskDefinitionHandler taskDefinitionHandler = taskDefinitionHandlerService.getTaskDefinitionHandler(name);

        return taskDefinitionHandler.getTaskDefinition();
    }

    @GetMapping(value = "/task-definitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TaskDefinition> getTaskDefinitions() {
        return taskDefinitionHandlerService
            .getTaskDefinitionHandlers()
            .stream()
            .map(TaskDefinitionHandler::getTaskDefinition)
            .collect(Collectors.toList());
    }
}
