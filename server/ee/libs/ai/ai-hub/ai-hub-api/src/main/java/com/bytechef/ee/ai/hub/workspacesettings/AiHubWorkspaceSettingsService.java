/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.workspacesettings;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Service for managing AI Hub per-workspace settings.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubWorkspaceSettingsService {

    /**
     * Looks up the settings row for {@code workspaceId}. Returns {@link Optional#empty()} if no row exists — the
     * workspace has never had any AI Hub settings configured.
     */
    Optional<AiHubWorkspaceSettings> findByWorkspaceId(long workspaceId);

    /**
     * Upsert the Path A voice webhook URL for {@code workspaceId}. Creates a row if none exists, otherwise updates the
     * existing one. Passing {@code null} clears the URL.
     */
    AiHubWorkspaceSettings updateVoiceWebhookUrl(long workspaceId, @Nullable String voiceWebhookUrl);
}
