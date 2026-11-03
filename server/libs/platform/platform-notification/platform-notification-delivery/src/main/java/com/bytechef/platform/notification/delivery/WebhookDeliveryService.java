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

package com.bytechef.platform.notification.delivery;

import static com.bytechef.platform.notification.delivery.WebhookDeliveryMessages.describe;
import static com.bytechef.platform.notification.delivery.WebhookDeliveryMessages.describeLabels;
import static com.bytechef.platform.notification.delivery.WebhookDeliveryMessages.host;
import static com.bytechef.platform.notification.delivery.WebhookDeliveryMessages.redact;

import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.commons.util.UrlValidatorUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.notification.delivery.EndpointState.Admission;
import com.bytechef.platform.notification.delivery.EndpointState.EndpointKey;
import com.bytechef.platform.notification.delivery.EndpointState.QueuedDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome.PayloadResult;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome.Status;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.Serial;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class WebhookDeliveryService {

    static final String SUSPENSIONS_COUNTER = "bytechef_webhook_delivery_suspensions_total";
    static final String UNSENT_COUNTER = "bytechef_webhook_delivery_unsent_total";

    private static final String DROPPED_OUTCOME = "dropped";
    private static final String FAILED_OUTCOME = "failed";
    private static final String OUTCOME_FORMAT = "{} delivery {} to host {}: {} of {} payloads sent, {} failed, {} " +
        "skipped (labels {}), first failure: {}, last failure: {}{}";
    private static final String SKIPPED_OUTCOME = "skipped";
    private static final String TASK_ENDED_UNEXPECTEDLY = "the delivery task ended unexpectedly";

    private static final ContextSnapshotFactory CONTEXT_SNAPSHOT_FACTORY = ContextSnapshotFactory.builder()
        .clearMissing(true)
        .build();

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);

    private volatile Clock clock = Clock.systemUTC();
    private final EndpointState endpointState = new EndpointState();
    private final Semaphore globalSemaphore;
    private final int maxPendingDeliveries;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final AsyncTaskExecutor webhookDeliveryExecutor;
    private final WebhookNotificationClient webhookNotificationClient;
    private volatile Consumer<String> urlValidator = url -> UrlValidatorUtils.validate(url, Set.of());

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI2"
    })
    public WebhookDeliveryService(
        WebhookNotificationClient webhookNotificationClient,
        @Qualifier("webhookDeliveryExecutor") AsyncTaskExecutor webhookDeliveryExecutor,
        ApplicationProperties applicationProperties, ObjectProvider<MeterRegistry> meterRegistryProvider) {

        ApplicationProperties.WebhookDelivery webhookDeliveryProperties = applicationProperties.getWebhookDelivery();

        this.globalSemaphore = new Semaphore(
            requireAtLeastOne(
                webhookDeliveryProperties.getConcurrencyLimit(), "bytechef.webhook-delivery.concurrency-limit"),
            true);
        this.maxPendingDeliveries = requireAtLeastOne(
            webhookDeliveryProperties.getMaxPendingDeliveries(), "bytechef.webhook-delivery.max-pending-deliveries");
        this.meterRegistryProvider = meterRegistryProvider;
        this.webhookDeliveryExecutor = webhookDeliveryExecutor;
        this.webhookNotificationClient = webhookNotificationClient;
    }

    public CompletableFuture<WebhookDeliveryOutcome> submit(WebhookDelivery delivery) {
        CompletableFuture<WebhookDeliveryOutcome> outcomeFuture = new CompletableFuture<>();

        endpointState.prune(clock.instant());

        EndpointKey endpointKey = new EndpointKey(delivery.tenantId(), delivery.url());

        Admission admission = endpointState.enqueue(
            endpointKey, new QueuedDelivery(delivery, outcomeFuture, CONTEXT_SNAPSHOT_FACTORY.captureAll()),
            maxPendingDeliveries);

        if (admission == Admission.REJECTED) {
            drop(
                delivery, maxPendingDeliveries + " deliveries to the endpoint are already pending", outcomeFuture);

            return outcomeFuture;
        }

        if (admission == Admission.START_DRAINER) {
            startDrainer(endpointKey);
        }

        return outcomeFuture;
    }

    public WebhookTestResult sendTest(WebhookDelivery webhookDelivery) {
        List<WebhookPayload> payloads = webhookDelivery.payloads();

        try {
            if (webhookDelivery.validateUrl()) {
                validateUrl(webhookDelivery.url());
            }

            WebhookNotificationClient.WebhookResponse webhookResponse = webhookNotificationClient.post(
                webhookDelivery, payloads.getFirst());

            return new WebhookTestResult(true, webhookResponse.statusCode(), webhookResponse.body(), null);
        } catch (UrlNotAllowedException urlNotAllowedException) {
            return new WebhookTestResult(false, null, null, urlNotAllowedException.getMessage());
        } catch (WebhookDeliveryException webhookDeliveryException) {
            return new WebhookTestResult(
                false, webhookDeliveryException.getStatusCode(), webhookDeliveryException.getResponseBody(),
                describe(webhookDeliveryException));
        } catch (RuntimeException runtimeException) {
            if (runtimeException.getCause() instanceof InterruptedException) {
                Thread.currentThread()
                    .interrupt();
            }

            return new WebhookTestResult(
                false, null, null, "Test send failed (" + runtimeException.getClass()
                    .getSimpleName() + ")");
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        int pendingDeliveryCount = endpointState.getPendingDeliveryCount();

        if (pendingDeliveryCount > 0) {
            log.warn(
                "Shutting down with {} outbound webhook deliveries still pending; running deliveries are interrupted " +
                    "and their remaining payloads skipped",
                pendingDeliveryCount);
        }
    }

    void clearEndpointState() {
        endpointState.clear();
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    void setUrlValidator(Consumer<String> urlValidator) {
        this.urlValidator = urlValidator;
    }

    private void startDrainer(EndpointKey endpointKey) {
        try {
            webhookDeliveryExecutor.execute(() -> drain(endpointKey));
        } catch (TaskRejectedException taskRejectedException) {
            dropQueue(endpointKey, "the webhook delivery executor is shut down");
        } catch (RuntimeException runtimeException) {
            dropQueue(endpointKey, "its delivery task could not be started (" + describe(runtimeException) + ")");
        } catch (Error error) {
            dropQueue(endpointKey, "its delivery task could not be started (" + describe(error) + ")");

            throw error;
        }
    }

    private void dropQueue(EndpointKey endpointKey, String reason) {
        for (QueuedDelivery abandonedDelivery : endpointState.abandon(endpointKey)) {
            drop(abandonedDelivery.delivery(), reason, abandonedDelivery.outcomeFuture());
        }
    }

    @SuppressWarnings("PMD.UnusedLocalVariable")
    private void drain(EndpointKey endpointKey) {
        boolean drained = false;
        AtomicBoolean deliveryStarted = new AtomicBoolean(true);
        QueuedDelivery runningDelivery = null;

        try {
            runningDelivery = endpointState.takeNext(endpointKey);

            while (runningDelivery != null) {
                ContextSnapshot contextSnapshot = runningDelivery.contextSnapshot();
                WebhookDelivery delivery = runningDelivery.delivery();
                CompletableFuture<WebhookDeliveryOutcome> outcomeFuture = runningDelivery.outcomeFuture();

                deliveryStarted.set(false);

                try (ContextSnapshot.Scope scope = contextSnapshot.setThreadLocals()) {
                    TenantContext.runWithTenantId(
                        delivery.tenantId(), () -> {
                            deliveryStarted.set(true);

                            deliver(endpointKey, delivery, outcomeFuture);
                        });
                }

                runningDelivery = endpointState.takeNext(endpointKey);
            }

            drained = true;
        } finally {
            if (!drained && runningDelivery != null && !deliveryStarted.get()) {
                DeliveryProgress unstartedDeliveryProgress = new DeliveryProgress();

                unstartedDeliveryProgress.skipReason = TASK_ENDED_UNEXPECTEDLY;

                complete(
                    endpointKey, runningDelivery.delivery(), unstartedDeliveryProgress,
                    runningDelivery.outcomeFuture(), true);
            }

            if (!drained) {
                for (QueuedDelivery abandonedDelivery : endpointState.abandon(endpointKey)) {
                    DeliveryProgress abandonedDeliveryProgress = new DeliveryProgress();

                    abandonedDeliveryProgress.skipReason = TASK_ENDED_UNEXPECTEDLY;

                    complete(
                        endpointKey, abandonedDelivery.delivery(), abandonedDeliveryProgress,
                        abandonedDelivery.outcomeFuture(), false);
                }
            }
        }
    }

    private void deliver(
        EndpointKey endpointKey, WebhookDelivery delivery, CompletableFuture<WebhookDeliveryOutcome> outcomeFuture) {

        DeliveryProgress deliveryProgress = new DeliveryProgress();

        Thread currentThread = Thread.currentThread();

        try {
            if (currentThread.isInterrupted()) {
                deliveryProgress.interrupted = true;
            } else {
                sendPayloads(endpointKey, delivery, deliveryProgress);
            }
        } catch (InterruptedException interruptedException) {
            currentThread.interrupt();

            deliveryProgress.interrupted = true;
        } catch (Error error) {
            deliveryProgress.skipReason = TASK_ENDED_UNEXPECTEDLY + " (" + describe(error) + ")";

            throw error;
        } finally {
            complete(endpointKey, delivery, deliveryProgress, outcomeFuture, true);
        }
    }

    private void sendPayloads(EndpointKey endpointKey, WebhookDelivery delivery, DeliveryProgress deliveryProgress)
        throws InterruptedException {

        for (WebhookPayload payload : delivery.payloads()) {
            Instant pausedUntil = endpointState.getPausedUntil(endpointKey, clock.instant());

            if (pausedUntil != null) {
                deliveryProgress.pausedUntil = pausedUntil;

                return;
            }

            try {
                send(endpointKey, delivery, payload);

                deliveryProgress.add(payload, Status.SENT);

                endpointState.resetFailures(endpointKey);
            } catch (EndpointPausedException endpointPausedException) {
                deliveryProgress.pausedUntil = endpointPausedException.pausedUntil;

                return;
            } catch (UrlNotAllowedException urlNotAllowedException) {
                deliveryProgress.dropReason = urlNotAllowedException.getMessage();

                return;
            } catch (WebhookDeliveryException webhookDeliveryException) {
                if (isInterrupted()) {
                    deliveryProgress.interrupted = true;

                    return;
                }

                deliveryProgress.addFailure(payload, describe(webhookDeliveryException));

                if (pausesTheEndpoint(endpointKey, delivery, webhookDeliveryException, deliveryProgress)) {
                    return;
                }
            } catch (RuntimeException runtimeException) {
                if (isInterrupted()) {
                    deliveryProgress.interrupted = true;

                    return;
                }

                deliveryProgress.addFailure(payload, describe(runtimeException));

                if (pausesTheEndpoint(endpointKey, delivery, null, deliveryProgress)) {
                    return;
                }
            }
        }
    }

    private void send(EndpointKey endpointKey, WebhookDelivery delivery, WebhookPayload payload)
        throws InterruptedException {

        WebhookRetry retry = delivery.retry();

        if (retry.maxRetries() <= 0) {
            attempt(endpointKey, delivery, payload);

            return;
        }

        RetryTemplate retryTemplate = new RetryTemplate(
            RetryPolicy.builder()
                .maxRetries(retry.maxRetries())
                .delay(retry.initialInterval())
                .multiplier(retry.multiplier())
                .predicate(throwable -> throwable instanceof WebhookDeliveryException webhookDeliveryException &&
                    webhookDeliveryException.isRetryable())
                .build());

        try {
            retryTemplate.execute(() -> {
                attempt(endpointKey, delivery, payload);

                return null;
            });
        } catch (RetryException retryException) {
            Throwable cause = retryException.getCause();

            if (cause instanceof InterruptedException interruptedException) {
                throw interruptedException;
            }

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw new WebhookDeliveryException("Webhook delivery to " + host(delivery.url()) + " failed", cause);
        }
    }

    private void attempt(EndpointKey endpointKey, WebhookDelivery delivery, WebhookPayload payload)
        throws InterruptedException {

        globalSemaphore.acquire();

        try {
            Instant pausedUntil = endpointState.getPausedUntil(endpointKey, clock.instant());

            if (pausedUntil != null) {
                throw new EndpointPausedException(pausedUntil);
            }

            if (delivery.validateUrl()) {
                validateUrl(delivery.url());
            }

            webhookNotificationClient.post(delivery, payload);
        } finally {
            globalSemaphore.release();
        }
    }

    private void validateUrl(String url) {
        try {
            urlValidator.accept(url);
        } catch (UrlValidationException urlValidationException) {
            throw new UrlNotAllowedException("its URL is not allowed (" + describe(urlValidationException) + ")");
        }
    }

    private boolean pausesTheEndpoint(
        EndpointKey endpointKey, WebhookDelivery delivery, @Nullable WebhookDeliveryException webhookDeliveryException,
        DeliveryProgress deliveryProgress) {

        Instant now = clock.instant();

        if (webhookDeliveryException != null && webhookDeliveryException.isPayloadRejected()) {
            endpointState.resetFailures(endpointKey);

            return false;
        }

        if ((webhookDeliveryException == null || !webhookDeliveryException.isEndpointUnusable()) &&
            endpointState.countFailure(endpointKey, now) < EndpointState.MAX_CONSECUTIVE_FAILURES) {

            return false;
        }

        deliveryProgress.pausedUntil = endpointState.pause(endpointKey, now);
        deliveryProgress.pauseStarted = true;

        countSuspension(delivery.source());

        return true;
    }

    private void complete(
        EndpointKey endpointKey, WebhookDelivery delivery, DeliveryProgress deliveryProgress,
        CompletableFuture<WebhookDeliveryOutcome> outcomeFuture, boolean running) {

        List<PayloadResult> payloadResults = new ArrayList<>(deliveryProgress.payloadResults);
        List<WebhookPayload> payloads = delivery.payloads();
        Status remainingStatus = deliveryProgress.dropReason == null ? Status.SKIPPED : Status.DROPPED;

        for (int index = payloadResults.size(); index < payloads.size(); index++) {
            WebhookPayload payload = payloads.get(index);

            payloadResults.add(new PayloadResult(payload.label(), remainingStatus));
        }

        WebhookDeliveryOutcome outcome = new WebhookDeliveryOutcome(
            payloadResults, deliveryProgress.pausedUntil, deliveryProgress.firstFailure,
            deliveryProgress.lastFailure);

        try {
            logOutcome(endpointKey, delivery, deliveryProgress, outcome);

            countUnsent(delivery.source(), DROPPED_OUTCOME, outcome.count(Status.DROPPED));
            countUnsent(delivery.source(), FAILED_OUTCOME, outcome.count(Status.FAILED));
            countUnsent(delivery.source(), SKIPPED_OUTCOME, outcome.count(Status.SKIPPED));
        } finally {
            try {
                if (running) {
                    endpointState.finishRunning(endpointKey);
                }
            } finally {
                outcomeFuture.complete(outcome);
            }
        }
    }

    private void logOutcome(
        EndpointKey endpointKey, WebhookDelivery delivery, DeliveryProgress deliveryProgress,
        WebhookDeliveryOutcome outcome) {

        List<PayloadResult> payloadResults = outcome.payloadResults();

        int droppedCount = outcome.count(Status.DROPPED);
        int failedCount = outcome.count(Status.FAILED);
        int sentCount = outcome.count(Status.SENT);
        int skippedCount = outcome.count(Status.SKIPPED);

        if (deliveryProgress.dropReason != null && failedCount == 0) {
            List<String> droppedLabels = payloadResults.stream()
                .filter(payloadResult -> payloadResult.status() == Status.DROPPED)
                .map(PayloadResult::label)
                .toList();

            logDropped(delivery, deliveryProgress.dropReason, droppedLabels);

            return;
        }

        if (failedCount == 0 && skippedCount == 0 && droppedCount == 0) {
            log.debug(
                "{} delivery {} to host {}: {} of {} payloads sent", delivery.source(), redact(delivery.description()),
                host(delivery.url()), sentCount, payloadResults.size());

            return;
        }

        List<String> unsentLabels = payloadResults.stream()
            .filter(payloadResult -> payloadResult.status() != Status.SENT)
            .map(PayloadResult::label)
            .toList();

        Instant pausedUntil = outcome.suspendedUntil();
        String suffix = "";

        if (deliveryProgress.dropReason != null) {
            suffix = "; " + droppedCount + " payloads dropped because " + deliveryProgress.dropReason;
        } else if (deliveryProgress.skipReason != null) {
            suffix = "; skipped because " + deliveryProgress.skipReason;
        } else if (deliveryProgress.interrupted) {
            suffix = "; interrupted by shutdown";
        } else if (pausedUntil != null) {
            suffix = "; paused until " + pausedUntil;
        }

        Object[] arguments = {
            delivery.source(), redact(delivery.description()), host(delivery.url()), sentCount,
            payloadResults.size(), failedCount, skippedCount, describeLabels(unsentLabels),
            orNone(outcome.firstFailure()), orNone(outcome.lastFailure()), suffix
        };

        boolean skippedByEarlierPause = pausedUntil != null && !deliveryProgress.pauseStarted &&
            !deliveryProgress.interrupted && failedCount == 0;

        if (!skippedByEarlierPause) {
            log.error(OUTCOME_FORMAT, arguments);
        } else if (endpointState.markSkipLogged(endpointKey, pausedUntil)) {
            arguments[arguments.length - 1] =
                suffix + "; later skipped deliveries are logged at debug level until then";

            log.error(OUTCOME_FORMAT, arguments);
        } else {
            log.debug(OUTCOME_FORMAT, arguments);
        }
    }

    private void drop(
        WebhookDelivery delivery, String reason, CompletableFuture<WebhookDeliveryOutcome> outcomeFuture) {

        List<WebhookPayload> payloads = delivery.payloads();

        try {
            List<String> labels = payloads.stream()
                .map(WebhookPayload::label)
                .toList();

            logDropped(delivery, reason, labels);

            countUnsent(delivery.source(), DROPPED_OUTCOME, payloads.size());
        } finally {
            outcomeFuture.complete(WebhookDeliveryOutcome.dropped(payloads));
        }
    }

    private static void logDropped(WebhookDelivery delivery, String reason, List<String> droppedLabels) {
        log.error(
            "{} delivery {} to host {} was dropped because {}: {} payloads not sent (labels {})", delivery.source(),
            redact(delivery.description()), host(delivery.url()), reason, droppedLabels.size(),
            describeLabels(droppedLabels));
    }

    private void countSuspension(String source) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter counter = Counter.builder(SUSPENSIONS_COUNTER)
            .description("Outbound webhook endpoints paused after an unusable response or repeated failures")
            .tag("source", source)
            .register(meterRegistry);

        counter.increment();
    }

    private void countUnsent(String source, String outcome, int payloadCount) {
        if (payloadCount == 0) {
            return;
        }

        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter counter = Counter.builder(UNSENT_COUNTER)
            .description("Outbound webhook payloads that were dropped, failed or skipped")
            .tag("source", source)
            .tag("outcome", outcome)
            .register(meterRegistry);

        counter.increment(payloadCount);
    }

    private static boolean isInterrupted() {
        Thread currentThread = Thread.currentThread();

        return currentThread.isInterrupted();
    }

    private static String orNone(@Nullable String text) {
        return text == null ? "none" : text;
    }

    private static int requireAtLeastOne(int value, String propertyName) {
        if (value < 1) {
            throw new IllegalArgumentException(propertyName + " must be at least 1, was " + value);
        }

        return value;
    }

    private static final class DeliveryProgress {

        private @Nullable String dropReason;
        private @Nullable String firstFailure;
        private boolean interrupted;
        private @Nullable String lastFailure;
        private final List<PayloadResult> payloadResults = new ArrayList<>();
        private boolean pauseStarted;
        private @Nullable Instant pausedUntil;
        private @Nullable String skipReason;

        private void add(WebhookPayload payload, Status status) {
            payloadResults.add(new PayloadResult(payload.label(), status));
        }

        private void addFailure(WebhookPayload payload, String failureDescription) {
            add(payload, Status.FAILED);

            if (firstFailure == null) {
                firstFailure = failureDescription;
            }

            lastFailure = failureDescription;
        }
    }

    private static final class UrlNotAllowedException extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 1L;

        private UrlNotAllowedException(String reason) {
            super(reason, null, false, false);
        }
    }

    private static final class EndpointPausedException extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 1L;

        private final transient Instant pausedUntil;

        private EndpointPausedException(Instant pausedUntil) {
            super("Endpoint is paused until " + pausedUntil, null, false, false);

            this.pausedUntil = pausedUntil;
        }
    }
}
