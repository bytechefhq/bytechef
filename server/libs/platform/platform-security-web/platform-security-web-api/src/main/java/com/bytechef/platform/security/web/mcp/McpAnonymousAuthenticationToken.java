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

package com.bytechef.platform.security.web.mcp;

import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Ivica Cardic
 */
public class McpAnonymousAuthenticationToken extends AbstractAuthenticationToken {

    private static final String AUTOMATION_PRINCIPAL_PREFIX = "mcp-anonymous:automation:";
    private static final String EMBEDDED_PRINCIPAL_PREFIX = "mcp-anonymous:embedded:";
    private static final String MANAGEMENT_PRINCIPAL = "mcp-anonymous:management";

    private final String principal;

    private McpAnonymousAuthenticationToken(String principal) {
        super(List.of());

        this.principal = principal;

        setAuthenticated(true);
    }

    public static McpAnonymousAuthenticationToken ofAutomationMcpServer(long mcpServerId) {
        return new McpAnonymousAuthenticationToken(AUTOMATION_PRINCIPAL_PREFIX + mcpServerId);
    }

    public static McpAnonymousAuthenticationToken ofEmbeddedMcpServer(long mcpServerId) {
        return new McpAnonymousAuthenticationToken(EMBEDDED_PRINCIPAL_PREFIX + mcpServerId);
    }

    public static McpAnonymousAuthenticationToken ofManagementMcpServer() {
        return new McpAnonymousAuthenticationToken(MANAGEMENT_PRINCIPAL);
    }

    public boolean isManagementMcpServer() {
        return MANAGEMENT_PRINCIPAL.equals(principal);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
