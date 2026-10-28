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

package com.bytechef.platform.billing.handler.prepaid;

import com.bytechef.platform.billing.exception.PaymentClientException;
import com.stripe.exception.StripeException;
import com.stripe.model.LineItem;
import com.stripe.model.SubscriptionSchedule;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionScheduleCreateParams;
import com.stripe.param.SubscriptionScheduleUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionListLineItemsParams;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author Matija Petanjek
 */
@Service
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class PrepaidStripeClientImpl implements PrepaidStripeClient {

    @Override
    public Session createSubscriptionCheckoutSession(
        String customerId, String flatPriceId, String planName, String successUrl, String cancelUrl,
        String tenantId) {

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(flatPriceId)
                        .setQuantity(1L)
                        .build())
                .putMetadata("planName", planName)
                .putMetadata("subscriptionModel", "PREPAID")
                .putMetadata("tenantId", tenantId)
                .setAllowPromotionCodes(true)
                .setClientReferenceId(tenantId)
                .setPaymentMethodCollection(SessionCreateParams.PaymentMethodCollection.IF_REQUIRED)
                .setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("tenantId", tenantId)
                        .build())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .build();

            return Session.create(params);
        } catch (StripeException stripeException) {
            throw new PaymentClientException(
                "Failed to create checkout session for customer " + customerId, stripeException);
        }
    }

    @Override
    public void scheduleDowngrade(
        String subscriptionId, String newFlatPriceId, String planName, String tenantId, long currentPeriodEnd) {

        try {
            SubscriptionSchedule schedule = SubscriptionSchedule.create(
                SubscriptionScheduleCreateParams.builder()
                    .setFromSubscription(subscriptionId)
                    .build());

            SubscriptionSchedule.Phase currentPhase = schedule.getPhases()
                .getFirst();

            SubscriptionScheduleUpdateParams.Phase.Builder phase1Builder =
                buildPhaseWithCurrentItems(currentPhase)
                    .setStartDate(currentPhase.getStartDate())
                    .setEndDate(currentPeriodEnd);

            addDiscountsToPhase(currentPhase, phase1Builder);

            SubscriptionScheduleUpdateParams.Phase.Builder phase2Builder =
                SubscriptionScheduleUpdateParams.Phase.builder()
                    .setStartDate(currentPeriodEnd)
                    .setEndDate(currentPeriodEnd + 120L)
                    .addItem(
                        SubscriptionScheduleUpdateParams.Phase.Item.builder()
                            .setPrice(newFlatPriceId)
                            .setQuantity(1L)
                            .build());

            addDiscountsToPhase(currentPhase, phase2Builder);

            schedule.update(
                SubscriptionScheduleUpdateParams.builder()
                    .setEndBehavior(SubscriptionScheduleUpdateParams.EndBehavior.RELEASE)
                    .addPhase(phase1Builder.build())
                    .addPhase(phase2Builder.build())
                    .putMetadata("planName", planName)
                    .putMetadata("tenantId", tenantId)
                    .build());
        } catch (StripeException stripeException) {
            throw new PaymentClientException(
                "Failed to schedule downgrade for subscription " + subscriptionId, stripeException);
        }
    }

    @Override
    public Session createJobExecutionsPackageCheckoutSession(
        String customerId, String packagePriceId, long quantity, String successUrl, String cancelUrl,
        String tenantId) {

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setCustomer(customerId)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(packagePriceId)
                        .setQuantity(quantity)
                        .build())
                .putMetadata("purchaseType", "JOB_EXECUTIONS_PACKAGE")
                .putMetadata("tenantId", tenantId)
                .setClientReferenceId(tenantId)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .build();

            return Session.create(params);
        } catch (StripeException stripeException) {
            throw new PaymentClientException(
                "Failed to create job executions package checkout session for customer " + customerId,
                stripeException);
        }
    }

    @Override
    public List<LineItem> retrieveCheckoutSessionLineItems(String sessionId) {
        try {
            SessionListLineItemsParams params = SessionListLineItemsParams.builder()
                .addExpand("data.price")
                .build();

            return Session.retrieve(sessionId)
                .listLineItems(params)
                .getData();
        } catch (StripeException stripeException) {
            throw new PaymentClientException(
                "Failed to retrieve line items of checkout session " + sessionId, stripeException);
        }
    }

    private SubscriptionScheduleUpdateParams.Phase.Builder buildPhaseWithCurrentItems(
        SubscriptionSchedule.Phase phase) {

        SubscriptionScheduleUpdateParams.Phase.Builder builder = SubscriptionScheduleUpdateParams.Phase.builder();

        for (SubscriptionSchedule.Phase.Item item : phase.getItems()) {
            SubscriptionScheduleUpdateParams.Phase.Item.Builder itemBuilder =
                SubscriptionScheduleUpdateParams.Phase.Item.builder()
                    .setPrice(item.getPrice());

            if (item.getQuantity() != null) {
                itemBuilder.setQuantity(item.getQuantity());
            }

            builder.addItem(itemBuilder.build());
        }

        return builder;
    }

    private void addDiscountsToPhase(
        SubscriptionSchedule.Phase sourcePhase,
        SubscriptionScheduleUpdateParams.Phase.Builder targetPhaseBuilder) {

        List<SubscriptionSchedule.Phase.Discount> discounts = sourcePhase.getDiscounts();

        if (discounts == null || discounts.isEmpty()) {
            return;
        }

        for (SubscriptionSchedule.Phase.Discount discount : discounts) {
            if (discount.getCoupon() != null) {
                targetPhaseBuilder.addDiscount(
                    SubscriptionScheduleUpdateParams.Phase.Discount.builder()
                        .setCoupon(discount.getCoupon())
                        .build());
            }
        }
    }
}
