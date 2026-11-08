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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Request-scoped holder for the external MCP issuers the current request's tenant trusts, keyed by issuer URI.
 * Populated by {@code McpTenantTrustResolutionFilter} once the tenant has been resolved from the endpoint's URL secret,
 * and read on the same request thread by both the JWT decoder factory (to admit a per-tenant issuer's token for
 * decoding) and {@link TenantAwareJwtAuthenticationFilter} (the authoritative per-tenant trust and policy check).
 * Thread-local, cleared at the end of the request - the JWT analogue of {@code TenantContext}.
 *
 * <p>
 * <strong>Base / CE seam:</strong> this class carries no federation logic - it is a neutral request-scoped map of
 * issuer URIs, empty unless something populates it. It is designed to move to CE with the base resource server (the
 * decoder factory and {@link TenantAwareJwtAuthenticationFilter} that read it), so the base can compile and run
 * standalone: on a CE-only build the map is always empty, so only statically configured issuers are admitted. The
 * <em>writer</em> - {@code McpTenantTrustResolutionFilter}, which resolves a tenant's identity providers - is the
 * enterprise-only half and stays EE.
 *
 * @author Ivica Cardic
 */
public final class McpTenantTrustContext {

    private static final ThreadLocal<Map<String, McpTenantIssuer>> ISSUERS_BY_URI = new ThreadLocal<>();

    private McpTenantTrustContext() {
    }

    public static void set(List<McpTenantIssuer> tenantIssuers) {
        ISSUERS_BY_URI.set(
            tenantIssuers.stream()
                .collect(Collectors.toMap(McpTenantIssuer::issuerUri, Function.identity(), (first, second) -> first)));
    }

    @Nullable
    public static McpTenantIssuer getIssuer(String issuerUri) {
        Map<String, McpTenantIssuer> issuersByUri = ISSUERS_BY_URI.get();

        return issuersByUri == null ? null : issuersByUri.get(issuerUri);
    }

    public static Set<String> getIssuerUris() {
        Map<String, McpTenantIssuer> issuersByUri = ISSUERS_BY_URI.get();

        return issuersByUri == null ? Set.of() : issuersByUri.keySet();
    }

    public static void clear() {
        ISSUERS_BY_URI.remove();
    }
}
