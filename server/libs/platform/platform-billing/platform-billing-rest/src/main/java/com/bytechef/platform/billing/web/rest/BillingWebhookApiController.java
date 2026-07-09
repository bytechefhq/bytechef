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
import com.bytechef.platform.billing.facade.BillingCheckoutFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Matija Petanjek
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}")
@ConditionalOnCoordinator
public class BillingWebhookApiController implements BillingWebhookApi {

    private final BillingCheckoutFacade billingCheckoutFacade;

    public BillingWebhookApiController(BillingCheckoutFacade billingCheckoutFacade) {
        this.billingCheckoutFacade = billingCheckoutFacade;
    }

    @Override
    public ResponseEntity<Void> handleWebhook(String stripeSignature, String body) {
        billingCheckoutFacade.handleWebhookEvent(body, stripeSignature);

        return ResponseEntity.ok()
            .build();
    }
}
