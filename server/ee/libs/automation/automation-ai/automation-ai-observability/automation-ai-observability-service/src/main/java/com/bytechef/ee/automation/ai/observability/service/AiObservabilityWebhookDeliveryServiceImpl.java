/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.platform.ai.gateway.event.AiGatewayBudgetExceededEvent;
import com.bytechef.ee.platform.ai.gateway.event.AiGatewayTraceCompletedEvent;
import com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.platform.ai.gateway.reliability.AiGatewayRetryableStatuses;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayThrowables;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookDelivery;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookDeliveryStatus;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilityWebhookDeliveryRepository;
import com.bytechef.ee.platform.ai.observability.security.AiObservabilityUrlValidator;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityWebhookDeliveryService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityWebhookSubscriptionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
 * @author Ivica Cardic
 * @version ee
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiObservabilityWebhookDeliveryServiceImpl
    implements AiObservabilityWebhookDeliveryService, WorkspaceAiObservabilityWebhookDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(AiObservabilityWebhookDeliveryServiceImpl.class);

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AiGatewayMetrics aiGatewayMetrics;
    private final AiObservabilityWebhookDeliveryRepository aiObservabilityWebhookDeliveryRepository;
    private final AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService;
    private final WorkspaceAiObservabilityWebhookSubscriptionService workspaceAiObservabilityWebhookSubscriptionService;
    private final HttpClient httpClient;
    private final ObjectProvider<WorkspaceAiObservabilityWebhookDeliveryService> selfProvider;
    private final TaskScheduler taskScheduler;

    public AiObservabilityWebhookDeliveryServiceImpl(
        AiGatewayMetrics aiGatewayMetrics,
        AiObservabilityWebhookDeliveryRepository aiObservabilityWebhookDeliveryRepository,
        AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService,
        WorkspaceAiObservabilityWebhookSubscriptionService workspaceAiObservabilityWebhookSubscriptionService,
        ObjectProvider<WorkspaceAiObservabilityWebhookDeliveryService> selfProvider,
        TaskScheduler taskScheduler) {

        this.aiGatewayMetrics = aiGatewayMetrics;
        this.aiObservabilityWebhookDeliveryRepository = aiObservabilityWebhookDeliveryRepository;
        this.aiObservabilityWebhookSubscriptionService = aiObservabilityWebhookSubscriptionService;
        this.workspaceAiObservabilityWebhookSubscriptionService = workspaceAiObservabilityWebhookSubscriptionService;
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
            workspaceAiObservabilityWebhookSubscriptionService.getEnabledWebhookSubscriptionsByWorkspace(workspaceId);

        for (AiObservabilityWebhookSubscription subscription : subscriptions) {
            try {
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
            } catch (RuntimeException exception) {
                log.warn(
                    "Webhook event delivery skipped for subscription id={} eventType={} workspaceId={} reason={}",
                    subscription.getId(), eventType, workspaceId, exception.getMessage(), exception);
            } catch (Error error) {
                log.error(
                    "Webhook event delivery hit JVM Error: subscriptionId={} eventType={} workspaceId={} reason={}",
                    subscription.getId(), eventType, workspaceId, error.getMessage(), error);

                throw error;
            }
        }
    }

    @Override
    @Async
    public void deliverTestEvent(long subscriptionId) {
        try {
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
        } catch (RuntimeException exception) {
            log.warn(
                "Test webhook delivery skipped for subscription id={} reason={}",
                subscriptionId, exception.getMessage(), exception);
        } catch (Error error) {
            log.error(
                "Test webhook delivery hit JVM Error: subscriptionId={} reason={}",
                subscriptionId, error.getMessage(), error);

            throw error;
        }
    }

    @Override
    public List<AiObservabilityWebhookDelivery> getDeliveriesBySubscription(long subscriptionId) {
        return aiObservabilityWebhookDeliveryRepository.findAllBySubscriptionIdOrderByCreatedDateDesc(subscriptionId);
    }

    @Override
    public AiObservabilityWebhookDelivery update(AiObservabilityWebhookDelivery delivery) {
        return aiObservabilityWebhookDeliveryRepository.save(delivery);
    }

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

            selfProvider.getObject()
                .deliverEvent(event.workspaceId(), "trace.completed", payload);
        } catch (RuntimeException exception) {
            log.error(
                "Failed to dispatch trace.completed webhook proxy/payload for trace {} — async fan-out did not " +
                    "start; in-flight delivery failures are reported separately by deliverEvent",
                event.traceId(), exception);

            aiGatewayMetrics.incrementWebhookDispatchFailure("trace.completed");
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
        } catch (RuntimeException exception) {
            log.error(
                "Failed to dispatch budget.exceeded webhook proxy/payload for workspace {} — async fan-out did " +
                    "not start; in-flight delivery failures are reported separately by deliverEvent",
                event.workspaceId(), exception);

            aiGatewayMetrics.incrementWebhookDispatchFailure("budget.exceeded");
        }
    }

    private boolean isSubscribedToEvent(AiObservabilityWebhookSubscription subscription, String eventType) {
        String events = subscription.getEvents();

        if (events == null || events.isBlank()) {
            return false;
        }

        try {
            List<String> subscribedEvents = JsonUtils.readList(events, String.class);

            return subscribedEvents.contains(eventType);
        } catch (Exception exception) {
            log.warn(
                "Subscription {} has malformed events JSON", subscription.getId(), exception);

            aiGatewayMetrics.incrementWebhookSubscriptionEventsParseFailure();

            return false;
        }
    }

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
            AiObservabilityUrlValidator.validateExternalUrl(subscription.getUrl());

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
                updateSubscriptionLastTriggeredQuietly(subscription);

                delivery.setStatus(AiObservabilityWebhookDeliveryStatus.SUCCESS);
                delivery.setDeliveredDate(Instant.now());

                saveDeliveryQuietly(delivery, subscription, attempt);

                aiGatewayMetrics.incrementWebhookDelivery(delivery.getEventType(), true);

                return;
            }

            delivery.setErrorMessage("HTTP " + response.statusCode());

            boolean retryableStatus = isRetryableStatus(response.statusCode());

            delivery.setStatus(
                retryableStatus && attempt < MAX_RETRY_ATTEMPTS
                    ? AiObservabilityWebhookDeliveryStatus.RETRYING
                    : AiObservabilityWebhookDeliveryStatus.FAILED);

            saveDeliveryQuietly(delivery, subscription, attempt);

            if (retryableStatus) {
                log.warn(
                    "Webhook delivery to {} returned status {} (attempt {}/{})",
                    subscription.getUrl(), response.statusCode(), attempt, MAX_RETRY_ATTEMPTS);
            } else {
                log.warn(
                    "Webhook delivery to {} returned non-retryable status {} (attempt {}); not retrying",
                    subscription.getUrl(), response.statusCode(), attempt);
            }

            shouldRetry = retryableStatus && attempt < MAX_RETRY_ATTEMPTS;
        } catch (InterruptedException interruptedException) {
            handleInterruptedDelivery(subscription, delivery, attempt, interruptedException);

            shouldRetry = false;
        } catch (BadHmacSecretException badHmacSecretException) {
            handleBadHmacSecret(subscription, delivery, attempt, badHmacSecretException);

            shouldRetry = false;
        } catch (IOException | UncheckedIOException transportException) {
            shouldRetry = handleTransportFailure(subscription, delivery, attempt, transportException);
        } catch (RuntimeException internalException) {
            handleInternalDeliveryFailure(subscription, delivery, attempt, internalException);

            shouldRetry = false;
        }

        if (shouldRetry && scheduleRetry(subscription, jsonPayload, delivery, attempt)) {
            return;
        }

        aiGatewayMetrics.incrementWebhookDelivery(delivery.getEventType(), false);

        if (delivery.getStatus() == AiObservabilityWebhookDeliveryStatus.FAILED
            && delivery.getErrorMessage() != null) {

            persistSubscriptionLastError(subscription, delivery.getErrorMessage());
        }

        if (attempt >= MAX_RETRY_ATTEMPTS) {
            log.error(
                "Webhook delivery to {} failed after exhausting {} retry attempt(s)",
                subscription.getUrl(), attempt);
        } else {
            log.warn(
                "Webhook delivery to {} failed on attempt {} (not retried — non-retryable failure or scheduler " +
                    "rejected the retry)",
                subscription.getUrl(), attempt);
        }
    }

    private boolean scheduleRetry(
        AiObservabilityWebhookSubscription subscription, String jsonPayload,
        AiObservabilityWebhookDelivery delivery, int attempt) {

        long backoffMs = (long) Math.pow(2, attempt) * 1000;

        try {
            taskScheduler.schedule(
                () -> attemptDelivery(subscription, jsonPayload, delivery, attempt + 1),
                Instant.now()
                    .plusMillis(backoffMs));

            return true;
        } catch (org.springframework.core.task.TaskRejectedException scheduleRejected) {
            String scheduleErrorSummary = "Retry scheduling rejected: "
                + AiGatewayThrowables.summarize(scheduleRejected);

            delivery.setErrorMessage(scheduleErrorSummary);
            delivery.setStatus(AiObservabilityWebhookDeliveryStatus.FAILED);

            saveDeliveryQuietly(delivery, subscription, attempt);

            log.warn(
                "Webhook retry scheduling rejected for {} attempt {}; marking FAILED",
                subscription.getUrl(), attempt, scheduleRejected);

            return false;
        }
    }

    private void saveDeliveryQuietly(
        AiObservabilityWebhookDelivery delivery, AiObservabilityWebhookSubscription subscription, int attempt) {

        try {
            aiObservabilityWebhookDeliveryRepository.save(delivery);
        } catch (Exception persistFailure) {
            log.warn(
                "Failed to persist webhook delivery row for subscription {} attempt {} — retry decision will be made " +
                    "from in-memory status, the row may be out-of-sync with reality",
                subscription.getId(), attempt, persistFailure);

            aiGatewayMetrics.incrementWebhookDeliveryRowDivergence();
        }
    }

    private void handleInterruptedDelivery(
        AiObservabilityWebhookSubscription subscription, AiObservabilityWebhookDelivery delivery, int attempt,
        InterruptedException interruptedException) {

        Thread.currentThread()
            .interrupt();

        String errorSummary = AiGatewayThrowables.summarize(interruptedException);

        delivery.setErrorMessage(errorSummary);
        delivery.setStatus(AiObservabilityWebhookDeliveryStatus.FAILED);

        saveDeliveryQuietly(delivery, subscription, attempt);

        log.warn(
            "Webhook delivery to {} interrupted (attempt {}); not rescheduling",
            subscription.getUrl(), attempt, interruptedException);

        persistSubscriptionLastError(subscription, errorSummary);
    }

    private boolean handleTransportFailure(
        AiObservabilityWebhookSubscription subscription, AiObservabilityWebhookDelivery delivery, int attempt,
        Exception transportException) {

        String errorSummary = AiGatewayThrowables.summarize(transportException);

        delivery.setErrorMessage(errorSummary);
        delivery.setStatus(
            attempt < MAX_RETRY_ATTEMPTS
                ? AiObservabilityWebhookDeliveryStatus.RETRYING
                : AiObservabilityWebhookDeliveryStatus.FAILED);

        saveDeliveryQuietly(delivery, subscription, attempt);

        log.warn(
            "Webhook delivery to {} failed (attempt {}/{}): {}",
            subscription.getUrl(), attempt, MAX_RETRY_ATTEMPTS, errorSummary, transportException);

        if (attempt >= MAX_RETRY_ATTEMPTS) {
            persistSubscriptionLastError(subscription, errorSummary);
        }

        return attempt < MAX_RETRY_ATTEMPTS;
    }

    private void handleInternalDeliveryFailure(
        AiObservabilityWebhookSubscription subscription, AiObservabilityWebhookDelivery delivery, int attempt,
        RuntimeException internalException) {

        String errorSummary = "INTERNAL_ERROR: " + AiGatewayThrowables.summarize(internalException);

        delivery.setErrorMessage(errorSummary);
        delivery.setStatus(AiObservabilityWebhookDeliveryStatus.FAILED);

        saveDeliveryQuietly(delivery, subscription, attempt);

        log.error(
            "Webhook delivery to {} hit internal error on attempt {}; not retrying",
            subscription.getUrl(), attempt, internalException);

        persistSubscriptionLastError(subscription, errorSummary);
    }

    private void handleBadHmacSecret(
        AiObservabilityWebhookSubscription subscription, AiObservabilityWebhookDelivery delivery, int attempt,
        BadHmacSecretException badHmacSecretException) {

        String errorSummary = "BAD_SECRET: " + AiGatewayThrowables.summarize(badHmacSecretException);

        delivery.setErrorMessage(errorSummary);
        delivery.setStatus(AiObservabilityWebhookDeliveryStatus.FAILED);

        saveDeliveryQuietly(delivery, subscription, attempt);

        log.error(
            "Webhook delivery to {} aborted on attempt {}: subscriber HMAC secret rejected — not retrying",
            subscription.getUrl(), attempt, badHmacSecretException);

        persistSubscriptionLastError(subscription, errorSummary);
    }

    private void persistSubscriptionLastError(AiObservabilityWebhookSubscription subscription, String message) {
        try {
            subscription.setLastDeliveryError(message, Instant.now());

            aiObservabilityWebhookSubscriptionService.update(subscription);
        } catch (Exception persistException) {
            log.warn(
                "Failed to persist lastDeliveryError for webhook subscription {}",
                subscription.getId(), persistException);
        }
    }

    private void updateSubscriptionLastTriggeredQuietly(AiObservabilityWebhookSubscription subscription) {
        try {
            subscription.setLastTriggeredDate(Instant.now());
            subscription.setLastDeliveryError(null, null);

            aiObservabilityWebhookSubscriptionService.update(subscription);
        } catch (Exception persistException) {
            log.warn(
                "Failed to update lastTriggeredDate for webhook subscription {} after successful delivery — " +
                    "delivery row remains SUCCESS, subscription bookkeeping is now stale",
                subscription.getId(), persistException);

            aiGatewayMetrics.incrementWebhookDeliveryRowDivergence();
        }
    }

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
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException("Failed to compute HMAC signature", noSuchAlgorithmException);
        } catch (InvalidKeyException invalidKeyException) {
            throw new BadHmacSecretException(invalidKeyException);
        }
    }

    private static final class BadHmacSecretException extends RuntimeException {

        BadHmacSecretException(Throwable cause) {
            super("Webhook subscriber secret rejected by HMAC init (empty, wrong length, or corrupt)", cause);
        }
    }

}
