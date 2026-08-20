/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.agent.security;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps an agent id to its owning workspace ({@code ai_agent.workspace_id}). Reads the repository directly (not the
 * {@code @PreAuthorize}-guarded facade) to avoid recursion. Fails closed when the agent cannot be resolved.
 *
 * <p>
 * {@code ai_agent.workspace_id} is nullable in the schema and {@link AiAgent#getWorkspaceId()} is
 * {@code @Nullable Long}, so a workspace-less agent is a real row shape rather than a theoretical one. It must fail
 * closed: {@code Optional.map} collapses the null to {@link ResourceOwner#unknown()}, which both editions'
 * {@code PermissionServiceImpl} deny. Substituting any default workspace here would grant every member of that
 * workspace a foreign agent.
 *
 * <p>
 * The scope check is no longer the whole gate. {@link AiAgentVisibilityProvider} registers {@code 'AiAgent'} with
 * {@code PermissionServiceImpl.isResourceVisible}, so visibility is now a precondition of every
 * {@code hasPermission(…, 'AiAgent', …)} gate: a member holding {@code AGENT_VIEW} in the workspace is denied an agent
 * a colleague has withheld, unless they hold a grant. The answer comes from the agent's hidden {@code __AI_AGENT__}
 * project rather than from a second column on {@link AiAgent} — an agent's generated workflow is not reachable as a
 * capability separate from the agent, so the two can never need to diverge, and one question is kept to one record.
 *
 * <p>
 * Ownership and visibility answer different halves and both are needed: this resolver names the workspace whose role
 * carries the scope, the provider names who may see the row at all.
 *
 * @author Ivica Cardic
 */
@Component
public class AiAgentOwnershipResolver implements ResourceOwnershipResolver {

    private final AiAgentRepository aiAgentRepository;

    @SuppressFBWarnings("EI")
    public AiAgentOwnershipResolver(AiAgentRepository aiAgentRepository) {
        this.aiAgentRepository = aiAgentRepository;
    }

    @Override
    public String resourceType() {
        return "AiAgent";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return aiAgentRepository.findById(id)
            .map(AiAgent::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
