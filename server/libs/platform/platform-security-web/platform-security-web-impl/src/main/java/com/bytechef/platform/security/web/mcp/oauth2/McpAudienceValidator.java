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

import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Decides whether a validated MCP access token is audience-bound to the endpoint being called (RFC 8707), so a token
 * minted for one endpoint cannot be replayed against another. Audience is <strong>required</strong> for every issuer -
 * since the tenant is anchored to the endpoint URL, an unbound token from a shared issuer would otherwise be replayable
 * across tenants.
 *
 * <ul>
 * <li>When the issuer has a configured fixed {@link Issuer#getAudience() audience} (an external IdP that emits a stable
 * audience identifier), the token's {@code aud} must contain that value.</li>
 * <li>Otherwise (the ByteChef embedded authorization server, or an external IdP that emits the requested resource as
 * its {@code aud} per RFC 8707), the token's {@code aud} must contain the current endpoint URL.</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public class McpAudienceValidator {

    public boolean isAudienceValid(Jwt jwt, Issuer issuer, String requestUrl) {
        List<String> audiences = jwt.getAudience();

        String expectedAudience = issuer.getAudience();

        if (StringUtils.isNotBlank(expectedAudience)) {
            return audiences != null && audiences.contains(expectedAudience);
        }

        return containsUrl(audiences, requestUrl);
    }

    /**
     * Audience check for a per-tenant identity provider's token. Because trust is scoped to the single tenant that
     * configured the provider, an unbound token cannot be replayed across tenants, so audience binding is optional -
     * enforced only when the provider opts in via {@code validateMcpAudience}, in which case the token's {@code aud}
     * must contain the endpoint URL (RFC 8707).
     */
    public boolean isTenantAudienceValid(Jwt jwt, McpTenantIssuer tenantIssuer, String requestUrl) {
        if (!tenantIssuer.validateAudience()) {
            return true;
        }

        return containsUrl(jwt.getAudience(), requestUrl);
    }

    private static boolean containsUrl(List<String> audiences, String requestUrl) {
        if (audiences == null) {
            return false;
        }

        String normalizedRequestUrl = stripTrailingSlash(requestUrl);

        return audiences.stream()
            .map(McpAudienceValidator::stripTrailingSlash)
            .anyMatch(normalizedRequestUrl::equals);
    }

    private static String stripTrailingSlash(String value) {
        if (value != null && value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }
}
