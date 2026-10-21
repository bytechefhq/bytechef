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

package com.bytechef.platform.data.table.execution.listener;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serial;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * POSTs the row payloads of a committed {@link DataTableWebhookEvent} to each matching webhook, one delivery task per
 * webhook on {@code webhookDeliveryExecutor}.
 *
 * @author Ivica Cardic
 */
@Component
public final class DataTableWebhookEventListener {

    private static final Logger log = LoggerFactory.getLogger(DataTableWebhookEventListener.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final Duration INITIAL_RETRY_INTERVAL = Duration.ofSeconds(2);
    static final int MAX_CONSECUTIVE_FAILURES = 10;
    private static final int MAX_FAILURE_MESSAGE_LENGTH = 200;
    private static final int MAX_LOGGED_FAILED_IDS = 20;
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    static final Duration SUSPENSION_DURATION = Duration.ofMinutes(5);
    private static final Pattern URL_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9+.-]*://[^\\s\"'<>]+");

    private volatile Clock clock = Clock.systemUTC();
    private final Map<SuspensionKey, Integer> consecutiveFailedCounts = new ConcurrentHashMap<>();
    private final DataTableWebhookService dataTableWebhookService;
    private final Semaphore deliverySemaphore;
    private final int maxPendingDeliveries;
    private final Map<WebhookKey, Integer> pendingDeliveryCounts = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate = createRestTemplate();
    private final Map<SuspensionKey, Instant> suspendedUntilInstants = new ConcurrentHashMap<>();
    private final TaskExecutor webhookDeliveryExecutor;

    @SuppressFBWarnings("EI")
    public DataTableWebhookEventListener(
        DataTableWebhookService dataTableWebhookService,
        @Qualifier("webhookDeliveryExecutor") TaskExecutor webhookDeliveryExecutor,
        ApplicationProperties applicationProperties) {

        ApplicationProperties.WebhookDelivery webhookDelivery = applicationProperties.getWebhookDelivery();

        int concurrencyLimit = webhookDelivery.getConcurrencyLimit();

        if (concurrencyLimit < 1) {
            throw new IllegalArgumentException(
                "bytechef.webhook-delivery.concurrency-limit must be at least 1, was " + concurrencyLimit);
        }

        int maxPendingDeliveries = webhookDelivery.getMaxPendingDeliveries();

        if (maxPendingDeliveries < 1) {
            throw new IllegalArgumentException(
                "bytechef.webhook-delivery.max-pending-deliveries must be at least 1, was " + maxPendingDeliveries);
        }

        this.dataTableWebhookService = dataTableWebhookService;
        this.deliverySemaphore = new Semaphore(concurrencyLimit, true);
        this.maxPendingDeliveries = maxPendingDeliveries;
        this.webhookDeliveryExecutor = webhookDeliveryExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDataTableWebhookEvent(DataTableWebhookEvent event) {
        String baseName = event.getBaseName();
        long environmentId = event.getEnvironmentId();
        List<Map<String, Object>> payloads = event.getPayloads();
        DataTableWebhookType type = event.getType();

        List<DataTableWebhookService.Webhook> hooks;

        try {
            hooks = dataTableWebhookService.listWebhooks(baseName, environmentId)
                .stream()
                .filter(webhook -> webhook.type() == type)
                .toList();
        } catch (RuntimeException runtimeException) {
            log.error(
                "Data table webhooks for {} on table '{}' in environment {} could not be listed; {} payloads " +
                    "(record ids {}) were not sent",
                type, baseName, environmentId, payloads.size(), describeRecordIds(payloads), runtimeException);

            return;
        }

        for (DataTableWebhookService.Webhook hook : hooks) {
            scheduleDelivery(hook, type, baseName, environmentId, payloads);
        }
    }

    private void scheduleDelivery(
        DataTableWebhookService.Webhook hook, DataTableWebhookType type, String baseName, long environmentId,
        List<Map<String, Object>> payloads) {

        String tenantId = TenantContext.getCurrentTenantId();

        WebhookKey webhookKey = new WebhookKey(tenantId, hook.id());

        if (pendingDeliveryCounts.merge(webhookKey, 1, Integer::sum) > maxPendingDeliveries) {
            releasePendingDelivery(webhookKey);

            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} was dropped because {} " +
                    "of its deliveries are already pending; {} payloads (record ids {}) were not sent",
                hook.id(), getHost(hook), type, baseName, environmentId, maxPendingDeliveries, payloads.size(),
                describeRecordIds(payloads));

            return;
        }

