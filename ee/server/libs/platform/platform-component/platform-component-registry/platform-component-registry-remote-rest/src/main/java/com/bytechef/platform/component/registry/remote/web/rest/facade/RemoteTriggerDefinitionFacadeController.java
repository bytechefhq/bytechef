/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.component.registry.remote.web.rest.facade;

import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.registry.trigger.TriggerOutput;
import com.bytechef.platform.component.registry.trigger.WebhookRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/remote/trigger-definition-facade")
public class RemoteTriggerDefinitionFacadeController {

    private final TriggerDefinitionFacade triggerDefinitionFacade;

    public RemoteTriggerDefinitionFacadeController(TriggerDefinitionFacade triggerDefinitionFacade) {
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
            dynamicWebhookDisableRequest.triggerName, dynamicWebhookDisableRequest.inputParameters,
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
            dynamicWebhookEnableRequest.triggerName, dynamicWebhookEnableRequest.inputParameters,
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
                editorDescriptionRequest.triggerName, editorDescriptionRequest.inputParameters,
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
            listenerDisableRequest.triggerName, listenerDisableRequest.inputParameters,
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
            listenerEnableRequest.triggerName, listenerEnableRequest.inputParameters,
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
    public ResponseEntity<List<Option>> executeOptions(@Valid @RequestBody OptionsRequest optionsRequest) {
        return ResponseEntity.ok(
            triggerDefinitionFacade.executeOptions(
                optionsRequest.componentName, optionsRequest.componentVersion, optionsRequest.triggerName,
                optionsRequest.propertyName, optionsRequest.inputParameters, optionsRequest.connectionId,
                optionsRequest.searchText));
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
    public ResponseEntity<List<Property>>
        executeProperties(@Valid @RequestBody PropertiesRequest propertiesRequest) {
        return ResponseEntity.ok(
            triggerDefinitionFacade.executeDynamicProperties(
                propertiesRequest.componentName, propertiesRequest.componentVersion, propertiesRequest.triggerName,
                propertiesRequest.propertyName, propertiesRequest.inputParameters, propertiesRequest.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-output",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<Output> executeOutputSchema(@Valid @RequestBody OutputRequest outputRequest) {
        return ResponseEntity.ok(
            triggerDefinitionFacade.executeOutput(
                outputRequest.componentName, outputRequest.componentVersion, outputRequest.triggerName,
                outputRequest.inputParameters, outputRequest.connectionId));
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
                triggerRequest.triggerName, triggerRequest.inputParameters, triggerRequest.state,
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
                webhookValidateRequest.triggerName, webhookValidateRequest.inputParameters,
                webhookValidateRequest.webhookRequest, webhookValidateRequest.connectionId));
    }

    @SuppressFBWarnings("EI")
    public record DynamicWebhookDisableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, Object> inputParameters, @NotNull Map<String, ?> outputParameters,
        @NotNull String workflowExecutionId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record DynamicWebhookEnableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, Object> inputParameters, @NotNull String workflowExecutionId, @NotNull String webhookUrl,
        Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record DynamicWebhookRefreshRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, Long> outputParameters) {
    }

    @SuppressFBWarnings("EI")
    public record EditorDescriptionRequest(
        @NotNull String componentName, String triggerName, int componentVersion, Map<String, ?> inputParameters,
        long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record ListenerDisableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, ?> inputParameters, @NotNull String workflowExecutionId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record ListenerEnableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, ?> inputParameters, @NotNull String workflowExecutionId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record OptionsRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName, @NotNull String propertyName,
        @NotNull Map<String, ?> inputParameters, Long connectionId, String searchText) {
    }

    @SuppressFBWarnings("EI")
    public record OutputRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, ?> inputParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record PropertiesRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName, @NotNull String propertyName,
        @NotNull Map<String, Object> inputParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record SampleOutputRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, ?> inputParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record TriggerRequest(
        String componentName, int componentVersion, String triggerName, @NotNull Map<String, ?> inputParameters,
        Object state, @NotNull WebhookRequest webhookRequest, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record WebhookValidateRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        @NotNull Map<String, ?> inputParameters, @NotNull WebhookRequest webhookRequest, Long connectionId) {
    }
}
