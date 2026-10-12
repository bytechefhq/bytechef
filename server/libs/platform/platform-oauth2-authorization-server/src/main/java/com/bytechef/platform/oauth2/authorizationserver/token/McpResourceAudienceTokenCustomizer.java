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

package com.bytechef.platform.oauth2.authorizationserver.token;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * @author Ivica Cardic
 */
public class McpResourceAudienceTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private static final String INVALID_TARGET = "invalid_target";
    private static final String RESOURCE = "resource";

    @Override
    public void customize(JwtEncodingContext context) {
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType()) ||
            context.getAuthorizedScopes()
                .contains(OidcScopes.OPENID)) {

            return;
        }

        String authorizedResource = getAuthorizedResource(context.getAuthorization());
        String requestedResource = getRequestedResource(context);

        if (requestedResource != null && authorizedResource != null && !requestedResource.equals(authorizedResource)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(INVALID_TARGET, "The resource differs from the authorized one", null));
        }

        String resource = requestedResource == null ? authorizedResource : requestedResource;

        if (resource != null) {
            context.getClaims()
                .claim(JwtClaimNames.AUD, resource);
        }
    }

    @Nullable
    private static String getAuthorizedResource(@Nullable OAuth2Authorization authorization) {
        if (authorization == null) {
            return null;
        }

        OAuth2AuthorizationRequest authorizationRequest = authorization.getAttribute(
            OAuth2AuthorizationRequest.class.getName());

        if (authorizationRequest == null) {
            return null;
        }

        return toResource(authorizationRequest.getAdditionalParameters());
    }

    @Nullable
    private static String getRequestedResource(JwtEncodingContext context) {
        if (!(context.getAuthorizationGrant() instanceof OAuth2AuthorizationGrantAuthenticationToken grant)) {
            return null;
        }

        return toResource(grant.getAdditionalParameters());
    }

    @Nullable
    private static String toResource(Map<String, Object> parameters) {
        Object resource = parameters.get(RESOURCE);

        if (resource instanceof String[] resources) {
            return resources.length == 0 ? null : resources[0];
        }

        return resource == null ? null : resource.toString();
    }
}
