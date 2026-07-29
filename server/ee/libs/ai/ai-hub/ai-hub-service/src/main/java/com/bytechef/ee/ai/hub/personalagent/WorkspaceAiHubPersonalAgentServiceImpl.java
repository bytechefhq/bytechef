/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.personalagent.repository.AiHubPersonalAgentRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JDBC backed {@link WorkspaceAiHubPersonalAgentService}. Owns workspace-by-agent lookups against the
 * nullable {@code ai_hub_personal_agent.workspace_id} column — the platform-side {@code AiHubPersonalAgentService}
 * applies no workspace rules of its own and stays out of this surface.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI_EXPOSE_REP2")
class WorkspaceAiHubPersonalAgentServiceImpl implements WorkspaceAiHubPersonalAgentService {

    private final AiHubPersonalAgentRepository aiHubPersonalAgentRepository;

    WorkspaceAiHubPersonalAgentServiceImpl(AiHubPersonalAgentRepository aiHubPersonalAgentRepository) {
        this.aiHubPersonalAgentRepository = aiHubPersonalAgentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public long getWorkspaceId(long agentId) {
        // A null workspace_id is the same data-integrity state a missing membership row used to represent: the agent
        // is unreachable through every workspace-scoped path, so fail loudly rather than return a sentinel.
        return aiHubPersonalAgentRepository.findById(agentId)
            .map(AiHubPersonalAgent::getWorkspaceId)
            .orElseThrow(() -> new NotFoundException("No workspace for ai_hub_personal_agent id=" + agentId));
    }
}
