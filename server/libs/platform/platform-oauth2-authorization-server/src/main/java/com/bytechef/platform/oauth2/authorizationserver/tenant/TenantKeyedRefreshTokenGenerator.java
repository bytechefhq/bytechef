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

import com.bytechef.tenant.TenantContext;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

/**
 * @author Ivica Cardic
 */
public class TenantKeyedRefreshTokenGenerator implements OAuth2TokenGenerator<OAuth2RefreshToken> {

    private final OAuth2RefreshTokenGenerator delegate = new OAuth2RefreshTokenGenerator();

    @Override
    @Nullable
    public OAuth2RefreshToken generate(OAuth2TokenContext context) {
        OAuth2RefreshToken refreshToken = delegate.generate(context);

        if (refreshToken == null) {
            if (!isRotatingPublicClient(context)) {
                return null;
            }

            Instant issuedAt = Instant.now();

            RegisteredClient registeredClient = context.getRegisteredClient();

            TokenSettings tokenSettings = registeredClient.getTokenSettings();

            return new OAuth2RefreshToken(
                TenantKeyedTokenValues.generate(TenantContext.getCurrentTenantId()), issuedAt,
                issuedAt.plus(tokenSettings.getRefreshTokenTimeToLive()));
        }

        return new OAuth2RefreshToken(
            TenantKeyedTokenValues.generate(TenantContext.getCurrentTenantId()), refreshToken.getIssuedAt(),
            refreshToken.getExpiresAt());
    }

    private static boolean isRotatingPublicClient(OAuth2TokenContext context) {
        if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
            return false;
        }

        RegisteredClient registeredClient = context.getRegisteredClient();

        TokenSettings tokenSettings = registeredClient.getTokenSettings();

        return registeredClient.getAuthorizationGrantTypes()
            .contains(AuthorizationGrantType.REFRESH_TOKEN) &&
            registeredClient.getClientAuthenticationMethods()
                .contains(ClientAuthenticationMethod.NONE)
            &&
            !tokenSettings.isReuseRefreshTokens();
    }
}
