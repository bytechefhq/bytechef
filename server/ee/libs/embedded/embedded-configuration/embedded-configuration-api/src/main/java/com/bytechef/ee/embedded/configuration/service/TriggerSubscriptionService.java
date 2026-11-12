/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface TriggerSubscriptionService {

    TriggerSubscription create(TriggerSubscription triggerSubscription);

    void delete(long id);

    /**
     * Moves the registration lease of the subscription to {@code extendedRegistrationLeaseUntil} while it still expires
     * at {@code registrationLeaseUntil}, without waiting on the row lock of the subscription. Returns
     * {@link RegistrationLeaseExtension#EXTENDED} when the lease moved, {@link RegistrationLeaseExtension#LOCKED} when
     * another transaction holds the row lock while the stored lease is still {@code registrationLeaseUntil}, and
     * {@link RegistrationLeaseExtension#NOT_HELD} when another lease, or none, is stored. The {@code version} is left
     * unchanged: extending a lease does not change the subscription.
     */
    RegistrationLeaseExtension extendRegistrationLease(
        long id, Instant registrationLeaseUntil, Instant extendedRegistrationLeaseUntil);

    Optional<TriggerSubscription> fetchTriggerSubscription(long id);

    /**
     * Returns the subscription and locks its row until the current transaction ends, or an empty result when it does
     * not exist. Requires an active transaction.
     */
    Optional<TriggerSubscription> fetchLockedTriggerSubscription(long id);

    List<TriggerSubscription> getConnectedUserTriggerSubscriptions(long connectedUserId);

    List<TriggerSubscription> getConnectedUserTriggerSubscriptions(long connectedUserId, Environment environment);

    List<TriggerSubscription> getConnectionTriggerSubscriptions(long connectionId);

    List<TriggerSubscription> getIntegrationInstanceTriggerSubscriptions(long integrationInstanceId);

    TriggerSubscription getTriggerSubscription(long id);

    /**
     * Returns the subscription and locks its row until the current transaction ends. Requires an active transaction.
     */
    TriggerSubscription lockTriggerSubscription(long id);

    TriggerSubscription update(TriggerSubscription triggerSubscription);

    /**
     * Records the delivery outcome of the event that occurred at {@code lastEventDate}, unless the outcome of a later
     * event is already recorded.
     */
    void updateDelivery(
        long id, Instant lastEventDate, String lastDeliveryStatus, @Nullable String lastDeliveryError);

    /**
     * The outcome of {@link #extendRegistrationLease}.
     */
    enum RegistrationLeaseExtension {

        EXTENDED, LOCKED, NOT_HELD
    }
}
