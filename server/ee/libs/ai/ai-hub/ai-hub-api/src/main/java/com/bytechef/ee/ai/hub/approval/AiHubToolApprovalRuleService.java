/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import java.util.List;

/**
 * CRUD over a workspace's {@link AiHubToolApprovalRule} rows.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubToolApprovalRuleService {

    List<AiHubToolApprovalRule> getRules(long workspaceId);

    AiHubToolApprovalRule save(AiHubToolApprovalRule rule);

    void delete(long workspaceId, long ruleId);
}
