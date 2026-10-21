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
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.Serial;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Date;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.task.TaskExecutor;
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
import org.springframework.web.util.UriTemplateHandler;

/**
 * POSTs each row of a {@link DataTableWebhookEvent} to each matching webhook, one delivery task per webhook on
 * {@code webhookDeliveryExecutor}. A webhook runs one delivery at a time, so a slow endpoint queues for the shared POST
 * limit with at most one delivery. Events published in a transaction are delivered after it commits, and events
 * published outside one right away.
 *
 * @author Ivica Cardic
 */
@Component
public final class DataTableWebhookEventListener {

    private static final Logger log = LoggerFactory.getLogger(DataTableWebhookEventListener.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final String DROPPED_OUTCOME = "dropped";
    static final String FAILED_OUTCOME = "failed";
    static final Duration INITIAL_RETRY_INTERVAL = Duration.ofSeconds(2);
    static final int MAX_CONSECUTIVE_FAILURES = 10;
    private static final int MAX_FAILURE_MESSAGE_LENGTH = 200;
    private static final int MAX_LOGGED_RECORD_IDS = 20;
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    static final String SKIPPED_OUTCOME = "skipped";
    static final Duration SUSPENSION_DURATION = Duration.ofMinutes(5);
    static final String SUSPENSIONS_COUNTER = "bytechef_data_table_webhook_suspensions";
    static final String UNSENT_ROWS_COUNTER = "bytechef_data_table_webhook_unsent_rows";
    private static final Pattern URL_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9+.-]*://[^\\s\"'<>]+");

    private volatile Clock clock = Clock.systemUTC();
    private final Map<SuspensionKey, Integer> consecutiveFailedCounts = new ConcurrentHashMap<>();
    private final DataTableWebhookService dataTableWebhookService;
    private final Semaphore deliverySemaphore;
    private final int maxPendingDeliveries;
    private final int maxPendingRows;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final Map<WebhookKey, Integer> pendingDeliveryCounts = new ConcurrentHashMap<>();
    private final AtomicLong pendingRowCount = new AtomicLong();
    private final RestTemplate restTemplate = createRestTemplate();
    private final Map<SuspensionKey, Instant> skipLoggedSuspendedUntilInstants = new ConcurrentHashMap<>();
    private final Map<SuspensionKey, Instant> suspendedUntilInstants = new ConcurrentHashMap<>();
    private final TaskExecutor webhookDeliveryExecutor;
    private final Map<WebhookKey, Semaphore> webhookSemaphores = new ConcurrentHashMap<>();

