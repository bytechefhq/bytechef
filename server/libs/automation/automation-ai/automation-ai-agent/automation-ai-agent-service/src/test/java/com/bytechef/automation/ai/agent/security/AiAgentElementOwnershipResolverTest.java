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
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.repository.AiAgentElementRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class AiAgentElementOwnershipResolverTest {

    private static final long CALLER_WORKSPACE_ID = 42L;
    private static final long FOREIGN_WORKSPACE_ID = 99L;

    private final AiAgentElementRepository aiAgentElementRepository = mock(AiAgentElementRepository.class);
    private final AiAgentRepository aiAgentRepository = mock(AiAgentRepository.class);
    private final AiAgentElementOwnershipResolver resolver = new AiAgentElementOwnershipResolver(
        aiAgentElementRepository, aiAgentRepository);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("AiAgentElement");
    }

    @Test
    void testResolvesWorkspaceViaAgent() {
        givenElement(1L, 7L, CALLER_WORKSPACE_ID);

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).hasValue(CALLER_WORKSPACE_ID);
    }

    @Test
    void testElementOfAForeignAgentResolvesToTheForeignWorkspace() {
        givenElement(1L, 7L, FOREIGN_WORKSPACE_ID);

        assertThat(resolver.resolveOwner(1L)
            .workspaceId())
                .hasValue(FOREIGN_WORKSPACE_ID)
                .isNotEqualTo(OptionalLong.of(CALLER_WORKSPACE_ID));
    }

    @Test
    void testWorkspacelessAgentIsUnknown() {
        givenElement(1L, 7L, null);

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).isEmpty();
    }

    @Test
    void testUnknownElementIsUnknown() {
        when(aiAgentElementRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .workspaceId()).isEmpty();
    }

    @Test
    void testDanglingAgentIsUnknown() {
        AiAgentElement element = new AiAgentElement(7L, AiAgentElement.KIND_TOOL);

        when(aiAgentElementRepository.findById(1L)).thenReturn(Optional.of(element));
        when(aiAgentRepository.findById(7L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).isEmpty();
    }

    @Test
    void testWorkspacelessAgentIsDeniedThroughTheEvaluator() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("user", null, List.of()));

        PermissionServiceImpl permissionService = new PermissionServiceImpl(mock(UserService.class), List.of(resolver));

        givenElement(1L, 7L, CALLER_WORKSPACE_ID);

        assertThat(permissionService.hasResourceScope(1L, "AiAgentElement", "AGENT_EDIT")).isTrue();

        givenElement(1L, 7L, null);

        assertThat(permissionService.hasResourceScope(1L, "AiAgentElement", "AGENT_EDIT")).isFalse();
    }

    private void givenElement(long elementId, long agentId, Long workspaceId) {
        AiAgentElement element = new AiAgentElement(agentId, AiAgentElement.KIND_TOOL);
        AiAgent agent = new AiAgent();

        agent.setWorkspaceId(workspaceId);

        when(aiAgentElementRepository.findById(elementId)).thenReturn(Optional.of(element));
        when(aiAgentRepository.findById(agentId)).thenReturn(Optional.of(agent));
    }
}
