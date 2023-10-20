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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class TaskDefinitionController {

    private final List<TaskDefinitionHandler> taskDefinitionHandlers;

    public TaskDefinitionController(List<TaskDefinitionHandler> taskDefinitionHandlers) {
        this.taskDefinitionHandlers = taskDefinitionHandlers;
    }

    @GetMapping(value = "/task-definitions/{name}")
    public TaskDefinition getTaskDefinition(@PathVariable("name") String name) {
        return taskDefinitionHandlers
            .stream()
            .map(TaskDefinitionHandler::getTaskDefinition)
            .filter(taskDefinition -> Objects.equals(taskDefinition.getName(), name))
            .findFirst()
            .orElseThrow();
    }

    @GetMapping(value = "/task-definitions")
    public List<TaskDefinition> getTaskDefinitions() {
        return taskDefinitionHandlers
            .stream()
            .map(TaskDefinitionHandler::getTaskDefinition)
            .collect(Collectors.toList());
    }
}
