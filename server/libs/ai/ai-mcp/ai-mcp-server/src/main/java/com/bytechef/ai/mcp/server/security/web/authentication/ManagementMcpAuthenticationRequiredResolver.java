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

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.web.mcp.McpAuthenticationRequiredResolver;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reports whether the management MCP server requires authentication, reading the {@code authenticationRequired} entry
 * of the tenant's {@code mcp.server} platform property - the same property
 * {@code ManagementMcpServerApiKeyAuthenticationProvider} consults. Authentication is required unless the entry is
 * explicitly {@code false}: a property missing the entry reads as {@code true}, so a legacy row written before the
 * entry existed is challenged rather than served anonymously. An explicit {@code false} still opts a tenant out.
 *
 * <p>
 * This default matches the rest of the platform - {@code McpServer.authenticationRequired} and
 * {@code A2aServer.authenticationRequired} both initialise to {@code true}, and {@code ManagementMcpServerServiceImpl}
 * already writes {@code true} when it creates the property - so the management surface is no longer the outlier. Both
 * readers of the entry must express the same rule; a mismatch would let the filter challenge a request the provider
 * would have served anonymously, or the reverse.
 *
 * @author Ivica Cardic
 */
public class ManagementMcpAuthenticationRequiredResolver implements McpAuthenticationRequiredResolver {

    private static final Pattern SECRET_KEY_PATH_PATTERN = Pattern.compile("/api/management/(.+)/mcp");

    private final PropertyService propertyService;

    @SuppressFBWarnings("EI2")
    public ManagementMcpAuthenticationRequiredResolver(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    public Optional<Boolean> resolveAuthenticationRequired(HttpServletRequest request) {
        Matcher matcher = SECRET_KEY_PATH_PATTERN.matcher(request.getServletPath());

        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            TenantKey tenantKey = TenantKey.parse(matcher.group(1));

            Property property = TenantContext.callWithTenantId(
                tenantKey.getTenantId(),
                () -> propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null));

            return Optional.of(!Boolean.FALSE.equals(property.get("authenticationRequired")));
        } catch (Exception exception) {
            // An unparseable secret or unreadable property is challenged rather than served anonymously.

            return Optional.of(true);
        }
    }
}
