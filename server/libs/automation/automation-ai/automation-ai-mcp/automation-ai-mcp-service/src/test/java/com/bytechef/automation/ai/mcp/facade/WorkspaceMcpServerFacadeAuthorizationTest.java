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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @author Ivica Cardic
 */
class WorkspaceMcpServerFacadeAuthorizationTest {

    @Test
    void testCreateWorkspaceMcpServerNeedsMcpCreateInTheServersEnvironment() throws Exception {
        Method method = WorkspaceMcpServerFacadeImpl.class.getMethod(
            "createWorkspaceMcpServer", String.class, PlatformType.class, Environment.class, Boolean.class,
            Boolean.class, Long.class);

        assertThat(method.getAnnotation(PreAuthorize.class))
            .extracting(PreAuthorize::value)
            .isEqualTo("hasWorkspaceScopeInEnvironment(#workspaceId, 'MCP_CREATE', #environment)");
    }

    @Test
    void testDeleteWorkspaceMcpServerNeedsMcpDelete() throws Exception {
        Method method = WorkspaceMcpServerFacadeImpl.class.getMethod("deleteWorkspaceMcpServer", Long.class);

        assertThat(method.getAnnotation(PreAuthorize.class))
            .extracting(PreAuthorize::value)
            .isEqualTo("hasPermission(#mcpServerId, 'McpServer', 'MCP_DELETE')");
    }
}
