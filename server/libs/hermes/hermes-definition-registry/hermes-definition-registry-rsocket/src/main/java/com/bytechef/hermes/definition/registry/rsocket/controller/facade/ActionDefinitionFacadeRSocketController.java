
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

package com.bytechef.hermes.definition.registry.rsocket.controller.facade;

import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacade;
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
public class ActionDefinitionFacadeRSocketController {

    private final ActionDefinitionFacade actionDefinitionFacade;

    public ActionDefinitionFacadeRSocketController(ActionDefinitionFacade actionDefinitionFacade) {
        this.actionDefinitionFacade = actionDefinitionFacade;
    }

    @MessageMapping("ActionDefinitionFacade.executeEditorDescription")
    public Mono<String> executeEditorDescription(EditorDescription editorDescription) {
        return actionDefinitionFacade.executeEditorDescription(
            editorDescription.actionName, editorDescription.componentName, editorDescription.componentVersion,
            editorDescription.actionParameters, editorDescription.connectionId);
    }

    @MessageMapping("ActionDefinitionFacade.executeOptions")
    public Mono<List<Option<?>>> executeOptions(Options options) {

        return actionDefinitionFacade.executeOptions(
            options.propertyName, options.actionName, options.componentName, options.componentVersion,
            options.actionParameters, options.connectionId);
    }

    @MessageMapping("ActionDefinitionFacade.executeProperties")
    public Mono<List<? extends Property<?>>> executeProperties(Properties properties) {

        return actionDefinitionFacade.executeDynamicProperties(
            properties.propertyName, properties.actionName, properties.componentName, properties.componentVersion,
            properties.actionParameters, properties.connectionId);
    }

    @MessageMapping("ActionDefinitionFacade.executeOutputSchema")
    public Mono<List<? extends Property<?>>> executeOutputSchema(OutputSchema outputSchema) {

        return actionDefinitionFacade.executeOutputSchema(
            outputSchema.actionName, outputSchema.componentName, outputSchema.componentVersion,
            outputSchema.actionParameters, outputSchema.connectionId);
    }

    @MessageMapping("ActionDefinitionService.executeSampleOutput")
    public Mono<Object> executeSampleOutput(SampleOutput sampleOutput) {

        return actionDefinitionFacade.executeSampleOutput(
            sampleOutput.actionName, sampleOutput.componentName, sampleOutput.componentVersion,
            sampleOutput.actionParameters, sampleOutput.connectionId);
    }

    private record EditorDescription(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        long connectionId) {
    }

    private record Options(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        long connectionId, String propertyName) {
    }

    private record OutputSchema(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        long connectionId) {
    }

    private record Properties(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        long connectionId, String propertyName) {
    }

    private record SampleOutput(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        long connectionId) {
    }
}
