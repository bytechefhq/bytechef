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
 * Delivers webhook payloads to subscribed endpoints with HMAC-SHA256 signing and exponential backoff retry (3
 * attempts).
 *
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

        // Per-subscription try/catch: a failure on subscriber #2 (DB hiccup during the row save, JSON serialization
        // throw, transient persistence error) must not silently skip subscribers #3..N. Without this, the @Async
        // proxy escapes the exception to SimpleAsyncUncaughtExceptionHandler, which logs once — operators see "1 of
        // N delivered" with no record of what was lost. Mirrors the per-row discipline in
        // AiExternalScoreFacadeImpl.recordBatch and AiObservabilityOtlpIngestFacadeImpl.ingest.
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
                // JVM-level distress (OOM, StackOverflow). Mirrors AiExternalScoreFacadeImpl.recordBatch's
                // discipline: log at ERROR with workspace + subscription context, then rethrow so the @Async escape
                // path treats this as JVM distress instead of burying it in a single
                // SimpleAsyncUncaughtExceptionHandler line. Without this branch, an Error during one subscriber's
                // payload silently aborts the loop, leaving #N+1..M un-notified with no actionable signal.
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
        // Wrap the entire body so a failure in subscription lookup, JSON write, or delivery row save does not
        // escape to SimpleAsyncUncaughtExceptionHandler — the controller has already returned 202, so the user
        // will see the "Send test event" UI report success while operators get a single context-free log line
        // with no subscriptionId. Mirrors the per-subscription discipline in deliverEvent above.
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
            // JVM-level distress: log at ERROR with subscription context, then rethrow so the @Async escape path
            // treats this as JVM distress instead of producing a single context-free line via
            // SimpleAsyncUncaughtExceptionHandler. The controller has already returned 202; without this branch
            // the operator sees neither a structured error nor any indication of which subscriptionId hit the wall.
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
        } catch (RuntimeException exception) {
            // This catch is narrow: deliverEvent is @Async, so it returns immediately and any actual delivery
            // failure surfaces via the per-subscription logs INSIDE deliverEvent (which has its own
            // RuntimeException + Error catches). What we DO observe synchronously here is proxy-acquisition
            // failure (selfProvider.getObject() under a misconfigured context, or a NoSuchBeanDefinition during
            // shutdown) and any pre-async payload-construction failure. The counter therefore tags
            // proxy/dispatch faults — distinct from in-flight delivery failures, which have their own metrics.
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
            // See onTraceCompleted: this catch sees proxy-acquisition + pre-async payload failures only;
            // in-flight delivery failures land in deliverEvent's per-subscription log.
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

        // Parse the JSON array once rather than relying on substring match — substring would falsely match
        // "trace.completed" against a subscription that only lists "trace.completed.v2", and vice versa.
        try {
            List<String> subscribedEvents = JsonUtils.readList(events, String.class);

            return subscribedEvents.contains(eventType);
        } catch (Exception exception) {
            // Pass the throwable as the trailing arg so SLF4J emits the full stack trace; otherwise a
            // regression where Jackson throws something other than a parse error (NPE in a custom
            // deserializer, OOM on a huge JSON blob) would surface as a single context-free message line.
            log.warn(
                "Subscription {} has malformed events JSON", subscription.getId(), exception);

            // Without this counter the silent-skip is invisible: the subscription delivers nothing for the
            // lifetime of the row, the only signal is repeated WARN lines operators learn to filter, and a
            // customer ticket is the first time anyone notices the row is dead.
            aiGatewayMetrics.incrementWebhookSubscriptionEventsParseFailure();

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

            // 4xx responses other than 408 (Request Timeout), 425 (Too Early), and 429 (Too Many Requests) mean
            // the subscriber has a permanent problem (wrong path, revoked credentials, HMAC mismatch, etc.).
            // Retrying is wasted work — it doubles the log noise and delays the FAILED state, masking misconfigured
            // subscriptions in the UI. Transient 4xx (408/425/429) and all 5xx remain eligible for retry.
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

        // Scale severity with retry exhaustion: a single-attempt non-retryable failure (e.g. 401, malformed url,
        // HMAC mismatch) is an expected operator-misconfiguration signal and does not warrant the same severity as
        // exhausted retries on a transient-looking failure. Avoids training operators to filter ERROR webhook lines,
        // which would mask genuine retry-exhaustion incidents.
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

    /**
     * Schedules the next retry with exponential backoff. Returns {@code true} if the scheduler accepted the task,
     * {@code false} on {@link org.springframework.core.task.TaskRejectedException} — scheduler queue full or executor
     * torn down. The false-return path marks the delivery FAILED with full context so the caller's FAILED-bookkeeping
     * block (metrics + subscription last-error) still fires; without this guard the row would stay permanently RETRYING
     * and the FAILED counter would never increment, making the failure invisible to dashboards.
     */
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

    /**
     * Saves the delivery row, swallowing DB-tier failures so they cannot bypass the retry / FAILED bookkeeping
     * downstream. Without this isolation, a transient DB outage during a status-bookkeeping write would re-raise into
     * the outer {@code catch (Exception)} as "delivery failed (attempt 1)" — masking the real outcome (the HTTP send
     * may have succeeded or returned a non-retryable status) and producing misleading "after 1 attempt(s)" log lines
     * with no retry queued. Logging at warn keeps the breadcrumb visible without aborting the retry decision.
     */
    private void saveDeliveryQuietly(
        AiObservabilityWebhookDelivery delivery, AiObservabilityWebhookSubscription subscription, int attempt) {

        try {
            aiObservabilityWebhookDeliveryRepository.save(delivery);
        } catch (Exception persistFailure) {
            log.warn(
                "Failed to persist webhook delivery row for subscription {} attempt {} — retry decision will be made " +
                    "from in-memory status, the row may be out-of-sync with reality",
                subscription.getId(), attempt, persistFailure);

            // Increment a metric so dashboards can alert on the divergence rate. Without this, an operator
            // inspecting the table sees stale rows but has no aggregate signal pointing at the swallow path.
            aiGatewayMetrics.incrementWebhookDeliveryRowDivergence();
        }
    }

    /**
     * Restores the interrupt flag, marks the delivery FAILED, persists the row, and refuses to reschedule. Rescheduling
     * during JVM shutdown leaks delivery threads past the executor's shutdown coordination and produces zombie retries
     * on the next start. Mirrors the gold-standard pattern in
     * {@code WorkspaceAiGatewayProviderFacadeImpl.testWorkspaceProviderConnection}.
     */
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

    /**
     * Handles a transport-layer failure (connection refused, DNS failure, read timeout, TLS handshake failure,
     * {@code HttpTimeoutException} which extends {@code IOException}). Returns {@code true} if the delivery should be
     * retried, {@code false} if retries are exhausted. Persists the subscription's last error only on terminal
     * exhaustion so a transient flap does not flood the column with stale messages.
     */
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

    /**
     * Handles a non-transport runtime failure (NPE from a corrupt subscription row, IllegalArgumentException from
     * {@code AiObservabilityUrlValidator}/{@code URI.create} on a malformed URL, IllegalStateException from a
     * misconfigured HttpClient, ConcurrentModificationException, etc.). Retrying just re-runs the same broken code path
     * against the same broken input, so this path always marks FAILED with an INTERNAL_ERROR tag — dashboards
     * distinguish "subscriber rejected us" from "we crashed before sending" and operators do not chase a phantom
     * "subscriber down" while a code defect or misconfiguration keeps firing.
     */
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

    /**
     * Marks the delivery FAILED with a {@code BAD_SECRET}-tagged error, persists the row, logs at ERROR, and updates
     * the subscription's last-delivery-error. Extracted from {@link #attemptDelivery} so the caller can short-circuit
     * the retry path on a misconfigured subscriber secret without bloating the parent method past the project's
     * 150-line Checkstyle cap.
     */
    private void handleBadHmacSecret(
        AiObservabilityWebhookSubscription subscription, AiObservabilityWebhookDelivery delivery, int attempt,
        BadHmacSecretException badHmacSecretException) {

        // No transport was attempted — the failure is local to HMAC init and cannot self-correct across retries.
        // Mark FAILED on the first attempt with a BAD_SECRET-tagged error message so operators can grep for the
        // cause class instead of triaging it as "subscriber down".
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

    /**
     * The subscriber accepted the payload — that's the load-bearing fact. Updating
     * {@code lastTriggeredDate}/{@code lastDeliveryError} on the subscription row is best-effort bookkeeping on top of
     * that fact. A failure here (optimistic-lock collision, transient DB hiccup) must not cascade into the outer
     * {@code catch (RuntimeException)} which would mark the delivery FAILED with INTERNAL_ERROR — corrupting the
     * delivery row, the success metric, and the acceptedCount-vs-deliveries reconciliation a downstream consumer relies
     * on. Mirrors the {@link #saveDeliveryQuietly} discipline.
     */
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
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            // JVM ships with HmacSHA256 — this can only happen if a custom security provider is misconfigured.
            // Treat as IllegalStateException so the generic delivery catch surfaces it and the dispatch infra is
            // alerted; retrying won't help, but the JVM-wide misconfig is not subscriber-specific.
            throw new IllegalStateException("Failed to compute HMAC signature", noSuchAlgorithmException);
        } catch (InvalidKeyException invalidKeyException) {
            // Bad subscriber secret (empty, wrong length, corrupt). Tag the throwable so attemptDelivery can mark
            // the row FAILED with reason BAD_SECRET and skip the retry — exponential backoff cannot fix a
            // misconfigured secret, and the resulting MAX_RETRY_ATTEMPTS x backoff delay was masking the real
            // diagnosis as "subscriber down" in dashboards.
            throw new BadHmacSecretException(invalidKeyException);
        }
    }

    /**
     * Sentinel for unrecoverable HMAC-secret misconfiguration. Caught by {@link #attemptDelivery} ahead of the generic
     * Exception catch so the row is marked FAILED with a {@code BAD_SECRET} reason on the first attempt instead of
     * being retried MAX_RETRY_ATTEMPTS times against a secret that cannot self-correct.
     */
    private static final class BadHmacSecretException extends RuntimeException {

        BadHmacSecretException(Throwable cause) {
            super("Webhook subscriber secret rejected by HMAC init (empty, wrong length, or corrupt)", cause);
        }
    }

}
