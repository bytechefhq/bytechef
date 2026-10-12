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
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

/**
 * Generates authorization codes whose value carries the current tenant.
 *
 * @author Ivica Cardic
 */
public class TenantKeyedAuthorizationCodeGenerator implements OAuth2TokenGenerator<OAuth2AuthorizationCode> {

    @Override
    @Nullable
    public OAuth2AuthorizationCode generate(OAuth2TokenContext context) {
        OAuth2TokenType tokenType = context.getTokenType();

        if (tokenType == null || !OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
            return null;
        }

        Instant issuedAt = Instant.now();
        RegisteredClient registeredClient = context.getRegisteredClient();

        TokenSettings tokenSettings = registeredClient.getTokenSettings();

        Instant expiresAt = issuedAt.plus(tokenSettings.getAuthorizationCodeTimeToLive());

        return new OAuth2AuthorizationCode(
            TenantKeyedTokenValues.generate(TenantContext.getCurrentTenantId()), issuedAt, expiresAt);
    }
}
