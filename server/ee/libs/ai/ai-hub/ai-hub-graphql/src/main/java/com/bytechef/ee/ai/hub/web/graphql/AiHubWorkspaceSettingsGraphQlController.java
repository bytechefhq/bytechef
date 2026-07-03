/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.ee.ai.hub.workspacesettings.AiHubWorkspaceSettings;
import com.bytechef.ee.ai.hub.workspacesettings.AiHubWorkspaceSettingsFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for {@link AiHubWorkspaceSettings}. Read access requires the workspace VIEWER role (any member);
 * mutations are admin-only because changing the voice webhook URL affects every member of the workspace.
 *
 * <p>
 * Authorization is enforced on {@link AiHubWorkspaceSettingsFacade}, not here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class AiHubWorkspaceSettingsGraphQlController {

    private final AiHubWorkspaceSettingsFacade aiHubWorkspaceSettingsFacade;

    @SuppressFBWarnings("EI")
    public AiHubWorkspaceSettingsGraphQlController(AiHubWorkspaceSettingsFacade aiHubWorkspaceSettingsFacade) {
        this.aiHubWorkspaceSettingsFacade = aiHubWorkspaceSettingsFacade;
    }

    @QueryMapping
    public @Nullable AiHubWorkspaceSettings aiHubWorkspaceSettings(@Argument Long workspaceId) {
        return aiHubWorkspaceSettingsFacade.findByWorkspaceId(workspaceId)
            .orElse(null);
    }

    @MutationMapping
    public AiHubWorkspaceSettings updateAiHubVoiceWebhookUrl(@Argument UpdateAiHubVoiceWebhookUrlInput input) {
        return aiHubWorkspaceSettingsFacade.updateVoiceWebhookUrl(input.workspaceId(), input.voiceWebhookUrl());
    }

    public record UpdateAiHubVoiceWebhookUrlInput(Long workspaceId, @Nullable String voiceWebhookUrl) {
    }
}
