/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityNotificationChannelRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiObservabilityNotificationChannelServiceImpl implements AiObservabilityNotificationChannelService {

    private final AiObservabilityNotificationChannelRepository aiObservabilityNotificationChannelRepository;
    private final AiObservabilityNotificationDispatcher aiObservabilityNotificationDispatcher;

    AiObservabilityNotificationChannelServiceImpl(
        AiObservabilityNotificationChannelRepository aiObservabilityNotificationChannelRepository,
        AiObservabilityNotificationDispatcher aiObservabilityNotificationDispatcher) {

        this.aiObservabilityNotificationChannelRepository = aiObservabilityNotificationChannelRepository;
        this.aiObservabilityNotificationDispatcher = aiObservabilityNotificationDispatcher;
    }

    @Override
    public AiObservabilityNotificationChannel create(AiObservabilityNotificationChannel notificationChannel) {
        Validate.notNull(notificationChannel, "notificationChannel must not be null");
        Validate.isTrue(notificationChannel.getId() == null, "notificationChannel id must be null for creation");

        return aiObservabilityNotificationChannelRepository.save(notificationChannel);
    }

    @Override
    public void delete(long id) {
        aiObservabilityNotificationChannelRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityNotificationChannel getNotificationChannel(long id) {
        return aiObservabilityNotificationChannelRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("AiObservabilityNotificationChannel not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityNotificationChannel> getNotificationChannelsByWorkspace(Long workspaceId) {
        return aiObservabilityNotificationChannelRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public boolean test(long id) {
        AiObservabilityNotificationChannel notificationChannel = getNotificationChannel(id);

        aiObservabilityNotificationDispatcher.dispatchTest(notificationChannel);

        return true;
    }

    @Override
    public AiObservabilityNotificationChannel update(AiObservabilityNotificationChannel notificationChannel) {
        Validate.notNull(notificationChannel, "notificationChannel must not be null");
        Validate.notNull(notificationChannel.getId(), "notificationChannel id must not be null for update");

        return aiObservabilityNotificationChannelRepository.save(notificationChannel);
    }
}
