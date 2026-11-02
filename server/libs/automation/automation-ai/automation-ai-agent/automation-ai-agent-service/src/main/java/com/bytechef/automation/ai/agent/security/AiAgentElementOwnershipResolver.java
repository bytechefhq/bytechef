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
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.repository.AiAgentElementRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AiAgentElementOwnershipResolver implements ResourceOwnershipResolver {

    private final AiAgentElementRepository aiAgentElementRepository;
    private final AiAgentRepository aiAgentRepository;

    @SuppressFBWarnings("EI")
    public AiAgentElementOwnershipResolver(
        AiAgentElementRepository aiAgentElementRepository, AiAgentRepository aiAgentRepository) {

        this.aiAgentElementRepository = aiAgentElementRepository;
        this.aiAgentRepository = aiAgentRepository;
    }

    @Override
    public String resourceType() {
        return "AiAgentElement";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return aiAgentElementRepository.findById(id)
            .map(AiAgentElement::getAgentId)
            .flatMap(aiAgentRepository::findById)
            .map(AiAgent::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
