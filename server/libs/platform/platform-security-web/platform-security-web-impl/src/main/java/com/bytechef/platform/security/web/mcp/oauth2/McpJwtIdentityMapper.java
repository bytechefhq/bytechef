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

    private static final List<String> RESERVED_AUTHORITY_PREFIXES = List.of("ROLE_", "SCOPE_");

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
