
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
        @Valid @RequestBody DynamicWebhookDisable dynamicWebhookDisable) {

        triggerDefinitionService.executeDynamicWebhookDisable(
            dynamicWebhookDisable.componentName, dynamicWebhookDisable.componentVersion,
            dynamicWebhookDisable.triggerName,
            dynamicWebhookDisable.connectionParameters,
            dynamicWebhookDisable.authorizationName, dynamicWebhookDisable.triggerParameters,
            dynamicWebhookDisable.workflowExecutionId, dynamicWebhookDisable.output);

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
        @Valid @RequestBody DynamicWebhookEnable dynamicWebhookEnable) {

        return ResponseEntity.ok(triggerDefinitionService.executeDynamicWebhookEnable(
            dynamicWebhookEnable.componentName, dynamicWebhookEnable.componentVersion, dynamicWebhookEnable.triggerName,
            dynamicWebhookEnable.connectionParameters, dynamicWebhookEnable.authorizationName,
            dynamicWebhookEnable.triggerParameters, dynamicWebhookEnable.webhookUrl,
            dynamicWebhookEnable.workflowExecutionId));
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
        @Valid @RequestBody DynamicWebhookRefresh dynamicWebhookRefresh) {

        return ResponseEntity.ok(triggerDefinitionService.executeDynamicWebhookRefresh(
            dynamicWebhookRefresh.componentName, dynamicWebhookRefresh.componentVersion,
            dynamicWebhookRefresh.triggerName,
            dynamicWebhookRefresh.output));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-definition-service/execute-listener-disable",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> executeListenerDisable(@Valid @RequestBody ListenerDisable listenerDisable) {
        triggerDefinitionService.executeListenerDisable(
            listenerDisable.componentName, listenerDisable.componentVersion, listenerDisable.triggerName,
            listenerDisable.connectionParameters, listenerDisable.authorizationName,
            listenerDisable.triggerParameters, listenerDisable.workflowExecutionId);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-definition-service/execute-listener-enable",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> executeListenerEnable(@Valid @RequestBody ListenerEnable listenerEnable) {
        triggerDefinitionService.executeListenerEnable(
            listenerEnable.componentName, listenerEnable.componentVersion, listenerEnable.triggerName,
            listenerEnable.connectionParameters, listenerEnable.authorizationName, listenerEnable.triggerParameters,
            listenerEnable.workflowExecutionId);

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

    private record DynamicWebhookDisable(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, Object> triggerParameters, String authorizationName,
        Map<String, Object> connectionParameters, DynamicWebhookEnableOutput output,
        @NotNull String workflowExecutionId) {
    }

    private record DynamicWebhookEnable(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, Object> triggerParameters, String authorizationName,
        Map<String, Object> connectionParameters, @NotNull String webhookUrl, @NotNull String workflowExecutionId) {
    }

    private record DynamicWebhookRefresh(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        DynamicWebhookEnableOutput output) {
    }

    private record ListenerDisable(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, Object> triggerParameters, String authorizationName, Map<String, Object> connectionParameters,
        @NotNull String workflowExecutionId) {
    }

    private record ListenerEnable(
        @NotNull String componentName, int componentVersion, @NotNull String triggerName,
        Map<String, Object> triggerParameters, String authorizationName, Map<String, Object> connectionParameters,
        @NotNull String workflowExecutionId) {
    }
}
