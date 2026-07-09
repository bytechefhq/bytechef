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

import java.util.Collection;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

/**
 * Production {@link McpJwtDecoderFactory}: for a trusted issuer it builds a {@link JwtDecoder} from the issuer's
 * OpenID/OAuth2 metadata ({@code /.well-known}), which validates the token's signature (against the issuer's published
 * JWKS), its {@code iss} claim, and its expiry. Untrusted issuers yield {@code null} so {@link MultiIssuerJwtDecoder}
 * rejects them before any signature check.
 *
 * <p>
 * An issuer is trusted when it is a statically configured issuer <em>or</em> a per-tenant identity provider trusted for
 * the current request's tenant ({@link McpTenantTrustContext}). Trust is checked before the JWKS is ever fetched, so an
 * attacker-chosen issuer URL cannot trigger an outbound metadata request. {@link MultiIssuerJwtDecoder} caches the
 * built decoder per issuer across requests; that cache can outlive a single tenant's trust, but the built decoder only
 * proves signature/issuer/expiry - the authoritative per-tenant trust decision is re-made in
 * {@link TenantAwareJwtAuthenticationFilter}.
 *
 * <p>
 * <strong>Base / CE seam:</strong> the only federation touch point is the read of the neutral
 * {@link McpTenantTrustContext} thread-local - no {@code IdentityProvider} or resolver type is referenced. This factory
 * is designed to move to CE with the trust context; on a CE-only build that context is always empty, so this reduces to
 * "trust only statically configured issuers" - the pre-federation behavior. Populating the context (resolving a
 * tenant's identity providers) is the enterprise-only half.
 *
 * @author Ivica Cardic
 */
public class IssuerLocationMcpJwtDecoderFactory implements McpJwtDecoderFactory {

    private final Set<String> trustedIssuerUris;

    public IssuerLocationMcpJwtDecoderFactory(Collection<String> trustedIssuerUris) {
        this.trustedIssuerUris = Set.copyOf(trustedIssuerUris);
    }

    @Override
    @Nullable
    public JwtDecoder createJwtDecoder(String issuer) {
        if (!trustedIssuerUris.contains(issuer) && !McpTenantTrustContext.getIssuerUris()
            .contains(issuer)) {

            return null;
        }

        return JwtDecoders.fromIssuerLocation(issuer);
    }
}
