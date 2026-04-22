/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.automation.ai.observability.domain.WorkspaceAiObservabilityNotificationChannel;
import com.bytechef.ee.automation.ai.observability.repository.WorkspaceAiObservabilityNotificationChannelRepository;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityNotificationChannelService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
class WorkspaceAiObservabilityNotificationChannelServiceImpl
    implements WorkspaceAiObservabilityNotificationChannelService {

    private final AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService;
    private final WorkspaceAiObservabilityNotificationChannelRepository workspaceAiObservabilityNotificationChannelRepository;

    WorkspaceAiObservabilityNotificationChannelServiceImpl(
        AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService,
        WorkspaceAiObservabilityNotificationChannelRepository workspaceAiObservabilityNotificationChannelRepository) {

        this.aiObservabilityNotificationChannelService = aiObservabilityNotificationChannelService;
        this.workspaceAiObservabilityNotificationChannelRepository =
            workspaceAiObservabilityNotificationChannelRepository;
    }

    @Override
    public AiObservabilityNotificationChannel createInWorkspace(
        AiObservabilityNotificationChannel notificationChannel, long workspaceId) {

        AiObservabilityNotificationChannel saved =
            aiObservabilityNotificationChannelService.create(notificationChannel);

        workspaceAiObservabilityNotificationChannelRepository.save(
            new WorkspaceAiObservabilityNotificationChannel(saved.getId(), workspaceId));

        return saved;
    }

    @Override
    public void delete(long id) {
        workspaceAiObservabilityNotificationChannelRepository.findByAiObservabilityNotificationChannelId(id)
            .ifPresent(membership -> workspaceAiObservabilityNotificationChannelRepository.deleteById(
                membership.getId()));

        aiObservabilityNotificationChannelService.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityNotificationChannel> getNotificationChannelsByWorkspace(Long workspaceId) {
        return workspaceAiObservabilityNotificationChannelRepository.findAllChannelsByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long notificationChannelId) {
        return workspaceAiObservabilityNotificationChannelRepository
            .findByAiObservabilityNotificationChannelId(notificationChannelId)
            .map(WorkspaceAiObservabilityNotificationChannel::getWorkspaceId)
            .orElse(null);
    }
}
