/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionDisableReason;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.event.ConnectionCredentialsInvalidatedEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Disables the trigger subscriptions of a connection whose credentials became invalid. Runs after the transaction that
 * marked the credentials invalid commits, or at once when there is none, so a failure can never roll back the INVALID
 * status, and nothing is disabled when that transaction rolls back. Each subscription is disabled in its own
 * transaction; a failure is logged and recorded on that subscription. Disabling a trigger can itself refresh the
 * credentials of the same connection and mark them invalid again; that nested event is ignored.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class TriggerSubscriptionConnectionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TriggerSubscriptionConnectionEventListener.class);

    private static final ThreadLocal<Set<Long>> IN_PROGRESS_CONNECTION_IDS = ThreadLocal.withInitial(HashSet::new);

    private final TriggerSubscriptionManager triggerSubscriptionManager;

    @SuppressFBWarnings("EI")
    public TriggerSubscriptionConnectionEventListener(TriggerSubscriptionManager triggerSubscriptionManager) {
        this.triggerSubscriptionManager = triggerSubscriptionManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onConnectionCredentialsInvalidated(ConnectionCredentialsInvalidatedEvent event) {
        long connectionId = event.connectionId();
        Set<Long> inProgressConnectionIds = IN_PROGRESS_CONNECTION_IDS.get();

        if (!inProgressConnectionIds.add(connectionId)) {
            return;
        }

        try {
            triggerSubscriptionManager.disableByConnectionId(
                connectionId, TriggerSubscriptionDisableReason.CONNECTION_INVALID);
        } catch (RuntimeException exception) {
            log.error(
                "Unable to disable the trigger subscriptions of connection id={} with invalid credentials",
                connectionId, exception);
        } finally {
            inProgressConnectionIds.remove(connectionId);

            if (inProgressConnectionIds.isEmpty()) {
                IN_PROGRESS_CONNECTION_IDS.remove();
            }
        }
    }
}
