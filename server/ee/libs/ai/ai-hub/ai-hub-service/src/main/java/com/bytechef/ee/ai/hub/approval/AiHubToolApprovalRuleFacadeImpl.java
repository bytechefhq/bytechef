/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AiHubToolApprovalRuleFacade}. Listing requires the workspace {@code VIEWER} role; creating
 * and deleting a rule require the tenant {@code ADMIN} authority. Only CATALOG rules are accepted. The built-in gated
 * tool names listed are those of the hub agents' registered tools that {@link AiHubToolApprovalDefaults} gates.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnAiHub
@Transactional
public class AiHubToolApprovalRuleFacadeImpl implements AiHubToolApprovalRuleFacade {

    private static final String VIEWER_ROLE = "VIEWER";

    private final AiHubPinnedToolCallbacks pinnedToolCallbacks;
    private final AiHubToolApprovalRuleService ruleService;
    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    public AiHubToolApprovalRuleFacadeImpl(
        AiHubPinnedToolCallbacks pinnedToolCallbacks, AiHubToolApprovalRuleService ruleService,
        PermissionService permissionService) {

        this.pinnedToolCallbacks = pinnedToolCallbacks;
        this.ruleService = ruleService;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubToolApprovalRule> getRules(long workspaceId) {
        if (!permissionService.hasWorkspaceRole(workspaceId, VIEWER_ROLE)) {
            throw new AccessDeniedException("Access denied to workspace " + workspaceId);
        }

        return ruleService.getRules(workspaceId);
    }

    @Override
    public List<String> getDefaultToolNames() {
        return Arrays.stream(Mode.values())
            .flatMap(mode -> pinnedToolCallbacks.pinnedToolCallbacks(mode)
                .stream())
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .filter(AiHubToolApprovalDefaults::isGatedByDefault)
            .distinct()
            .sorted()
            .toList();
    }

    @Override
    public AiHubToolApprovalRule createRule(
        long workspaceId, AiHubToolApproval.ToolKind toolKind, String toolName, AiHubToolApprovalRule.Mode mode) {

        checkAdmin();

        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName is required");
        }

        if (toolKind != AiHubToolApproval.ToolKind.CATALOG) {
            throw new IllegalArgumentException("Only CATALOG tool approval rules are supported");
        }

        AiHubToolApprovalRule rule = new AiHubToolApprovalRule();

        rule.setWorkspaceId(workspaceId);
        rule.setToolKind(toolKind);
        rule.setToolName(toolName.trim());
        rule.setMode(mode);

        return ruleService.save(rule);
    }

    @Override
    public void deleteRule(long workspaceId, long ruleId) {
        checkAdmin();

        ruleService.delete(workspaceId, ruleId);
    }

    private static void checkAdmin() {
        if (!SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)) {
            throw new AccessDeniedException("Managing tool approval rules requires the admin authority");
        }
    }
}
