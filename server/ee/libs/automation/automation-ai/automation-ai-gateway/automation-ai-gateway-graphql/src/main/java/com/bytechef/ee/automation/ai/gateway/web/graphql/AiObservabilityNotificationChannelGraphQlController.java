/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannelType;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityNotificationChannelService;
import com.bytechef.ee.automation.ai.gateway.web.graphql.authorization.WorkspaceAuthorization;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiObservabilityNotificationChannelGraphQlController {

    private final AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService;
    private final WorkspaceAuthorization workspaceAuthorization;

    @SuppressFBWarnings("EI")
    AiObservabilityNotificationChannelGraphQlController(
        AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService,
        WorkspaceAuthorization workspaceAuthorization) {

        this.aiObservabilityNotificationChannelService = aiObservabilityNotificationChannelService;
        this.workspaceAuthorization = workspaceAuthorization;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityNotificationChannel aiObservabilityNotificationChannel(@Argument long id) {
        AiObservabilityNotificationChannel notificationChannel =
            aiObservabilityNotificationChannelService.getNotificationChannel(id);

        workspaceAuthorization.requireWorkspaceRole(notificationChannel.getWorkspaceId(), "VIEWER");

        return notificationChannel;
    }

    @QueryMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")
    public List<AiObservabilityNotificationChannel> aiObservabilityNotificationChannels(
        @Argument Long workspaceId) {

        return aiObservabilityNotificationChannelService.getNotificationChannelsByWorkspace(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityNotificationChannel createAiObservabilityNotificationChannel(
        @Argument Map<String, Object> input) {

        long workspaceId = Long.parseLong((String) input.get("workspaceId"));

        workspaceAuthorization.requireWorkspaceRole(workspaceId, "EDITOR");

        AiObservabilityNotificationChannel notificationChannel = new AiObservabilityNotificationChannel(
            workspaceId,
            (String) input.get("name"),
            AiObservabilityNotificationChannelType.valueOf((String) input.get("type")),
            (String) input.get("config"));

        notificationChannel.setEnabled((boolean) input.get("enabled"));

        return aiObservabilityNotificationChannelService.create(notificationChannel);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteAiObservabilityNotificationChannel(@Argument long id) {
        AiObservabilityNotificationChannel notificationChannel =
            aiObservabilityNotificationChannelService.getNotificationChannel(id);

        workspaceAuthorization.requireWorkspaceRole(notificationChannel.getWorkspaceId(), "EDITOR");

        aiObservabilityNotificationChannelService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean testAiObservabilityNotificationChannel(@Argument long id) {
        AiObservabilityNotificationChannel notificationChannel =
            aiObservabilityNotificationChannelService.getNotificationChannel(id);

        workspaceAuthorization.requireWorkspaceRole(notificationChannel.getWorkspaceId(), "EDITOR");

        return aiObservabilityNotificationChannelService.test(id);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityNotificationChannel updateAiObservabilityNotificationChannel(
        @Argument long id, @Argument Map<String, Object> input) {

        AiObservabilityNotificationChannel notificationChannel =
            aiObservabilityNotificationChannelService.getNotificationChannel(id);

        workspaceAuthorization.requireWorkspaceRole(notificationChannel.getWorkspaceId(), "EDITOR");

        notificationChannel.setName((String) input.get("name"));
        notificationChannel.setType(AiObservabilityNotificationChannelType.valueOf((String) input.get("type")));
        notificationChannel.setConfig((String) input.get("config"));
        notificationChannel.setEnabled((boolean) input.get("enabled"));

        return aiObservabilityNotificationChannelService.update(notificationChannel);
    }

}
