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

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extension point through which {@link TenantAwareJwtAuthenticationFilter} authenticates a Bearer JWT whose issuer is
 * <em>not</em> statically configured - i.e. a per-tenant external identity provider trusted only for the current
 * request's tenant. The base resource server (the statically configured self / external issuers) stands on its own;
 * when no implementation is present, a non-statically-configured issuer is rejected, which is exactly the
 * pre-federation behavior.
 *
 * <p>
 * This is the CE-facing seam of the external-IdP federation split: the interface is designed to move to CE so the base
 * resource server can compile without federation, while the implementation
 * ({@code TenantIdpFederatedIssuerAuthenticator} and everything it reads) stays enterprise-only.
 *
 * @author Ivica Cardic
 */
public interface McpFederatedIssuerAuthenticator {

    /**
     * Authenticates a token from a non-statically-configured issuer and returns the derived ByteChef identity, or empty
     * to reject the request (the filter responds 401). The {@code urlTenantId} is the authoritative tenant parsed from
     * the endpoint's URL path secret; the token is treated as identity-only.
     */
    Optional<McpJwtIdentity> authenticate(Jwt jwt, HttpServletRequest request, String urlTenantId);
}
