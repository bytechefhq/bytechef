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

package com.bytechef.automation.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.config.McpMethodSecurityTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = McpProjectIntTestConfiguration.class)
@Import({
    McpMethodSecurityTestConfiguration.class, PostgreSQLContainerConfiguration.class
})
@McpProjectIntTestConfigurationSharedMocks
@WithMockUser
class McpProjectWorkflowFacadeIntTest {

    @Autowired
    private McpProjectWorkflowFacade mcpProjectWorkflowFacade;

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @AfterEach
    void resetPermissionEvaluator() {
        reset(permissionEvaluator);
    }

    @Test
    void testDeleteRequiresEditor() {
        when(permissionEvaluator.hasPermission(any(), eq(8L), eq("McpProjectWorkflow"), eq("MCP_EDIT")))
            .thenReturn(false);

        assertThatThrownBy(() -> mcpProjectWorkflowFacade.deleteMcpProjectWorkflow(8L))
            .isInstanceOf(AccessDeniedException.class);
    }
}
