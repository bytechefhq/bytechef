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

import static com.bytechef.platform.oauth2.authorizationserver.constant.OAuth2AuthorizationServerConstants.MCP_SCOPES;

import java.util.List;
import java.util.function.Consumer;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2ClientRegistration;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationValidator;

/**
 * @author Ivica Cardic
 */
public class McpClientRegistrationValidator implements Consumer<OAuth2ClientRegistrationAuthenticationContext> {

    private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc7591#section-3.2.2";

    @Override
    public void accept(OAuth2ClientRegistrationAuthenticationContext authenticationContext) {
        OAuth2ClientRegistrationAuthenticationValidator.DEFAULT_REDIRECT_URI_VALIDATOR.accept(authenticationContext);
        OAuth2ClientRegistrationAuthenticationValidator.DEFAULT_JWK_SET_URI_VALIDATOR.accept(authenticationContext);

        OAuth2ClientRegistrationAuthenticationToken clientRegistrationAuthenticationToken =
            authenticationContext.getAuthentication();

        OAuth2ClientRegistration clientRegistration = clientRegistrationAuthenticationToken.getClientRegistration();

        List<String> scopes = clientRegistration.getScopes();

        if (scopes != null && !MCP_SCOPES.containsAll(scopes)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE, "Invalid Client Registration: scope", ERROR_URI));
        }
    }
}
