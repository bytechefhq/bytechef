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

import com.integri.atlas.engine.coordinator.annotation.ConditionalOnCoordinator;
import com.integri.atlas.task.definition.TaskDefinition;
import com.integri.atlas.task.definition.dsl.TaskSpecification;
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
public class TaskSpecificationController {

    private final List<TaskDefinition> taskDefinitions;

    public TaskSpecificationController(List<TaskDefinition> taskDefinitions) {
        this.taskDefinitions = taskDefinitions;
    }

    @GetMapping(value = "/task-specifications/{name}")
    public TaskSpecification getTaskSpecification(@PathVariable("name") String name) {
        return taskDefinitions
            .stream()
            .map(TaskDefinition::getSpecification)
            .filter(taskSpecification -> Objects.equals(taskSpecification.getName(), name))
            .findFirst()
            .orElseThrow();
    }

    @GetMapping(value = "/task-specifications")
    public List<TaskSpecification> getTaskSpecifications() {
        return taskDefinitions.stream().map(TaskDefinition::getSpecification).collect(Collectors.toList());
    }
}
