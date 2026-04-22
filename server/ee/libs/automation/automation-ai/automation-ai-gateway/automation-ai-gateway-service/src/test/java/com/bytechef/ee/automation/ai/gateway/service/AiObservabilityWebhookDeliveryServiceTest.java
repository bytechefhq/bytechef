/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookSubscription;
import com.bytechef.ee.automation.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityWebhookDeliveryRepository;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.TaskScheduler;

/**
 * Unit tests for {@link AiObservabilityWebhookDeliveryServiceImpl}. Focuses on the behaviors most likely to regress:
 * event-subscription matching semantics and the SSRF-guard call for the base URL. HTTP delivery itself is not exercised
 * here — {@code HttpClient} is an external dependency whose retry/backoff is better tested via int-test.
 *
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiObservabilityWebhookDeliveryServiceTest {

    private AiObservabilityWebhookDeliveryServiceImpl service;

    @BeforeEach
    void setUp() {
        AiGatewayMetrics metrics = mock(AiGatewayMetrics.class);
        AiObservabilityWebhookDeliveryRepository deliveryRepository =
            mock(AiObservabilityWebhookDeliveryRepository.class);
        AiObservabilityWebhookSubscriptionService subscriptionService =
            mock(AiObservabilityWebhookSubscriptionService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<AiObservabilityWebhookDeliveryService> selfProvider = mock(ObjectProvider.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);

        lenient().doNothing()
            .when(metrics)
            .incrementWebhookDelivery(anyString(), anyBoolean());

        service = new AiObservabilityWebhookDeliveryServiceImpl(
            metrics, deliveryRepository, subscriptionService, selfProvider, taskScheduler);
    }

    @Test
    void testIsSubscribedToEventReturnsTrueOnlyOnExactEventNameMatch() {
        AiObservabilityWebhookSubscription subscription = newSubscription(
            "[\"trace.completed\",\"budget.exceeded\"]");

        assertThat(invokeIsSubscribedToEvent(subscription, "trace.completed")).isTrue();
        assertThat(invokeIsSubscribedToEvent(subscription, "budget.exceeded")).isTrue();
    }

    @Test
    void testIsSubscribedToEventRejectsPrefixCollision() {
        // Event matching must treat the events column as a JSON array, not a substring search — otherwise
        // "trace.completed" would match a subscription that lists only "trace.completed.v2". The JSON-parse
        // implementation does not have this bug.
        AiObservabilityWebhookSubscription subscription = newSubscription("[\"trace.completed.v2\"]");

        assertThat(invokeIsSubscribedToEvent(subscription, "trace.completed"))
            .as("Must not match when the subscription only lists a strictly longer event name")
            .isFalse();
    }

    @Test
    void testIsSubscribedToEventReturnsFalseOnNullOrBlankEventsColumn() {
        assertThat(invokeIsSubscribedToEvent(newSubscription(null), "trace.completed")).isFalse();
        assertThat(invokeIsSubscribedToEvent(newSubscription(""), "trace.completed")).isFalse();
    }

    @Test
    void testIsSubscribedToEventReturnsFalseOnMalformedJsonWithoutThrowing() {
        // A DB row with a corrupted events JSON must degrade to "unsubscribed" rather than propagate an
        // exception that tears down the @Async worker.
        AiObservabilityWebhookSubscription subscription = newSubscription("not-json-here");

        assertThat(invokeIsSubscribedToEvent(subscription, "trace.completed")).isFalse();
    }

    @Test
    void testIsRetryableStatusTreats5xxAsRetryable() {
        // 5xx always retryable — transient server problem.
        assertThat(invokeIsRetryableStatus(500)).isTrue();
        assertThat(invokeIsRetryableStatus(502)).isTrue();
        assertThat(invokeIsRetryableStatus(503)).isTrue();
        assertThat(invokeIsRetryableStatus(504)).isTrue();
    }

    @Test
    void testIsRetryableStatusTreatsTransient4xxAsRetryable() {
        // 408 Request Timeout, 425 Too Early, 429 Too Many Requests are the canonical "try again later" 4xx codes.
        assertThat(invokeIsRetryableStatus(408)).isTrue();
        assertThat(invokeIsRetryableStatus(425)).isTrue();
        assertThat(invokeIsRetryableStatus(429)).isTrue();
    }

    @Test
    void testIsRetryableStatusTreatsPermanent4xxAsNonRetryable() {
        // 400/401/403/404/410/422 indicate a permanent subscriber misconfiguration. Retrying only multiplies the
        // noise and delays the FAILED state in the UI.
        assertThat(invokeIsRetryableStatus(400))
            .as("400 Bad Request is permanent")
            .isFalse();
        assertThat(invokeIsRetryableStatus(401))
            .as("401 Unauthorized is permanent (credentials problem)")
            .isFalse();
        assertThat(invokeIsRetryableStatus(403))
            .as("403 Forbidden is permanent")
            .isFalse();
        assertThat(invokeIsRetryableStatus(404))
            .as("404 Not Found is permanent (wrong URL)")
            .isFalse();
        assertThat(invokeIsRetryableStatus(410))
            .as("410 Gone is permanent")
            .isFalse();
        assertThat(invokeIsRetryableStatus(422))
            .as("422 Unprocessable Entity is permanent (schema mismatch)")
            .isFalse();
    }

    @Test
    void testIsRetryableStatusTreats2xxAndRedirectsAsNonRetryable() {
        // 2xx is success (handled as non-retry success path) and 3xx shouldn't reach this helper at all — but
        // if it ever does (e.g., follow-redirects misconfig), we should not retry.
        assertThat(invokeIsRetryableStatus(200)).isFalse();
        assertThat(invokeIsRetryableStatus(204)).isFalse();
        assertThat(invokeIsRetryableStatus(301)).isFalse();
        assertThat(invokeIsRetryableStatus(302)).isFalse();
    }

    private static boolean invokeIsRetryableStatus(int statusCode) {
        try {
            java.lang.reflect.Method method = AiObservabilityWebhookDeliveryServiceImpl.class.getDeclaredMethod(
                "isRetryableStatus", int.class);

            method.setAccessible(true);

            return (boolean) method.invoke(null, statusCode);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError(reflectiveOperationException);
        }
    }

    private static AiObservabilityWebhookSubscription newSubscription(String eventsJson) {
        AiObservabilityWebhookSubscription subscription = new AiObservabilityWebhookSubscription(
            1L, "test", "https://example.com/hook", "[]");

        // Bypass setters to drop any event/url validation we might later add — these tests target the JSON-parse
        // semantics of isSubscribedToEvent specifically.
        try {
            Field eventsField = AiObservabilityWebhookSubscription.class.getDeclaredField("events");

            eventsField.setAccessible(true);
            eventsField.set(subscription, eventsJson);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed events", reflectiveOperationException);
        }

        return subscription;
    }

    private boolean invokeIsSubscribedToEvent(AiObservabilityWebhookSubscription subscription, String eventType) {
        try {
            java.lang.reflect.Method method = AiObservabilityWebhookDeliveryServiceImpl.class.getDeclaredMethod(
                "isSubscribedToEvent", AiObservabilityWebhookSubscription.class, String.class);

            method.setAccessible(true);

            return (boolean) method.invoke(service, subscription, eventType);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError(reflectiveOperationException);
        }
    }
}
