/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import java.util.List;

/**
 * Workspace-scoped CRUD over {@link AiHubToolApprovalRule} rows, plus the built-in default tool names a rule set can
 * override. Authorization is enforced here, not on the GraphQL controller: listing requires the workspace
 * {@code VIEWER} role, creating and deleting require the tenant {@code ADMIN} authority. Only CATALOG rules are
 * accepted.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubToolApprovalRuleFacade {

    List<AiHubToolApprovalRule> getRules(long workspaceId);

    List<String> getDefaultToolNames();

    AiHubToolApprovalRule createRule(
        long workspaceId, AiHubToolApproval.ToolKind toolKind, String toolName, AiHubToolApprovalRule.Mode mode);

    void deleteRule(long workspaceId, long ruleId);
}
