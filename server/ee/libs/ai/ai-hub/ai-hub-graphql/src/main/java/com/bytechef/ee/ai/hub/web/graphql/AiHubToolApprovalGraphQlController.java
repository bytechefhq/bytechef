/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.approval.AiHubToolApproval;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalFacade;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for the {@code aiHubToolApprovals} query and {@code resolveAiHubToolApproval} mutation declared
 * alongside {@link AiHubToolApprovalRuleGraphQlController} in {@code ai-hub-tool-approval.graphqls}. Authorization is
 * enforced on {@link AiHubToolApprovalFacade} (chat ownership); this controller additionally requires the workspace
 * VIEWER role, so a chat owner removed from the workspace can neither list nor resolve approvals.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnAiHub
public class AiHubToolApprovalGraphQlController {

    private final PermissionService permissionService;
    private final AiHubToolApprovalFacade toolApprovalFacade;

    @SuppressFBWarnings("EI")
    public AiHubToolApprovalGraphQlController(
        AiHubToolApprovalFacade toolApprovalFacade, PermissionService permissionService) {

        this.permissionService = permissionService;
        this.toolApprovalFacade = toolApprovalFacade;
    }

    @QueryMapping
    public List<AiHubToolApproval> aiHubToolApprovals(@Argument long workspaceId, @Argument long chatId) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        return toolApprovalFacade.list(workspaceId, chatId);
    }

    @MutationMapping
    public AiHubToolApprovalFacade.Resolution resolveAiHubToolApproval(
        @Argument long workspaceId, @Argument long approvalId, @Argument boolean approved,
        @Argument @Nullable String comment) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        return toolApprovalFacade.resolve(workspaceId, approvalId, approved, comment);
    }

    @SchemaMapping(typeName = "AiHubToolApproval", field = "toolKind")
    public String toolApprovalToolKind(AiHubToolApproval approval) {
        return approval.getToolKind()
            .name();
    }

    @SchemaMapping(typeName = "AiHubToolApproval", field = "status")
    public String toolApprovalStatus(AiHubToolApproval approval) {
        return approval.getStatus()
            .name();
    }

    @SchemaMapping(typeName = "AiHubToolApproval", field = "decidedAt")
    public @Nullable Long toolApprovalDecidedAt(AiHubToolApproval approval) {
        Instant decidedAt = approval.getDecidedAt();

        return decidedAt == null ? null : decidedAt.toEpochMilli();
    }

    @SchemaMapping(typeName = "AiHubToolApproval", field = "expiresAt")
    public long toolApprovalExpiresAt(AiHubToolApproval approval) {
        return approval.getExpiresAt()
            .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiHubToolApproval", field = "createdDate")
    public long toolApprovalCreatedDate(AiHubToolApproval approval) {
        return approval.getCreatedDate()
            .toEpochMilli();
    }
}
