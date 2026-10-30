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

import com.bytechef.tenant.domain.TenantKey;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An {@link McpEndpoint} for platform tests, which cannot depend on the modules serving the real MCP servers.
 *
 * @author Ivica Cardic
 */
public final class TestMcpEndpoint implements McpEndpoint {

    private final Pattern pathPattern;
    private final String scope;
    private final Pattern secretKeyPathPattern;
    private final String tenantLessPath;

    private TestMcpEndpoint(String pathRegex, String secretKeyPathRegex, String tenantLessPath, String scope) {
        this.pathPattern = Pattern.compile(pathRegex);
        this.scope = scope;
        this.secretKeyPathPattern = Pattern.compile(secretKeyPathRegex);
        this.tenantLessPath = tenantLessPath;
    }

    /**
     * @return an endpoint served only at {@code /api/automation/{secretKey}/mcp}
     */
    public static TestMcpEndpoint automation() {
        return new TestMcpEndpoint(
            "^/api/automation/.+/mcp$", "^/api/automation/(.+)/mcp$", null, "mcp:automation");
    }

    /**
     * @return an endpoint served at {@code /api/management/{secretKey}/mcp} and the tenant-less
     *         {@code /api/management/mcp}
     */
    public static TestMcpEndpoint management() {
        return new TestMcpEndpoint(
            "^/api/management(/[^/]+)?/mcp$", "^/api/management/([^/]+)/mcp$", "/api/management/mcp",
            "mcp:management");
    }

    @Override
    public boolean matches(String path) {
        Matcher matcher = pathPattern.matcher(path);

        return matcher.matches();
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public Optional<String> resolveTenantId(String path) {
        if (path.equals(tenantLessPath)) {
            return Optional.empty();
        }

        Matcher matcher = secretKeyPathPattern.matcher(path);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("MCP server secret key not found");
        }

        TenantKey tenantKey = TenantKey.parse(matcher.group(1));

        return Optional.of(tenantKey.getTenantId());
    }

    @Override
    public Optional<String> resolveResourceName(String resource) {
        return Optional.empty();
    }
}
