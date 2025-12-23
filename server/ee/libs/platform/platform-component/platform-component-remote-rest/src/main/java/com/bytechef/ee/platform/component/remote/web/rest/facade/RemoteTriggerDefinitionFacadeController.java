/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.web.rest.facade;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.domain.OutputResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
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
        value = "/execute-properties",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<List<Property>> executeDynamicProperties(
        @Valid @RequestBody PropertiesRequest propertiesRequest) {

        return ResponseEntity.ok(
            triggerDefinitionFacade.executeDynamicProperties(
                propertiesRequest.componentName, propertiesRequest.componentVersion, propertiesRequest.triggerName,
                propertiesRequest.propertyName, propertiesRequest.inputParameters,
                propertiesRequest.lookupDependsOnPaths,
                propertiesRequest.connectionId));
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
    public ResponseEntity<WebhookEnableOutput> executeDynamicWebhookRefresh(
        @Valid @RequestBody DynamicWebhookRefreshRequest dynamicWebhookRefreshRequest) {

        return ResponseEntity.ok(triggerDefinitionFacade.executeDynamicWebhookRefresh(
            dynamicWebhookRefreshRequest.componentName, dynamicWebhookRefreshRequest.componentVersion,
            dynamicWebhookRefreshRequest.triggerName, dynamicWebhookRefreshRequest.outputParameters,
            dynamicWebhookRefreshRequest.connectionId));
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
                optionsRequest.propertyName, optionsRequest.inputParameters, optionsRequest.lookupDependsOnPaths,
                optionsRequest.searchText, optionsRequest.connectionId));
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
    public ResponseEntity<OutputResponse> executeOutputSchema(@Valid @RequestBody OutputRequest outputRequest) {
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
                triggerRequest.componentName, triggerRequest.componentVersion, triggerRequest.triggerName,
                triggerRequest.jobPrincipalId, triggerRequest.workflowUuid, triggerRequest.inputParameters,
                triggerRequest.state, triggerRequest.webhookRequest, triggerRequest.connectionId,
                triggerRequest.environmentId, triggerRequest.type, false));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-webhook-disable",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> executeWebhookDisable(
        @Valid @RequestBody DynamicWebhookDisableRequest dynamicWebhookDisableRequest) {

        triggerDefinitionFacade.executeWebhookDisable(
            dynamicWebhookDisableRequest.componentName, dynamicWebhookDisableRequest.componentVersion,
            dynamicWebhookDisableRequest.triggerName, dynamicWebhookDisableRequest.inputParameters,
            dynamicWebhookDisableRequest.workflowExecutionId, dynamicWebhookDisableRequest.outputParameters,
            dynamicWebhookDisableRequest.connectionId);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-webhook-enable",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<WebhookEnableOutput> executeWebhookEnable(
        @Valid @RequestBody DynamicWebhookEnableRequest dynamicWebhookEnableRequest) {

        return ResponseEntity.ok(triggerDefinitionFacade.executeWebhookEnable(
            dynamicWebhookEnableRequest.componentName, dynamicWebhookEnableRequest.componentVersion,
            dynamicWebhookEnableRequest.triggerName, dynamicWebhookEnableRequest.inputParameters,
            dynamicWebhookEnableRequest.workflowExecutionId, dynamicWebhookEnableRequest.connectionId,
            dynamicWebhookEnableRequest.webhookUrl));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-webhook-validate",
        produces = {
            "application/json"
        })
    public ResponseEntity<WebhookValidateResponse> executeWebhookValidate(
        @Valid @RequestBody WebhookValidateRequest webhookValidateRequest) {

        return ResponseEntity.ok(
            triggerDefinitionFacade.executeWebhookValidate(
                webhookValidateRequest.componentName, webhookValidateRequest.componentVersion,
                webhookValidateRequest.triggerName, webhookValidateRequest.inputParameters,
                webhookValidateRequest.webhookRequest, webhookValidateRequest.connectionId));
    }

    @SuppressFBWarnings("EI")
    public record DynamicWebhookDisableRequest(
        String componentName, int componentVersion, String triggerName,
        Map<String, Object> inputParameters, Map<String, ?> outputParameters,
        String workflowExecutionId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record DynamicWebhookEnableRequest(
        String componentName, int componentVersion, String triggerName,
        Map<String, Object> inputParameters, String workflowExecutionId, String webhookUrl,
        Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record DynamicWebhookRefreshRequest(
        String componentName, int componentVersion, String triggerName,
        Map<String, Long> outputParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record ListenerDisableRequest(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, String workflowExecutionId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record ListenerEnableRequest(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, String workflowExecutionId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record OptionsRequest(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, Long connectionId, List<String> lookupDependsOnPaths,
        String searchText) {
    }

    @SuppressFBWarnings("EI")
    public record OutputRequest(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record PropertiesRequest(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, Object> inputParameters, Long connectionId, List<String> lookupDependsOnPaths) {
    }

    @SuppressFBWarnings("EI")
    public record TriggerRequest(
        String componentName, int componentVersion, String triggerName,
        ModeType type, Long jobPrincipalId, String workflowUuid, Map<String, ?> inputParameters,
        Object state, WebhookRequest webhookRequest, Long connectionId, Long environmentId) {
    }

    @SuppressFBWarnings("EI")
    public record WebhookValidateRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, Long connectionId) {
    }
}
