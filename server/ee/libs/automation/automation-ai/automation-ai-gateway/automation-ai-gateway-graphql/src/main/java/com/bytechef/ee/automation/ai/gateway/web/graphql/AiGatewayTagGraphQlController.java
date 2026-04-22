/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayTag;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayTagService;
import com.bytechef.ee.automation.ai.gateway.web.graphql.authorization.WorkspaceAuthorization;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
class AiGatewayTagGraphQlController {

    private final AiGatewayTagService aiGatewayTagService;
    private final WorkspaceAuthorization workspaceAuthorization;

    @SuppressFBWarnings("EI")
    AiGatewayTagGraphQlController(
        AiGatewayTagService aiGatewayTagService, WorkspaceAuthorization workspaceAuthorization) {

        this.aiGatewayTagService = aiGatewayTagService;
        this.workspaceAuthorization = workspaceAuthorization;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AiGatewayTag aiGatewayTag(@Argument long id) {
        AiGatewayTag tag = aiGatewayTagService.getTag(id);

        workspaceAuthorization.requireWorkspaceRole(tag.getWorkspaceId(), "VIEWER");

        return tag;
    }

    @QueryMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")
    public List<AiGatewayTag> aiGatewayTags(@Argument Long workspaceId) {
        return aiGatewayTagService.getTagsByWorkspace(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#input.workspaceId(), 'EDITOR')")
    public AiGatewayTag createAiGatewayTag(@Argument CreateAiGatewayTagInput input) {
        AiGatewayTag tag = new AiGatewayTag(input.workspaceId(), input.name());

        tag.setColor(input.color());

        return aiGatewayTagService.create(tag);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteAiGatewayTag(@Argument long id) {
        AiGatewayTag tag = aiGatewayTagService.getTag(id);

        workspaceAuthorization.requireWorkspaceRole(tag.getWorkspaceId(), "EDITOR");

        aiGatewayTagService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiGatewayTag updateAiGatewayTag(
        @Argument long id, @Argument String name, @Argument String color) {

        AiGatewayTag tag = aiGatewayTagService.getTag(id);

        workspaceAuthorization.requireWorkspaceRole(tag.getWorkspaceId(), "EDITOR");

        return aiGatewayTagService.update(id, name, color);
    }

    public record CreateAiGatewayTagInput(String color, String name, Long workspaceId) {
    }
}
