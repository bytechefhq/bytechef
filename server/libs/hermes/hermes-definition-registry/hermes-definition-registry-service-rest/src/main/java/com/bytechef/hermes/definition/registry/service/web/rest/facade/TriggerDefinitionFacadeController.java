
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
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacade;
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
public class TriggerDefinitionFacadeController {

    private final TriggerDefinitionFacade triggerDefinitionFacade;

    public TriggerDefinitionFacadeController(TriggerDefinitionFacade triggerDefinitionFacade) {
        this.triggerDefinitionFacade = triggerDefinitionFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-definitions/editor-description",
        consumes = {
            "application/json"
        })
    public Mono<String> executeEditorDescription(@Valid @RequestBody EditorDescription editorDescription) {
        return Mono.just(
            triggerDefinitionFacade.executeEditorDescription(
                editorDescription.triggerName, editorDescription.componentName, editorDescription.componentVersion,
                editorDescription.triggerParameters, editorDescription.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-definitions/options",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public Mono<List<OptionDTO>> executeOptions(@Valid @RequestBody Options options) {
        return Mono.just(
            triggerDefinitionFacade.executeOptions(
                options.propertyName, options.triggerName, options.componentName, options.componentVersion,
                options.triggerParameters, options.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-definitions/properties",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public Mono<List<? extends ValuePropertyDTO<?>>> executeProperties(@Valid @RequestBody Properties properties) {
        return Mono.just(
            triggerDefinitionFacade.executeDynamicProperties(
                properties.propertyName, properties.triggerName, properties.componentName, properties.componentVersion,
                properties.triggerParameters, properties.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-definitions/output-schema",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public Mono<List<? extends ValuePropertyDTO<?>>> executeOutputSchema(
        @Valid @RequestBody OutputSchema outputSchema) {

        return Mono.just(
            triggerDefinitionFacade.executeOutputSchema(
                outputSchema.triggerName, outputSchema.componentName, outputSchema.componentVersion,
                outputSchema.triggerParameters, outputSchema.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-definitions/sample-output",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public Mono<Object> executeSampleOutput(@Valid @RequestBody SampleOutput sampleOutput) {
        return Mono.just(
            triggerDefinitionFacade.executeSampleOutput(
                sampleOutput.triggerName, sampleOutput.componentName, sampleOutput.componentVersion,
                sampleOutput.triggerParameters, sampleOutput.connectionId));
    }

    private record EditorDescription(
        @NotNull String componentName, int componentVersion, long connectionId, String triggerName,
        Map<String, Object> triggerParameters) {
    }

    private record Options(
        @NotNull String componentName, int componentVersion, long connectionId, @NotNull String propertyName,
        @NotNull String triggerName, Map<String, Object> triggerParameters) {
    }

    private record OutputSchema(
        @NotNull String componentName, int componentVersion, long connectionId, @NotNull String triggerName,
        Map<String, Object> triggerParameters) {
    }

    private record Properties(
        @NotNull String componentName, int componentVersion, long connectionId, @NotNull String propertyName,
        @NotNull String triggerName, Map<String, Object> triggerParameters) {
    }

    private record SampleOutput(
        @NotNull String componentName, int componentVersion, long connectionId, @NotNull String triggerName,
        Map<String, Object> triggerParameters) {
    }
}
