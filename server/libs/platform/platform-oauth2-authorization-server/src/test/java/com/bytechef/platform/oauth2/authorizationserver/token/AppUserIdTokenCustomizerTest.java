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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

class AppUserIdTokenCustomizerTest {

    private static final OAuth2TokenType ID_TOKEN_TOKEN_TYPE = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);
    private static final String LOGIN = "user";

    private final UserService userService = mock(UserService.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<UserService> userServiceProvider = mock(ObjectProvider.class);

    private final AppUserIdTokenCustomizer appUserIdTokenCustomizer =
        new AppUserIdTokenCustomizer(userServiceProvider);

    @BeforeEach
    void beforeEach() {
        when(userServiceProvider.getIfAvailable()).thenReturn(userService);
    }

    @Test
    void testIdTokenOfAppClientCarriesNameAndEmail() {
        User user = createUser("Ada", "Lovelace", "ada@example.com");

        when(userService.fetchUserByLogin(LOGIN)).thenReturn(Optional.of(user));

        JwtEncodingContext jwtEncodingContext = createContext(ID_TOKEN_TOKEN_TYPE, "app:use");

        appUserIdTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getClaims(jwtEncodingContext)).containsEntry("name", "Ada Lovelace")
            .containsEntry("email", "ada@example.com");
    }

    @Test
    void testBlankNamePartsAreLeftOut() {
        User user = createUser(null, " ", "");

        when(userService.fetchUserByLogin(LOGIN)).thenReturn(Optional.of(user));

        JwtEncodingContext jwtEncodingContext = createContext(ID_TOKEN_TOKEN_TYPE, "app:use");

        appUserIdTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getClaims(jwtEncodingContext)).doesNotContainKeys("name", "email");
    }

    @Test
    void testUnknownUserAddsNoClaims() {
        when(userService.fetchUserByLogin(LOGIN)).thenReturn(Optional.empty());

        JwtEncodingContext jwtEncodingContext = createContext(ID_TOKEN_TOKEN_TYPE, "app:use");

        appUserIdTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getClaims(jwtEncodingContext)).doesNotContainKeys("name", "email");
    }

    @Test
    void testIdTokenOfNonAppClientIsNotCustomized() {
        JwtEncodingContext jwtEncodingContext = createContext(ID_TOKEN_TOKEN_TYPE, "mcp:management");

        appUserIdTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getClaims(jwtEncodingContext)).doesNotContainKeys("name", "email");

        verifyNoInteractions(userService);
    }

    @Test
    void testAccessTokenOfAppClientIsNotCustomized() {
        JwtEncodingContext jwtEncodingContext = createContext(OAuth2TokenType.ACCESS_TOKEN, "app:use");

        appUserIdTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getClaims(jwtEncodingContext)).doesNotContainKeys("name", "email");

        verifyNoInteractions(userService);
    }

    @Test
    void testMissingUserServiceAddsNoClaims() {
        when(userServiceProvider.getIfAvailable()).thenReturn(null);

        JwtEncodingContext jwtEncodingContext = createContext(ID_TOKEN_TOKEN_TYPE, "app:use");

        appUserIdTokenCustomizer.customize(jwtEncodingContext);

        assertThat(getClaims(jwtEncodingContext)).doesNotContainKeys("name", "email");
    }

    private static JwtEncodingContext createContext(OAuth2TokenType tokenType, String clientScope) {
        RegisteredClient registeredClient = RegisteredClient.withId("registered-client-id")
            .clientId("client-id")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://client.example.com/callback")
            .scope(clientScope)
            .build();

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
            .subject(LOGIN);

        return JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), claimsBuilder)
            .registeredClient(registeredClient)
            .principal(new TestingAuthenticationToken(LOGIN, null))
            .tokenType(tokenType)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of(clientScope))
            .build();
    }

    private static User createUser(String firstName, String lastName, String email) {
        User user = mock(User.class);

        when(user.getEmail()).thenReturn(email);
        when(user.getFirstName()).thenReturn(firstName);
        when(user.getLastName()).thenReturn(lastName);

        return user;
    }

    private static Map<String, Object> getClaims(JwtEncodingContext jwtEncodingContext) {
        JwtClaimsSet.Builder claimsBuilder = jwtEncodingContext.getClaims();

        JwtClaimsSet jwtClaimsSet = claimsBuilder.build();

        return jwtClaimsSet.getClaims();
    }
}