    @SuppressFBWarnings("EI")
    public DataTableWebhookEventListener(
        DataTableWebhookService dataTableWebhookService,
        @Qualifier("webhookDeliveryExecutor") TaskExecutor webhookDeliveryExecutor,
        ApplicationProperties applicationProperties, ObjectProvider<MeterRegistry> meterRegistryProvider) {

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

        int maxPendingRows = webhookDelivery.getMaxPendingRows();

        if (maxPendingRows < 1) {
            throw new IllegalArgumentException(
                "bytechef.webhook-delivery.max-pending-rows must be at least 1, was " + maxPendingRows);
        }

        this.dataTableWebhookService = dataTableWebhookService;
        this.deliverySemaphore = new Semaphore(concurrencyLimit, true);
        this.maxPendingDeliveries = maxPendingDeliveries;
        this.maxPendingRows = maxPendingRows;
        this.meterRegistryProvider = meterRegistryProvider;
        this.webhookDeliveryExecutor = webhookDeliveryExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDataTableWebhookEvent(DataTableWebhookEvent event) {
        String baseName = event.getBaseName();
        long environmentId = event.getEnvironmentId();
        List<DataTableRow> rows = event.getRows();
        DataTableWebhookType type = event.getType();

        List<DataTableWebhookService.Webhook> hooks;

        try {
            hooks = dataTableWebhookService.listWebhooks(baseName, environmentId)
                .stream()
                .filter(webhook -> webhook.type() == type)
                .toList();
        } catch (RuntimeException runtimeException) {
            log.error(
                "Data table webhooks for {} on table '{}' in environment {} could not be listed; {} rows " +
                    "(record ids {}) were not sent",
                type, baseName, environmentId, rows.size(), describeRecordIds(type, rows), runtimeException);

            countUnsentRows(type, DROPPED_OUTCOME, rows.size());

            return;
        }

        if (hooks.isEmpty()) {
            return;
        }

        if (!reservePendingRows(rows.size())) {
            log.error(
                "Data table webhooks {} for {} on table '{}' in environment {} were dropped because {} rows of " +
                    "webhook deliveries are already pending; {} rows (record ids {}) were not sent",
                hooks.stream()
                    .map(DataTableWebhookService.Webhook::id)
                    .toList(),
                type, baseName, environmentId, pendingRowCount.get(), rows.size(), describeRecordIds(type, rows));

            countUnsentRows(type, DROPPED_OUTCOME, rows.size() * hooks.size());

            return;
        }

        PendingRows pendingRows = new PendingRows(rows.size(), hooks.size());

        for (DataTableWebhookService.Webhook hook : hooks) {
            scheduleDelivery(hook, type, baseName, environmentId, rows, pendingRows);
        }
    }

    private void scheduleDelivery(
        DataTableWebhookService.Webhook hook, DataTableWebhookType type, String baseName, long environmentId,
        List<DataTableRow> rows, PendingRows pendingRows) {

        String tenantId = TenantContext.getCurrentTenantId();

        WebhookKey webhookKey = new WebhookKey(tenantId, hook.id());

        if (pendingDeliveryCounts.merge(webhookKey, 1, Integer::sum) > maxPendingDeliveries) {
            releasePending(webhookKey, pendingRows);

            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} was dropped because {} " +
                    "of its deliveries are already pending; {} rows (record ids {}) were not sent",
                hook.id(), getHost(hook), type, baseName, environmentId, maxPendingDeliveries, rows.size(),
                describeRecordIds(type, rows));

            countUnsentRows(type, DROPPED_OUTCOME, rows.size());

            return;
        }

