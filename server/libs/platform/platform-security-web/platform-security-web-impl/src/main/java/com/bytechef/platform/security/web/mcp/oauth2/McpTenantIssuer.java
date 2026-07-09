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

package com.bytechef.platform.security.web.mcp.oauth2;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A per-tenant external MCP issuer resolved from a tenant's {@code IdentityProvider} record: the issuer trusted for
 * this tenant only, whether its tokens must be audience-bound, which token claim carries the caller's groups, and how
 * those groups map to ByteChef authorities. Because trust is per-tenant, such an issuer's tokens need no
 * ByteChef-specific shaping (no scope, audience optional). A blank {@code authoritiesClaim} means the conventional OIDC
 * {@code groups} claim.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI", "EI2"
})
public record McpTenantIssuer(
    String issuerUri, boolean validateAudience, @Nullable String authoritiesClaim,
    Map<String, String> authorityMappings) {
}
