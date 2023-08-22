
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

import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.definition.registry.domain.Option;
import com.bytechef.hermes.definition.registry.domain.ValueProperty;
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacade;
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
@RequestMapping("/internal/trigger-definition-facade")
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class TriggerDefinitionFacadeController {

    private final TriggerDefinitionFacade triggerDefinitionFacade;

    public TriggerDefinitionFacadeController(TriggerDefinitionFacade triggerDefinitionFacade) {
        this.triggerDefinitionFacade = triggerDefinitionFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-dynamic-webhook-disable",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> executeDynamicWebhookDisable(
        @Valid @RequestBody DynamicWebhookDisableRequest dynamicWebhookDisableRequest) {

        triggerDefinitionFacade.executeDynamicWebhookDisable(
            dynamicWebhookDisableRequest.componentName, dynamicWebhookDisableRequest.componentVersion,
            dynamicWebhookDisableRequest.triggerName, dynamicWebhookDisableRequest.triggerParameters,
            dynamicWebhookDisableRequest.workflowExecutionId, dynamicWebhookDisableRequest.output,
            dynamicWebhookDisableRequest.connectionId);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-dynamic-webhook-enable",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<DynamicWebhookEnableOutput> executeDynamicWebhookEnable(
        @Valid @RequestBody DynamicWebhookEnableRequest dynamicWebhookEnableRequest) {

        return ResponseEntity.ok(triggerDefinitionFacade.executeDynamicWebhookEnable(
            dynamicWebhookEnableRequest.componentName, dynamicWebhookEnableRequest.componentVersion,
            dynamicWebhookEnableRequest.triggerName, dynamicWebhookEnableRequest.triggerParameters,
            dynamicWebhookEnableRequest.workflowExecutionId, dynamicWebhookEnableRequest.connectionId,
            dynamicWebhookEnableRequest.webhookUrl));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-editor-description",
        consumes = {
            "application/json"
        })
    public ResponseEntity<String> executeEditorDescription(
        @Valid @RequestBody EditorDescriptionRequest editorDescriptionRequest) {

        return ResponseEntity.ok(
            triggerDefinitionFacade.executeEditorDescription(
                editorDescriptionRequest.componentName, editorDescriptionRequest.componentVersion,
                editorDescriptionRequest.triggerName, editorDescriptionRequest.triggerParameters,
                editorDescriptionRequest.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-listener-disable",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void>
        executeListenerDisable(@Valid @RequestBody ListenerDisableRequest listenerDisableRequest) {

        triggerDefinitionFacade.executeListenerDisable(
            listenerDisableRequest.componentName, listenerDisableRequest.componentVersion,
            listenerDisableRequest.triggerName, listenerDisableRequest.triggerParameters,
            listenerDisableRequest.workflowExecutionId, listenerDisableRequest.connectionId);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-listener-enable",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> executeListenerEnable(@Valid @RequestBody ListenerEnableRequest listenerEnableRequest) {
        triggerDefinitionFacade.executeListenerEnable(
            listenerEnableRequest.componentName, listenerEnableRequest.componentVersion,
            listenerEnableRequest.triggerName, listenerEnableRequest.triggerParameters,
            listenerEnableRequest.workflowExecutionId, listenerEnableRequest.connectionId);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-options",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public List<Option> executeOptions(@Valid @RequestBody OptionsRequest optionsRequest) {
        return triggerDefinitionFacade.executeOptions(
            optionsRequest.componentName, optionsRequest.componentVersion, optionsRequest.triggerName,
            optionsRequest.propertyName, optionsRequest.triggerParameters, optionsRequest.connectionId,
            optionsRequest.searchText);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-properties",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public List<? extends ValueProperty<?>>
        executeProperties(@Valid @RequestBody PropertiesRequest propertiesRequest) {
        return triggerDefinitionFacade.executeDynamicProperties(
            propertiesRequest.componentName, propertiesRequest.componentVersion, propertiesRequest.triggerName,
            propertiesRequest.propertyName, propertiesRequest.triggerParameters, propertiesRequest.connectionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-output-schema",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public List<? extends ValueProperty<?>> executeOutputSchema(
        @Valid @RequestBody TriggerDefinitionFacadeController.OutputSchemaRequest outputSchemaRequest) {

        return triggerDefinitionFacade.executeOutputSchema(
            outputSchemaRequest.componentName, outputSchemaRequest.componentVersion, outputSchemaRequest.triggerName,
            outputSchemaRequest.triggerParameters, outputSchemaRequest.connectionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-sample-output",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public Object executeSampleOutput(@Valid @RequestBody SampleOutputRequest sampleOutputRequest) {
        return triggerDefinitionFacade.executeSampleOutput(
            sampleOutputRequest.componentName, sampleOutputRequest.componentVersion, sampleOutputRequest.triggerName,
            sampleOutputRequest.triggerParameters, sampleOutputRequest.connectionId);
    }

    @SuppressFBWarnings("EI")
    public record DynamicWebhookDisableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, Object> triggerParameters, DynamicWebhookEnableOutput output, @NotNull String workflowExecutionId,
        Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record DynamicWebhookEnableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, Object> triggerParameters, @NotNull String workflowExecutionId, Long connectionId,
        String webhookUrl) {
    }

    @SuppressFBWarnings("EI")
    public record EditorDescriptionRequest(
        @NotNull String componentName, String triggerName, int componentVersion, Map<String, ?> triggerParameters,
        long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record ListenerDisableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, ?> triggerParameters, @NotNull String workflowExecutionId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record ListenerEnableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, ?> triggerParameters, @NotNull String workflowExecutionId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record OptionsRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName, @NotNull String propertyName,
        Map<String, ?> triggerParameters, Long connectionId, String searchText) {
    }

    @SuppressFBWarnings("EI")
    public record OutputSchemaRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, ?> triggerParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record PropertiesRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName, @NotNull String propertyName,
        Map<String, Object> triggerParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record SampleOutputRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, ?> triggerParameters, Long connectionId) {
    }
}
