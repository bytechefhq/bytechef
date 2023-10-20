
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

package com.bytechef.hermes.definition.registry.remote.web.rest.facade;

import com.bytechef.hermes.definition.registry.domain.Option;
import com.bytechef.hermes.definition.registry.domain.ValueProperty;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/internal/action-definition-facade")
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class ActionDefinitionFacadeController {

    private final ActionDefinitionFacade actionDefinitionFacade;

    public ActionDefinitionFacadeController(ActionDefinitionFacade actionDefinitionFacade) {
        this.actionDefinitionFacade = actionDefinitionFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-editor-description",
        consumes = {
            "application/json"
        })
    public ResponseEntity<String> executeEditorDescription(
        @Valid @RequestBody EditorDescriptionRequest editorDescriptionRequest) {

        return ResponseEntity.ok(actionDefinitionFacade.executeEditorDescription(
            editorDescriptionRequest.componentName, editorDescriptionRequest.componentVersion,
            editorDescriptionRequest.actionName, editorDescriptionRequest.actionParameters,
            editorDescriptionRequest.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-options",
        consumes = {
            "application/json"
        })
    public ResponseEntity<List<Option>> executeOptions(@Valid @RequestBody OptionsRequest optionsRequest) {

        return ResponseEntity.ok(
            actionDefinitionFacade.executeOptions(
                optionsRequest.componentName, optionsRequest.componentVersion, optionsRequest.actionName,
                optionsRequest.propertyName, optionsRequest.actionParameters, optionsRequest.connectionId,
                optionsRequest.searchText));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-properties",
        consumes = {
            "application/json"
        })
    public ResponseEntity<List<? extends ValueProperty<?>>> executeProperties(
        @Valid @RequestBody PropertiesRequest propertiesRequest) {

        return ResponseEntity.ok(
            actionDefinitionFacade.executeDynamicProperties(
                propertiesRequest.componentName, propertiesRequest.componentVersion, propertiesRequest.actionName,
                propertiesRequest.propertyName, propertiesRequest.actionParameters, propertiesRequest.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-output-schema",
        consumes = {
            "application/json"
        })
    public ResponseEntity<List<? extends ValueProperty<?>>> executeOutputSchema(
        @Valid @RequestBody OutputSchemaRequest outputSchemaRequest) {

        return ResponseEntity.ok(
            actionDefinitionFacade.executeOutputSchema(
                outputSchemaRequest.componentName, outputSchemaRequest.componentVersion, outputSchemaRequest.actionName,
                outputSchemaRequest.actionParameters, outputSchemaRequest.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-sample-output",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Object> executeSampleOutput(@Valid @RequestBody SampleOutputRequest sampleOutputRequest) {
        return ResponseEntity.ok(
            actionDefinitionFacade.executeSampleOutput(
                sampleOutputRequest.actionName, sampleOutputRequest.componentName, sampleOutputRequest.componentVersion,
                sampleOutputRequest.actionParameters, sampleOutputRequest.connectionId));
    }

    @SuppressFBWarnings("EI")
    public record EditorDescriptionRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName,
        Map<String, Object> actionParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record OptionsRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName, @NotNull String propertyName,
        Map<String, Object> actionParameters, Long connectionId, String searchText) {
    }

    @SuppressFBWarnings("EI")
    public record OutputSchemaRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName,
        Map<String, Object> actionParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record PropertiesRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName,
        @NotNull String propertyName, Map<String, Object> actionParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record SampleOutputRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName,
        Map<String, Object> actionParameters, Long connectionId) {
    }
}
