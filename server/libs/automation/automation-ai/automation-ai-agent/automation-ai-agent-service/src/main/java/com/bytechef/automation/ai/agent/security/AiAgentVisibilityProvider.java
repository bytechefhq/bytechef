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
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * An agent has no visibility of its own: it is exactly as visible as the hidden {@code __AI_AGENT__} project it keeps
 * its generated workflow in, so the record returned here is that PROJECT's and its grants are looked up under
 * {@code "Project"} — the same inheritance {@code ProjectDeploymentVisibilityProvider} and
 * {@code ProjectWorkflowVisibilityProvider} express for their own parents.
 *
 * <p>
 * One question, one record, deliberately. An agent's generated workflow is not reachable as a capability separate from
 * the agent, so "who can see this agent" and "who can see its project" can never need to diverge; a second
 * {@code visibility} column on {@code ai_agent} would be a second answer to one question and could only drift from this
 * one. {@code AiAgent.getProjectId()} is a non-null {@code long}, so the traversal below has no absent middle term.
 *
 * <p>
 * This governs the MANAGEMENT surfaces only. Marking an agent private removes it from colleagues' agent, agent
 * deployment and in-app chat launcher lists and from every {@code hasPermission(…, 'AiAgent', …)} by-id read, and does
 * nothing whatever to its channels: its Slack, WhatsApp, webhook and hosted-chat triggers keep answering everyone
 * exactly as before, because no runtime path consults visibility. Not being listed is not being switched off.
 *
 * <p>
 * Reads {@link AiAgentRepository} rather than the {@code @PreAuthorize}-guarded facade to avoid recursion, exactly as
 * {@link AiAgentOwnershipResolver} does. The project side goes through {@link ProjectService} rather than
 * {@code ProjectRepository} because the repository lives in {@code automation-configuration-service}, which this module
 * cannot depend on without inverting the dependency; {@code JobVisibilityProvider} resolves its parent project the same
 * way and for the same reason. {@code ProjectService.fetchProject(long)} carries no gate of its own, so the hop adds no
 * recursion.
 *
 * @author Ivica Cardic
 */
@Component
public class AiAgentVisibilityProvider implements ResourceVisibilityProvider {

    private final AiAgentRepository aiAgentRepository;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public AiAgentVisibilityProvider(AiAgentRepository aiAgentRepository, ProjectService projectService) {
        this.aiAgentRepository = aiAgentRepository;
        this.projectService = projectService;
    }

    @Override
    public String resourceType() {
        return "AiAgent";
    }

    @Override
    public String visibilityResourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return aiAgentRepository.findById(id)
            .map(AiAgent::getProjectId)
            .flatMap(projectService::fetchProject)
            .map(ProjectVisibilityFilter::toVisibilityRecord);
    }
}
