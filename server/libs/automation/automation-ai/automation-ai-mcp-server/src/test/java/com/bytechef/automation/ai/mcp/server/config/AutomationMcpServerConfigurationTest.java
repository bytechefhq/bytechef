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

package com.bytechef.automation.ai.mcp.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AutomationMcpServerConfigurationTest {

    @Test
    void testEnforcingServerKeepsOnlyAuthorizedComponents() {
        McpServer mcpServer = enforcingServer();

        McpComponent salesforce = component("salesforce", Set.of("ROLE_SALES"));
        McpComponent admin = component("admin", Set.of("ROLE_ADMIN"));

        List<McpComponent> authorized = AutomationMcpServerConfiguration.authorizedComponents(
            mcpServer, List.of(salesforce, admin), Set.of("ROLE_USER", "ROLE_SALES"));

        assertThat(authorized).containsExactly(salesforce);
    }

    @Test
    void testEnforcingServerDeniesComponentWithoutGrantingAuthority() {
        McpServer mcpServer = enforcingServer();

        McpComponent admin = component("admin", Set.of("ROLE_ADMIN"));

        List<McpComponent> authorized = AutomationMcpServerConfiguration.authorizedComponents(
            mcpServer, List.of(admin), Set.of("ROLE_USER"));

        assertThat(authorized).isEmpty();
    }

    @Test
    void testNonEnforcingServerExposesAllComponents() {
        McpServer mcpServer = new McpServer();

        McpComponent salesforce = component("salesforce", Set.of("ROLE_SALES"));
        McpComponent admin = component("admin", Set.of("ROLE_ADMIN"));

        List<McpComponent> authorized = AutomationMcpServerConfiguration.authorizedComponents(
            mcpServer, List.of(salesforce, admin), Set.of());

        assertThat(authorized).containsExactly(salesforce, admin);
    }

    private static McpServer enforcingServer() {
        McpServer mcpServer = new McpServer();

        mcpServer.setEnforceToolAuthorization(true);

        return mcpServer;
    }

    private static McpComponent component(String componentName, Set<String> requiredAuthorities) {
        McpComponent mcpComponent = new McpComponent();

        mcpComponent.setComponentName(componentName);
        mcpComponent.setRequiredAuthorities(requiredAuthorities);

        return mcpComponent;
    }
}
