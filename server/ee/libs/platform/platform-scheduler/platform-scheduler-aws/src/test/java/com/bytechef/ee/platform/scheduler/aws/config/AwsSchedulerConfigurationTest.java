/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.config;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsConnectionRefreshSchedulerConstants.CONNECTION_REFRESH_LISTENER_ID;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.DYNAMIC_WEBHOOK_TRIGGER_REFRESH_LISTENER_ID;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.POLLING_TRIGGER_LISTENER_ID;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULE_TRIGGER_LISTENER_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AwsSchedulerConfigurationTest {

    private MessageListenerContainerRegistry messageListenerContainerRegistry;
    private AwsSchedulerConfiguration awsSchedulerConfiguration;

    @BeforeEach
    void beforeEach() {
        messageListenerContainerRegistry = mock(MessageListenerContainerRegistry.class);

        awsSchedulerConfiguration = new AwsSchedulerConfiguration(
            mock(ApplicationProperties.class), messageListenerContainerRegistry);
    }

    @Test
    void testSchedulerSqsListenerContainerFactoryDisablesAutoStartup() {
        SqsMessageListenerContainerFactory<Object> factory =
            awsSchedulerConfiguration.schedulerSqsListenerContainerFactory(mock(SqsAsyncClient.class));

        assertNotNull(factory);

        MessageListenerContainer<Object> container = factory.createContainer("scheduler-test-queue");

        assertFalse(
            container.isAutoStartup(),
            "Scheduler SQS listener containers must not auto-start so they are not consumed during cold start");
    }

    @Test
    void testStartSchedulerListenersStartsStoppedContainers() {
        MessageListenerContainer<?> pollingContainer = mockContainer(POLLING_TRIGGER_LISTENER_ID, false);
        MessageListenerContainer<?> scheduleContainer = mockContainer(SCHEDULE_TRIGGER_LISTENER_ID, false);
        MessageListenerContainer<?> dynamicWebhookContainer =
            mockContainer(DYNAMIC_WEBHOOK_TRIGGER_REFRESH_LISTENER_ID, false);
        MessageListenerContainer<?> connectionRefreshContainer = mockContainer(CONNECTION_REFRESH_LISTENER_ID, false);

        awsSchedulerConfiguration.startSchedulerListeners();

        verify(pollingContainer).start();
        verify(scheduleContainer).start();
        verify(dynamicWebhookContainer).start();
        verify(connectionRefreshContainer).start();
    }

    @Test
    void testStartSchedulerListenersSkipsRunningContainers() {
        MessageListenerContainer<?> pollingContainer = mockContainer(POLLING_TRIGGER_LISTENER_ID, true);

        mockContainer(SCHEDULE_TRIGGER_LISTENER_ID, false);
        mockContainer(DYNAMIC_WEBHOOK_TRIGGER_REFRESH_LISTENER_ID, false);
        mockContainer(CONNECTION_REFRESH_LISTENER_ID, false);

        awsSchedulerConfiguration.startSchedulerListeners();

        verify(pollingContainer, never()).start();
    }

    @Test
    void testStartSchedulerListenersToleratesMissingContainers() {
        doReturn(null).when(messageListenerContainerRegistry)
            .getContainerById(POLLING_TRIGGER_LISTENER_ID);

        mockContainer(SCHEDULE_TRIGGER_LISTENER_ID, false);
        mockContainer(DYNAMIC_WEBHOOK_TRIGGER_REFRESH_LISTENER_ID, false);
        mockContainer(CONNECTION_REFRESH_LISTENER_ID, false);

        assertDoesNotThrow(() -> awsSchedulerConfiguration.startSchedulerListeners());
    }

    @Test
    void testStartSchedulerListenersContinuesWhenContainerFailsToStart() {
        MessageListenerContainer<?> pollingContainer = mockContainer(POLLING_TRIGGER_LISTENER_ID, false);

        doThrow(new RuntimeException("Transient SQS failure")).when(pollingContainer)
            .start();

        MessageListenerContainer<?> scheduleContainer = mockContainer(SCHEDULE_TRIGGER_LISTENER_ID, false);
        MessageListenerContainer<?> dynamicWebhookContainer =
            mockContainer(DYNAMIC_WEBHOOK_TRIGGER_REFRESH_LISTENER_ID, false);
        MessageListenerContainer<?> connectionRefreshContainer = mockContainer(CONNECTION_REFRESH_LISTENER_ID, false);

        assertDoesNotThrow(() -> awsSchedulerConfiguration.startSchedulerListeners());

        verify(scheduleContainer).start();
        verify(dynamicWebhookContainer).start();
        verify(connectionRefreshContainer).start();
    }

    private MessageListenerContainer<?> mockContainer(String listenerId, boolean running) {
        MessageListenerContainer<?> container = mock(MessageListenerContainer.class);

        when(container.isRunning()).thenReturn(running);

        doReturn(container).when(messageListenerContainerRegistry)
            .getContainerById(listenerId);

        return container;
    }
}
