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

import com.bytechef.platform.security.web.authentication.TenantWebAuthenticationDetails;
import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * @author Ivica Cardic
 */
public class McpTenantJwtAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationProvider jwtAuthenticationProvider;
    private final McpResourceServerProperties mcpResourceServerProperties;

    @SuppressFBWarnings("EI2")
    public McpTenantJwtAuthenticationProvider(
        AuthenticationProvider jwtAuthenticationProvider, McpResourceServerProperties mcpResourceServerProperties) {

        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
        this.mcpResourceServerProperties = mcpResourceServerProperties;
    }

    @Override
    public @Nullable Authentication authenticate(Authentication authentication) {
        Authentication result = jwtAuthenticationProvider.authenticate(authentication);

        if (result instanceof JwtAuthenticationToken jwtAuthenticationToken &&
            jwtAuthenticationToken.getDetails() instanceof TenantWebAuthenticationDetails tenantWebAuthenticationDetails
            &&
            tenantWebAuthenticationDetails.getTenantId() == null) {

            String claimTenantId = resolveClaimTenantId(jwtAuthenticationToken.getToken());

            if (claimTenantId != null) {
                jwtAuthenticationToken.setDetails(tenantWebAuthenticationDetails.withTenantId(claimTenantId));
            }
        }

        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return jwtAuthenticationProvider.supports(authentication);
    }

    private @Nullable String resolveClaimTenantId(Jwt jwt) {
        Optional<Issuer> issuer = mcpResourceServerProperties.findIssuer(String.valueOf(jwt.getIssuer()));

        if (issuer.isEmpty() || !issuer.get()
            .isSelf()) {

            return null;
        }

        String tenantClaim = issuer.get()
            .getTenantClaim();

        if (StringUtils.isBlank(tenantClaim)) {
            return null;
        }

        String claimTenantId = jwt.getClaimAsString(tenantClaim);

        return StringUtils.isBlank(claimTenantId) ? null : claimTenantId;
    }
}
