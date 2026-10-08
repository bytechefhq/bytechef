/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.WebhookTriggerTestAdminFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.StartWebhookTriggerTest200ResponseModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@ConditionalOnEEVersion
public class WebhookTriggerTestApiController implements WebhookTriggerTestApi {

    private final WebhookTriggerTestAdminFacade webhookTriggerTestAdminFacade;

    public WebhookTriggerTestApiController(WebhookTriggerTestAdminFacade webhookTriggerTestAdminFacade) {
        this.webhookTriggerTestAdminFacade = webhookTriggerTestAdminFacade;
    }

    @Override
    public ResponseEntity<StartWebhookTriggerTest200ResponseModel> startWebhookTriggerTest(
        String workflowId, Long environmentId, String triggerName) {

        String webhookUrl = webhookTriggerTestAdminFacade.startWebhookTriggerTest(
            workflowId, triggerName, environmentId);

        return ResponseEntity.ok(
            new StartWebhookTriggerTest200ResponseModel()
                .webhookUrl(webhookUrl));
    }

    @Override
    public ResponseEntity<Void> stopWebhookTriggerTest(String workflowId, Long environmentId, String triggerName) {
        webhookTriggerTestAdminFacade.stopWebhookTriggerTest(
            workflowId, triggerName, environmentId);

        return ResponseEntity.noContent()
            .build();
    }
}
