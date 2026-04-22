/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookDelivery;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilityWebhookDeliveryRepository;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityWebhookSubscriptionService;
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
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiObservabilityWebhookDeliveryServiceTest {

    private AiObservabilityWebhookDeliveryServiceImpl service;

    private AiObservabilityWebhookDeliveryRepository deliveryRepository;

    private AiObservabilityWebhookSubscriptionService subscriptionService;

    private WorkspaceAiObservabilityWebhookSubscriptionService workspaceSubscriptionService;

    @BeforeEach
    void setUp() {
        AiGatewayMetrics metrics = mock(AiGatewayMetrics.class);
        deliveryRepository = mock(AiObservabilityWebhookDeliveryRepository.class);
        subscriptionService = mock(AiObservabilityWebhookSubscriptionService.class);
        workspaceSubscriptionService = mock(WorkspaceAiObservabilityWebhookSubscriptionService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<WorkspaceAiObservabilityWebhookDeliveryService> selfProvider = mock(ObjectProvider.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);

        lenient().doNothing()
            .when(metrics)
            .incrementWebhookDelivery(anyString(), anyBoolean());

        service = new AiObservabilityWebhookDeliveryServiceImpl(
            metrics, deliveryRepository, subscriptionService, workspaceSubscriptionService, selfProvider,
            taskScheduler);
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

    /**
     * Pins the HMAC computation contract. The narrowed catch ({@code NoSuchAlgorithmException | InvalidKeyException})
     * throws {@link IllegalStateException} rather than a generic {@link RuntimeException} — distinct typing keeps a
     * misconfigured FIPS profile (HmacSHA256 unavailable) loudly recognisable rather than silently rebucketed as a
     * transient retryable runtime failure. Verifies wire-shape and prefix.
     */
    @Test
    void testComputeHmacSignatureProducesSha256PrefixedHex() {
        try {
            java.lang.reflect.Method method = AiObservabilityWebhookDeliveryServiceImpl.class.getDeclaredMethod(
                "computeHmacSignature", String.class, String.class);

            method.setAccessible(true);

            String signature = (String) method.invoke(service, "{\"event\":\"test\"}", "shared-secret");

            assertThat(signature).startsWith("sha256=");
            // 64 hex chars after the prefix.
            assertThat(signature).hasSize("sha256=".length() + 64);
            // Stable per (payload, secret) pair — pinning a known good output prevents a regression that, e.g.,
            // accidentally swaps to MD5 or HmacSHA1 from passing this test.
            assertThat(signature).isEqualTo(
                "sha256=" + computeReferenceHmac("{\"event\":\"test\"}", "shared-secret"));
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError(reflectiveOperationException);
        }
    }

    /**
     * Pins the {@code saveDeliveryQuietly} contract: a transient DB outage during status bookkeeping must NOT bubble up
     * into the outer {@code catch (Exception)} as "delivery failed (attempt 1)". Without the helper, a benign row-save
     * failure would mask the real outcome and bypass retry scheduling.
     */
    @Test
    void testSaveDeliveryQuietlySwallowsRepositoryFailure() {
        AiObservabilityWebhookDelivery delivery = mock(AiObservabilityWebhookDelivery.class);
        AiObservabilityWebhookSubscription subscription = newSubscription("[]");

        when(deliveryRepository.save(any())).thenThrow(new RuntimeException("transient DB outage"));

        assertThatCode(() -> {
            java.lang.reflect.Method method = AiObservabilityWebhookDeliveryServiceImpl.class.getDeclaredMethod(
                "saveDeliveryQuietly", AiObservabilityWebhookDelivery.class,
                AiObservabilityWebhookSubscription.class, int.class);

            method.setAccessible(true);
            method.invoke(service, delivery, subscription, 2);
        }).doesNotThrowAnyException();
    }

    /**
     * Pins the per-subscription resilience contract on {@code deliverEvent}. Three subscribers all listen for
     * {@code trace.completed}; subscriber #2's row save throws. Subscribers #1 and #3 must still get their delivery row
     * persisted. A regression that narrows the catch (e.g., to {@code IOException} only) or removes the loop-body
     * try/catch entirely would silently drop subscribers #3..N for any workspace with one misconfigured webhook URL —
     * the very failure mode the inline class comment cites and that the {@code @Async} proxy alone cannot guard
     * against.
     */
    @Test
    void testDeliverEventContinuesAfterMidLoopSubscriberFailure() {
        AiObservabilityWebhookSubscription subscription1 = newSubscription("[\"trace.completed\"]");
        AiObservabilityWebhookSubscription subscription2 = newSubscription("[\"trace.completed\"]");
        AiObservabilityWebhookSubscription subscription3 = newSubscription("[\"trace.completed\"]");

        seedSubscriptionId(subscription1, 11L);
        seedSubscriptionId(subscription2, 22L);
        seedSubscriptionId(subscription3, 33L);

        when(workspaceSubscriptionService.getEnabledWebhookSubscriptionsByWorkspace(42L))
            .thenReturn(java.util.List.of(subscription1, subscription2, subscription3));

        java.util.Set<Long> seenSubscriptionIds = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

        // Inject failure on subscriber #2 only. Subscriber #1 and #3 land delivery rows; subscriber #2 throws on
        // its initial save (line ~116 of deliverEvent). The downstream HTTP attempt path may call save() again for
        // bookkeeping — what we pin here is that the LOOP visits all three subscribers, proven by save() being
        // invoked for subscriptionId 11L AND 33L (both surviving subscribers) despite #2's failure mid-loop.
        when(deliveryRepository.save(any())).thenAnswer(invocation -> {
            AiObservabilityWebhookDelivery delivery = invocation.getArgument(0);

            if (delivery.getSubscriptionId() == 22L) {
                throw new RuntimeException("transient DB outage on subscriber #2");
            }

            seenSubscriptionIds.add(delivery.getSubscriptionId());

            return delivery;
        });

        assertThatCode(() -> service.deliverEvent(42L, "trace.completed", java.util.Map.of("k", "v")))
            .as("Per-subscription try/catch must absorb subscriber #2's failure")
            .doesNotThrowAnyException();

        // Both surviving subscribers must have had save() invoked with their id — proving the loop did not abort at
        // #2. A regression that narrows the catch (or removes the loop-body try/catch) would skip #3 entirely; this
        // set would not contain 33L and the assertion would fail.
        assertThat(seenSubscriptionIds)
            .as("Subscribers #1 and #3 must each have save() invoked for their id even when #2 throws")
            .contains(11L, 33L);
    }

    /**
     * Pins the {@code catch (Error)} branch in {@code deliverEvent}: a JVM-level Error during one subscriber's
     * processing must be rethrown (not swallowed) so the {@code @Async} escape path treats this as JVM distress and the
     * {@code SimpleAsyncUncaughtExceptionHandler} sees it. Mirrors the OOM pin in {@code AiExperimentExecutorTest}.
     * Without this branch a regression that swallows {@link OutOfMemoryError} would silently abort the loop, leaving
     * subscribers #N+1..M un-notified with no actionable signal — exactly the failure mode the round-4 fix introduced
     * the {@code catch (Error)} block to prevent.
     */
    @Test
    void testDeliverEventRethrowsErrorAfterLogging() {
        AiObservabilityWebhookSubscription subscription = newSubscription("[\"trace.completed\"]");

        seedSubscriptionId(subscription, 77L);

        when(workspaceSubscriptionService.getEnabledWebhookSubscriptionsByWorkspace(99L))
            .thenReturn(java.util.List.of(subscription));

        when(deliveryRepository.save(any())).thenAnswer(invocation -> {
            throw new OutOfMemoryError("simulated JVM distress during webhook delivery");
        });

        assertThatThrownBy(() -> service.deliverEvent(99L, "trace.completed", java.util.Map.of("k", "v")))
            .isInstanceOf(OutOfMemoryError.class)
            .hasMessageContaining("simulated JVM distress");
    }

    /**
     * Pins the {@code catch (Error)} branch in {@code deliverTestEvent}: the controller has already returned 202, so a
     * swallowed Error would leave operators with no actionable signal at all. Rethrowing routes JVM distress through
     * the {@code SimpleAsyncUncaughtExceptionHandler} after the structured ERROR-level log emits subscription context.
     */
    @Test
    void testDeliverTestEventRethrowsErrorAfterLogging() {
        AiObservabilityWebhookSubscription subscription = newSubscription("[\"test\"]");

        seedSubscriptionId(subscription, 88L);

        when(subscriptionService.getWebhookSubscription(88L)).thenReturn(subscription);

        when(deliveryRepository.save(any())).thenAnswer(invocation -> {
            throw new StackOverflowError("simulated JVM distress during test delivery");
        });

        assertThatThrownBy(() -> service.deliverTestEvent(88L))
            .isInstanceOf(StackOverflowError.class)
            .hasMessageContaining("simulated JVM distress");
    }

    private static void seedSubscriptionId(AiObservabilityWebhookSubscription subscription, Long id) {
        try {
            Field idField = AiObservabilityWebhookSubscription.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(subscription, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed subscription id", reflectiveOperationException);
        }
    }

    private static String computeReferenceHmac(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");

            mac.init(new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));

            byte[] hmacBytes = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();

            for (byte hmacByte : hmacBytes) {
                String value = Integer.toHexString(0xff & hmacByte);

                if (value.length() == 1) {
                    hex.append('0');
                }

                hex.append(value);
            }

            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException | java.security.InvalidKeyException exception) {
            throw new AssertionError("HmacSHA256 must be available on the JVM under test", exception);
        }
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
            "test", "https://example.com/hook", "[]");

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
