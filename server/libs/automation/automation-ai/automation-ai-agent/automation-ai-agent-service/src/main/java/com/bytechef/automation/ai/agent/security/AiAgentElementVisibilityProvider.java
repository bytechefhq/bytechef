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

import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.repository.AiAgentElementRepository;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The visibility half of {@link AiAgentElementOwnershipResolver}, and registered for the same reason: an element id is
 * the whole argument list of {@code updateAgentElement} / {@code deleteAgentElement}, so without a provider here those
 * two gates would be the one way into a withheld agent that skipped the visibility precondition every
 * {@code 'AiAgent'}-keyed gate has.
 *
 * <p>
 * The tail is delegated to {@link AiAgentVisibilityProvider} rather than repeated, so the child path and the agent path
 * cannot disagree about which project an agent inherits from — the same pairing the ownership resolvers already have.
 *
 * @author Ivica Cardic
 */
@Component
public class AiAgentElementVisibilityProvider implements ResourceVisibilityProvider {

    private final AiAgentElementRepository aiAgentElementRepository;
    private final AiAgentVisibilityProvider aiAgentVisibilityProvider;

    @SuppressFBWarnings("EI")
    public AiAgentElementVisibilityProvider(
        AiAgentElementRepository aiAgentElementRepository, AiAgentVisibilityProvider aiAgentVisibilityProvider) {

        this.aiAgentElementRepository = aiAgentElementRepository;
        this.aiAgentVisibilityProvider = aiAgentVisibilityProvider;
    }

    @Override
    public String resourceType() {
        return "AiAgentElement";
    }

    @Override
    public String visibilityResourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return aiAgentElementRepository.findById(id)
            .map(AiAgentElement::getAgentId)
            .flatMap(aiAgentVisibilityProvider::fetchVisibility);
    }
}
