/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.workspacesettings;

import com.bytechef.ee.ai.hub.audit.AiHubAuditEvent;
import com.bytechef.ee.ai.hub.audit.AuditAiHub;
import com.bytechef.ee.ai.hub.workspacesettings.repository.AiHubWorkspaceSettingsRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@Transactional
public class AiHubWorkspaceSettingsServiceImpl implements AiHubWorkspaceSettingsService {

    private final AiHubWorkspaceSettingsRepository repository;

    @SuppressFBWarnings("EI")
    public AiHubWorkspaceSettingsServiceImpl(AiHubWorkspaceSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiHubWorkspaceSettings> findByWorkspaceId(long workspaceId) {
        return repository.findByWorkspaceId(workspaceId);
    }

    @Override
    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_WORKSPACE_SETTINGS_UPDATED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "changedFields", value = "'voiceWebhookUrl'")
        })
    public AiHubWorkspaceSettings updateVoiceWebhookUrl(long workspaceId, @Nullable String voiceWebhookUrl) {
        AiHubWorkspaceSettings settings = repository.findByWorkspaceId(workspaceId)
            .orElseGet(() -> new AiHubWorkspaceSettings(workspaceId));

        settings.setVoiceWebhookUrl(voiceWebhookUrl);

        return repository.save(settings);
    }
}
