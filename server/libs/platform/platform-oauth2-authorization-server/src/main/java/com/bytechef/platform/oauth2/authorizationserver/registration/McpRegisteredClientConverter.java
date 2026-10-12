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

package com.bytechef.platform.oauth2.authorizationserver.registration;

import static com.bytechef.platform.oauth2.authorizationserver.constant.OAuth2AuthorizationServerConstants.MCP_ACCESS_TOKEN_TIME_TO_LIVE;
import static com.bytechef.platform.oauth2.authorizationserver.constant.OAuth2AuthorizationServerConstants.MCP_REFRESH_TOKEN_TIME_TO_LIVE;
import static com.bytechef.platform.oauth2.authorizationserver.constant.OAuth2AuthorizationServerConstants.MCP_SCOPES;

import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.server.authorization.OAuth2ClientRegistration;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.converter.OAuth2ClientRegistrationRegisteredClientConverter;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

/**
 * @author Ivica Cardic
 */
public class McpRegisteredClientConverter implements Converter<OAuth2ClientRegistration, RegisteredClient> {

    private final OAuth2ClientRegistrationRegisteredClientConverter delegate =
        new OAuth2ClientRegistrationRegisteredClientConverter();

    @Override
    public RegisteredClient convert(OAuth2ClientRegistration clientRegistration) {
        RegisteredClient registeredClient = delegate.convert(clientRegistration);

        TokenSettings tokenSettings = registeredClient.getTokenSettings();

        RegisteredClient.Builder builder = RegisteredClient.from(registeredClient)
            .tokenSettings(
                TokenSettings.withSettings(tokenSettings.getSettings())
                    .accessTokenTimeToLive(MCP_ACCESS_TOKEN_TIME_TO_LIVE)
                    .refreshTokenTimeToLive(MCP_REFRESH_TOKEN_TIME_TO_LIVE)
                    .reuseRefreshTokens(false)
                    .build());

        Set<String> scopes = registeredClient.getScopes();

        if (scopes.isEmpty()) {
            builder.scopes(registeredScopes -> registeredScopes.addAll(MCP_SCOPES));
        }

        return builder.build();
    }
}
