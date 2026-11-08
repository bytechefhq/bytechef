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
import org.apache.commons.lang3.Strings;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * @author Ivica Cardic
 */
public class McpJwtIdentityMapper {

    private static final String DEFAULT_GROUPS_CLAIM = "groups";
    private static final List<String> RESERVED_AUTHORITY_PREFIXES = List.of("ROLE_", "SCOPE_");
    private static final String SCOPE_AUTHORITY_PREFIX = "SCOPE_";

    public McpJwtIdentity map(
        Jwt jwt, Issuer issuer, Collection<GrantedAuthority> scopeAuthorities, String urlTenantId) {

        String tenantClaim = issuer.getTenantClaim();

        if (StringUtils.isNotBlank(tenantClaim) && !isMatchingTenant(jwt.getClaimAsString(tenantClaim), urlTenantId)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN),
                "Token tenant claim is missing or does not match the endpoint tenant");
        }

        List<GrantedAuthority> authorities = new ArrayList<>(scopeAuthorities);

        String authoritiesClaim = issuer.getAuthoritiesClaim();

        if (StringUtils.isNotBlank(authoritiesClaim)) {
            List<String> authorityNames = jwt.getClaimAsStringList(authoritiesClaim);

            if (authorityNames != null) {
                authorityNames.stream()
                    .filter(McpJwtIdentityMapper::isGrantableClaimValue)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
            }
        }

        issuer.getAuthorities()
            .stream()
            .map(SimpleGrantedAuthority::new)
            .forEach(authorities::add);

        return new McpJwtIdentity(urlTenantId, getSubject(jwt), authorities);
    }

    public McpJwtIdentity mapTenantIssuer(Jwt jwt, McpTenantIssuer tenantIssuer, String urlTenantId) {
        String groupsClaim = StringUtils.isNotBlank(tenantIssuer.authoritiesClaim())
            ? tenantIssuer.authoritiesClaim() : DEFAULT_GROUPS_CLAIM;

        List<String> groups = jwt.getClaimAsStringList(groupsClaim);
        Map<String, String> authorityMappings = tenantIssuer.authorityMappings();

        List<GrantedAuthority> authorities = new ArrayList<>();

        if (groups != null) {
            for (String group : groups) {
                String authority = authorityMappings.get(group);

                if (authority != null && !Strings.CI.startsWith(authority, SCOPE_AUTHORITY_PREFIX)) {
                    authorities.add(new SimpleGrantedAuthority(authority));
                } else if (authority == null && authorityMappings.isEmpty() && isGrantableClaimValue(group)) {
                    authorities.add(new SimpleGrantedAuthority(group));
                }
            }
        }

        return new McpJwtIdentity(urlTenantId, getSubject(jwt), List.copyOf(authorities));
    }

    private static boolean isMatchingTenant(String claimTenantId, String urlTenantId) {
        return StringUtils.isNotBlank(claimTenantId) && claimTenantId.equals(urlTenantId);
    }

    private static boolean isGrantableClaimValue(String claimValue) {
        if (StringUtils.isBlank(claimValue)) {
            return false;
        }

        return RESERVED_AUTHORITY_PREFIXES.stream()
            .noneMatch(prefix -> Strings.CI.startsWith(claimValue, prefix));
    }

    private static String getSubject(Jwt jwt) {
        String subject = jwt.getSubject();

        if (StringUtils.isBlank(subject)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN), "Token does not carry a subject");
        }

        return subject;
    }
}
