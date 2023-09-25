
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

package com.bytechef.hermes.component.registry.remote.web.rest.facade;

import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.registry.facade.RemoteTriggerDefinitionFacade;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.registry.domain.Option;
import com.bytechef.hermes.registry.domain.ValueProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/remote/trigger-definition-facade")
public class RemoteTriggerDefinitionFacadeController {

    private final RemoteTriggerDefinitionFacade triggerDefinitionFacade;

    public RemoteTriggerDefinitionFacadeController(RemoteTriggerDefinitionFacade triggerDefinitionFacade) {
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
            dynamicWebhookDisableRequest.workflowExecutionId, dynamicWebhookDisableRequest.outputParameters,
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
        value = "/execute-dynamic-webhook-refresh",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<DynamicWebhookEnableOutput> executeDynamicWebhookRefresh(
        @Valid @RequestBody DynamicWebhookRefreshRequest dynamicWebhookRefreshRequest) {

        return ResponseEntity.ok(triggerDefinitionFacade.executeDynamicWebhookRefresh(
            dynamicWebhookRefreshRequest.componentName, dynamicWebhookRefreshRequest.componentVersion,
            dynamicWebhookRefreshRequest.triggerName, dynamicWebhookRefreshRequest.outputParameters));
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
        @Valid @RequestBody RemoteTriggerDefinitionFacadeController.OutputSchemaRequest outputSchemaRequest) {

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

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-trigger",
        produces = {
            "application/json"
        })
    public ResponseEntity<TriggerOutput> executeTrigger(@Valid @RequestBody TriggerRequest triggerRequest) {
        return ResponseEntity.ok(
            triggerDefinitionFacade.executeTrigger(
                triggerRequest.componentName, triggerRequest.componentVersion,
                triggerRequest.triggerName, triggerRequest.triggerParameters, triggerRequest.state,
                triggerRequest.webhookRequest, triggerRequest.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-webhook-validate",
        produces = {
            "application/json"
        })
    public ResponseEntity<Boolean> executeWebhookValidate(
        @Valid @RequestBody WebhookValidateRequest webhookValidateRequest) {

        return ResponseEntity.ok(
            triggerDefinitionFacade.executeWebhookValidate(
                webhookValidateRequest.componentName, webhookValidateRequest.componentVersion,
                webhookValidateRequest.triggerName, webhookValidateRequest.triggerParameters,
                webhookValidateRequest.webhookRequest, webhookValidateRequest.connectionId));
    }

    @SuppressFBWarnings("EI")
    public record DynamicWebhookDisableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, Object> triggerParameters, @NotNull Map<String, ?> outputParameters,
        @NotNull String workflowExecutionId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record DynamicWebhookEnableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, Object> triggerParameters, @NotNull String workflowExecutionId, @NotNull String webhookUrl,
        Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record DynamicWebhookRefreshRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, Long> outputParameters) {
    }

    @SuppressFBWarnings("EI")
    public record EditorDescriptionRequest(
        @NotNull String componentName, String triggerName, int componentVersion, Map<String, ?> triggerParameters,
        long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record ListenerDisableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, ?> triggerParameters, @NotNull String workflowExecutionId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record ListenerEnableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, ?> triggerParameters, @NotNull String workflowExecutionId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record OptionsRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName, @NotNull String propertyName,
        @NotNull Map<String, ?> triggerParameters, Long connectionId, String searchText) {
    }

    @SuppressFBWarnings("EI")
    public record OutputSchemaRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, ?> triggerParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record PropertiesRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName, @NotNull String propertyName,
        @NotNull Map<String, Object> triggerParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record SampleOutputRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, ?> triggerParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record TriggerRequest(
        String componentName, int componentVersion, String triggerName, @NotNull Map<String, ?> triggerParameters,
        Object state, @NotNull WebhookRequest webhookRequest, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record WebhookValidateRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, ?> triggerParameters, @NotNull WebhookRequest webhookRequest, Long connectionId) {
    }
}