        try {
            webhookDeliveryExecutor.execute(
                () -> deliver(webhookKey, hook, type, baseName, environmentId, payloads));
        } catch (TaskRejectedException taskRejectedException) {
            releasePendingDelivery(webhookKey);

            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} could not be scheduled; {} " +
                    "payloads (record ids {}) were not sent",
                hook.id(), getHost(hook), type, baseName, environmentId, payloads.size(), describeRecordIds(payloads),
                taskRejectedException);
        }
    }

    private void deliver(
        WebhookKey webhookKey, DataTableWebhookService.Webhook hook, DataTableWebhookType type, String baseName,
        long environmentId, List<Map<String, Object>> payloads) {

        try {
            callWebhook(
                createRetryTemplate(), new SuspensionKey(webhookKey.tenantId(), hook.id(), hook.url()), hook, type,
                baseName, environmentId, payloads);
        } catch (RuntimeException runtimeException) {
            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} failed unexpectedly; " +
                    "{} payloads (record ids {}) may not have been sent",
                hook.id(), getHost(hook), type, baseName, environmentId, payloads.size(), describeRecordIds(payloads),
                runtimeException);
        } finally {
            releasePendingDelivery(webhookKey);
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        int pendingDeliveryCount = pendingDeliveryCounts.values()
            .stream()
            .mapToInt(Integer::intValue)
            .sum();

        if (pendingDeliveryCount > 0) {
            log.warn(
                "Shutting down with {} data table webhook deliveries to webhooks {} still pending; their remaining " +
                    "payloads may not be sent",
                pendingDeliveryCount, pendingDeliveryCounts.keySet());
        }
    }

    void clearSuspensions() {
        consecutiveFailedCounts.clear();
        suspendedUntilInstants.clear();
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Nullable
    Instant getSuspendedUntil(DataTableWebhookService.Webhook hook) {
        return getSuspendedUntil(new SuspensionKey(TenantContext.getCurrentTenantId(), hook.id(), hook.url()));
    }

    private @Nullable Instant getSuspendedUntil(SuspensionKey suspensionKey) {
        Instant suspendedUntil = suspendedUntilInstants.get(suspensionKey);

        if (suspendedUntil == null) {
            return null;
        }

        Instant now = clock.instant();

        if (now.isBefore(suspendedUntil)) {
            return suspendedUntil;
        }

        suspendedUntilInstants.remove(suspensionKey, suspendedUntil);

        return null;
    }

    private void releasePendingDelivery(WebhookKey webhookKey) {
        pendingDeliveryCounts.computeIfPresent(webhookKey, (key, count) -> count > 1 ? count - 1 : null);
    }

    private Instant suspend(SuspensionKey suspensionKey) {
        Instant now = clock.instant();

        Instant suspendedUntil = now.plus(SUSPENSION_DURATION);

        suspendedUntilInstants.put(suspensionKey, suspendedUntil);
        consecutiveFailedCounts.remove(suspensionKey);

        return suspendedUntil;
    }

    private void callWebhook(
        RetryTemplate retryTemplate, SuspensionKey suspensionKey, DataTableWebhookService.Webhook hook,
        DataTableWebhookType type, String baseName, long environmentId, List<Map<String, Object>> payloads) {

        int failedCount = 0;
        List<Object> failedIds = new ArrayList<>();
        RetryException firstRetryException = null;
        Object lastFailedId = null;
        int skippedCount = 0;
        RetryException stoppingRetryException = null;
        Instant suspendedUntil = null;

        for (int index = 0; index < payloads.size(); index++) {
            Map<String, Object> payload = payloads.get(index);
            Map<String, Object> body = new HashMap<>();

            body.put("type", type.name());
            body.put("table", baseName);
            body.put("timestamp", String.valueOf(clock.instant()));
            body.put("payload", payload);

            try {
                retryTemplate.execute(() -> {
                    if (log.isDebugEnabled()) {
                        log.debug(
                            "Calling data table webhook {} on host {} for {} record id {}", hook.id(), getHost(hook),
                            type, payload.get("id"));
                    }

                    return post(suspensionKey, hook, body);
                });

                consecutiveFailedCounts.remove(suspensionKey);
            } catch (RetryException retryException) {
                Throwable cause = retryException.getCause();

                if (cause instanceof DeliverySuspendedException deliverySuspendedException) {
                    skippedCount = payloads.size() - index;
                    suspendedUntil = deliverySuspendedException.getSuspendedUntil();

                    break;
                }

                failedCount++;

                int consecutiveFailedCount;

                if (isPayloadRejected(cause)) {
                    consecutiveFailedCounts.remove(suspensionKey);

                    consecutiveFailedCount = 0;
                } else {
                    consecutiveFailedCount = consecutiveFailedCounts.merge(suspensionKey, 1, Integer::sum);
                }

                if (failedIds.size() < MAX_LOGGED_FAILED_IDS) {
                    failedIds.add(payload.get("id"));
                }

                lastFailedId = payload.get("id");

                if (firstRetryException == null) {
                    firstRetryException = retryException;
                }

                Thread currentThread = Thread.currentThread();

                if (currentThread.isInterrupted()) {
                    skippedCount = payloads.size() - index - 1;
                    stoppingRetryException = retryException;

                    break;
                }

                if (isEndpointUnusable(cause) || consecutiveFailedCount >= MAX_CONSECUTIVE_FAILURES) {
                    skippedCount = payloads.size() - index - 1;
                    stoppingRetryException = retryException;

                    Instant currentSuspendedUntil = getSuspendedUntil(suspensionKey);

                    suspendedUntil = currentSuspendedUntil == null ? suspend(suspensionKey) : currentSuspendedUntil;

                    break;
                }
            }
        }

        if (stoppingRetryException != null) {
            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} stopped after {} of {} " +
                    "payloads failed (first record ids {}, last record id {}, first failure: {}, last failure: {}) " +
                    "and skipped {} payloads (record ids {} to {}){}",
                hook.id(), getHost(hook), type, baseName, environmentId, failedCount, payloads.size(), failedIds,
                lastFailedId, describeFailure(hook, firstRetryException), describeFailure(hook, stoppingRetryException),
                skippedCount, getSkippedId(payloads, skippedCount, true), getSkippedId(payloads, skippedCount, false),
                suspendedUntil == null ? "" : "; deliveries are suspended until " + suspendedUntil);
        } else if (suspendedUntil != null) {
            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} is suspended until {} after " +
                    "its endpoint failed; {} of {} payloads (record ids {} to {}) were not sent{}",
                hook.id(), getHost(hook), type, baseName, environmentId, suspendedUntil, skippedCount,
                payloads.size(), getSkippedId(payloads, skippedCount, true),
                getSkippedId(payloads, skippedCount, false),
                firstRetryException == null
                    ? ""
                    : " and " + failedCount + " payloads failed before (first record ids " + failedIds +
                        ", first failure: " + describeFailure(hook, firstRetryException) + ")");
        } else if (firstRetryException != null) {
            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} failed for {} of {} " +
                    "payloads (first record ids {}, last record id {}, first failure: {})",
                hook.id(), getHost(hook), type, baseName, environmentId, failedCount, payloads.size(), failedIds,
                lastFailedId, describeFailure(hook, firstRetryException));
        }
    }

    private @Nullable String post(
        SuspensionKey suspensionKey, DataTableWebhookService.Webhook hook, Map<String, Object> body)
        throws InterruptedException {

        try {
            deliverySemaphore.acquire();
        } catch (InterruptedException interruptedException) {
            Thread currentThread = Thread.currentThread();

            currentThread.interrupt();

            throw interruptedException;
        }

        try {
            Instant suspendedUntil = getSuspendedUntil(suspensionKey);

            if (suspendedUntil != null) {
                throw new DeliverySuspendedException(suspendedUntil);
            }

            return restTemplate.postForObject(hook.url(), body, String.class);
        } catch (RestClientException | IllegalArgumentException exception) {
            if (isEndpointUnusable(exception)) {
                suspend(suspensionKey);
            }

            throw exception;
        } finally {
            deliverySemaphore.release();
        }
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();

        simpleClientHttpRequestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        simpleClientHttpRequestFactory.setReadTimeout(READ_TIMEOUT);

        RestTemplate restTemplate = new RestTemplate(simpleClientHttpRequestFactory);

        restTemplate.setErrorHandler(new Non2xxResponseErrorHandler());

        return restTemplate;
    }

    private static String describeRecordIds(List<Map<String, Object>> payloads) {
        Map<String, Object> firstPayload = payloads.getFirst();
        Map<String, Object> lastPayload = payloads.getLast();

        if (payloads.size() == 1) {
            return String.valueOf(firstPayload.get("id"));
        }

        return firstPayload.get("id") + " to " + lastPayload.get("id");
    }

    private static String abbreviate(String text) {
        String singleLineText = text.replaceAll("\\s+", " ")
            .strip();

        if (singleLineText.length() <= MAX_FAILURE_MESSAGE_LENGTH) {
            return singleLineText;
        }

        return singleLineText.substring(0, MAX_FAILURE_MESSAGE_LENGTH) + "...";
    }

    private static String describeFailure(
        DataTableWebhookService.Webhook hook, @Nullable RetryException retryException) {

        Throwable cause = retryException == null ? null : retryException.getCause();

        if (cause == null) {
            return "unknown";
        }

        if (cause instanceof RestClientResponseException restClientResponseException) {
            return describeResponseFailure(hook, restClientResponseException);
        }

        Throwable mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(cause);

        Class<? extends Throwable> mostSpecificCauseClass = mostSpecificCause.getClass();

        String message = mostSpecificCause.getMessage();

        if (message == null || message.isBlank()) {
            return mostSpecificCauseClass.getSimpleName();
        }

        return mostSpecificCauseClass.getSimpleName() + ": " + abbreviate(redactUrls(message));
    }

    private static String describeResponseFailure(
        DataTableWebhookService.Webhook hook, RestClientResponseException restClientResponseException) {

        HttpStatusCode statusCode = restClientResponseException.getStatusCode();

        String description = "HTTP " + statusCode.value();

        if (statusCode.is3xxRedirection()) {
            HttpHeaders responseHeaders = restClientResponseException.getResponseHeaders();

            URI location = responseHeaders == null ? null : responseHeaders.getLocation();

            if (location == null) {
                return description + " redirect without a location";
            }

            String locationHost = location.getHost();

            return description + " redirect to host " + (locationHost == null ? getHost(hook) : locationHost);
        }

        return description;
    }

    private static boolean isEndpointUnusable(@Nullable Throwable throwable) {
        if (throwable instanceof RestClientResponseException restClientResponseException) {
            HttpStatusCode statusCode = restClientResponseException.getStatusCode();

            if (statusCode.is3xxRedirection()) {
                return true;
            }
        }

        return throwable instanceof HttpClientErrorException.NotFound
            || throwable instanceof HttpClientErrorException.Gone
            || throwable instanceof HttpClientErrorException.MethodNotAllowed
            || throwable instanceof HttpClientErrorException.UnsupportedMediaType
            || throwable instanceof IllegalArgumentException;
    }

    private static boolean isPayloadRejected(@Nullable Throwable throwable) {
        return throwable instanceof HttpClientErrorException.BadRequest
            || throwable instanceof HttpClientErrorException.Conflict
            || throwable instanceof HttpClientErrorException.UnprocessableContent;
    }

    private static @Nullable Object getSkippedId(List<Map<String, Object>> payloads, int skippedCount, boolean first) {
        if (skippedCount == 0) {
            return null;
        }

        Map<String, Object> payload = payloads.get(first ? payloads.size() - skippedCount : payloads.size() - 1);

        return payload.get("id");
    }

    private static boolean isRetryable(@Nullable Throwable throwable) {
        return throwable instanceof ResourceAccessException || throwable instanceof HttpServerErrorException
            || throwable instanceof HttpClientErrorException.TooManyRequests;
    }

    private RetryTemplate createRetryTemplate() {
        ExponentialBackOff backOff = new ExponentialBackOff(INITIAL_RETRY_INTERVAL.toMillis(), 2.0);

        backOff.setMaxElapsedTime(60000);

        return new RetryTemplate(
            RetryPolicy.builder()
                .backOff(backOff)
                .predicate(DataTableWebhookEventListener::isRetryable)
                .build());
    }

    static String redactUrls(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);

        return matcher.replaceAll(matchResult -> Matcher.quoteReplacement(getUrlHost(matchResult.group())));
    }

    private static String getUrlHost(String url) {
        try {
            URI uri = URI.create(url);

            String host = uri.getHost();

            return host == null ? "<url>" : host;
        } catch (IllegalArgumentException illegalArgumentException) {
            return "<url>";
        }
    }

    private static String getHost(DataTableWebhookService.Webhook hook) {
        try {
            URI uri = URI.create(hook.url());

            return String.valueOf(uri.getHost());
        } catch (IllegalArgumentException illegalArgumentException) {
            return "<invalid url>";
        }
    }

    private static final class Non2xxResponseErrorHandler extends DefaultResponseErrorHandler {

        @Override
        protected boolean hasError(HttpStatusCode statusCode) {
            return !statusCode.is2xxSuccessful();
        }
    }

    private static final class DeliverySuspendedException extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 1L;

        private final Instant suspendedUntil;

        private DeliverySuspendedException(Instant suspendedUntil) {
            super("Deliveries are suspended until " + suspendedUntil);

            this.suspendedUntil = suspendedUntil;
        }

        private Instant getSuspendedUntil() {
            return suspendedUntil;
        }
    }

    private record SuspensionKey(String tenantId, long webhookId, String url) {
    }

    private record WebhookKey(String tenantId, long webhookId) {
    }
}
