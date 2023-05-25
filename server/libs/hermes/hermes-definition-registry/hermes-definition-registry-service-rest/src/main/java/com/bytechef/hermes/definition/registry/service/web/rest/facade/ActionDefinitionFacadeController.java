
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

package com.bytechef.hermes.definition.registry.service.web.rest.facade;

import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.ValuePropertyDTO;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class ActionDefinitionFacadeController {

    private final ActionDefinitionFacade actionDefinitionFacade;

    public ActionDefinitionFacadeController(ActionDefinitionFacade actionDefinitionFacade) {
        this.actionDefinitionFacade = actionDefinitionFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/action-definitions/editor-description",
        consumes = {
            "application/json"
        })
    public Mono<String> executeEditorDescription(@Valid @RequestBody EditorDescription editorDescription) {
        return Mono.just(
            actionDefinitionFacade.executeEditorDescription(
                editorDescription.actionName, editorDescription.componentName, editorDescription.componentVersion,
                editorDescription.actionParameters, editorDescription.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/action-definitions/options",
        consumes = {
            "application/json"
        })
    public Mono<List<OptionDTO>> executeOptions(@Valid @RequestBody Options options) {

        return Mono.just(
            actionDefinitionFacade.executeOptions(
                options.propertyName, options.actionName, options.componentName, options.componentVersion,
                options.actionParameters, options.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/action-definitions/properties",
        consumes = {
            "application/json"
        })
    public Mono<List<? extends ValuePropertyDTO<?>>> executeProperties(@Valid @RequestBody Properties properties) {

        return Mono.just(
            actionDefinitionFacade.executeDynamicProperties(
                properties.propertyName, properties.actionName, properties.componentName, properties.componentVersion,
                properties.actionParameters, properties.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/action-definitions/output-schema",
        consumes = {
            "application/json"
        })
    public Mono<List<? extends ValuePropertyDTO<?>>>
        executeOutputSchema(@Valid @RequestBody OutputSchema outputSchema) {

        return Mono.just(
            actionDefinitionFacade.executeOutputSchema(
                outputSchema.actionName, outputSchema.componentName, outputSchema.componentVersion,
                outputSchema.actionParameters, outputSchema.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/action-definitions/sample-output",
        consumes = {
            "application/json"
        })
    public Mono<Object> executeSampleOutput(@Valid @RequestBody SampleOutput sampleOutput) {

        return Mono.just(
            actionDefinitionFacade.executeSampleOutput(
                sampleOutput.actionName, sampleOutput.componentName, sampleOutput.componentVersion,
                sampleOutput.actionParameters, sampleOutput.connectionId));
    }

    private record EditorDescription(
        @NotNull String actionName, Map<String, Object> actionParameters, @NotNull String componentName,
        int componentVersion, long connectionId) {
    }

    private record Options(
        @NotNull String actionName, Map<String, Object> actionParameters, @NotNull String componentName,
        int componentVersion, long connectionId, @NotNull String propertyName) {
    }

    private record OutputSchema(
        @NotNull String actionName, Map<String, Object> actionParameters, @NotNull String componentName,
        int componentVersion, long connectionId) {
    }

    private record Properties(
        @NotNull String actionName, Map<String, Object> actionParameters, @NotNull String componentName,
        int componentVersion, long connectionId, @NotNull String propertyName) {
    }

    private record SampleOutput(
        @NotNull String actionName, Map<String, Object> actionParameters, @NotNull String componentName,
        int componentVersion, long connectionId) {
    }
}
