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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.repository.AiAgentChannelRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentElementRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pins the inheritance invariant for the agent family: an agent and its two child row types carry no visibility of
 * their own, so every provider here answers with the hidden backing PROJECT's record under the {@code "Project"} type.
 *
 * <p>
 * Both halves of each assertion do work that the other cannot. The record must be the project's, because a provider
 * that returned the agent's own id would look up grants under {@code ("Project", agentId)} — rows that do not exist —
 * and hide an agent its owner had shared. And {@code visibilityResourceType()} must say {@code "Project"}, because that
 * is the string {@code PermissionServiceImpl.isResourceVisible} hands the resolver; the ids alone would resolve against
 * {@code "AiAgent"} grants and deny for the same reason.
 *
 * @author Ivica Cardic
 */
class AiAgentVisibilityProvidersTest {

    private static final long AGENT_ID = 3L;
    private static final long CHANNEL_ID = 6L;
    private static final long ELEMENT_ID = 7L;
    private static final long PROJECT_ID = 5L;

    private final AiAgentRepository aiAgentRepository = mock(AiAgentRepository.class);
    private final AiAgentChannelRepository aiAgentChannelRepository = mock(AiAgentChannelRepository.class);
    private final AiAgentElementRepository aiAgentElementRepository = mock(AiAgentElementRepository.class);
    private final ProjectService projectService = mock(ProjectService.class);

    private AiAgentVisibilityProvider aiAgentVisibilityProvider;

    @BeforeEach
    void setUp() {
        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setVisibility(ResourceVisibility.PRIVATE);

        // created_by is @CreatedBy-managed; the test seeds it the way the persistence layer would
        ReflectionTestUtils.setField(project, "createdBy", "ivica");

        when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project));

        AiAgent agent = new AiAgent();

        agent.setId(AGENT_ID);
        agent.setProjectId(PROJECT_ID);

        when(aiAgentRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent));

        AiAgentChannel agentChannel = mock(AiAgentChannel.class);

        when(agentChannel.getAgentId()).thenReturn(AGENT_ID);
        when(aiAgentChannelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(agentChannel));

        AiAgentElement agentElement = mock(AiAgentElement.class);

        when(agentElement.getAgentId()).thenReturn(AGENT_ID);
        when(aiAgentElementRepository.findById(ELEMENT_ID)).thenReturn(Optional.of(agentElement));

        aiAgentVisibilityProvider = new AiAgentVisibilityProvider(aiAgentRepository, projectService);
    }

    @Test
    void testAgentProviderInheritsProjectRecord() {
        assertThat(aiAgentVisibilityProvider.resourceType()).isEqualTo("AiAgent");
        assertThat(aiAgentVisibilityProvider.visibilityResourceType()).isEqualTo("Project");
        assertThat(aiAgentVisibilityProvider.fetchVisibility(AGENT_ID)).contains(projectRecord());
        assertThat(aiAgentVisibilityProvider.fetchVisibility(999L)).as("an unknown agent must fail closed")
            .isEmpty();
    }

    /**
     * An agent whose row exists but whose backing project has been deleted resolves to nothing rather than to a record,
     * so the by-id gates deny it. The alternative — treating a missing project as unrestricted — would make deleting
     * the project the way to un-hide the agent.
     */
    @Test
    void testAgentWithoutProjectFailsClosed() {
        AiAgent orphanAgent = new AiAgent();

        orphanAgent.setId(4L);
        orphanAgent.setProjectId(404L);

        when(aiAgentRepository.findById(4L)).thenReturn(Optional.of(orphanAgent));
        when(projectService.fetchProject(404L)).thenReturn(Optional.empty());

        assertThat(aiAgentVisibilityProvider.fetchVisibility(4L)).isEmpty();
    }

    @Test
    void testChannelProviderInheritsProjectRecord() {
        AiAgentChannelVisibilityProvider provider =
            new AiAgentChannelVisibilityProvider(aiAgentChannelRepository, aiAgentVisibilityProvider);

        assertThat(provider.resourceType()).isEqualTo("AiAgentChannel");
        assertThat(provider.visibilityResourceType()).isEqualTo("Project");
        assertThat(provider.fetchVisibility(CHANNEL_ID)).contains(projectRecord());
        assertThat(provider.fetchVisibility(999L)).as("an unknown channel must fail closed")
            .isEmpty();
    }

    @Test
    void testElementProviderInheritsProjectRecord() {
        AiAgentElementVisibilityProvider provider =
            new AiAgentElementVisibilityProvider(aiAgentElementRepository, aiAgentVisibilityProvider);

        assertThat(provider.resourceType()).isEqualTo("AiAgentElement");
        assertThat(provider.visibilityResourceType()).isEqualTo("Project");
        assertThat(provider.fetchVisibility(ELEMENT_ID)).contains(projectRecord());
        assertThat(provider.fetchVisibility(999L)).as("an unknown element must fail closed")
            .isEmpty();
    }

    private static VisibilityRecord projectRecord() {
        return new VisibilityRecord(PROJECT_ID, ResourceVisibility.PRIVATE, "ivica");
    }
}
