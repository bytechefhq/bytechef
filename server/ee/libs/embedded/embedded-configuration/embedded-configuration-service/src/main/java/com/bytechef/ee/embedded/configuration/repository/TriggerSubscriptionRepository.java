/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface TriggerSubscriptionRepository extends ListCrudRepository<TriggerSubscription, Long> {

    List<TriggerSubscription> findAllByConnectedUserId(long connectedUserId);

    List<TriggerSubscription> findAllByConnectedUserIdAndEnvironment(long connectedUserId, int environment);

    List<TriggerSubscription> findAllByConnectionId(long connectionId);

    List<TriggerSubscription> findAllByIntegrationInstanceId(long integrationInstanceId);

    @Query("SELECT * FROM connected_user_trigger_subscription WHERE id = :id FOR UPDATE")
    Optional<TriggerSubscription> findByIdForUpdate(@Param("id") long id);

    /**
     * Moves the registration lease while it is still {@code registrationLeaseUntil}, and skips a row another
     * transaction has locked instead of waiting for it, so a renewal never blocks on a request that changes the
     * subscription.
     */
    @Modifying
    @Query("""
        UPDATE connected_user_trigger_subscription SET registration_lease_until = :extendedRegistrationLeaseUntil
        WHERE id = (
            SELECT id FROM connected_user_trigger_subscription
            WHERE id = :id AND registration_lease_until = :registrationLeaseUntil
            FOR UPDATE SKIP LOCKED)
        """)
    int extendRegistrationLease(
        @Param("id") long id, @Param("registrationLeaseUntil") Instant registrationLeaseUntil,
        @Param("extendedRegistrationLeaseUntil") Instant extendedRegistrationLeaseUntil);

    @Modifying
    @Query("""
        UPDATE connected_user_trigger_subscription
        SET last_event_date = :lastEventDate, last_delivery_status = :lastDeliveryStatus,
            last_delivery_error = :lastDeliveryError
        WHERE id = :id AND (last_event_date IS NULL OR last_event_date <= :lastEventDate)
        """)
    void updateDelivery(
        @Param("id") long id, @Param("lastEventDate") Instant lastEventDate,
        @Param("lastDeliveryStatus") String lastDeliveryStatus,
        @Param("lastDeliveryError") @Nullable String lastDeliveryError);
}
