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

package com.bytechef.automation.ai.agent.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.ai.agent.channel.AgentChannelResolver;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.service.AiAgentChannelService;
import com.bytechef.automation.ai.agent.service.AiAgentElementService;
import com.bytechef.automation.ai.agent.service.AiAgentService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The list half of agent visibility. {@code AiAgentVisibilityProvider} makes the by-id gates deny a withheld agent;
 * nothing in it touches a listing, because the listings are gated on the WORKSPACE id and a workspace-keyed gate cannot
 * answer a per-row question. Without the filter these methods would keep naming an agent that every by-id read of it
 * denied — the same list/by-id disagreement {@code PermissionServiceVisibilityTest} pins for connections.
 *
 * <p>
 * The real {@link ProjectVisibilityFilter} is used, not a mock of it, so the test fails if the facade stops routing
 * through the one component every project list surface shares. Only the resolver behind it is stubbed.
 *
 * @author Ivica Cardic
 */
class AiAgentFacadeVisibilityFilterTest {

    private static final long HIDDEN_AGENT_ID = 10L;
    private static final long HIDDEN_AGENT_TAG_ID = 20L;
    private static final long HIDDEN_PROJECT_ID = 100L;
    private static final long SHARED_AGENT_ID = 11L;
    private static final long SHARED_AGENT_TAG_ID = 21L;
    private static final long SHARED_PROJECT_ID = 101L;
    private static final long WORKSPACE_ID = 1L;

