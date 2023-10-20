/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.workflow.web;

import com.integri.atlas.engine.coordinator.annotation.ConditionalOnCoordinator;
import com.integri.atlas.engine.core.task.TaskDescriptor;
import com.integri.atlas.engine.core.task.description.TaskDescription;
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
public class TaskController {

    private final List<TaskDescriptor> taskDescriptors;

    public TaskController(List<TaskDescriptor> taskDescriptors) {
        this.taskDescriptors = taskDescriptors;
    }

    @GetMapping(value = "/tasks")
    public List<TaskDescription> getTaskDescriptors() {
        return taskDescriptors.stream().map(TaskDescriptor::getDescription).collect(Collectors.toList());
    }

    @GetMapping(value = "/tasks/{name}")
    public TaskDescription getTaskDescriptors(@PathVariable("name") String name) {
        return taskDescriptors
            .stream()
            .map(TaskDescriptor::getDescription)
            .filter(taskDescription -> Objects.equals(taskDescription.getName(), name))
            .findFirst()
            .orElseThrow();
    }
}
