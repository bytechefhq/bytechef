
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

package com.bytechef.hermes.task.dispatcher.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.hermes.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
@SuppressFBWarnings("EI")
@Tag(name = "task-dispatcher-definitions")
public class TaskDispatcherDefinitionController {

    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    public TaskDispatcherDefinitionController(TaskDispatcherDefinitionService taskDispatcherDefinitionService) {
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
    }

    /**
     * GET /definitions/task-dispatchers Returns all task dispatcher definitions
     *
     * @return OK (status code 200)
     */
    @Operation(
        description = "Get all task dispatcher definitions.",
        operationId = "getTaskDispatcherDefinitions",
        summary = "Get all task dispatcher definitions.",
        tags = {
            "task-dispatcher-definitions"
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "OK",
                content = {
                    @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = TaskDispatcherDefinition.class))
                })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/definitions/task-dispatchers",
        produces = {
            "application/json"
        })
    public Mono<ResponseEntity<Flux<TaskDispatcherDefinition>>> getTaskDispatcherDefinitions(
        @Parameter(hidden = true) ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                taskDispatcherDefinitionService.getTaskDispatcherDefinitions()));
    }
}
