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

package com.integri.atlas.engine.web.rest;

import com.integri.atlas.engine.annotation.ConditionalOnCoordinator;
import com.integri.atlas.task.definition.TaskDefinitionHandler;
import com.integri.atlas.task.definition.dsl.TaskDefinition;
import com.integri.atlas.task.definition.registry.TaskDefinitionHandlerRegistry;
import java.util.List;
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

    private final TaskDefinitionHandlerRegistry taskDefinitionHandlerRegistry;

    public TaskDefinitionController(TaskDefinitionHandlerRegistry taskDefinitionHandlerRegistry) {
        this.taskDefinitionHandlerRegistry = taskDefinitionHandlerRegistry;
    }

    @GetMapping(value = "/task-definitions/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TaskDefinition getTaskDefinition(@PathVariable("name") String name) {
        TaskDefinitionHandler taskDefinitionHandler = taskDefinitionHandlerRegistry.getTaskDefinitionHandler(name);

        return taskDefinitionHandler.getTaskDefinition();
    }

    @GetMapping(value = "/task-definitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TaskDefinition> getTaskDefinitions() {
        return taskDefinitionHandlerRegistry
            .getTaskDefinitionHandlers()
            .stream()
            .map(TaskDefinitionHandler::getTaskDefinition)
            .collect(Collectors.toList());
    }
}
