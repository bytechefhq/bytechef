
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

package com.bytechef.hermes.component.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.service.ComponentDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnApi
@RequestMapping("${openapi.openAPIDefinition.base-path:}")
@SuppressFBWarnings("EI")
@Tag(name = "action-definitions")
public class ActionDefinitionController {

    private final ComponentDefinitionService componentDefinitionService;

    public ActionDefinitionController(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    /**
     * GET /definitions/components/{componentName}/actions/{actionName} Get an action of a component definition by name
     *
     * @return OK (status code 200)
     */
    @Operation(
        description = "Get an action of a component definition by name.",
        operationId = "getComponentDefinitionAction",
        summary = "Get an action of a component definition by name.",
        tags = {
            "component-definitions"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ActionDefinition.class))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/definitions/components/{componentName}/actions/{actionName}",
        produces = {
            "application/json"
        })
    public Mono<ResponseEntity<ActionDefinition>> getComponentDefinitionAction(
        @Parameter(
            name = "componentName", description = "The name of the component.",
            required = true) @PathVariable("componentName") String componentName,
        @Parameter(
            name = "actionName", description = "The name of the action to get.",
            required = true) @PathVariable("actionName") String actionName,
        @Parameter(hidden = true) final ServerWebExchange exchange) {
        return componentDefinitionService.getComponentDefinitionAction(componentName, actionName)
            .map(ResponseEntity::ok);
    }
}
