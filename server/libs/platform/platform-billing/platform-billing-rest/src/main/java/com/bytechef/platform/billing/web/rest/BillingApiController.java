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
import com.bytechef.platform.billing.web.rest.model.BillingSubscriptionModel;
import com.bytechef.platform.billing.web.rest.model.CheckoutSessionModel;
import com.bytechef.platform.billing.web.rest.model.CheckoutSessionRequestModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Matija Petanjek
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
@ConditionalOnProperty(prefix = "bytechef.billing", name = "enabled", havingValue = "true")
public class BillingApiController implements BillingApi {

    private final BillingSubscriptionFacade billingSubscriptionFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public BillingApiController(BillingSubscriptionFacade billingSubscriptionFacade,
        ConversionService conversionService) {
        this.billingSubscriptionFacade = billingSubscriptionFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Void> upgradeSubscription(CheckoutSessionRequestModel checkoutSessionRequestModel) {
        billingSubscriptionFacade.updateSubscription(
            checkoutSessionRequestModel.getPlanName()
                .getValue());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> reactivateSubscription() {
        billingSubscriptionFacade.reactivateSubscription();

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> cancelSubscription() {
        billingSubscriptionFacade.cancelSubscription();

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<CheckoutSessionModel> createCheckoutSession(
        CheckoutSessionRequestModel checkoutSessionRequestModel) {

        String checkoutUrl = billingSubscriptionFacade.createCheckoutSession(
            checkoutSessionRequestModel.getPlanName()
                .getValue());

        return ResponseEntity.ok(new CheckoutSessionModel().checkoutUrl(checkoutUrl));
    }

    @Override
    public ResponseEntity<BillingSubscriptionModel> getCurrentSubscription() {
        return ResponseEntity.ok(
            billingSubscriptionFacade.fetchCurrentSubscription()
                .map(dto -> conversionService.convert(dto, BillingSubscriptionModel.class))
                .orElse(null));
    }
}
