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

package com.bytechef.platform.billing.facade;

import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.billing.audit.BillingAuditEvents;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import com.bytechef.platform.billing.handler.SubscriptionModelHandlerRegistry;
import com.bytechef.platform.billing.handler.prepaid.PrepaidSubscriptionModelHandler;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @author Matija Petanjek
 */
@Service
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class BillingSubscriptionFacadeImpl implements BillingSubscriptionFacade {

    private static final int MAX_PACKAGE_QUANTITY = 100;
    private static final int MIN_PACKAGE_QUANTITY = 1;

    private final BillingSubscriptionService billingSubscriptionService;
    private final PrepaidSubscriptionModelHandler prepaidSubscriptionModelHandler;
    private final SubscriptionModelHandlerRegistry subscriptionModelHandlerRegistry;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public BillingSubscriptionFacadeImpl(
        BillingSubscriptionService billingSubscriptionService,
        PrepaidSubscriptionModelHandler prepaidSubscriptionModelHandler,
        SubscriptionModelHandlerRegistry subscriptionModelHandlerRegistry) {

        this.billingSubscriptionService = billingSubscriptionService;
        this.prepaidSubscriptionModelHandler = prepaidSubscriptionModelHandler;
        this.subscriptionModelHandlerRegistry = subscriptionModelHandlerRegistry;
    }

    @Override
    @Audited(event = BillingAuditEvents.BILLING_SUBSCRIPTION_CANCELLED)
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void cancelSubscription() {
        BillingSubscription subscription = billingSubscriptionService.fetchCurrentSubscription()
            .orElseThrow(() -> new IllegalStateException("No active subscription found"));

        subscriptionModelHandlerRegistry.getSubscriptionModelHandler(subscription.getSubscriptionModel())
            .cancelSubscription(subscription);
    }

    @Override
    @Audited(event = BillingAuditEvents.BILLING_CHECKOUT_SESSION_CREATED, data = {
        @AuditData(key = "planName", value = "#planName")
    })
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public String createCheckoutSession(String planName) {
        return subscriptionModelHandlerRegistry.getSubscriptionModelHandler(SubscriptionModel.PREPAID)
            .createCheckoutSession(planName);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public String createJobExecutionsPackageCheckoutSession(int quantity) {
        if (quantity < MIN_PACKAGE_QUANTITY || quantity > MAX_PACKAGE_QUANTITY) {
            throw new IllegalArgumentException(
                "Package quantity must be between " + MIN_PACKAGE_QUANTITY + " and " + MAX_PACKAGE_QUANTITY);
        }

        BillingSubscription subscription = billingSubscriptionService.fetchCurrentSubscription()
            .orElseThrow(() -> new IllegalStateException("No active subscription found"));

        if (subscription.getSubscriptionModel() != SubscriptionModel.PREPAID
            || "TRIAL".equals(subscription.getPlanName())
            || subscription.getStatus() != BillingSubscription.Status.ACTIVE
            || subscription.isCancelAtPeriodEnd()) {

            throw new IllegalStateException(
                "Job executions packages can only be purchased with an active, non-canceling paid subscription");
        }

        return prepaidSubscriptionModelHandler.createJobExecutionsPackageCheckoutSession(quantity);
    }

    @Override
    @Audited(event = BillingAuditEvents.BILLING_SUBSCRIPTION_REACTIVATED)
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void reactivateSubscription() {
        BillingSubscription subscription = billingSubscriptionService.fetchCurrentSubscription()
            .orElseThrow(() -> new IllegalStateException("No active subscription found"));

        subscriptionModelHandlerRegistry.getSubscriptionModelHandler(subscription.getSubscriptionModel())
            .reactivateSubscription(subscription);
    }

    @Override
    @Audited(event = BillingAuditEvents.BILLING_SUBSCRIPTION_UPDATED, data = {
        @AuditData(key = "planName", value = "#newPlanName")
    })
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void updateSubscription(String newPlanName) {
        BillingSubscription currentSubscription = billingSubscriptionService.fetchCurrentSubscription()
            .orElseThrow(() -> new IllegalStateException("No active subscription found"));

        subscriptionModelHandlerRegistry.getSubscriptionModelHandler(currentSubscription.getSubscriptionModel())
            .updateSubscription(currentSubscription, newPlanName);
    }

    @Override
    public Optional<BillingSubscriptionDTO> fetchCurrentSubscription() {
        return billingSubscriptionService.fetchCurrentSubscription()
            .map(subscription -> subscriptionModelHandlerRegistry
                .getSubscriptionModelHandler(subscription.getSubscriptionModel())
                .toBillingSubscriptionDTO(subscription));
    }
}
