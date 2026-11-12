/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.trigger.subscription;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionDisableReason;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionWebhookSettingsDTO;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationEvent;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationRecorder;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.delivery.WebhookRetry;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sends trigger subscription events to the developer's webhook endpoint through the shared webhook delivery lane,
 * without waiting for the delivery to finish.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class TriggerSubscriptionEventSender {

    static final String DELIVERY_SOURCE = "triggerSubscription";
    static final String TRIGGER_EVENT_TYPE = "trigger.event";

    private static final String DELIVERY_ERROR_TYPE = "DELIVERY_ERROR";
    private static final String DELIVERY_ERROR_TYPE_PREFIX = "DELIVERY_";
    private static final String DROPPED_STATUS = "DROPPED";
    private static final String FAILED_STATUS = "FAILED";
    private static final String NO_DESTINATION_ERROR_TYPE = "NO_DESTINATION";
    private static final String NO_SIGNING_SECRET_ERROR_TYPE = "NO_SIGNING_SECRET";
    private static final String RETRY_PROPERTY_PREFIX = "bytechef.embedded.trigger-subscription.delivery.retry.";
    private static final String SENT_STATUS = "SENT";
    private static final String TEST_SUBSCRIPTION_ID = "test";

    private static final Logger log = LoggerFactory.getLogger(TriggerSubscriptionEventSender.class);

    private final ConnectedUserService connectedUserService;
    private final Executor deliveryCallbackExecutor;
    private final ExternalInvocationRecorder externalInvocationRecorder;
    private final TriggerSubscriptionService triggerSubscriptionService;
    private final TriggerSubscriptionWebhookSettingsAccessor triggerSubscriptionWebhookSettingsAccessor;
    private final WebhookDeliveryService webhookDeliveryService;
    private final WebhookRetry webhookRetry;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public TriggerSubscriptionEventSender(
        ApplicationProperties applicationProperties, ConnectedUserService connectedUserService,
        @Qualifier("webhookDeliveryExecutor") Executor deliveryCallbackExecutor,
        ExternalInvocationRecorder externalInvocationRecorder, TriggerSubscriptionService triggerSubscriptionService,
        TriggerSubscriptionWebhookSettingsAccessor triggerSubscriptionWebhookSettingsAccessor,
        WebhookDeliveryService webhookDeliveryService) {

        this.connectedUserService = connectedUserService;
        this.deliveryCallbackExecutor = deliveryCallbackExecutor;
        this.externalInvocationRecorder = externalInvocationRecorder;
        this.triggerSubscriptionService = triggerSubscriptionService;
        this.triggerSubscriptionWebhookSettingsAccessor = triggerSubscriptionWebhookSettingsAccessor;
        this.webhookDeliveryService = webhookDeliveryService;
        this.webhookRetry = toWebhookRetry(applicationProperties);
    }

    /**
     * Submits the event and returns at once. Delivery bookkeeping (status update and invocation row) always runs on the
     * unbounded webhook delivery executor, never on the calling worker thread: the invocation log listener needs a
     * worker permit, so recording from a worker thread could deadlock the worker lane. A failure here never propagates,
     * so it cannot fail the job that produced the event.
     */
    public void sendTriggerEvent(TriggerSubscription triggerSubscription, @Nullable Object data, long jobId) {
        Instant timestamp = Instant.now();
        String tenantId = TenantContext.getCurrentTenantId();

        try {
            submitTriggerEvent(triggerSubscription, data, jobId, timestamp, tenantId);
        } catch (RuntimeException runtimeException) {
            log.error(
                "Unable to deliver trigger subscription id={} event for job id={}", triggerSubscription.getId(),
                jobId, runtimeException);

            recordFailureAsync(
                triggerSubscription, null, jobId, timestamp, tenantId, DELIVERY_ERROR_TYPE,
                getErrorMessage(runtimeException));
        }
    }

    /**
     * Builds the synthetic {@code trigger.event} delivery used to test an environment's webhook settings. It is meant
     * for {@link WebhookDeliveryService#sendTest(WebhookDelivery)}, which is synchronous and so must only be called
     * from an admin request thread.
     */
    public WebhookDelivery createTestDelivery(Environment environment, String url, String signingSecret) {
        Instant timestamp = Instant.now();
        String deliveryId = String.valueOf(UUID.randomUUID());

        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("type", TRIGGER_EVENT_TYPE);
        envelope.put("id", deliveryId);
        envelope.put("subscriptionId", TEST_SUBSCRIPTION_ID);
        envelope.put("externalId", null);
        envelope.put("externalUserId", null);
        envelope.put("environment", environment.name());
        envelope.put("componentName", TEST_SUBSCRIPTION_ID);
        envelope.put("componentVersion", 1);
        envelope.put("triggerName", TEST_SUBSCRIPTION_ID);
        envelope.put("timestamp", timestamp.toString());
        envelope.put("jobId", null);
        envelope.put("data", Map.of("test", true));

        return WebhookDelivery.builder(DELIVERY_SOURCE, url)
            .description("trigger subscription test event")
            .tenantId(TenantContext.getCurrentTenantId())
            .eventType(TRIGGER_EVENT_TYPE)
            .secret(signingSecret)
            .validateUrl(true)
            .payload(TEST_SUBSCRIPTION_ID, JsonUtils.write(envelope))
            .retry(webhookRetry)
            .deliveryId(deliveryId)
            .timestamp(timestamp)
            .build();
    }

    /**
     * Builds the event at once, reading everything it needs while the caller's transaction still sees it. Inside a
     * transaction the delivery is submitted only after that transaction commits, so a rollback never announces a change
     * that did not happen; without a transaction it is submitted immediately.
     */
    public void sendLifecycleEvent(
        TriggerSubscription triggerSubscription, String type, @Nullable TriggerSubscriptionDisableReason reason) {

        try {
            submitLifecycleEvent(triggerSubscription, type, reason);
        } catch (RuntimeException runtimeException) {
            log.error(
                "Unable to deliver trigger subscription id={} {} event", triggerSubscription.getId(), type,
                runtimeException);
        }
    }

    private static String getErrorMessage(Throwable throwable) {
        Throwable cause = throwable;

        if (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();
        Class<? extends Throwable> causeClass = cause.getClass();

        return message == null ? causeClass.getName() : message;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    private static Map<String, Object> createEnvelope(
        String type, String deliveryId, TriggerSubscription triggerSubscription, @Nullable String externalUserId,
        Instant timestamp) {

        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("type", type);
        envelope.put("id", deliveryId);
        envelope.put("subscriptionId", triggerSubscription.getId());
        envelope.put("externalId", triggerSubscription.getExternalId());
        envelope.put("externalUserId", externalUserId);

        Environment environment = triggerSubscription.getEnvironment();

        envelope.put("environment", environment.name());
        envelope.put("componentName", triggerSubscription.getComponentName());
        envelope.put("componentVersion", triggerSubscription.getComponentVersion());
        envelope.put("triggerName", triggerSubscription.getTriggerName());
        envelope.put("timestamp", timestamp.toString());

        return envelope;
    }

    private WebhookDelivery createWebhookDelivery(
        String url, @Nullable String secret, String eventType, String deliveryId, Map<String, Object> envelope,
        TriggerSubscription triggerSubscription, String tenantId) {

        return WebhookDelivery.builder(DELIVERY_SOURCE, url)
            .description("trigger subscription " + triggerSubscription.getId() + " " + eventType)
            .tenantId(tenantId)
            .eventType(eventType)
            .secret(secret)
            .validateUrl(true)
            .payload(String.valueOf(triggerSubscription.getId()), JsonUtils.write(envelope))
            .retry(webhookRetry)
            .deliveryId(deliveryId)
            .build();
    }

    private static @Nullable String resolveUrl(
        TriggerSubscription triggerSubscription, TriggerSubscriptionWebhookSettingsDTO settings) {

        String subscriptionUrl = triggerSubscription.getWebhookUrl();

        if (!isBlank(subscriptionUrl)) {
            return subscriptionUrl;
        }

        String environmentUrl = settings.webhookUrl();

        if (!isBlank(environmentUrl)) {
            return environmentUrl;
        }

        return null;
    }

    private static String toDeliveryStatus(WebhookDeliveryOutcome outcome) {
        if (outcome.anySent()) {
            return SENT_STATUS;
        }

        if (outcome.count(WebhookDeliveryOutcome.Status.DROPPED) > 0) {
            return DROPPED_STATUS;
        }

        return FAILED_STATUS;
    }

    private static WebhookRetry toWebhookRetry(ApplicationProperties applicationProperties) {
        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        ApplicationProperties.Embedded.TriggerSubscription triggerSubscription = embedded.getTriggerSubscription();

        ApplicationProperties.Embedded.TriggerSubscription.Delivery delivery = triggerSubscription.getDelivery();

        ApplicationProperties.Embedded.TriggerSubscription.Delivery.Retry retry = delivery.getRetry();

        int maxRetries = retry.getMaxRetries();

        if (maxRetries < 0) {
            throw new IllegalStateException(
                RETRY_PROPERTY_PREFIX + "max-retries must be 0 or greater, but is " + maxRetries);
        }

        if (maxRetries == 0) {
            return WebhookRetry.none();
        }

        Duration initialInterval = retry.getInitialInterval();

        if (initialInterval == null || initialInterval.isNegative() || initialInterval.isZero()) {
            throw new IllegalStateException(
                RETRY_PROPERTY_PREFIX + "initial-interval must be greater than 0 when max-retries is greater than " +
                    "0, but is " + initialInterval);
        }

        double multiplier = retry.getMultiplier();

        if (multiplier < 1) {
            throw new IllegalStateException(
                RETRY_PROPERTY_PREFIX + "multiplier must be 1 or greater when max-retries is greater than 0, but " +
                    "is " + multiplier);
        }

        return new WebhookRetry(maxRetries, initialInterval, multiplier);
    }

    private @Nullable String fetchExternalUserId(TriggerSubscription triggerSubscription) {
        Optional<ConnectedUser> connectedUserOptional = connectedUserService.fetchConnectedUser(
            triggerSubscription.getConnectedUserId());

        if (connectedUserOptional.isEmpty()) {
            return null;
        }

        ConnectedUser connectedUser = connectedUserOptional.get();

        return connectedUser.getExternalId();
    }

    private void submitLifecycleEvent(
        TriggerSubscription triggerSubscription, String type, @Nullable TriggerSubscriptionDisableReason reason) {

        Instant timestamp = Instant.now();

        TriggerSubscriptionWebhookSettingsDTO settings = triggerSubscriptionWebhookSettingsAccessor.getSettings(
            triggerSubscription.getEnvironment());

        String url = resolveUrl(triggerSubscription, settings);

        if (url == null) {
            log.warn(
                "Trigger subscription id={} {} event not sent: no webhook URL configured for environment {}",
                triggerSubscription.getId(), type, triggerSubscription.getEnvironment());

            return;
        }

        String signingSecret = settings.signingSecret();

        if (isBlank(signingSecret)) {
            log.warn(
                "Trigger subscription id={} {} event not sent: no signing secret configured for environment {}",
                triggerSubscription.getId(), type, triggerSubscription.getEnvironment());

            return;
        }

        String deliveryId = String.valueOf(UUID.randomUUID());

        Map<String, Object> envelope = createEnvelope(
            type, deliveryId, triggerSubscription, fetchExternalUserId(triggerSubscription), timestamp);

        if (reason != null) {
            envelope.put("reason", reason.name());
        }

        WebhookDelivery webhookDelivery = createWebhookDelivery(
            url, signingSecret, type, deliveryId, envelope, triggerSubscription,
            TenantContext.getCurrentTenantId());

        if (TransactionSynchronizationManager.isActualTransactionActive() &&
            TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    try {
                        submitLifecycleDelivery(triggerSubscription, type, webhookDelivery);
                    } catch (RuntimeException runtimeException) {
                        log.error(
                            "Unable to deliver trigger subscription id={} {} event after commit",
                            triggerSubscription.getId(), type, runtimeException);
                    }
                }
            });

            return;
        }

        submitLifecycleDelivery(triggerSubscription, type, webhookDelivery);
    }

    private void submitLifecycleDelivery(
        TriggerSubscription triggerSubscription, String type, WebhookDelivery webhookDelivery) {

        CompletableFuture<WebhookDeliveryOutcome> outcomeFuture = webhookDeliveryService.submit(webhookDelivery);

        outcomeFuture.whenCompleteAsync(
            (outcome, throwable) -> logLifecycleOutcome(triggerSubscription, type, outcome, throwable),
            deliveryCallbackExecutor);
    }

    private void submitTriggerEvent(
        TriggerSubscription triggerSubscription, @Nullable Object data, long jobId, Instant timestamp,
        String tenantId) {

        String externalUserId = fetchExternalUserId(triggerSubscription);
        Environment environment = triggerSubscription.getEnvironment();

        TriggerSubscriptionWebhookSettingsDTO settings = triggerSubscriptionWebhookSettingsAccessor.getSettings(
            environment);

        String url = resolveUrl(triggerSubscription, settings);

        if (url == null) {
            recordFailureAsync(
                triggerSubscription, externalUserId, jobId, timestamp, tenantId, NO_DESTINATION_ERROR_TYPE,
                "No webhook URL configured for environment " + environment);

            return;
        }

        String signingSecret = settings.signingSecret();

        if (isBlank(signingSecret)) {
            recordFailureAsync(
                triggerSubscription, externalUserId, jobId, timestamp, tenantId, NO_SIGNING_SECRET_ERROR_TYPE,
                "No signing secret configured for environment " + environment);

            return;
        }

        String deliveryId = String.valueOf(UUID.randomUUID());

        Map<String, Object> envelope = createEnvelope(
            TRIGGER_EVENT_TYPE, deliveryId, triggerSubscription, externalUserId, timestamp);

        envelope.put("jobId", jobId);
        envelope.put("data", data);

        WebhookDelivery webhookDelivery = createWebhookDelivery(
            url, signingSecret, TRIGGER_EVENT_TYPE, deliveryId, envelope, triggerSubscription, tenantId);

        long startNanos = System.nanoTime();

        CompletableFuture<WebhookDeliveryOutcome> outcomeFuture = webhookDeliveryService.submit(webhookDelivery);

        outcomeFuture.whenCompleteAsync(
            (outcome, throwable) -> onTriggerEventDelivered(
                triggerSubscription, externalUserId, jobId, timestamp, tenantId, startNanos, outcome, throwable),
            deliveryCallbackExecutor);
    }

    private void logLifecycleOutcome(
        TriggerSubscription triggerSubscription, String type, @Nullable WebhookDeliveryOutcome outcome,
        @Nullable Throwable throwable) {

        if (throwable != null) {
            log.error(
                "Trigger subscription id={} {} event delivery failed", triggerSubscription.getId(), type, throwable);

            return;
        }

        if (outcome != null && outcome.anySent()) {
            if (log.isDebugEnabled()) {
                log.debug("Trigger subscription id={} {} event delivered", triggerSubscription.getId(), type);
            }

            return;
        }

        log.warn(
            "Trigger subscription id={} {} event not delivered: {}", triggerSubscription.getId(), type,
            outcome == null ? null : outcome.lastFailure());
    }

    private void onTriggerEventDelivered(
        TriggerSubscription triggerSubscription, @Nullable String externalUserId, long jobId, Instant timestamp,
        String tenantId, long startNanos, @Nullable WebhookDeliveryOutcome outcome, @Nullable Throwable throwable) {

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        runWithTenantId(
            tenantId, triggerSubscription, jobId, () -> recordDelivery(
                triggerSubscription, externalUserId, jobId, timestamp, durationMs, outcome, throwable));
    }

    private void recordDelivery(
        TriggerSubscription triggerSubscription, @Nullable String externalUserId, long jobId, Instant timestamp,
        long durationMs, @Nullable WebhookDeliveryOutcome outcome, @Nullable Throwable throwable) {

        String status;
        String errorMessage;

        if (throwable != null || outcome == null) {
            status = FAILED_STATUS;
            errorMessage = throwable == null ? "Delivery ended without an outcome" : getErrorMessage(throwable);
        } else {
            status = toDeliveryStatus(outcome);
            errorMessage = SENT_STATUS.equals(status) ? null : outcome.lastFailure();
        }

        if (DROPPED_STATUS.equals(status) && errorMessage == null) {
            errorMessage = "Delivery dropped before it was sent";
        }

        triggerSubscriptionService.updateDelivery(triggerSubscription.getId(), timestamp, status, errorMessage);

        recordInvocation(
            triggerSubscription, externalUserId, jobId,
            SENT_STATUS.equals(status) ? null : DELIVERY_ERROR_TYPE_PREFIX + status, errorMessage, durationMs);
    }

    private void recordFailureAsync(
        TriggerSubscription triggerSubscription, @Nullable String externalUserId, long jobId, Instant timestamp,
        String tenantId, String errorType, String errorMessage) {

        try {
            deliveryCallbackExecutor.execute(() -> runWithTenantId(
                tenantId, triggerSubscription, jobId, () -> {
                    triggerSubscriptionService.updateDelivery(
                        triggerSubscription.getId(), timestamp, FAILED_STATUS, errorMessage);

                    recordInvocation(triggerSubscription, externalUserId, jobId, errorType, errorMessage, 0);
                }));
        } catch (RuntimeException runtimeException) {
            log.error(
                "Unable to record failed delivery of trigger subscription id={} event for job id={}",
                triggerSubscription.getId(), jobId, runtimeException);
        }
    }

    private void runWithTenantId(
        String tenantId, TriggerSubscription triggerSubscription, long jobId, TenantContext.Runnable runnable) {

        try {
            TenantContext.runWithTenantId(tenantId, runnable);
        } catch (RuntimeException runtimeException) {
            log.error(
                "Unable to record delivery of trigger subscription id={} event for job id={}",
                triggerSubscription.getId(), jobId, runtimeException);
        }
    }

    private void recordInvocation(
        TriggerSubscription triggerSubscription, @Nullable String externalUserId, long jobId,
        @Nullable String errorType, @Nullable String errorMessage, long durationMs) {

        Environment environment = triggerSubscription.getEnvironment();

        ExternalInvocationEvent.Builder builder = ExternalInvocationEvent.builder(
            PlatformType.EMBEDDED, ExternalInvocationSource.API_TRIGGER, ExternalInvocationKind.COMPONENT,
            triggerSubscription.getTriggerName());

        ExternalInvocationEvent externalInvocationEvent = builder
            .componentName(triggerSubscription.getComponentName())
            .componentVersion(triggerSubscription.getComponentVersion())
            .operationName(triggerSubscription.getTriggerName())
            .connectionId(triggerSubscription.getConnectionId())
            .environment(environment.ordinal())
            .externalUserId(externalUserId)
            .connectedUserId(triggerSubscription.getConnectedUserId())
            .integrationInstanceId(triggerSubscription.getIntegrationInstanceId())
            .jobId(jobId)
            .outcome(errorType == null ? ExternalInvocationOutcome.SUCCESS : ExternalInvocationOutcome.ERROR)
            .errorType(errorType)
            .errorMessage(errorMessage)
            .durationMs(durationMs)
            .build();

        externalInvocationRecorder.record(externalInvocationEvent);
    }
}
