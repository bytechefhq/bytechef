
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

package com.bytechef.hermes.definition.registry.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.hermes.definition.registry.service.TaskDispatcherDefinitionService;
import com.bytechef.hermes.definition.registry.web.rest.model.TaskDispatcherDefinitionModel;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnApi
@RequestMapping("${openapi.openAPIDefinition.base-path:}")
public class TaskDispatcherDefinitionController implements TaskDispatcherDefinitionsApi {

    private final ConversionService conversionService;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    public TaskDispatcherDefinitionController(
        ConversionService conversionService, TaskDispatcherDefinitionService taskDispatcherDefinitionService) {

        this.conversionService = conversionService;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
    }

    @Override
    public Mono<ResponseEntity<Flux<TaskDispatcherDefinitionModel>>> getTaskDispatcherDefinitions(
        @Parameter(hidden = true) ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                taskDispatcherDefinitionService.getTaskDispatcherDefinitions()
                    .mapNotNull(taskDispatcherDefinitions -> taskDispatcherDefinitions.stream()
                        .map(taskDispatcherDefinition -> conversionService.convert(
                            taskDispatcherDefinition, TaskDispatcherDefinitionModel.class))
                        .toList())
                    .flatMapMany(Flux::fromIterable)));
    }
}
