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

package com.bytechef.platform.billing.client;

import com.bytechef.platform.billing.config.BillingProperties;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.PriceCollection;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionSchedule;
import com.stripe.model.billing.MeterEvent;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.PriceRetrieveParams;
import com.stripe.param.SubscriptionScheduleCreateParams;
import com.stripe.param.SubscriptionScheduleReleaseParams;
import com.stripe.param.SubscriptionScheduleUpdateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.billing.MeterEventCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author Matija Petanjek
 */
@Service
public class StripeClientImpl implements StripeClient {

    private final BillingProperties billingProperties;

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public StripeClientImpl(BillingProperties billingProperties) {
        this.billingProperties = billingProperties;

        Stripe.apiKey = billingProperties.stripe()
            .secretKey();
    }

    @Override
    public String fetchProductDefaultPriceId(String productId) {
        try {
            PriceListParams params = PriceListParams.builder()
                .setProduct(productId)
                .setActive(true)
                .build();

            PriceCollection priceCollection = Price.list(params);

            List<Price> prices = priceCollection.getData();

            Assert.isTrue(prices.size() == 1, "Product " + productId + " has more than one price configured");

            return prices.getFirst()
                .getId();
        } catch (StripeException stripeException) {
            throw new RuntimeException(stripeException);
        }
    }

    @Override
    public void cancelAtPeriodEnd(String stripeSubscriptionId, String tenantId) {
        try {
            Subscription subscription = Subscription.retrieve(stripeSubscriptionId);

            releaseScheduleIfPresent(subscription);

            subscription.update(
                SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .putMetadata("tenantId", tenantId)
                    .build());
        } catch (StripeException stripeException) {
            throw new RuntimeException(stripeException);
        }
    }

    @Override
    public void reactivateSubscription(String stripeSubscriptionId, String tenantId) {
        try {
            Subscription subscription = Subscription.retrieve(stripeSubscriptionId);

            releaseScheduleIfPresent(subscription);

            SubscriptionUpdateParams.Builder paramsBuilder = SubscriptionUpdateParams.builder()
                .putMetadata("tenantId", tenantId);

            if (subscription.getCancelAt() != null) {
                paramsBuilder.putExtraParam("cancel_at", "");
            } else {
                paramsBuilder.setCancelAtPeriodEnd(false);
            }

            subscription.update(paramsBuilder.build());
        } catch (StripeException stripeException) {
            throw new RuntimeException(stripeException);
        }
    }

    @Override
    public void upgradeSubscriptionNow(
        String subscriptionId, String existingFlatItemId, String newFlatPriceId, String planName, String tenantId) {

        try {
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .addItem(
                    SubscriptionUpdateParams.Item.builder()
                        .setId(existingFlatItemId)
                        .setPrice(newFlatPriceId)
                        .build())
                .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE)
                .putMetadata("planName", planName)
                .putMetadata("tenantId", tenantId)
                .build();

            Subscription.retrieve(subscriptionId)
                .update(params);
        } catch (StripeException stripeException) {
            throw new RuntimeException(stripeException);
        }
    }

    @Override
    public void scheduleDowngrade(
        String subscriptionId, String existingFlatItemId, String existingMeteredItemId, String newFlatPriceId,
        String newMeteredPriceId, String planName, String tenantId, long currentPeriodEnd) {

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
                            .build())
                    .addItem(
                        SubscriptionScheduleUpdateParams.Phase.Item.builder()
                            .setPrice(newMeteredPriceId)
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
            throw new RuntimeException(stripeException);
        }
    }

    @Override
    public void releaseSubscriptionScheduleIfPresent(Subscription subscription) {
        try {
            releaseScheduleIfPresent(subscription);
        } catch (StripeException stripeException) {
            throw new RuntimeException(stripeException);
        }
    }

    @Override
    public String createCustomer(String email, String tenantId) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .putMetadata("tenantId", tenantId)
                .build();

            Customer customer = Customer.create(params);

            return customer.getId();
        } catch (StripeException stripeException) {
            throw new RuntimeException(stripeException);
        }
    }

    @Override
    public Session createCheckoutSession(
        String customerId, String flatPriceId, String usagePriceId, String planName, String successUrl,
        String cancelUrl, String tenantId) {

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(flatPriceId)
                        .setQuantity(1L)
                        .build())
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(usagePriceId)
                        .build())
                .putMetadata("planName", planName)
                .setClientReferenceId(tenantId)
                .setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("tenantId", tenantId)
                        .build())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .build();

            return Session.create(params);
        } catch (StripeException stripeException) {
            throw new RuntimeException(stripeException);
        }
    }

    @Override
    public Price retrievePrice(String priceId) {
        try {
            PriceRetrieveParams priceRetrieveParams =
                PriceRetrieveParams.builder()
                    .addExpand("tiers")
                    .build();

            return Price.retrieve(priceId, priceRetrieveParams, null);
        } catch (StripeException stripeException) {
            throw new RuntimeException(stripeException);
        }
    }

    @Override
    public Subscription retrieveSubscription(String subscriptionId) {
        try {
            return Subscription.retrieve(subscriptionId);
        } catch (StripeException stripeException) {
            throw new RuntimeException(stripeException);
        }
    }

    @Override
    public Event verifyWebhookSignature(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(payload, sigHeader, billingProperties.stripe()
                .webhookSecret());
        } catch (SignatureVerificationException signatureVerificationException) {
            throw new RuntimeException("Invalid Stripe signature");
        }
    }

    @Override
    public void reportMeterEvent(String stripeCustomerId, int quantity, String idempotencyKey) {
        try {
            MeterEventCreateParams params = MeterEventCreateParams.builder()
                .setEventName(billingProperties.stripe()
                    .meterEventName())
                .putExtraParam("payload", Map.of(
                    "stripe_customer_id", stripeCustomerId,
                    "value", String.valueOf(quantity)))
                .build();

            RequestOptions requestOptions = RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build();

            MeterEvent.create(params, requestOptions);
        } catch (StripeException stripeException) {
            throw new RuntimeException("Failed to report meter event to Stripe", stripeException);
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

    private void releaseScheduleIfPresent(Subscription subscription) throws StripeException {
        String scheduleId = subscription.getSchedule();

        if (scheduleId == null || scheduleId.isBlank()) {
            return;
        }

        SubscriptionSchedule schedule = SubscriptionSchedule.retrieve(scheduleId);

        if ("active".equals(schedule.getStatus()) || "not_started".equals(schedule.getStatus())) {
            schedule.release(SubscriptionScheduleReleaseParams.builder()
                .build());
        }
    }
}
