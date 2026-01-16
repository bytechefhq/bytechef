/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.web.rest.model.StartWebhookTriggerTest200ResponseModel;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.constant.PlatformType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.automation.configuration.web.rest.WebhookTriggerTestApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnCoordinator
public class WebhookTriggerTestApiController implements WebhookTriggerTestApi {

    private final WebhookTriggerTestFacade webhookTriggerTestFacade;

    public WebhookTriggerTestApiController(WebhookTriggerTestFacade webhookTriggerTestFacade) {
        this.webhookTriggerTestFacade = webhookTriggerTestFacade;
    }

    @Override
    public ResponseEntity<StartWebhookTriggerTest200ResponseModel> startWebhookTriggerTest(
        String workflowId, Long environmentId) {

        String webhookUrl = webhookTriggerTestFacade.enableTrigger(workflowId, environmentId, PlatformType.AUTOMATION);

        return ResponseEntity.ok(
            new StartWebhookTriggerTest200ResponseModel()
                .webhookUrl(webhookUrl));
    }

    @Override
    public ResponseEntity<Void> stopWebhookTriggerTest(String workflowId, Long environmentId) {
        webhookTriggerTestFacade.disableTrigger(workflowId, environmentId, PlatformType.AUTOMATION);

        return ResponseEntity.noContent()
            .build();
    }
}