    private final AiAgentService agentService = mock(AiAgentService.class);
    private final AiAgentChannelService agentChannelService = mock(AiAgentChannelService.class);
    private final AiAgentElementService agentElementService = mock(AiAgentElementService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final TagService tagService = mock(TagService.class);

    private AiAgentFacadeImpl aiAgentFacade;

    @BeforeEach
    void setUp() {
        when(agentService.getAgents(WORKSPACE_ID))
            .thenReturn(
                List.of(agent(HIDDEN_AGENT_ID, HIDDEN_PROJECT_ID), agent(SHARED_AGENT_ID, SHARED_PROJECT_ID)));

        when(projectService.getProject(HIDDEN_PROJECT_ID)).thenReturn(project(HIDDEN_PROJECT_ID));
        when(projectService.getProject(SHARED_PROJECT_ID)).thenReturn(project(SHARED_PROJECT_ID));

        when(agentChannelService.getByAgentId(SHARED_AGENT_ID)).thenReturn(List.of());
        when(agentElementService.getByAgentId(SHARED_AGENT_ID)).thenReturn(List.of());
        when(tagService.getTags(anyList())).thenReturn(List.of());

        when(agentChannelService.getByAgentId(HIDDEN_AGENT_ID)).thenReturn(List.of());
        when(agentElementService.getByAgentId(HIDDEN_AGENT_ID)).thenReturn(List.of());

        aiAgentFacade = facadeWith(hidingVisibilityFilter());
    }

    private AiAgentFacadeImpl facadeWith(ProjectVisibilityFilter projectVisibilityFilter) {
        return new AiAgentFacadeImpl(
            agentService, mock(AgentChannelResolver.class), agentChannelService, agentElementService,
            mock(EnvironmentService.class), projectService, projectVisibilityFilter,
            mock(ProjectWorkflowService.class), projectDeploymentService,
            mock(ProjectDeploymentWorkflowService.class), mock(TriggerDefinitionService.class),
            mock(WorkflowService.class), mock(WorkflowTestConfigurationService.class),
            mock(WorkflowNodeTestOutputService.class), tagService, mock(PrincipalJobService.class),
            mock(JobService.class), mock(UserService.class), "http://localhost/webhooks/{id}", List.of());
    }

    @Test
    void testGetAgentsDropsAnAgentTheResolverHides() {
        List<AiAgentDTO> agents = aiAgentFacade.getAgents(WORKSPACE_ID);

        assertThat(agents)
            .extracting(agentDTO -> agentDTO.agent()
                .getId())
            .as("the withheld agent must not be named to a member who cannot open it")
            .containsExactly(SHARED_AGENT_ID);
    }

    /**
     * The reach that reaches the client is each agent's OWN backing project's, read on the way out rather than stored a
     * second time on {@code ai_agent}. Run against a resolver that admits both agents, and asserting two different
     * values, so a facade that returned a constant — {@code WORKSPACE} is both the enum's usual answer and
     * {@code Project}'s column default — fails here instead of passing on the majority case.
     */
    @Test
    void testGetAgentsCarriesEachBackingProjectsOwnVisibility() {
        List<AiAgentDTO> agents = facadeWith(admitAllVisibilityFilter()).getAgents(WORKSPACE_ID);

        assertThat(agents).extracting(AiAgentDTO::visibility)
            .containsExactly(ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE);
    }

    /**
     * Asserting the returned list is empty would not distinguish "filtered" from "this agent happens to have no
     * deployment", so the assertion is that the hidden agent's project is never even asked about — which is false the
     * moment the filter is dropped, whatever the deployment stubs say.
     */
    @Test
    void testGetAgentDeploymentsNeverLooksUpAHiddenAgentsDeployments() {
        aiAgentFacade.getAgentDeployments(WORKSPACE_ID);

        verify(projectDeploymentService, never()).fetchProjectDeployment(eq(HIDDEN_PROJECT_ID), any());
        verify(projectDeploymentService, atLeastOnce()).fetchProjectDeployment(eq(SHARED_PROJECT_ID), any());
    }

    /**
     * The in-app chat launcher's agent cascade, filtered for the same reason its sibling cascade
     * {@code ProjectDeploymentFacadeImpl.getWorkspaceChatWorkflows} already was: the two halves of one popup cannot
     * disagree about who may be named in it, and what this half discloses is an agent's name and title.
     *
     * <p>
     * Asserted as "the hidden agent's project is never looked up" rather than as an empty result, for the reason
     * {@link #testGetAgentDeploymentsNeverLooksUpAHiddenAgentsDeployments} gives: with no deployment stubbed the
     * returned list is empty either way, so an emptiness assertion would stay green with the filter deleted.
     */
    @Test
    void testGetWorkspaceChatAgentsNeverLooksUpAHiddenAgentsDeployment() {
        aiAgentFacade.getWorkspaceChatAgents(WORKSPACE_ID, 0L);

        verify(projectDeploymentService, never()).fetchProjectDeployment(eq(HIDDEN_PROJECT_ID), any());
        verify(projectDeploymentService, atLeastOnce()).fetchProjectDeployment(eq(SHARED_PROJECT_ID), any());
    }

    /**
     * A tag name is weak information, but this listing feeds the filter dropdown over
     * {@link AiAgentFacadeImpl#getAgents}, so a name aggregated off a withheld agent is both a disclosure and a
     * dropdown option that selects nothing.
     *
     * <p>
     * The two agents carry DIFFERENT tag ids and the assertion is on the ids handed to {@code TagService}: a facade
     * that stopped filtering asks for both and fails here, where asserting the returned tags would only pin what the
     * stub was told to return.
     */
    @Test
    void testGetAgentTagsAggregatesOnlyTheTagsOfVisibleAgents() {
        aiAgentFacade.getAgentTags(WORKSPACE_ID);

        verify(tagService).getTags(List.of(SHARED_AGENT_TAG_ID));
    }

    /**
     * The control the assertion above leans on: the same two agents, and the only change is a resolver that hides
     * neither — so the tag found missing above is one the listing would otherwise have offered.
     */
    @Test
    void testGetAgentTagsAggregatesBothWhenTheResolverHidesNeither() {
        facadeWith(admitAllVisibilityFilter()).getAgentTags(WORKSPACE_ID);

        verify(tagService).getTags(List.of(HIDDEN_AGENT_TAG_ID, SHARED_AGENT_TAG_ID));
    }

    private static AiAgent agent(long id, long projectId) {
        AiAgent agent = new AiAgent();

        agent.setId(id);
        agent.setName("agent-" + id);
        agent.setTitle("Agent " + id);
        agent.setProjectId(projectId);
        agent.setTagIds(List.of(id == HIDDEN_AGENT_ID ? HIDDEN_AGENT_TAG_ID : SHARED_AGENT_TAG_ID));
        agent.setWorkspaceId(WORKSPACE_ID);

        return agent;
    }

    private static Project project(long id) {
        Project project = new Project();

        project.setId(id);
        project.setWorkspaceId(WORKSPACE_ID);
        project.setVisibility(
            id == HIDDEN_PROJECT_ID ? ResourceVisibility.PRIVATE : ResourceVisibility.WORKSPACE);

        return project;
    }

    /**
     * The production filter over a resolver that hides exactly the withheld project — the shape
     * {@code ResourceVisibilityResolverImpl} produces for a member who is neither the owner nor a grantee.
     */
    private static ProjectVisibilityFilter hidingVisibilityFilter() {
        return projectVisibilityFilter(id -> id != HIDDEN_PROJECT_ID);
    }

    /**
     * The production filter over a resolver that hides nothing — used where the assertion is about what the facade
     * reads off each project rather than about which ones survive.
     */
    private static ProjectVisibilityFilter admitAllVisibilityFilter() {
        return projectVisibilityFilter(id -> true);
    }

    @SuppressWarnings("unchecked")
    private static ProjectVisibilityFilter projectVisibilityFilter(LongPredicate visible) {
        ResourceVisibilityResolver resourceVisibilityResolver =
            (resourceType, workspaceId, candidates) -> candidates.stream()
                .map(ResourceVisibilityResolver.VisibilityRecord::id)
                .filter(visible::test)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        ObjectProvider<ResourceVisibilityResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(resourceVisibilityResolver);

        return new ProjectVisibilityFilter(objectProvider);
    }
}
