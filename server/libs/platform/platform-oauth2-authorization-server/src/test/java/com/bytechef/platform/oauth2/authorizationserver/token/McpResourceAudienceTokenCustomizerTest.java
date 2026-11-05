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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

class McpResourceAudienceTokenCustomizerTest {

    private static final Authentication CLIENT_PRINCIPAL = new TestingAuthenticationToken("mcp-client", null);
    private static final String CLIENT_ID = "mcp-client";
    private static final String OTHER_RESOURCE = "https://other.example.com/mcp";
    private static final String RESOURCE = "https://bytechef.example.com/api/mcp";

    private final McpResourceAudienceTokenCustomizer mcpResourceAudienceTokenCustomizer =
        new McpResourceAudienceTokenCustomizer();

    @Test
    void testAuthorizationCodeGrantUsesMatchingRequestedResourceAsAudience() {
        JwtEncodingContext jwtEncodingContext = createContextBuilder(Set.of("mcp"))
            .authorization(createAuthorization(Map.of("resource", RESOURCE)))
            .authorizationGrant(createAuthorizationCodeGrant(Map.of("resource", RESOURCE)))
            .build();

        mcpResourceAudienceTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getAudience(jwtEncodingContext)).isEqualTo(RESOURCE);
    }

    @Test
    void testRefreshTokenGrantFallsBackToAuthorizedResource() {
        JwtEncodingContext jwtEncodingContext = createContextBuilder(Set.of("mcp"))
            .authorization(createAuthorization(Map.of("resource", RESOURCE)))
            .authorizationGrant(createRefreshTokenGrant(Map.of()))
            .build();

        mcpResourceAudienceTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getAudience(jwtEncodingContext)).isEqualTo(RESOURCE);
    }

    @Test
    void testRequestedResourceIsUsedWhenAuthorizationHasNoResource() {
        JwtEncodingContext jwtEncodingContext = createContextBuilder(Set.of("mcp"))
            .authorization(createAuthorization(Map.of()))
            .authorizationGrant(createAuthorizationCodeGrant(Map.of("resource", RESOURCE)))
            .build();

        mcpResourceAudienceTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getAudience(jwtEncodingContext)).isEqualTo(RESOURCE);
    }

    @Test
    void testClientCredentialsGrantWithoutAuthorizationUsesRequestedResource() {
        JwtEncodingContext jwtEncodingContext = createContextBuilder(Set.of("mcp"))
            .authorizationGrant(
                new OAuth2ClientCredentialsAuthenticationToken(
                    CLIENT_PRINCIPAL, Set.of("mcp"), Map.of("resource", RESOURCE)))
            .build();

        mcpResourceAudienceTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getAudience(jwtEncodingContext)).isEqualTo(RESOURCE);
    }

    @Test
    void testAuthorizationWithoutStoredAuthorizationRequestHasNoAuthorizedResource() {
        RegisteredClient registeredClient = createRegisteredClient();

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user")
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .build();

        JwtEncodingContext jwtEncodingContext = createContextBuilder(Set.of("mcp"))
            .authorization(authorization)
            .authorizationGrant(createRefreshTokenGrant(Map.of("resource", RESOURCE)))
            .build();

        mcpResourceAudienceTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getAudience(jwtEncodingContext)).isEqualTo(RESOURCE);
    }

    @Test
    void testNonGrantAuthenticationFallsBackToAuthorizedResource() {
        JwtEncodingContext jwtEncodingContext = createContextBuilder(Set.of("mcp"))
            .authorization(createAuthorization(Map.of("resource", RESOURCE)))
            .authorizationGrant(new TestingAuthenticationToken("someone", null))
            .build();

        mcpResourceAudienceTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getAudience(jwtEncodingContext)).isEqualTo(RESOURCE);
    }

    @Test
    void testMismatchedRequestedResourceIsRejectedWithInvalidTarget() {
        JwtEncodingContext jwtEncodingContext = createContextBuilder(Set.of("mcp"))
            .authorization(createAuthorization(Map.of("resource", RESOURCE)))
            .authorizationGrant(createRefreshTokenGrant(Map.of("resource", OTHER_RESOURCE)))
            .build();

        assertThatThrownBy(() -> mcpResourceAudienceTokenCustomizer.customize(jwtEncodingContext))
            .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception -> {
                OAuth2Error oAuth2Error = exception.getError();

                assertThat(oAuth2Error.getErrorCode()).isEqualTo("invalid_target");
                assertThat(oAuth2Error.getDescription()).isEqualTo("The resource differs from the authorized one");
            });

        assertThat(getAudience(jwtEncodingContext)).isEqualTo(CLIENT_ID);
    }

    @Test
    void testFirstValueOfMultiValuedResourceParameterIsUsed() {
        JwtEncodingContext jwtEncodingContext = createContextBuilder(Set.of("mcp"))
            .authorization(createAuthorization(Map.of("resource", new String[] {
                RESOURCE, OTHER_RESOURCE
            })))
            .authorizationGrant(
                createAuthorizationCodeGrant(Map.of("resource", new String[] {
                    RESOURCE, OTHER_RESOURCE
                })))
            .build();

        mcpResourceAudienceTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getAudience(jwtEncodingContext)).isEqualTo(RESOURCE);
    }

    @Test
    void testEmptyResourceArrayIsTreatedAsAbsent() {
        JwtEncodingContext jwtEncodingContext = createContextBuilder(Set.of("mcp"))
            .authorization(createAuthorization(Map.of("resource", RESOURCE)))
            .authorizationGrant(createAuthorizationCodeGrant(Map.of("resource", new String[0])))
            .build();

        mcpResourceAudienceTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getAudience(jwtEncodingContext)).isEqualTo(RESOURCE);
    }

    @Test
    void testAudienceIsLeftUntouchedWhenNoResourceIsKnown() {
        JwtEncodingContext jwtEncodingContext = createContextBuilder(Set.of("mcp"))
            .authorization(createAuthorization(Map.of()))
            .authorizationGrant(createAuthorizationCodeGrant(Map.of()))
            .build();

        mcpResourceAudienceTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getAudience(jwtEncodingContext)).isEqualTo(CLIENT_ID);
    }

    @Test
    void testNonAccessTokenIsNotCustomized() {
        JwtEncodingContext jwtEncodingContext = createContextBuilder(Set.of("mcp"))
            .tokenType(new OAuth2TokenType(OidcParameterNames.ID_TOKEN))
            .authorization(createAuthorization(Map.of("resource", RESOURCE)))
            .authorizationGrant(createAuthorizationCodeGrant(Map.of("resource", OTHER_RESOURCE)))
            .build();

        mcpResourceAudienceTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getAudience(jwtEncodingContext)).isEqualTo(CLIENT_ID);
    }

    @Test
    void testOpenIdScopedAccessTokenIsNotCustomized() {
        JwtEncodingContext jwtEncodingContext = createContextBuilder(Set.of(OidcScopes.OPENID, "mcp"))
            .authorization(createAuthorization(Map.of("resource", RESOURCE)))
            .authorizationGrant(createAuthorizationCodeGrant(Map.of("resource", OTHER_RESOURCE)))
            .build();

        mcpResourceAudienceTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getAudience(jwtEncodingContext)).isEqualTo(CLIENT_ID);
    }

    private static JwtEncodingContext.Builder createContextBuilder(Set<String> authorizedScopes) {
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
            .subject("user")
            .claim(JwtClaimNames.AUD, CLIENT_ID);

        return JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), claimsBuilder)
            .registeredClient(createRegisteredClient())
            .tokenType(OAuth2TokenType.ACCESS_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(authorizedScopes);
    }

    private static OAuth2Authorization createAuthorization(Map<String, Object> additionalParameters) {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://bytechef.example.com/oauth2/authorize")
            .clientId(CLIENT_ID)
            .additionalParameters(new HashMap<>(additionalParameters))
            .build();

        return OAuth2Authorization.withRegisteredClient(createRegisteredClient())
            .principalName("user")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .attribute(OAuth2AuthorizationRequest.class.getName(), authorizationRequest)
            .build();
    }

    private static OAuth2AuthorizationCodeAuthenticationToken createAuthorizationCodeGrant(
        Map<String, Object> additionalParameters) {

        return new OAuth2AuthorizationCodeAuthenticationToken(
            "code", CLIENT_PRINCIPAL, "https://client.example.com/callback", additionalParameters);
    }

    private static OAuth2RefreshTokenAuthenticationToken createRefreshTokenGrant(
        Map<String, Object> additionalParameters) {

        return new OAuth2RefreshTokenAuthenticationToken(
            "refresh-token", CLIENT_PRINCIPAL, Set.of(), additionalParameters);
    }

    private static RegisteredClient createRegisteredClient() {
        return RegisteredClient.withId("registered-client-id")
            .clientId(CLIENT_ID)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("https://client.example.com/callback")
            .build();
    }

    private static Object getAudience(JwtEncodingContext jwtEncodingContext) {
        JwtClaimsSet.Builder claimsBuilder = jwtEncodingContext.getClaims();

        JwtClaimsSet jwtClaimsSet = claimsBuilder.build();

        Map<String, Object> claims = jwtClaimsSet.getClaims();

        return claims.get(JwtClaimNames.AUD);
    }
}
