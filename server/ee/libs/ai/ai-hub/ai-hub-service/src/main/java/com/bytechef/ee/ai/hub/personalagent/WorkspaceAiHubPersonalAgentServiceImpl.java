/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.personalagent.repository.WorkspaceAiHubPersonalAgentRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JDBC backed {@link WorkspaceAiHubPersonalAgentService}. Owns workspace-by-agent lookups against the
 * {@code workspace_ai_hub_personal_agent} relation table — the platform-side {@code AiHubPersonalAgentService} is
 * intentionally workspace-agnostic and stays out of this surface.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI_EXPOSE_REP2")
class WorkspaceAiHubPersonalAgentServiceImpl implements WorkspaceAiHubPersonalAgentService {

    private final WorkspaceAiHubPersonalAgentRepository workspaceAiHubPersonalAgentRepository;

    WorkspaceAiHubPersonalAgentServiceImpl(
        WorkspaceAiHubPersonalAgentRepository workspaceAiHubPersonalAgentRepository) {

        this.workspaceAiHubPersonalAgentRepository = workspaceAiHubPersonalAgentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public long getWorkspaceId(long agentId) {
        return workspaceAiHubPersonalAgentRepository.findByAiHubPersonalAgentId(agentId)
            .orElseThrow(() -> new NotFoundException(
                "No workspace membership row for ai_hub_personal_agent id=" + agentId))
            .getWorkspaceId();
    }
}
