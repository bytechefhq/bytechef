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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Derives a ByteChef {@link McpJwtIdentity} from a validated MCP access token. The tenant is the ByteChef-issued
 * {@code urlTenantId} (parsed from the endpoint's URL path secret), so the token is identity-only; a token that also
 * carries a tenant claim disagreeing with the URL tenant is rejected. Authorities are the token's scope authorities
 * plus the values of the issuer's configured authorities claim (if any) plus the issuer's static authorities, treating
 * the embedded authorization server and external IdPs uniformly.
 *
 * @author Ivica Cardic
 */
public class McpJwtIdentityMapper {

    /**
     * The default OIDC group-membership claim carried by a per-tenant external IdP token when the provider does not
     * configure one. Its values are mapped to ByteChef authorities through the identity provider's group-to-authority
     * map.
     */
    private static final String DEFAULT_GROUPS_CLAIM = "groups";

    public McpJwtIdentity map(
        Jwt jwt, Issuer issuer, Collection<GrantedAuthority> scopeAuthorities, String urlTenantId) {

        String tenantClaim = issuer.getTenantClaim();
        String claimTenantId = StringUtils.isNotBlank(tenantClaim) ? jwt.getClaimAsString(tenantClaim) : null;

        if (StringUtils.isNotBlank(claimTenantId) && !claimTenantId.equals(urlTenantId)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN),
                "Token tenant claim does not match the endpoint tenant");
        }

        List<GrantedAuthority> authorities = new ArrayList<>(scopeAuthorities);

        String authoritiesClaim = issuer.getAuthoritiesClaim();

        if (StringUtils.isNotBlank(authoritiesClaim)) {
            List<String> authorityNames = jwt.getClaimAsStringList(authoritiesClaim);

            if (authorityNames != null) {
                authorityNames.stream()
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
            }
        }

        issuer.getAuthorities()
            .stream()
            .map(SimpleGrantedAuthority::new)
            .forEach(authorities::add);

        return new McpJwtIdentity(urlTenantId, jwt.getSubject(), List.copyOf(authorities));
    }

    /**
     * Derives the identity for a token from a per-tenant external identity provider. The tenant is the URL-anchored
     * {@code urlTenantId}; authorities come from the token's {@code groups} claim mapped through the provider's
     * group-to-authority map. When the provider defines no mapping, the raw group values are used as authorities so a
     * provider can opt out of remapping; a group with no matching entry is dropped (deny by default).
     */
    public McpJwtIdentity mapTenantIssuer(Jwt jwt, McpTenantIssuer tenantIssuer, String urlTenantId) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        String groupsClaim = StringUtils.isNotBlank(tenantIssuer.authoritiesClaim())
            ? tenantIssuer.authoritiesClaim()
            : DEFAULT_GROUPS_CLAIM;

        List<String> groups = jwt.getClaimAsStringList(groupsClaim);
        Map<String, String> authorityMappings = tenantIssuer.authorityMappings();

        if (groups != null) {
            for (String group : groups) {
                String authority = authorityMappings.get(group);

                if (authority != null) {
                    authorities.add(new SimpleGrantedAuthority(authority));
                } else if (authorityMappings.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority(group));
                }
            }
        }

        return new McpJwtIdentity(urlTenantId, jwt.getSubject(), List.copyOf(authorities));
    }
}