        try {
            webhookDeliveryExecutor.execute(
                () -> deliver(webhookKey, hook, type, baseName, environmentId, rows, pendingRows));
        } catch (RuntimeException runtimeException) {
            releasePending(webhookKey, pendingRows);

            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} could not be scheduled; {} " +
                    "rows (record ids {}) were not sent",
                hook.id(), getHost(hook), type, baseName, environmentId, rows.size(), describeRecordIds(type, rows),
                runtimeException);

            countUnsentRows(type, DROPPED_OUTCOME, rows.size());
        }
    }

    private void deliver(
        WebhookKey webhookKey, DataTableWebhookService.Webhook hook, DataTableWebhookType type, String baseName,
        long environmentId, List<DataTableRow> rows, PendingRows pendingRows) {

        Semaphore webhookSemaphore = webhookSemaphores.computeIfAbsent(webhookKey, key -> new Semaphore(1, true));

        try {
            webhookSemaphore.acquire();
        } catch (InterruptedException interruptedException) {
            Thread currentThread = Thread.currentThread();

            currentThread.interrupt();

            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} was interrupted by shutdown; "
                    +
                    "{} of {} rows (record ids {}) were not sent",
                hook.id(), getHost(hook), type, baseName, environmentId, rows.size(), rows.size(),
                describeRecordIds(type, rows));

            countUnsentRows(type, SKIPPED_OUTCOME, rows.size());

            releasePending(webhookKey, pendingRows);

            return;
        }

        try {
            callWebhook(
                createRetryTemplate(), new SuspensionKey(webhookKey.tenantId(), hook.id(), hook.url()), hook, type,
                baseName, environmentId, rows);
        } catch (RuntimeException runtimeException) {
            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} failed unexpectedly; " +
                    "{} rows (record ids {}) may not have been sent",
                hook.id(), getHost(hook), type, baseName, environmentId, rows.size(),
                describeRecordIds(type, rows), runtimeException);

            countUnsentRows(type, FAILED_OUTCOME, rows.size());
        } finally {
            webhookSemaphore.release();

            releasePending(webhookKey, pendingRows);
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
                "Shutting down with {} data table webhook deliveries of {} rows to webhooks {} still pending; " +
                    "deliveries that do not stop within the shutdown wait are cut off without logging their rows",
                pendingDeliveryCount, pendingRowCount.get(), pendingDeliveryCounts.keySet());
        }
    }

    void clearSuspensions() {
        consecutiveFailedCounts.clear();
        skipLoggedSuspendedUntilInstants.clear();
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

    private void releasePending(WebhookKey webhookKey, PendingRows pendingRows) {
        releasePendingDelivery(webhookKey);
        releasePendingRows(pendingRows);
    }

    private void releasePendingDelivery(WebhookKey webhookKey) {
        pendingDeliveryCounts.compute(webhookKey, (key, count) -> {
            if (count == null || count <= 1) {
                webhookSemaphores.remove(key);

                return null;
            }

            return count - 1;
        });
    }

    private void releasePendingRows(PendingRows pendingRows) {
        if (pendingRows.release()) {
            pendingRowCount.addAndGet(-pendingRows.getRowCount());
        }
    }

    private boolean reservePendingRows(int rowCount) {
        long currentPendingRowCount;

        do {
            currentPendingRowCount = pendingRowCount.get();

            if (currentPendingRowCount > 0 && currentPendingRowCount + rowCount > maxPendingRows) {
                return false;
            }
        } while (!pendingRowCount.compareAndSet(currentPendingRowCount, currentPendingRowCount + rowCount));

        return true;
    }

    private Instant suspend(SuspensionKey suspensionKey) {
        Instant now = clock.instant();

        Instant suspendedUntil = now.plus(SUSPENSION_DURATION);

        suspendedUntilInstants.put(suspensionKey, suspendedUntil);
        consecutiveFailedCounts.remove(suspensionKey);

        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry != null) {
            Counter.builder(SUSPENSIONS_COUNTER)
                .description("Data table webhook endpoints suspended after failed deliveries")
                .register(meterRegistry)
                .increment();
        }

        return suspendedUntil;
    }

    private void callWebhook(
        RetryTemplate retryTemplate, SuspensionKey suspensionKey, DataTableWebhookService.Webhook hook,
        DataTableWebhookType type, String baseName, long environmentId, List<DataTableRow> rows) {

        int failedCount = 0;
        List<Long> failedIds = new ArrayList<>();
        RetryException firstRetryException = null;
        boolean interrupted = false;
        Long lastFailedId = null;
        int skippedCount = 0;
        RetryException stoppingRetryException = null;
        Instant suspendedUntil = null;

        for (int index = 0; index < rows.size(); index++) {
            DataTableRow row = rows.get(index);
            Map<String, Object> body = createBody(type, baseName, row);

            try {
                retryTemplate.execute(() -> {
                    if (log.isDebugEnabled()) {
                        log.debug(
                            "Calling data table webhook {} on host {} for {} record id {}", hook.id(), getHost(hook),
                            type, row.id());
                    }

                    return post(suspensionKey, hook, body);
                });

                consecutiveFailedCounts.remove(suspensionKey);
            } catch (RetryException retryException) {
                Throwable cause = retryException.getCause();

                if (cause instanceof DeliverySuspendedException deliverySuspendedException) {
                    skippedCount = rows.size() - index;
                    suspendedUntil = deliverySuspendedException.getSuspendedUntil();

                    break;
                }

                if (cause instanceof InterruptedException) {
                    skippedCount = rows.size() - index;
                    interrupted = true;

                    break;
                }

                failedCount++;

                if (type == DataTableWebhookType.RECORD_DELETED || failedIds.size() < MAX_LOGGED_RECORD_IDS) {
                    failedIds.add(row.id());
                }

                lastFailedId = row.id();

                if (firstRetryException == null) {
                    firstRetryException = retryException;
                }

                Thread currentThread = Thread.currentThread();

                if (currentThread.isInterrupted()) {
                    skippedCount = rows.size() - index - 1;
                    interrupted = true;

                    break;
                }

                int consecutiveFailedCount = 0;

                if (isPayloadRejected(cause)) {
                    consecutiveFailedCounts.remove(suspensionKey);
                } else if (isEndpointFailure(cause)) {
                    consecutiveFailedCount = consecutiveFailedCounts.merge(suspensionKey, 1, Integer::sum);
                }

                if (isEndpointUnusable(cause) || consecutiveFailedCount >= MAX_CONSECUTIVE_FAILURES) {
                    skippedCount = rows.size() - index - 1;
                    stoppingRetryException = retryException;

                    Instant currentSuspendedUntil = getSuspendedUntil(suspensionKey);

                    suspendedUntil = currentSuspendedUntil == null ? suspend(suspensionKey) : currentSuspendedUntil;

                    break;
                }
            }
        }

        countUnsentRows(type, FAILED_OUTCOME, failedCount);
        countUnsentRows(type, SKIPPED_OUTCOME, skippedCount);

        List<DataTableRow> skippedRows = rows.subList(rows.size() - skippedCount, rows.size());

        if (interrupted) {
            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} was interrupted by shutdown; "
                    +
                    "{} of {} rows (record ids {}) were not sent{}",
                hook.id(), getHost(hook), type, baseName, environmentId, skippedCount, rows.size(),
                describeRecordIds(type, skippedRows),
                firstRetryException == null
                    ? ""
                    : " and " + failedCount + " rows failed before (first record ids " + failedIds +
                        ", first failure: " + describeFailure(hook, firstRetryException) + ")");
        } else if (stoppingRetryException != null) {
            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} stopped after {} of {} " +
                    "rows failed (first record ids {}, last record id {}, first failure: {}, last failure: {}) " +
                    "and skipped {} rows (record ids {}){}",
                hook.id(), getHost(hook), type, baseName, environmentId, failedCount, rows.size(), failedIds,
                lastFailedId, describeFailure(hook, firstRetryException), describeFailure(hook, stoppingRetryException),
                skippedCount, describeRecordIds(type, skippedRows),
                suspendedUntil == null ? "" : "; deliveries are suspended until " + suspendedUntil);
        } else if (suspendedUntil != null) {
            if (firstRetryException != null) {
                log.error(
                    "Data table webhook {} on host {} for {} on table '{}' in environment {} is suspended until {} " +
                        "after its endpoint failed; {} of {} rows (record ids {}) were not sent and {} rows failed " +
                        "before (first record ids {}, first failure: {})",
                    hook.id(), getHost(hook), type, baseName, environmentId, suspendedUntil, skippedCount,
                    rows.size(), describeRecordIds(type, skippedRows), failedCount, failedIds,
                    describeFailure(hook, firstRetryException));
            } else if (type == DataTableWebhookType.RECORD_DELETED) {
                log.error(
                    "Data table webhook {} on host {} for {} on table '{}' in environment {} is suspended until {} " +
                        "after its endpoint failed; {} of {} rows (record ids {}) were not sent",
                    hook.id(), getHost(hook), type, baseName, environmentId, suspendedUntil, skippedCount,
                    rows.size(), describeRecordIds(type, skippedRows));
            } else if (!suspendedUntil.equals(skipLoggedSuspendedUntilInstants.put(suspensionKey, suspendedUntil))) {
                log.error(
                    "Data table webhook {} on host {} for {} on table '{}' in environment {} is suspended until {} " +
                        "after its endpoint failed; {} of {} rows (record ids {}) were not sent; later skipped " +
                        "writes are logged at debug level until then",
                    hook.id(), getHost(hook), type, baseName, environmentId, suspendedUntil, skippedCount,
                    rows.size(), describeRecordIds(type, skippedRows));
            } else if (log.isDebugEnabled()) {
                log.debug(
                    "Data table webhook {} on host {} for {} on table '{}' in environment {} is suspended until {}; " +
                        "{} of {} rows (record ids {}) were not sent",
                    hook.id(), getHost(hook), type, baseName, environmentId, suspendedUntil, skippedCount,
                    rows.size(), describeRecordIds(type, skippedRows));
            }
        } else if (firstRetryException != null) {
            log.error(
                "Data table webhook {} on host {} for {} on table '{}' in environment {} failed for {} of {} " +
                    "rows (first record ids {}, last record id {}, first failure: {})",
                hook.id(), getHost(hook), type, baseName, environmentId, failedCount, rows.size(), failedIds,
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

            return restTemplate.postForObject(getUri(hook), body, String.class);
        } catch (RestClientException | InvalidWebhookUrlException exception) {
            if (isEndpointUnusable(exception)) {
                suspend(suspensionKey);
            }

            throw exception;
        } finally {
            deliverySemaphore.release();
        }
    }

    private Map<String, Object> createBody(DataTableWebhookType type, String baseName, DataTableRow row) {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("id", row.id());
        payload.put("values", toPayloadValues(row.values()));

        body.put("type", type.name());
        body.put("table", baseName);
        body.put("timestamp", String.valueOf(clock.instant()));
        body.put("payload", payload);

        return body;
    }

    private URI getUri(DataTableWebhookService.Webhook hook) {
        URI uri = parseUri(hook.url());

        String scheme = uri.getScheme();

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new InvalidWebhookUrlException("Webhook URL is not an absolute http or https URL", null);
        }

        return uri;
    }

    private URI parseUri(String url) {
        try {
            return URI.create(url);
        } catch (IllegalArgumentException illegalArgumentException) {
            UriTemplateHandler uriTemplateHandler = restTemplate.getUriTemplateHandler();

            try {
                return uriTemplateHandler.expand(url);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw new InvalidWebhookUrlException("Webhook URL is not valid", exception);
            }
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

    private void countUnsentRows(DataTableWebhookType type, String outcome, int rowCount) {
        if (rowCount == 0) {
            return;
        }

        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry != null) {
            Counter.builder(UNSENT_ROWS_COUNTER)
                .tag("type", type.name())
                .tag("outcome", outcome)
                .description("Data table rows not sent to a webhook, per webhook")
                .register(meterRegistry)
                .increment(rowCount);
        }
    }

    private static String describeRecordIds(DataTableWebhookType type, List<DataTableRow> rows) {
        int maxLoggedRecordIds = type == DataTableWebhookType.RECORD_DELETED ? rows.size() : MAX_LOGGED_RECORD_IDS;

        List<Long> recordIds = rows.stream()
            .limit(maxLoggedRecordIds)
            .map(DataTableRow::id)
            .toList();

        if (rows.size() > maxLoggedRecordIds) {
            return recordIds + " and " + (rows.size() - maxLoggedRecordIds) + " more";
        }

        return String.valueOf(recordIds);
    }

    private static Map<String, @Nullable Object> toPayloadValues(Map<String, Object> values) {
        Map<String, @Nullable Object> payloadValues = new HashMap<>();

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Date date) {
                LocalDate localDate = date.toLocalDate();

                value = localDate.toString();
            }

            payloadValues.put(entry.getKey(), value);
        }

        return payloadValues;
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

            String locationHost = getHost(location);

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
            || throwable instanceof InvalidWebhookUrlException;
    }

    private static boolean isEndpointFailure(@Nullable Throwable throwable) {
        return throwable instanceof RestClientResponseException || throwable instanceof ResourceAccessException;
    }

    private static boolean isPayloadRejected(@Nullable Throwable throwable) {
        return throwable instanceof HttpClientErrorException.BadRequest
            || throwable instanceof HttpClientErrorException.Conflict
            || throwable instanceof HttpClientErrorException.UnprocessableContent;
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
        String host = getHost(url);

        return host == null ? "<url>" : host;
    }

    private static String getHost(DataTableWebhookService.Webhook hook) {
        String host = getHost(hook.url());

        return host == null ? "<invalid url>" : host;
    }

    private static @Nullable String getHost(String url) {
        try {
            return getHost(URI.create(url));
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private static @Nullable String getHost(URI uri) {
        String host = uri.getHost();

        if (host == null && uri.isAbsolute()) {
            try {
                URL url = uri.toURL();

                host = url.getHost();
            } catch (IllegalArgumentException | MalformedURLException exception) {
                return null;
            }
        }

        return host == null || host.isEmpty() ? null : host;
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

    private static final class InvalidWebhookUrlException extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 1L;

        private InvalidWebhookUrlException(String message, @Nullable RuntimeException cause) {
            super(message, cause);
        }
    }

    private static final class PendingRows {

        private final AtomicInteger remainingDeliveryCount;
        private final int rowCount;

        private PendingRows(int rowCount, int deliveryCount) {
            this.remainingDeliveryCount = new AtomicInteger(deliveryCount);
            this.rowCount = rowCount;
        }

        private int getRowCount() {
            return rowCount;
        }

        private boolean release() {
            return remainingDeliveryCount.decrementAndGet() == 0;
        }
    }

    private record SuspensionKey(String tenantId, long webhookId, String url) {
    }

    private record WebhookKey(String tenantId, long webhookId) {
    }
}
