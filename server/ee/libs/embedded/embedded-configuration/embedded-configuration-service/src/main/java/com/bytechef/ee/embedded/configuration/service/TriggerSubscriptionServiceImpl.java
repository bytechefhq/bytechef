/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.repository.TriggerSubscriptionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class TriggerSubscriptionServiceImpl implements TriggerSubscriptionService {

    private static final int ERROR_MAX_LENGTH = 1024;

    private final TriggerSubscriptionRepository triggerSubscriptionRepository;

    public TriggerSubscriptionServiceImpl(TriggerSubscriptionRepository triggerSubscriptionRepository) {
        this.triggerSubscriptionRepository = triggerSubscriptionRepository;
    }

    @Override
    public TriggerSubscription create(TriggerSubscription triggerSubscription) {
        truncateErrorMessage(triggerSubscription);

        return triggerSubscriptionRepository.save(triggerSubscription);
    }

    @Override
    public void delete(long id) {
        triggerSubscriptionRepository.deleteById(id);
    }

    @Override
    public RegistrationLeaseExtension extendRegistrationLease(
        long id, Instant registrationLeaseUntil, Instant extendedRegistrationLeaseUntil) {

        int extendedCount = triggerSubscriptionRepository.extendRegistrationLease(
            id, registrationLeaseUntil, extendedRegistrationLeaseUntil);

        if (extendedCount > 0) {
            return RegistrationLeaseExtension.EXTENDED;
        }

        Optional<TriggerSubscription> triggerSubscriptionOptional = triggerSubscriptionRepository.findById(id);

        if (triggerSubscriptionOptional.isPresent()) {
            TriggerSubscription triggerSubscription = triggerSubscriptionOptional.get();

            if (registrationLeaseUntil.equals(triggerSubscription.getRegistrationLeaseUntil())) {
                return RegistrationLeaseExtension.LOCKED;
            }
        }

        return RegistrationLeaseExtension.NOT_HELD;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<TriggerSubscription> fetchLockedTriggerSubscription(long id) {
        return triggerSubscriptionRepository.findByIdForUpdate(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TriggerSubscription> fetchTriggerSubscription(long id) {
        return triggerSubscriptionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriggerSubscription> getConnectedUserTriggerSubscriptions(long connectedUserId) {
        return triggerSubscriptionRepository.findAllByConnectedUserId(connectedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriggerSubscription> getConnectedUserTriggerSubscriptions(
        long connectedUserId, Environment environment) {

        return triggerSubscriptionRepository.findAllByConnectedUserIdAndEnvironment(
            connectedUserId, environment.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriggerSubscription> getConnectionTriggerSubscriptions(long connectionId) {
        return triggerSubscriptionRepository.findAllByConnectionId(connectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriggerSubscription> getIntegrationInstanceTriggerSubscriptions(long integrationInstanceId) {
        return triggerSubscriptionRepository.findAllByIntegrationInstanceId(integrationInstanceId);
    }

    @Override
    @Transactional(readOnly = true)
    public TriggerSubscription getTriggerSubscription(long id) {
        Optional<TriggerSubscription> triggerSubscriptionOptional = triggerSubscriptionRepository.findById(id);

        return triggerSubscriptionOptional.orElseThrow(
            () -> new IllegalArgumentException("Trigger subscription not found: " + id));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public TriggerSubscription lockTriggerSubscription(long id) {
        Optional<TriggerSubscription> triggerSubscriptionOptional = triggerSubscriptionRepository.findByIdForUpdate(id);

        return triggerSubscriptionOptional.orElseThrow(
            () -> new IllegalArgumentException("Trigger subscription not found: " + id));
    }

    @Override
    public TriggerSubscription update(TriggerSubscription triggerSubscription) {
        truncateErrorMessage(triggerSubscription);

        return triggerSubscriptionRepository.save(triggerSubscription);
    }

    @Override
    public void updateDelivery(
        long id, Instant lastEventDate, String lastDeliveryStatus, @Nullable String lastDeliveryError) {

        triggerSubscriptionRepository.updateDelivery(
            id, lastEventDate, lastDeliveryStatus, StringUtils.abbreviate(lastDeliveryError, ERROR_MAX_LENGTH));
    }

    private static void truncateErrorMessage(TriggerSubscription triggerSubscription) {
        triggerSubscription.setErrorMessage(
            StringUtils.abbreviate(triggerSubscription.getErrorMessage(), ERROR_MAX_LENGTH));
    }
}
