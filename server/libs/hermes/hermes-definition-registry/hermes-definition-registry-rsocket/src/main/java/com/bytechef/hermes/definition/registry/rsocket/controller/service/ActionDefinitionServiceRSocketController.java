
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

package com.bytechef.hermes.definition.registry.rsocket.controller.service;

import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.registry.dto.ActionDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class ActionDefinitionServiceRSocketController {

    private final ActionDefinitionService actionDefinitionService;

    public ActionDefinitionServiceRSocketController(ActionDefinitionService actionDefinitionService) {
        this.actionDefinitionService = actionDefinitionService;
    }

    @MessageMapping("ActionDefinitionService.executeEditorDescription")
    public Mono<String> executeEditorDescription(EditorDescription editorDescription) {
        return Mono.just(actionDefinitionService.executeEditorDescription(
            editorDescription.actionName, editorDescription.componentName, editorDescription.componentVersion,
            editorDescription.connectionParameters, editorDescription.authorizationName,
            editorDescription.actionParameters));
    }

    @MessageMapping("ActionDefinitionService.executeOptions")
    public Mono<List<Option<?>>> executeOptions(Options options) {

        return Mono.just(actionDefinitionService.executeOptions(
            options.propertyName, options.actionName, options.componentName, options.componentVersion,
            options.connectionParameters, options.authorizationName,
            options.actionParameters));
    }

    @MessageMapping("ActionDefinitionService.executeProperties")
    public Mono<List<? extends Property<?>>> executeProperties(Properties properties) {

        return Mono.just(actionDefinitionService.executeProperties(
            properties.propertyName, properties.actionName, properties.componentName, properties.componentVersion,
            properties.connectionParameters, properties.authorizationName,
            properties.actionParameters));
    }

    @MessageMapping("ActionDefinitionService.executeOutputSchema")
    public Mono<List<? extends Property<?>>> executeOutputSchema(OutputSchema outputSchema) {

        return Mono.just(actionDefinitionService.executeOutputSchema(
            outputSchema.actionName, outputSchema.componentName, outputSchema.componentVersion,
            outputSchema.connectionParameters, outputSchema.authorizationName,
            outputSchema.actionParameters));
    }

    @MessageMapping("ActionDefinitionService.executeSampleOutput")
    public Mono<Object> executeSampleOutput(SampleOutput sampleOutput) {

        return Mono.just(actionDefinitionService.executeSampleOutput(
            sampleOutput.actionName, sampleOutput.componentName, sampleOutput.componentVersion,
            sampleOutput.connectionParameters, sampleOutput.authorizationName,
            sampleOutput.actionParameters));
    }

    @MessageMapping("ActionDefinitionService.getComponentActionDefinition")
    public Mono<ActionDefinitionDTO> getComponentActionDefinition(Map<String, Object> map) {
        return actionDefinitionService.getComponentActionDefinitionMono(
            (String) map.get("actionName"), (String) map.get("componentName"), (Integer) map.get("componentVersion"));
    }

    @MessageMapping("ActionDefinitionService.getComponentActionDefinitions")
    public Mono<List<ActionDefinitionDTO>> getComponentActionDefinitions(Map<String, Object> map) {
        return actionDefinitionService.getComponentActionDefinitionsMono(
            (String) map.get("componentName"), (Integer) map.get("componentVersion"));
    }

    private record EditorDescription(
        String actionName, Map<String, Object> actionParameters, String authorizationName, String componentName,
        int componentVersion, Map<String, Object> connectionParameters) {
    }

    private record Options(
        String actionName, Map<String, Object> actionParameters, String authorizationName, String componentName,
        int componentVersion, Map<String, Object> connectionParameters, String propertyName) {
    }

    private record OutputSchema(
        String actionName, Map<String, Object> actionParameters, String authorizationName, String componentName,
        int componentVersion, Map<String, Object> connectionParameters) {
    }

    private record Properties(
        String actionName, Map<String, Object> actionParameters, String authorizationName, String componentName,
        int componentVersion, Map<String, Object> connectionParameters, String propertyName) {
    }

    private record SampleOutput(
        String actionName, Map<String, Object> actionParameters, String authorizationName, String componentName,
        int componentVersion, Map<String, Object> connectionParameters) {
    }
}
