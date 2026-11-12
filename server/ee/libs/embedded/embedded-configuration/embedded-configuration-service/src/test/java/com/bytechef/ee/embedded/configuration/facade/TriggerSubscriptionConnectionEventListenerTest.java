/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionDisableReason;
import com.bytechef.platform.connection.event.ConnectionCredentialsInvalidatedEvent;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class TriggerSubscriptionConnectionEventListenerTest {

    private static final long CONNECTION_ID = 42L;

    private final TriggerSubscriptionManager triggerSubscriptionManager = mock(TriggerSubscriptionManager.class);
    private final TriggerSubscriptionConnectionEventListener triggerSubscriptionConnectionEventListener =
        new TriggerSubscriptionConnectionEventListener(triggerSubscriptionManager);

    @Test
    void testEventDisablesSubscriptionsWithConnectionInvalid() {
        triggerSubscriptionConnectionEventListener.onConnectionCredentialsInvalidated(
            new ConnectionCredentialsInvalidatedEvent(CONNECTION_ID));

        verify(triggerSubscriptionManager).disableByConnectionId(
            CONNECTION_ID, TriggerSubscriptionDisableReason.CONNECTION_INVALID);
    }

    @Test
    void testListenerRunsOnlyAfterThePublishingTransactionCommits() throws NoSuchMethodException {
        Method method = TriggerSubscriptionConnectionEventListener.class.getMethod(
            "onConnectionCredentialsInvalidated", ConnectionCredentialsInvalidatedEvent.class);

        TransactionalEventListener transactionalEventListener = method.getAnnotation(
            TransactionalEventListener.class);

        assertThat(transactionalEventListener).isNotNull();
        assertThat(transactionalEventListener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(transactionalEventListener.fallbackExecution()).isTrue();
    }

    @Test
    void testEventForTheSameConnectionRaisedWhileDisablingIsIgnored() {
        doAnswer(invocation -> {
            triggerSubscriptionConnectionEventListener.onConnectionCredentialsInvalidated(
                new ConnectionCredentialsInvalidatedEvent(CONNECTION_ID));

            return null;
        }).when(triggerSubscriptionManager)
            .disableByConnectionId(CONNECTION_ID, TriggerSubscriptionDisableReason.CONNECTION_INVALID);

        triggerSubscriptionConnectionEventListener.onConnectionCredentialsInvalidated(
            new ConnectionCredentialsInvalidatedEvent(CONNECTION_ID));

        verify(triggerSubscriptionManager, times(1)).disableByConnectionId(
            CONNECTION_ID, TriggerSubscriptionDisableReason.CONNECTION_INVALID);

        triggerSubscriptionConnectionEventListener.onConnectionCredentialsInvalidated(
            new ConnectionCredentialsInvalidatedEvent(CONNECTION_ID));

        verify(triggerSubscriptionManager, times(2)).disableByConnectionId(
            CONNECTION_ID, TriggerSubscriptionDisableReason.CONNECTION_INVALID);
    }
}
