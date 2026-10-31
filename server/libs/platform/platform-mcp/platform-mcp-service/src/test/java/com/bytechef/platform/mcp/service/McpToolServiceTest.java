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

package com.bytechef.platform.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class McpToolServiceTest {

    private final McpToolRepository mcpToolRepository = mock(McpToolRepository.class);

    private final McpToolServiceImpl mcpToolService = new McpToolServiceImpl(mcpToolRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetMcpToolsRefusesAConnectedUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of("external-user"));

        assertThatThrownBy(mcpToolService::getMcpTools)
            .isInstanceOf(AccessDeniedException.class);

        verify(mcpToolRepository, never()).findAll();
    }

    @Test
    void testGetMcpToolsListsEveryToolForAPlatformUser() {
        McpTool mcpTool = new McpTool("sendMessage", Map.of(), 3L);

        SecurityContextHolder.getContext()
            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated("admin", null, List.of()));

        when(mcpToolRepository.findAll()).thenReturn(List.of(mcpTool));

        assertThat(mcpToolService.getMcpTools()).containsExactly(mcpTool);
    }
}
