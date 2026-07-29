/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.service;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRuleTarget;
import java.util.List;

/**
 * Workspace-scoped operations on eval rules. Mirrors the {@code platform-connection.ConnectionService} /
 * {@code automation-configuration.WorkspaceConnectionService} split: the platform module owns the entity + CRUD, the
 * automation module owns the rule's {@code workspace_id} binding and any workspace-scoped query.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiEvalRuleService {

    /**
     * Binds the {@link AiEvalRule} to {@code workspaceId} and creates it via the platform service.
     *
     * @return the persisted rule (with id assigned)
     */
    AiEvalRule createInWorkspace(AiEvalRule evalRule, long workspaceId);

    void deleteInWorkspace(long evalRuleId);

    Long getWorkspaceId(long evalRuleId);

    List<AiEvalRule> getEvalRulesByWorkspace(Long workspaceId);

    List<AiEvalRule> getEnabledEvalRulesByWorkspace(Long workspaceId);

    List<AiEvalRule> getEnabledEvalRulesByWorkspaceAndTarget(Long workspaceId, AiEvalRuleTarget target);
}
