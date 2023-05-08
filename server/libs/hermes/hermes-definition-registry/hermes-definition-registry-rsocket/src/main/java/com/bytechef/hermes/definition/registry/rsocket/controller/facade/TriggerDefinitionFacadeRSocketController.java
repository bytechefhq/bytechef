
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
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacade;
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
public class TriggerDefinitionFacadeRSocketController {

    private final TriggerDefinitionFacade triggerDefinitionFacade;

    public TriggerDefinitionFacadeRSocketController(TriggerDefinitionFacade triggerDefinitionFacade) {
        this.triggerDefinitionFacade = triggerDefinitionFacade;
    }

    @MessageMapping("TriggerDefinitionFacade.executeEditorDescription")
    public Mono<String> executeEditorDescription(EditorDescription editorDescription) {
        return triggerDefinitionFacade.executeEditorDescription(
            editorDescription.triggerName, editorDescription.componentName, editorDescription.componentVersion,
            editorDescription.triggerParameters, editorDescription.connectionId);
    }

    @MessageMapping("TriggerDefinitionFacade.executeOptions")
    public Mono<List<Option<?>>> executeOptions(Options options) {
        return triggerDefinitionFacade.executeOptions(
            options.propertyName, options.triggerName, options.componentName, options.componentVersion,
            options.triggerParameters, options.connectionId);
    }

    @MessageMapping("TriggerDefinitionFacade.executeProperties")
    public Mono<List<? extends Property<?>>> executeProperties(Properties properties) {
        return triggerDefinitionFacade.executeDynamicProperties(
            properties.propertyName, properties.triggerName, properties.componentName, properties.componentVersion,
            properties.triggerParameters, properties.connectionId);
    }

    @MessageMapping("TriggerDefinitionFacade.executeOutputSchema")
    public Mono<List<? extends Property<?>>> executeOutputSchema(OutputSchema outputSchema) {
        return triggerDefinitionFacade.executeOutputSchema(
            outputSchema.triggerName, outputSchema.componentName, outputSchema.componentVersion,
            outputSchema.triggerParameters, outputSchema.connectionId);
    }

    @MessageMapping("TriggerDefinitionFacade.executeSampleOutput")
    public Mono<Object> executeSampleOutput(SampleOutput sampleOutput) {
        return triggerDefinitionFacade.executeSampleOutput(
            sampleOutput.triggerName, sampleOutput.componentName, sampleOutput.componentVersion,
            sampleOutput.triggerParameters, sampleOutput.connectionId);
    }

    private record EditorDescription(
        String componentName, int componentVersion, long connectionId, String triggerName,
        Map<String, Object> triggerParameters) {
    }

    private record Options(
        String componentName, int componentVersion, long connectionId, String propertyName, String triggerName,
        Map<String, Object> triggerParameters) {
    }

    private record OutputSchema(
        String componentName, int componentVersion, long connectionId, String triggerName,
        Map<String, Object> triggerParameters) {
    }

    private record Properties(
        String componentName, int componentVersion, long connectionId, String propertyName, String triggerName,
        Map<String, Object> triggerParameters) {
    }

    private record SampleOutput(
        String componentName, int componentVersion, long connectionId, String triggerName,
        Map<String, Object> triggerParameters) {
    }
}
