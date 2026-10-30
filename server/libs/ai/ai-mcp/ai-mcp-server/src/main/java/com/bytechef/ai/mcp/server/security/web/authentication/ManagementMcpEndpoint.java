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

package com.bytechef.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.security.web.mcp.McpEndpoint;
import com.bytechef.tenant.domain.TenantKey;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The management MCP server endpoint, served at the tenant-less {@code /api/management/mcp}, where the tenant comes
 * from the access token, and at {@code /api/management/{secretKey}/mcp}.
 *
 * @author Ivica Cardic
 */
public class ManagementMcpEndpoint implements McpEndpoint {

    private static final String CANONICAL_PATH = "/api/management/mcp";
    private static final String NAME = "Management";
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("/api/management(?:/mcp|/[^/]+/mcp)/?$");
    private static final String SCOPE = "mcp:management";
    private static final Pattern SECRET_KEY_PATH_PATTERN = Pattern.compile("^/api/management/([^/]+)/mcp$");

    @Override
    public boolean matches(String path) {
        if (CANONICAL_PATH.equals(path)) {
            return true;
        }

        Matcher matcher = SECRET_KEY_PATH_PATTERN.matcher(path);

        return matcher.matches();
    }

    @Override
    public String getScope() {
        return SCOPE;
    }

    @Override
    public Optional<String> resolveTenantId(String path) {
        if (CANONICAL_PATH.equals(path)) {
            return Optional.empty();
        }

        Matcher matcher = SECRET_KEY_PATH_PATTERN.matcher(path);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Management MCP server secret key not found");
        }

        TenantKey tenantKey = TenantKey.parse(matcher.group(1));

        return Optional.of(tenantKey.getTenantId());
    }

    @Override
    public Optional<String> resolveResourceName(String resource) {
        Matcher matcher = RESOURCE_PATTERN.matcher(resource);

        if (!matcher.find()) {
            return Optional.empty();
        }

        return Optional.of(NAME);
    }
}
