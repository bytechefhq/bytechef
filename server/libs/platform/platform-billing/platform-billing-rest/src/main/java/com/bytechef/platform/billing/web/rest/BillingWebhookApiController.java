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

package com.bytechef.platform.billing.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.billing.facade.BillingSubscriptionFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Matija Petanjek
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}")
@ConditionalOnCoordinator
@ConditionalOnProperty(prefix = "bytechef.billing", name = "enabled", havingValue = "true")
public class BillingWebhookApiController implements BillingWebhookApi {

    private static final Logger log = LoggerFactory.getLogger(BillingWebhookApiController.class);

    private final BillingSubscriptionFacade billingSubscriptionFacade;

    public BillingWebhookApiController(BillingSubscriptionFacade billingSubscriptionFacade) {
        this.billingSubscriptionFacade = billingSubscriptionFacade;
    }

    @Override
    public ResponseEntity<Void> handleWebhook(String stripeSignature, String body) {
        log.info("Received Stripe webhook event");

        billingSubscriptionFacade.handleWebhookEvent(body, stripeSignature);

        return ResponseEntity.ok()
            .build();
    }
}
