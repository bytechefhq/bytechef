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

package com.integri.atlas.task.descriptor.web.rest;

import com.integri.atlas.engine.annotation.ConditionalOnCoordinator;
import com.integri.atlas.task.descriptor.handler.TaskDescriptorHandler;
import com.integri.atlas.task.descriptor.model.TaskAuthDescriptor;
import com.integri.atlas.task.descriptor.service.TaskDescriptorHandlerService;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class TaskAuthDescriptorController {

    private final TaskDescriptorHandlerService taskDescriptorHandlerService;

    public TaskAuthDescriptorController(TaskDescriptorHandlerService taskDescriptorHandlerService) {
        this.taskDescriptorHandlerService = taskDescriptorHandlerService;
    }

    @GetMapping(value = "/task-auth-descriptors/{name}/{authName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TaskAuthDescriptor getTaskAuthDescriptor(
        @PathVariable("name") String name,
        @PathVariable("authName") String authName
    ) {
        TaskDescriptorHandler taskDescriptorHandler = taskDescriptorHandlerService.getTaskDescriptorHandler(name);

        return taskDescriptorHandler
            .getTaskAuthDescriptors()
            .stream()
            .filter(taskAuthDescriptor -> Objects.equals(taskAuthDescriptor.getName(), authName))
            .findFirst()
            .orElse(null);
    }

    @GetMapping(value = "/task-auth-descriptors", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TaskAuthDescriptor> getTaskAuthDescriptors() {
        return taskDescriptorHandlerService
            .getTaskDescriptorHandlers()
            .stream()
            .flatMap(taskDescriptorHandler ->
                taskDescriptorHandler.getTaskAuthDescriptors() == null
                    ? Stream.of()
                    : taskDescriptorHandler.getTaskAuthDescriptors().stream()
            )
            .collect(Collectors.toList());
    }
}
