/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.hermes.configuration.web.rest;

import com.bytechef.hermes.configuration.web.rest.model.PropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.TaskDispatcherDefinitionBasicModel;
import com.bytechef.hermes.configuration.web.rest.model.TaskDispatcherDefinitionModel;
import com.bytechef.hermes.configuration.web.rest.model.TaskDispatcherOperationRequestModel;
import com.bytechef.hermes.task.dispatcher.registry.service.TaskDispatcherDefinitionService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.core:}")
@ConditionalOnProperty(prefix = "bytechef", name = "coordinator.enabled", matchIfMissing = true)
public class TaskDispatcherDefinitionApiController implements TaskDispatcherDefinitionApi {

    private final ConversionService conversionService;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    public TaskDispatcherDefinitionApiController(
        ConversionService conversionService, TaskDispatcherDefinitionService taskDispatcherDefinitionService) {

        this.conversionService = conversionService;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
    }

    @Override
    public ResponseEntity<TaskDispatcherDefinitionModel> getTaskDispatcherDefinition(
        String taskDispatcherName, Integer taskDispatcherVersion) {

        return ResponseEntity.ok(
            conversionService.convert(
                taskDispatcherDefinitionService
                    .getTaskDispatcherDefinition(taskDispatcherName, taskDispatcherVersion),
                TaskDispatcherDefinitionModel.class));
    }

    @Override
    public ResponseEntity<List<TaskDispatcherDefinitionModel>> getTaskDispatcherDefinitions() {
        return ResponseEntity.ok(
            taskDispatcherDefinitionService.getTaskDispatcherDefinitions()
                .stream()
                .map(taskDispatcherDefinition -> conversionService.convert(
                    taskDispatcherDefinition, TaskDispatcherDefinitionModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<TaskDispatcherDefinitionBasicModel>> getTaskDispatcherDefinitionVersions(
        String taskDispatcherName) {

        return ResponseEntity.ok(
            taskDispatcherDefinitionService.getTaskDispatcherDefinitionVersions(taskDispatcherName)
                .stream()
                .map(taskDispatcherDefinition -> conversionService.convert(
                    taskDispatcherDefinition, TaskDispatcherDefinitionBasicModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<PropertyModel>> getTaskDispatcherOutputSchema(
        String taskDispatcherName, Integer taskDispatcherVersion,
        TaskDispatcherOperationRequestModel taskDispatcherOperationRequestModel) {

        return ResponseEntity.ok(
            taskDispatcherDefinitionService.executeOutputSchema(
                taskDispatcherName, taskDispatcherVersion, taskDispatcherOperationRequestModel.getParameters())
                .stream()
                .map(property -> conversionService.convert(
                    property, PropertyModel.class))
                .toList());
    }
}
