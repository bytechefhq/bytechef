
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

package com.bytechef.hermes.component.registry.remote.web.rest.service;

import com.bytechef.hermes.component.registry.dto.WebhookTriggerFlags;
import com.bytechef.hermes.component.registry.domain.TriggerDefinition;
import com.bytechef.hermes.component.registry.service.RemoteTriggerDefinitionService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/trigger-definition-service")
public class RemoteTriggerDefinitionServiceController {

    private final RemoteTriggerDefinitionService triggerDefinitionService;

    public RemoteTriggerDefinitionServiceController(RemoteTriggerDefinitionService triggerDefinitionService) {
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-trigger-definition/{componentName}/{componentVersion}/{triggerName}",
        produces = {
            "application/json"
        })
    public ResponseEntity<TriggerDefinition> getTriggerDefinition(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion, @PathVariable("triggerName") String triggerName) {

        return ResponseEntity.ok(
            triggerDefinitionService.getTriggerDefinition(componentName, componentVersion, triggerName));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-trigger-definitions/{componentName}/{componentVersion}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<TriggerDefinition>> getTriggerDefinitions(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion) {

        return ResponseEntity.ok(triggerDefinitionService.getTriggerDefinitions(componentName, componentVersion));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/trigger-definition-service/get-webhook-trigger-flags/{componentName}/{componentVersion}" +
            "/{triggerName}",
        produces = {
            "application/json"
        })
    public ResponseEntity<WebhookTriggerFlags> getWebhookTriggerFlags(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion,
        @PathVariable("triggerName") String triggerName) {

        return ResponseEntity.ok(
            triggerDefinitionService.getWebhookTriggerFlags(componentName, componentVersion, triggerName));
    }
}
