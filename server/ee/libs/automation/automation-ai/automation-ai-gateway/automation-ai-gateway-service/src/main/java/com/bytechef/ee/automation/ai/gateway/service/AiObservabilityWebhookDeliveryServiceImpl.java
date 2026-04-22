/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookDelivery;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookDeliveryStatus;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookSubscription;
import com.bytechef.ee.automation.ai.gateway.event.AiGatewayBudgetExceededEvent;
import com.bytechef.ee.automation.ai.gateway.event.AiGatewayTraceCompletedEvent;
import com.bytechef.ee.automation.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.automation.ai.gateway.reliability.AiGatewayRetryableStatuses;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityWebhookDeliveryRepository;
import com.bytechef.ee.automation.ai.gateway.security.AiGatewayUrlValidator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers webhook payloads to subscribed endpoints with HMAC-SHA256 signing and exponential backoff retry (3
 * attempts).
 *
 * @version ee
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiObservabilityWebhookDeliveryServiceImpl implements AiObservabilityWebhookDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(AiObservabilityWebhookDeliveryServiceImpl.class);

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AiGatewayMetrics aiGatewayMetrics;
    private final AiObservabilityWebhookDeliveryRepository aiObservabilityWebhookDeliveryRepository;
    private final AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService;
    private final HttpClient httpClient;
    private final ObjectProvider<AiObservabilityWebhookDeliveryService> selfProvider;
    private final TaskScheduler taskScheduler;

    public AiObservabilityWebhookDeliveryServiceImpl(
        AiGatewayMetrics aiGatewayMetrics,
        AiObservabilityWebhookDeliveryRepository aiObservabilityWebhookDeliveryRepository,
        AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService,
        ObjectProvider<AiObservabilityWebhookDeliveryService> selfProvider,
        TaskScheduler taskScheduler) {

        this.aiGatewayMetrics = aiGatewayMetrics;
        this.aiObservabilityWebhookDeliveryRepository = aiObservabilityWebhookDeliveryRepository;
        this.aiObservabilityWebhookSubscriptionService = aiObservabilityWebhookSubscriptionService;
        this.selfProvider = selfProvider;
        this.taskScheduler = taskScheduler;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Override
    public AiObservabilityWebhookDelivery create(AiObservabilityWebhookDelivery delivery) {
        return aiObservabilityWebhookDeliveryRepository.save(delivery);
    }

    @Override
    @Async
    public void deliverEvent(Long workspaceId, String eventType, Map<String, Object> payload) {
        List<AiObservabilityWebhookSubscription> subscriptions =
            aiObservabilityWebhookSubscriptionService.getEnabledWebhookSubscriptionsByWorkspace(workspaceId);

        for (AiObservabilityWebhookSubscription subscription : subscriptions) {
            if (!isSubscribedToEvent(subscription, eventType)) {
                continue;
            }

            String jsonPayload = JsonUtils.write(Map.of(
                "event", eventType,
                "data", payload,
                "timestamp", Instant.now()
                    .toString()));

            AiObservabilityWebhookDelivery delivery = aiObservabilityWebhookDeliveryRepository.save(
                new AiObservabilityWebhookDelivery(subscription.getId(), eventType, jsonPayload));

            deliverWithRetry(subscription, jsonPayload, delivery);
        }
    }

    @Override
    @Async
    public void deliverTestEvent(long subscriptionId) {
        AiObservabilityWebhookSubscription subscription =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscription(subscriptionId);

        String jsonPayload = JsonUtils.write(Map.of(
            "event", "test",
            "data", Map.of("message", "Test webhook delivery from ByteChef AI Gateway"),
            "timestamp", Instant.now()
                .toString()));

        AiObservabilityWebhookDelivery delivery = aiObservabilityWebhookDeliveryRepository.save(
            new AiObservabilityWebhookDelivery(subscription.getId(), "test", jsonPayload));

        deliverWithRetry(subscription, jsonPayload, delivery);
    }

    @Override
    public List<AiObservabilityWebhookDelivery> getDeliveriesBySubscription(long subscriptionId) {
        return aiObservabilityWebhookDeliveryRepository.findAllBySubscriptionIdOrderByCreatedDateDesc(subscriptionId);
    }

    @Override
    public AiObservabilityWebhookDelivery update(AiObservabilityWebhookDelivery delivery) {
        return aiObservabilityWebhookDeliveryRepository.save(delivery);
    }

    // AFTER_COMMIT: never deliver a trace.completed webhook for a trace whose persisting transaction rolled back. Falls
    // back to a non-transactional invocation via fallbackExecution=true so that events published outside a transaction
    // still fire (e.g. programmatic publishes from an @Async path).
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTraceCompleted(AiGatewayTraceCompletedEvent event) {
        try {
            Map<String, Object> payload = new HashMap<>();

            payload.put("traceId", event.traceId());
            payload.put("externalTraceId", event.externalTraceId());
            payload.put("model", event.modelName());
            payload.put("totalInputTokens", event.totalInputTokens());
            payload.put("totalOutputTokens", event.totalOutputTokens());
            payload.put("totalLatencyMs", event.totalLatencyMs());
            payload.put("totalCost", event.totalCost());
            payload.put("success", event.success());
            payload.put("completedAt", event.completedAt() != null ? event.completedAt()
                .toEpochMilli() : null);

            // Resolve through the Spring proxy so @Async hops off the publishing thread instead of running inline.
            selfProvider.getObject()
                .deliverEvent(event.workspaceId(), "trace.completed", payload);
        } catch (Exception exception) {
            logger.error(
                "Failed to dispatch trace.completed webhook for trace {} — subscribers were not notified",
                event.traceId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBudgetExceeded(AiGatewayBudgetExceededEvent event) {
        try {
            Map<String, Object> payload = new HashMap<>();

            payload.put("model", event.model());
            payload.put("currentSpend", event.currentSpend());
            payload.put("budgetLimit", event.budgetLimit());

            selfProvider.getObject()
                .deliverEvent(event.workspaceId(), "budget.exceeded", payload);
        } catch (Exception exception) {
            logger.error(
                "Failed to dispatch budget.exceeded webhook for workspace {} — subscribers were not notified",
                event.workspaceId(), exception);
        }
    }

    private boolean isSubscribedToEvent(AiObservabilityWebhookSubscription subscription, String eventType) {
        String events = subscription.getEvents();

        if (events == null || events.isBlank()) {
            return false;
        }

        // Parse the JSON array once rather than relying on substring match — substring would falsely match
        // "trace.completed" against a subscription that only lists "trace.completed.v2", and vice versa.
        try {
            List<String> subscribedEvents = JsonUtils.readList(events, String.class);

            return subscribedEvents.contains(eventType);
        } catch (Exception exception) {
            logger.warn(
                "Subscription {} has malformed events JSON: {}", subscription.getId(), exception.getMessage());

            return false;
        }
    }

    /**
     * Kicks off delivery with retry. Each attempt is self-rescheduling via {@link TaskScheduler}: on transient failure
     * we schedule the next attempt with exponential backoff instead of blocking the async worker with
     * {@code Thread.sleep}. Without scheduling, a single slow/flapping subscriber could hold an async thread for tens
     * of seconds and starve the pool.
     */
    private void deliverWithRetry(
        AiObservabilityWebhookSubscription subscription, String jsonPayload,
        AiObservabilityWebhookDelivery delivery) {

        attemptDelivery(subscription, jsonPayload, delivery, 1);
    }

    private void attemptDelivery(
        AiObservabilityWebhookSubscription subscription, String jsonPayload,
        AiObservabilityWebhookDelivery delivery, int attempt) {

        delivery.setAttemptCount(attempt);

        boolean shouldRetry = false;

        try {
            // SSRF guard: re-validate per attempt so a DNS change between retries is caught at the next send.
            // Throws IllegalArgumentException if the host resolves to any private/loopback/link-local/CGNAT/IPv6-ULA
            // address.
            AiGatewayUrlValidator.validateExternalUrl(subscription.getUrl());

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(subscription.getUrl()))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

            if (subscription.getSecret() != null && !subscription.getSecret()
                .isEmpty()) {
                String signature = computeHmacSignature(jsonPayload, subscription.getSecret());

                requestBuilder.header("X-ByteChef-Signature", signature);
            }

            HttpResponse<String> response = httpClient.send(
                requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            delivery.setHttpStatus(response.statusCode());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                subscription.setLastTriggeredDate(Instant.now());
                subscription.setLastDeliveryError(null, null);

                aiObservabilityWebhookSubscriptionService.update(subscription);

                delivery.setStatus(AiObservabilityWebhookDeliveryStatus.SUCCESS);
                delivery.setDeliveredDate(Instant.now());

                aiObservabilityWebhookDeliveryRepository.save(delivery);

                aiGatewayMetrics.incrementWebhookDelivery(delivery.getEventType(), true);

                return;
            }

            delivery.setErrorMessage("HTTP " + response.statusCode());

            // 4xx responses other than 408 (Request Timeout), 425 (Too Early), and 429 (Too Many Requests) mean
            // the subscriber has a permanent problem (wrong path, revoked credentials, HMAC mismatch, etc.).
            // Retrying is wasted work — it doubles the log noise and delays the FAILED state, masking misconfigured
            // subscriptions in the UI. Transient 4xx (408/425/429) and all 5xx remain eligible for retry.
            boolean retryableStatus = isRetryableStatus(response.statusCode());

            delivery.setStatus(
                retryableStatus && attempt < MAX_RETRY_ATTEMPTS
                    ? AiObservabilityWebhookDeliveryStatus.RETRYING
                    : AiObservabilityWebhookDeliveryStatus.FAILED);

            aiObservabilityWebhookDeliveryRepository.save(delivery);

            if (retryableStatus) {
                logger.warn(
                    "Webhook delivery to {} returned status {} (attempt {}/{})",
                    subscription.getUrl(), response.statusCode(), attempt, MAX_RETRY_ATTEMPTS);
            } else {
                logger.warn(
                    "Webhook delivery to {} returned non-retryable status {} (attempt {}); not retrying",
                    subscription.getUrl(), response.statusCode(), attempt);
            }

            shouldRetry = retryableStatus && attempt < MAX_RETRY_ATTEMPTS;
        } catch (Exception exception) {
            delivery.setErrorMessage(exception.getMessage());
            delivery.setStatus(
                attempt < MAX_RETRY_ATTEMPTS
                    ? AiObservabilityWebhookDeliveryStatus.RETRYING
                    : AiObservabilityWebhookDeliveryStatus.FAILED);

            aiObservabilityWebhookDeliveryRepository.save(delivery);

            logger.warn(
                "Webhook delivery to {} failed (attempt {}/{}): {}",
                subscription.getUrl(), attempt, MAX_RETRY_ATTEMPTS, exception.getMessage());

            if (attempt >= MAX_RETRY_ATTEMPTS) {
                persistSubscriptionLastError(subscription, exception.getClass()
                    .getSimpleName() + ": " + exception.getMessage());
            }

            shouldRetry = attempt < MAX_RETRY_ATTEMPTS;
        }

        if (shouldRetry) {
            long backoffMs = (long) Math.pow(2, attempt) * 1000;

            taskScheduler.schedule(
                () -> attemptDelivery(subscription, jsonPayload, delivery, attempt + 1),
                Instant.now()
                    .plusMillis(backoffMs));

            return;
        }

        aiGatewayMetrics.incrementWebhookDelivery(delivery.getEventType(), false);

        if (delivery.getStatus() == AiObservabilityWebhookDeliveryStatus.FAILED
            && delivery.getErrorMessage() != null) {

            persistSubscriptionLastError(subscription, delivery.getErrorMessage());
        }

        logger.error(
            "Webhook delivery to {} failed after {} attempts", subscription.getUrl(), MAX_RETRY_ATTEMPTS);
    }

    private void persistSubscriptionLastError(AiObservabilityWebhookSubscription subscription, String message) {
        try {
            subscription.setLastDeliveryError(message, Instant.now());

            aiObservabilityWebhookSubscriptionService.update(subscription);
        } catch (Exception persistException) {
            logger.warn(
                "Failed to persist lastDeliveryError for webhook subscription {}",
                subscription.getId(), persistException);
        }
    }

    /**
     * Return {@code true} for HTTP status codes that should trigger a delivery retry. All 5xx responses and the
     * transient 4xx codes (408 Request Timeout, 425 Too Early, 429 Too Many Requests) are retryable. All other 4xx
     * codes indicate a permanent subscriber misconfiguration (wrong URL, bad credentials, schema mismatch) where
     * retrying just multiplies the noise without any chance of success.
     */
    private static boolean isRetryableStatus(int statusCode) {
        if (statusCode >= 500) {
            return true;
        }

        return AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX.contains(statusCode);
    }

    private String computeHmacSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));

            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte hmacByte : hmacBytes) {
                String hex = Integer.toHexString(0xff & hmacByte);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return "sha256=" + hexString;
        } catch (Exception exception) {
            throw new RuntimeException("Failed to compute HMAC signature", exception);
        }
    }
}
