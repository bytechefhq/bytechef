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

package com.bytechef.platform.oauth2.authorizationserver.tenant;

import com.bytechef.platform.security.web.mcp.SelfIssuedTokenRevocationChecker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

/**
 * Treats an access token as revoked once its authorization no longer exists or its access token is invalidated.
 *
 * @author Ivica Cardic
 */
public class AuthorizationServerTokenRevocationChecker implements SelfIssuedTokenRevocationChecker {

    private final TenantRoutingOAuth2AuthorizationService authorizationService;

    @SuppressFBWarnings("EI2")
    public AuthorizationServerTokenRevocationChecker(TenantRoutingOAuth2AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public boolean isRevoked(String tokenValue, String tenantId) {
        OAuth2Authorization authorization = authorizationService.findByAccessToken(tokenValue, tenantId);

        if (authorization == null) {
            return true;
        }

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();

        return accessToken == null || accessToken.isInvalidated();
    }
}
