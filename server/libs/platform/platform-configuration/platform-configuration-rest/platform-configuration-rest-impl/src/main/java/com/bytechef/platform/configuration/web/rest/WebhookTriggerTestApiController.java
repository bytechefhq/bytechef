/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.web.rest.model.StartWebhookTriggerTest200ResponseModel;
import com.bytechef.platform.constant.ModeType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class WebhookTriggerTestApiController implements WebhookTriggerTestApi {

    private final WebhookTriggerTestFacade webhookTriggerTestFacade;

    public WebhookTriggerTestApiController(WebhookTriggerTestFacade webhookTriggerTestFacade) {
        this.webhookTriggerTestFacade = webhookTriggerTestFacade;
    }

    @Override
    public ResponseEntity<StartWebhookTriggerTest200ResponseModel> startWebhookTriggerTest(
        Integer modeType, String workflowId) {

        String webhookUrl = webhookTriggerTestFacade.enableTrigger(workflowId, ModeType.values()[modeType]);

        System.out.println(webhookUrl);

        return ResponseEntity.ok(
            new StartWebhookTriggerTest200ResponseModel()
                .webhookUrl(webhookUrl)
        );
    }

    @Override
    public ResponseEntity<Void> stopWebhookTriggerTest(Integer modeType, String workflowId) {
        webhookTriggerTestFacade.disableTrigger(workflowId, ModeType.values()[modeType]);

        return ResponseEntity.noContent()
            .build();
    }
}
