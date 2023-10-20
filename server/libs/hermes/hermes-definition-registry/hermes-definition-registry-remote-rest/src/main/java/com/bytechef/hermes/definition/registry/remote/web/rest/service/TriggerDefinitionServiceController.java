
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

package com.bytechef.hermes.definition.registry.remote.web.rest.service;

import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class TriggerDefinitionServiceController {

    private final TriggerDefinitionService triggerDefinitionService;

    public TriggerDefinitionServiceController(TriggerDefinitionService triggerDefinitionService) {
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-definition-service/execute-dynamic-webhook-disable",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> executeDynamicWebhookDisable(
        @Valid @RequestBody DynamicWebhookDisableRequest dynamicWebhookDisableRequest) {

        triggerDefinitionService.executeDynamicWebhookDisable(
            dynamicWebhookDisableRequest.componentName, dynamicWebhookDisableRequest.componentVersion,
            dynamicWebhookDisableRequest.triggerName,
            dynamicWebhookDisableRequest.connectionParameters,
            dynamicWebhookDisableRequest.authorizationName, dynamicWebhookDisableRequest.triggerParameters,
            dynamicWebhookDisableRequest.workflowExecutionId, dynamicWebhookDisableRequest.output);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-definition-service/execute-dynamic-webhook-enable",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<DynamicWebhookEnableOutput> executeDynamicWebhookEnable(
        @Valid @RequestBody TriggerDefinitionServiceController.DynamicWebhookEnableRequest dynamicWebhookEnableRequest) {

        return ResponseEntity.ok(triggerDefinitionService.executeDynamicWebhookEnable(
            dynamicWebhookEnableRequest.componentName, dynamicWebhookEnableRequest.componentVersion,
            dynamicWebhookEnableRequest.triggerName, dynamicWebhookEnableRequest.connectionParameters,
            dynamicWebhookEnableRequest.authorizationName, dynamicWebhookEnableRequest.triggerParameters,
            dynamicWebhookEnableRequest.webhookUrl, dynamicWebhookEnableRequest.workflowExecutionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-definition-service/execute-dynamic-webhook-refresh",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<DynamicWebhookEnableOutput> executeDynamicWebhookRefresh(
        @Valid @RequestBody DynamicWebhookRefreshRequest dynamicWebhookRefreshRequest) {

        return ResponseEntity.ok(triggerDefinitionService.executeDynamicWebhookRefresh(
            dynamicWebhookRefreshRequest.componentName, dynamicWebhookRefreshRequest.componentVersion,
            dynamicWebhookRefreshRequest.triggerName,
            dynamicWebhookRefreshRequest.output));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-definition-service/execute-listener-disable",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void>
        executeListenerDisable(@Valid @RequestBody ListenerDisableRequest listenerDisableRequest) {
        triggerDefinitionService.executeListenerDisable(
            listenerDisableRequest.componentName, listenerDisableRequest.componentVersion,
            listenerDisableRequest.triggerName, listenerDisableRequest.connectionParameters,
            listenerDisableRequest.authorizationName, listenerDisableRequest.triggerParameters,
            listenerDisableRequest.workflowExecutionId);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-definition-service/execute-listener-enable",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> executeListenerEnable(@Valid @RequestBody ListenerEnableRequest listenerEnableRequest) {
        triggerDefinitionService.executeListenerEnable(
            listenerEnableRequest.componentName, listenerEnableRequest.componentVersion,
            listenerEnableRequest.triggerName, listenerEnableRequest.connectionParameters,
            listenerEnableRequest.authorizationName, listenerEnableRequest.triggerParameters,
            listenerEnableRequest.workflowExecutionId);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/trigger-definition-service/get-trigger-definition/{componentName}/{componentVersion}/{triggerName}",
        produces = {
            "application/json"
        })
    public ResponseEntity<TriggerDefinitionDTO> getTriggerDefinition(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion, @PathVariable("triggerName") String triggerName) {

        return ResponseEntity.ok(
            triggerDefinitionService.getTriggerDefinition(componentName, componentVersion, triggerName));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/trigger-definition-service/get-trigger-definitions/{componentName}/{componentVersion}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<TriggerDefinitionDTO>> getTriggerDefinitions(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion) {

        return ResponseEntity.ok(triggerDefinitionService.getTriggerDefinitions(componentName, componentVersion));
    }

    private record DynamicWebhookDisableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, Object> triggerParameters, String authorizationName,
        Map<String, Object> connectionParameters, DynamicWebhookEnableOutput output,
        @NotNull String workflowExecutionId) {
    }

    private record DynamicWebhookEnableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, Object> triggerParameters, String authorizationName,
        Map<String, Object> connectionParameters, @NotNull String webhookUrl, @NotNull String workflowExecutionId) {
    }

    private record DynamicWebhookRefreshRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        DynamicWebhookEnableOutput output) {
    }

    private record ListenerDisableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, Object> triggerParameters, String authorizationName, Map<String, Object> connectionParameters,
        @NotNull String workflowExecutionId) {
    }

    private record ListenerEnableRequest(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, Object> triggerParameters, String authorizationName, Map<String, Object> connectionParameters,
        @NotNull String workflowExecutionId) {
    }
}
