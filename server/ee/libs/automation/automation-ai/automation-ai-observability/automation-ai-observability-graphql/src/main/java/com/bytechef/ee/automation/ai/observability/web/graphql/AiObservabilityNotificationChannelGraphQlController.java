/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.observability.facade.AiObservabilityNotificationChannelFacade;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityNotificationChannelService;
import com.bytechef.ee.automation.ai.observability.web.graphql.authorization.WorkspaceAuthorization;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityNotificationChannelType;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityNotificationChannelService;
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

    private final AiObservabilityNotificationChannelFacade aiObservabilityNotificationChannelFacade;
    private final AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService;
    private final WorkspaceAiObservabilityNotificationChannelService workspaceAiObservabilityNotificationChannelService;
    private final WorkspaceAuthorization workspaceAuthorization;

    @SuppressFBWarnings("EI")
    AiObservabilityNotificationChannelGraphQlController(
        AiObservabilityNotificationChannelFacade aiObservabilityNotificationChannelFacade,
        AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService,
        WorkspaceAiObservabilityNotificationChannelService workspaceAiObservabilityNotificationChannelService,
        WorkspaceAuthorization workspaceAuthorization) {

        this.aiObservabilityNotificationChannelFacade = aiObservabilityNotificationChannelFacade;
        this.aiObservabilityNotificationChannelService = aiObservabilityNotificationChannelService;
        this.workspaceAiObservabilityNotificationChannelService = workspaceAiObservabilityNotificationChannelService;
        this.workspaceAuthorization = workspaceAuthorization;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityNotificationChannel aiObservabilityNotificationChannel(@Argument long id) {
        AiObservabilityNotificationChannel notificationChannel =
            aiObservabilityNotificationChannelService.getNotificationChannel(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityNotificationChannelService.getWorkspaceId(notificationChannel.getId()), "VIEWER");

        return notificationChannel;
    }

    @QueryMapping
    public List<AiObservabilityNotificationChannel> aiObservabilityNotificationChannels(
        @Argument Long workspaceId) {

        // Authorization (workspace VIEWER role) is enforced on AiObservabilityNotificationChannelFacade so it protects
        // every caller of the facade, not just this GraphQL entry point.
        return aiObservabilityNotificationChannelFacade.getNotificationChannelsByWorkspace(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityNotificationChannel createAiObservabilityNotificationChannel(
        @Argument Map<String, Object> input) {

        long workspaceId = Long.parseLong((String) input.get("workspaceId"));

        workspaceAuthorization.requireWorkspaceRole(workspaceId, "EDITOR");

        AiObservabilityNotificationChannel notificationChannel = new AiObservabilityNotificationChannel(
            (String) input.get("name"),
            AiObservabilityNotificationChannelType.valueOf((String) input.get("type")),
            (String) input.get("config"));

        notificationChannel.setEnabled((boolean) input.get("enabled"));

        return workspaceAiObservabilityNotificationChannelService.createInWorkspace(notificationChannel, workspaceId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteAiObservabilityNotificationChannel(@Argument long id) {
        AiObservabilityNotificationChannel notificationChannel =
            aiObservabilityNotificationChannelService.getNotificationChannel(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityNotificationChannelService.getWorkspaceId(notificationChannel.getId()), "EDITOR");

        aiObservabilityNotificationChannelService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean testAiObservabilityNotificationChannel(@Argument long id) {
        AiObservabilityNotificationChannel notificationChannel =
            aiObservabilityNotificationChannelService.getNotificationChannel(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityNotificationChannelService.getWorkspaceId(notificationChannel.getId()), "EDITOR");

        return aiObservabilityNotificationChannelService.test(id);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityNotificationChannel updateAiObservabilityNotificationChannel(
        @Argument long id, @Argument Map<String, Object> input) {

        AiObservabilityNotificationChannel notificationChannel =
            aiObservabilityNotificationChannelService.getNotificationChannel(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityNotificationChannelService.getWorkspaceId(notificationChannel.getId()), "EDITOR");

        notificationChannel.setName((String) input.get("name"));
        notificationChannel.setType(AiObservabilityNotificationChannelType.valueOf((String) input.get("type")));
        notificationChannel.setConfig((String) input.get("config"));
        notificationChannel.setEnabled((boolean) input.get("enabled"));

        return aiObservabilityNotificationChannelService.update(notificationChannel);
    }

}
