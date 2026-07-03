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
 * Facade for {@link AiHubWorkspaceSettings}. Hosts the workspace-role authorization guards (VIEWER to read, ADMIN to
 * mutate) so they apply to every caller of the facade rather than only the GraphQL entry point.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubWorkspaceSettingsFacade {

    Optional<AiHubWorkspaceSettings> findByWorkspaceId(Long workspaceId);

    AiHubWorkspaceSettings updateVoiceWebhookUrl(Long workspaceId, @Nullable String voiceWebhookUrl);
}
