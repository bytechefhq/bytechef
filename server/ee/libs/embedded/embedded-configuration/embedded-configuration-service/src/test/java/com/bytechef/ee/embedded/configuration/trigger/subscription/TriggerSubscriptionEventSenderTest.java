/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.trigger.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionDisableReason;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionWebhookSettingsDTO;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationEvent;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationRecorder;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome.PayloadResult;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome.Status;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.delivery.WebhookPayload;
import com.bytechef.platform.notification.delivery.WebhookRetry;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TriggerSubscriptionEventSenderTest {

    private static final String ENVIRONMENT_SECRET = "whsec_environment";
    private static final String ENVIRONMENT_URL = "https://environment.example.com/hook";

    private ConnectedUserService connectedUserService;
    private ExternalInvocationRecorder externalInvocationRecorder;
    private TriggerSubscriptionEventSender triggerSubscriptionEventSender;
    private TriggerSubscriptionService triggerSubscriptionService;
    private TriggerSubscriptionWebhookSettingsAccessor triggerSubscriptionWebhookSettingsAccessor;
    private WebhookDeliveryService webhookDeliveryService;
    private final List<Runnable> pendingCallbacks = new ArrayList<>();

    @BeforeEach
    void beforeEach() {
        connectedUserService = mock(ConnectedUserService.class);
        externalInvocationRecorder = mock(ExternalInvocationRecorder.class);
        triggerSubscriptionService = mock(TriggerSubscriptionService.class);
        triggerSubscriptionWebhookSettingsAccessor = mock(TriggerSubscriptionWebhookSettingsAccessor.class);
        webhookDeliveryService = mock(WebhookDeliveryService.class);

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getExternalId()).thenReturn("user-1");
        when(connectedUserService.fetchConnectedUser(7L)).thenReturn(Optional.of(connectedUser));

        when(triggerSubscriptionWebhookSettingsAccessor.getSettings(Environment.PRODUCTION))
            .thenReturn(new TriggerSubscriptionWebhookSettingsDTO(ENVIRONMENT_URL, ENVIRONMENT_SECRET));

        triggerSubscriptionEventSender = createTriggerSubscriptionEventSender(
            createApplicationProperties(Duration.ofSeconds(1), 3, 2.0));
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testRetryMultiplierBelowOneIsRejected() {
        assertInvalidRetry(Duration.ofSeconds(1), 3, 0.5, "multiplier");
    }

    @Test
    void testNegativeMaxRetriesIsRejected() {
        assertInvalidRetry(Duration.ofSeconds(1), -1, 2.0, "max-retries");
    }

    @Test
    void testNonPositiveRetryInitialIntervalIsRejected() {
        assertInvalidRetry(Duration.ZERO, 3, 2.0, "initial-interval");
    }

    @Test
    void testRetryWithoutRetriesIsAccepted() {
        assertThatCode(() -> createTriggerSubscriptionEventSender(createApplicationProperties(null, 0, 0.0)))
            .doesNotThrowAnyException();
    }

    @Test
    void testCreateTestDeliveryBuildsSyntheticTriggerEvent() {
        WebhookDelivery webhookDelivery = triggerSubscriptionEventSender.createTestDelivery(
            Environment.PRODUCTION, ENVIRONMENT_URL, ENVIRONMENT_SECRET);

        String json = webhookDelivery.payloads()
            .getFirst()
            .json();

        assertThat(webhookDelivery.url()).isEqualTo(ENVIRONMENT_URL);
        assertThat(webhookDelivery.secret()).isEqualTo(ENVIRONMENT_SECRET);
        assertThat(webhookDelivery.eventType()).isEqualTo("trigger.event");
        assertThat(webhookDelivery.validateUrl()).isTrue();
        assertThat(json).contains("\"type\":\"trigger.event\"");
        assertThat(json).contains("\"componentName\":\"test\"");
        assertThat(json).contains("\"triggerName\":\"test\"");
        assertThat(json).contains("\"componentVersion\":1");
        assertThat(json).contains("\"subscriptionId\":\"test\"");
        assertThat(json).contains("\"data\":{\"test\":true}");
    }

    @Test
    void testLifecycleEventInsideATransactionIsSubmittedAfterCommit() {
        when(webhookDeliveryService.submit(any())).thenReturn(new CompletableFuture<>());

        TransactionTemplate transactionTemplate = new TransactionTemplate(new NoOpTransactionManager());

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            sendConnectedUserDeletedEvent();

            verify(connectedUserService).fetchConnectedUser(7L);
            verify(webhookDeliveryService, never()).submit(any());
        });

        WebhookDelivery webhookDelivery = captureWebhookDelivery();

        String json = webhookDelivery.payloads()
            .getFirst()
            .json();

        assertThat(json).contains("\"externalUserId\":\"user-1\"");
        assertThat(json).contains("\"reason\":\"CONNECTED_USER_DELETED\"");
    }

    @Test
    void testLifecycleEventInsideATransactionThatRollsBackIsNeverSubmitted() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(new NoOpTransactionManager());

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            sendConnectedUserDeletedEvent();

            transactionStatus.setRollbackOnly();
        });

        verify(webhookDeliveryService, never()).submit(any());
    }

    @Test
    void testLifecycleEventSubmitFailureAfterCommitDoesNotFailTheCommit() {
        when(webhookDeliveryService.submit(any())).thenThrow(new IllegalStateException("queue unavailable"));

        TransactionTemplate transactionTemplate = new TransactionTemplate(new NoOpTransactionManager());

        assertThatCode(() -> transactionTemplate.executeWithoutResult(
            transactionStatus -> sendConnectedUserDeletedEvent()))
                .doesNotThrowAnyException();

        verify(webhookDeliveryService).submit(any());
    }

    @Test
    void testLifecycleEventWithoutATransactionIsSubmittedImmediately() {
        when(webhookDeliveryService.submit(any())).thenReturn(new CompletableFuture<>());

        sendConnectedUserDeletedEvent();

        verify(webhookDeliveryService).submit(any());
    }

    @Test
    void testSendLifecycleEventLeavesTheSignatureTimestampToEachAttempt() {
        when(webhookDeliveryService.submit(any())).thenReturn(new CompletableFuture<>());

        sendConnectedUserDeletedEvent();

        WebhookDelivery webhookDelivery = captureWebhookDelivery();

        assertThat(webhookDelivery.timestamp()).isNull();
        assertThat(webhookDelivery.payloads()
            .getFirst()
            .json()).containsPattern("\"timestamp\":\"\\d{4}-\\d{2}-\\d{2}T[^\"]+Z\"");
    }

    private void sendConnectedUserDeletedEvent() {
        triggerSubscriptionEventSender.sendLifecycleEvent(
            createTriggerSubscription(null), "subscription.deleted",
            TriggerSubscriptionDisableReason.CONNECTED_USER_DELETED);
    }

    @Test
    void testSendTriggerEventBuildsEnvelopeAndDoesNotWait() {
        when(webhookDeliveryService.submit(any())).thenReturn(new CompletableFuture<>());

        triggerSubscriptionEventSender.sendTriggerEvent(createTriggerSubscription(null), Map.of("id", "c1"), 42L);

        WebhookDelivery webhookDelivery = captureWebhookDelivery();

        assertThat(webhookDelivery.source()).isEqualTo("triggerSubscription");
        assertThat(webhookDelivery.url()).isEqualTo(ENVIRONMENT_URL);
        assertThat(webhookDelivery.eventType()).isEqualTo("trigger.event");
        assertThat(webhookDelivery.secret()).isEqualTo(ENVIRONMENT_SECRET);
        assertThat(webhookDelivery.deliveryId()).isNotBlank();
        assertThat(webhookDelivery.timestamp()).isNull();
        assertThat(webhookDelivery.retry()).isEqualTo(new WebhookRetry(3, Duration.ofSeconds(1), 2.0));

        List<WebhookPayload> payloads = webhookDelivery.payloads();

        assertThat(payloads).hasSize(1);

        String json = payloads.getFirst()
            .json();

        assertThat(json).contains("\"type\":\"trigger.event\"");
        assertThat(json).contains("\"id\":\"" + webhookDelivery.deliveryId() + "\"");
        assertThat(json).contains("\"subscriptionId\":11");
        assertThat(json).contains("\"externalId\":\"ext-1\"");
        assertThat(json).contains("\"externalUserId\":\"user-1\"");
        assertThat(json).contains("\"environment\":\"PRODUCTION\"");
        assertThat(json).contains("\"componentName\":\"slack\"");
        assertThat(json).contains("\"triggerName\":\"newMessage\"");
        assertThat(json).contains("\"componentVersion\":1");
        assertThat(json).containsPattern("\"timestamp\":\"\\d{4}-\\d{2}-\\d{2}T[^\"]+Z\"");
        assertThat(json).contains("\"jobId\":42");
        assertThat(json).contains("\"data\":{\"id\":\"c1\"}");

        verify(triggerSubscriptionService, never()).updateDelivery(anyLong(), any(), anyString(), any());
        verify(externalInvocationRecorder, never()).record(any(ExternalInvocationEvent.class));
    }

    @Test
    void testOverrideUrlWinsOverEnvironmentUrl() {
        when(webhookDeliveryService.submit(any())).thenReturn(new CompletableFuture<>());

        triggerSubscriptionEventSender.sendTriggerEvent(
            createTriggerSubscription("https://override.example.com/hook"), Map.of(), 42L);

        WebhookDelivery webhookDelivery = captureWebhookDelivery();

        assertThat(webhookDelivery.url()).isEqualTo("https://override.example.com/hook");
        assertThat(webhookDelivery.secret()).isEqualTo(ENVIRONMENT_SECRET);
    }

    @Test
    void testSuccessfulOutcomeRecordsSuccessRow() {
        CompletableFuture<WebhookDeliveryOutcome> outcomeFuture = new CompletableFuture<>();

        when(webhookDeliveryService.submit(any())).thenReturn(outcomeFuture);

        triggerSubscriptionEventSender.sendTriggerEvent(createTriggerSubscription(null), Map.of(), 42L);

        outcomeFuture.complete(createOutcome(Status.SENT, null));

        runPendingCallbacks();

        verify(triggerSubscriptionService).updateDelivery(eq(11L), any(Instant.class), eq("SENT"), eq(null));

        ExternalInvocationEvent externalInvocationEvent = captureExternalInvocationEvent();

        assertThat(externalInvocationEvent.type()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(externalInvocationEvent.source()).isEqualTo(ExternalInvocationSource.API_TRIGGER);
        assertThat(externalInvocationEvent.kind()).isEqualTo(ExternalInvocationKind.COMPONENT);
        assertThat(externalInvocationEvent.toolName()).isEqualTo("newMessage");
        assertThat(externalInvocationEvent.operationName()).isEqualTo("newMessage");
        assertThat(externalInvocationEvent.componentName()).isEqualTo("slack");
        assertThat(externalInvocationEvent.componentVersion()).isEqualTo(1);
        assertThat(externalInvocationEvent.connectionId()).isEqualTo(3L);
        assertThat(externalInvocationEvent.connectedUserId()).isEqualTo(7L);
        assertThat(externalInvocationEvent.integrationInstanceId()).isEqualTo(5L);
        assertThat(externalInvocationEvent.externalUserId()).isEqualTo("user-1");
        assertThat(externalInvocationEvent.environment()).isEqualTo(Environment.PRODUCTION.ordinal());
        assertThat(externalInvocationEvent.jobId()).isEqualTo(42L);
        assertThat(externalInvocationEvent.outcome()).isEqualTo(ExternalInvocationOutcome.SUCCESS);
        assertThat(externalInvocationEvent.errorType()).isNull();
        assertThat(externalInvocationEvent.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testFailedOutcomeRecordsErrorRowAndFailedStatus() {
        CompletableFuture<WebhookDeliveryOutcome> outcomeFuture = new CompletableFuture<>();

        when(webhookDeliveryService.submit(any())).thenReturn(outcomeFuture);

        triggerSubscriptionEventSender.sendTriggerEvent(createTriggerSubscription(null), Map.of(), 42L);

        outcomeFuture.complete(createOutcome(Status.FAILED, "HTTP 500"));

        runPendingCallbacks();

        verify(triggerSubscriptionService).updateDelivery(eq(11L), any(Instant.class), eq("FAILED"), eq("HTTP 500"));

        ExternalInvocationEvent externalInvocationEvent = captureExternalInvocationEvent();

        assertThat(externalInvocationEvent.outcome()).isEqualTo(ExternalInvocationOutcome.ERROR);
        assertThat(externalInvocationEvent.errorType()).isEqualTo("DELIVERY_FAILED");
        assertThat(externalInvocationEvent.errorMessage()).isEqualTo("HTTP 500");
    }

    @Test
    void testDroppedOutcomeRecordsErrorRow() {
        CompletableFuture<WebhookDeliveryOutcome> outcomeFuture = new CompletableFuture<>();

        when(webhookDeliveryService.submit(any())).thenReturn(outcomeFuture);

        triggerSubscriptionEventSender.sendTriggerEvent(createTriggerSubscription(null), Map.of(), 42L);

        outcomeFuture.complete(WebhookDeliveryOutcome.dropped(List.of(new WebhookPayload("11", "{}"))));

        runPendingCallbacks();

        verify(triggerSubscriptionService).updateDelivery(
            eq(11L), any(Instant.class), eq("DROPPED"), eq("Delivery dropped before it was sent"));

        ExternalInvocationEvent externalInvocationEvent = captureExternalInvocationEvent();

        assertThat(externalInvocationEvent.outcome()).isEqualTo(ExternalInvocationOutcome.ERROR);
        assertThat(externalInvocationEvent.errorType()).isEqualTo("DELIVERY_DROPPED");
    }

    @Test
    void testHandleWithoutDestinationRecordsError() {
        when(triggerSubscriptionWebhookSettingsAccessor.getSettings(Environment.PRODUCTION))
            .thenReturn(new TriggerSubscriptionWebhookSettingsDTO(null, ENVIRONMENT_SECRET));

        triggerSubscriptionEventSender.sendTriggerEvent(createTriggerSubscription(null), Map.of(), 42L);

        verify(webhookDeliveryService, never()).submit(any());
        verify(triggerSubscriptionService, never()).updateDelivery(anyLong(), any(), anyString(), any());
        verify(externalInvocationRecorder, never()).record(any(ExternalInvocationEvent.class));

        runPendingCallbacks();

        String message = "No webhook URL configured for environment PRODUCTION";

        verify(triggerSubscriptionService).updateDelivery(eq(11L), any(Instant.class), eq("FAILED"), eq(message));

        ExternalInvocationEvent externalInvocationEvent = captureExternalInvocationEvent();

        assertThat(externalInvocationEvent.outcome()).isEqualTo(ExternalInvocationOutcome.ERROR);
        assertThat(externalInvocationEvent.errorType()).isEqualTo("NO_DESTINATION");
        assertThat(externalInvocationEvent.errorMessage()).isEqualTo(message);
        assertThat(externalInvocationEvent.jobId()).isEqualTo(42L);
    }

    @Test
    void testOutcomeCallbackRunsUnderCapturedTenant() throws InterruptedException {
        CompletableFuture<WebhookDeliveryOutcome> outcomeFuture = new CompletableFuture<>();
        AtomicReference<String> updateTenantId = new AtomicReference<>();
        AtomicReference<String> recordTenantId = new AtomicReference<>();

        when(webhookDeliveryService.submit(any())).thenReturn(outcomeFuture);

        doAnswer(invocation -> {
            updateTenantId.set(TenantContext.getCurrentTenantId());

            return null;
        }).when(triggerSubscriptionService)
            .updateDelivery(anyLong(), any(), anyString(), any());

        doAnswer(invocation -> {
            recordTenantId.set(TenantContext.getCurrentTenantId());

            return null;
        }).when(externalInvocationRecorder)
            .record(any(ExternalInvocationEvent.class));

        TenantContext.setCurrentTenantId("tenant1");

        triggerSubscriptionEventSender.sendTriggerEvent(createTriggerSubscription(null), Map.of(), 42L);

        TenantContext.resetCurrentTenantId();

        Thread completingThread = new Thread(() -> outcomeFuture.complete(createOutcome(Status.SENT, null)));

        completingThread.start();
        completingThread.join();

        Thread callbackThread = new Thread(this::runPendingCallbacks);

        callbackThread.start();
        callbackThread.join();

        assertThat(updateTenantId.get()).isEqualTo("tenant1");
        assertThat(recordTenantId.get()).isEqualTo("tenant1");
    }

    @Test
    void testLifecycleEventHasReasonAndNoData() {
        CompletableFuture<WebhookDeliveryOutcome> outcomeFuture = new CompletableFuture<>();

        when(webhookDeliveryService.submit(any())).thenReturn(outcomeFuture);

        triggerSubscriptionEventSender.sendLifecycleEvent(
            createTriggerSubscription(null), "subscription.disabled",
            TriggerSubscriptionDisableReason.CONNECTION_DELETED);

        WebhookDelivery webhookDelivery = captureWebhookDelivery();

        assertThat(webhookDelivery.eventType()).isEqualTo("subscription.disabled");

        String json = webhookDelivery.payloads()
            .getFirst()
            .json();

        assertThat(json).contains("\"type\":\"subscription.disabled\"");
        assertThat(json).contains("\"reason\":\"CONNECTION_DELETED\"");
        assertThat(json).contains("\"subscriptionId\":11");
        assertThat(json).doesNotContain("\"data\"");
        assertThat(json).doesNotContain("\"jobId\"");

        outcomeFuture.complete(createOutcome(Status.SENT, null));

        runPendingCallbacks();

        verify(triggerSubscriptionService, never()).updateDelivery(anyLong(), any(), anyString(), any());
        verify(externalInvocationRecorder, never()).record(any(ExternalInvocationEvent.class));
    }

    @Test
    void testInlineCompletedOutcomeIsRecordedOffTheCallingThread() {
        when(webhookDeliveryService.submit(any()))
            .thenReturn(CompletableFuture.completedFuture(createOutcome(Status.SENT, null)));

        triggerSubscriptionEventSender.sendTriggerEvent(createTriggerSubscription(null), Map.of(), 42L);

        verify(triggerSubscriptionService, never()).updateDelivery(anyLong(), any(), anyString(), any());
        verify(externalInvocationRecorder, never()).record(any(ExternalInvocationEvent.class));
        assertThat(pendingCallbacks).hasSize(1);

        runPendingCallbacks();

        verify(triggerSubscriptionService).updateDelivery(eq(11L), any(Instant.class), eq("SENT"), eq(null));
        verify(externalInvocationRecorder).record(any(ExternalInvocationEvent.class));
    }

    @Test
    void testMissingSigningSecretRecordsErrorWithoutSubmitting() {
        when(triggerSubscriptionWebhookSettingsAccessor.getSettings(Environment.PRODUCTION))
            .thenReturn(new TriggerSubscriptionWebhookSettingsDTO(null, null));

        triggerSubscriptionEventSender.sendTriggerEvent(
            createTriggerSubscription("https://override.example.com/hook"), Map.of(), 42L);

        verify(webhookDeliveryService, never()).submit(any());
        verify(externalInvocationRecorder, never()).record(any(ExternalInvocationEvent.class));

        runPendingCallbacks();

        String message = "No signing secret configured for environment PRODUCTION";

        verify(triggerSubscriptionService).updateDelivery(eq(11L), any(Instant.class), eq("FAILED"), eq(message));

        ExternalInvocationEvent externalInvocationEvent = captureExternalInvocationEvent();

        assertThat(externalInvocationEvent.outcome()).isEqualTo(ExternalInvocationOutcome.ERROR);
        assertThat(externalInvocationEvent.errorType()).isEqualTo("NO_SIGNING_SECRET");
        assertThat(externalInvocationEvent.errorMessage()).isEqualTo(message);
    }

    @Test
    void testLifecycleEventWithoutSigningSecretIsSkipped() {
        when(triggerSubscriptionWebhookSettingsAccessor.getSettings(Environment.PRODUCTION))
            .thenReturn(new TriggerSubscriptionWebhookSettingsDTO(ENVIRONMENT_URL, " "));

        triggerSubscriptionEventSender.sendLifecycleEvent(
            createTriggerSubscription(null), "subscription.deleted", null);

        runPendingCallbacks();

        verify(webhookDeliveryService, never()).submit(any());
        verify(triggerSubscriptionService, never()).updateDelivery(anyLong(), any(), anyString(), any());
        verify(externalInvocationRecorder, never()).record(any(ExternalInvocationEvent.class));
    }

    @Test
    void testSubmitFailureRecordsErrorRowAndDoesNotThrow() {
        when(webhookDeliveryService.submit(any())).thenThrow(new IllegalStateException("queue unavailable"));

        triggerSubscriptionEventSender.sendTriggerEvent(createTriggerSubscription(null), Map.of(), 42L);

        verify(externalInvocationRecorder, never()).record(any(ExternalInvocationEvent.class));

        runPendingCallbacks();

        verify(triggerSubscriptionService).updateDelivery(
            eq(11L), any(Instant.class), eq("FAILED"), eq("queue unavailable"));

        ExternalInvocationEvent externalInvocationEvent = captureExternalInvocationEvent();

        assertThat(externalInvocationEvent.outcome()).isEqualTo(ExternalInvocationOutcome.ERROR);
        assertThat(externalInvocationEvent.errorType()).isEqualTo("DELIVERY_ERROR");
        assertThat(externalInvocationEvent.errorMessage()).isEqualTo("queue unavailable");
    }

    @Test
    void testExceptionalCompletionRecordsUnwrappedCause() {
        CompletableFuture<WebhookDeliveryOutcome> outcomeFuture = new CompletableFuture<>();

        when(webhookDeliveryService.submit(any())).thenReturn(outcomeFuture);

        triggerSubscriptionEventSender.sendTriggerEvent(createTriggerSubscription(null), Map.of(), 42L);

        outcomeFuture.completeExceptionally(new IllegalStateException("drainer stopped"));

        runPendingCallbacks();

        verify(triggerSubscriptionService).updateDelivery(
            eq(11L), any(Instant.class), eq("FAILED"), eq("drainer stopped"));

        ExternalInvocationEvent externalInvocationEvent = captureExternalInvocationEvent();

        assertThat(externalInvocationEvent.errorType()).isEqualTo("DELIVERY_FAILED");
        assertThat(externalInvocationEvent.errorMessage()).isEqualTo("drainer stopped");
    }

    @Test
    void testBlankOverrideUrlFallsBackToEnvironmentUrl() {
        when(webhookDeliveryService.submit(any())).thenReturn(new CompletableFuture<>());

        triggerSubscriptionEventSender.sendTriggerEvent(createTriggerSubscription(" "), Map.of(), 42L);

        WebhookDelivery webhookDelivery = captureWebhookDelivery();

        assertThat(webhookDelivery.url()).isEqualTo(ENVIRONMENT_URL);
    }

    private void runPendingCallbacks() {
        List<Runnable> callbacks = new ArrayList<>(pendingCallbacks);

        pendingCallbacks.clear();

        callbacks.forEach(Runnable::run);
    }

    private ExternalInvocationEvent captureExternalInvocationEvent() {
        ArgumentCaptor<ExternalInvocationEvent> externalInvocationEventCaptor = ArgumentCaptor.forClass(
            ExternalInvocationEvent.class);

        verify(externalInvocationRecorder).record(externalInvocationEventCaptor.capture());

        return externalInvocationEventCaptor.getValue();
    }

    private WebhookDelivery captureWebhookDelivery() {
        ArgumentCaptor<WebhookDelivery> webhookDeliveryCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).submit(webhookDeliveryCaptor.capture());

        return webhookDeliveryCaptor.getValue();
    }

    private void assertInvalidRetry(
        Duration initialInterval, int maxRetries, double multiplier, String propertyName) {

        ApplicationProperties applicationProperties = createApplicationProperties(
            initialInterval, maxRetries, multiplier);

        assertThatThrownBy(() -> createTriggerSubscriptionEventSender(applicationProperties))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bytechef.embedded.trigger-subscription.delivery.retry." + propertyName);
    }

    private TriggerSubscriptionEventSender createTriggerSubscriptionEventSender(
        ApplicationProperties applicationProperties) {

        return new TriggerSubscriptionEventSender(
            applicationProperties, connectedUserService, pendingCallbacks::add, externalInvocationRecorder,
            triggerSubscriptionService, triggerSubscriptionWebhookSettingsAccessor, webhookDeliveryService);
    }

    private static ApplicationProperties createApplicationProperties(
        Duration initialInterval, int maxRetries, double multiplier) {

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        ApplicationProperties.Embedded.TriggerSubscription.Delivery.Retry retry = embedded.getTriggerSubscription()
            .getDelivery()
            .getRetry();

        retry.setInitialInterval(initialInterval);
        retry.setMaxRetries(maxRetries);
        retry.setMultiplier(multiplier);

        return applicationProperties;
    }

    private static WebhookDeliveryOutcome createOutcome(Status status, String lastFailure) {
        return new WebhookDeliveryOutcome(List.of(new PayloadResult("11", status)), null, lastFailure, lastFailure);
    }

    private static TriggerSubscription createTriggerSubscription(String webhookUrl) {
        TriggerSubscription triggerSubscription = new TriggerSubscription();

        ReflectionTestUtils.setField(triggerSubscription, "id", 11L);

        triggerSubscription.setComponentName("slack");
        triggerSubscription.setComponentVersion(1);
        triggerSubscription.setConnectedUserId(7L);
        triggerSubscription.setConnectionId(3L);
        triggerSubscription.setEnvironment(Environment.PRODUCTION);
        triggerSubscription.setExternalId("ext-1");
        triggerSubscription.setIntegrationInstanceId(5L);
        triggerSubscription.setStatus(TriggerSubscriptionStatus.ENABLED);
        triggerSubscription.setTriggerName("newMessage");
        triggerSubscription.setWebhookUrl(webhookUrl);

        return triggerSubscription;
    }

    private static final class NoOpTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition transactionDefinition) {
            Objects.requireNonNull(transaction);
        }

        @Override
        protected void doCommit(DefaultTransactionStatus transactionStatus) {
            Objects.requireNonNull(transactionStatus);
        }

        @Override
        protected void doRollback(DefaultTransactionStatus transactionStatus) {
            Objects.requireNonNull(transactionStatus);
        }
    }
}
