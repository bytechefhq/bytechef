/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.web.rest.model.StartWebhookTriggerTest200ResponseModel;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.constant.ModeType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.web.rest.WebhookTriggerTestApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
public class WebhookTriggerTestApiController implements WebhookTriggerTestApi {

    private final WebhookTriggerTestFacade webhookTriggerTestFacade;

    public WebhookTriggerTestApiController(WebhookTriggerTestFacade webhookTriggerTestFacade) {
        this.webhookTriggerTestFacade = webhookTriggerTestFacade;
    }

    @Override
    public ResponseEntity<StartWebhookTriggerTest200ResponseModel> startWebhookTriggerTest(String workflowId) {

        String webhookUrl = webhookTriggerTestFacade.enableTrigger(workflowId, ModeType.EMBEDDED);

        return ResponseEntity.ok(
            new StartWebhookTriggerTest200ResponseModel()
                .webhookUrl(webhookUrl));
    }

    @Override
    public ResponseEntity<Void> stopWebhookTriggerTest(String workflowId) {
        webhookTriggerTestFacade.disableTrigger(workflowId, ModeType.EMBEDDED);

        return ResponseEntity.noContent()
            .build();
    }
}
