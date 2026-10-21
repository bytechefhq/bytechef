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

package com.bytechef.platform.oauth2.authorizationserver.authentication;

import static com.bytechef.platform.oauth2.authorizationserver.constant.OAuth2AuthorizationServerConstants.APP_REFRESH_TOKEN_REUSE_GRACE;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.oauth2.authorizationserver.registration.AppClients;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantKeyedTokenValues;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.transaction.support.TransactionOperations;

/**
 * @author Ivica Cardic
 */
public class AppRefreshTokenReuseAuthenticationProvider implements AuthenticationProvider {

    public static final String PREVIOUS_REFRESH_TOKEN_HASH_ATTRIBUTE = "bytechef.app.previousRefreshTokenHash";
    public static final String REFRESH_TOKEN_ROTATED_AT_ATTRIBUTE = "bytechef.app.refreshTokenRotatedAt";

    private static final String DPOP_PROOF_PARAMETER = "dpop_proof";
    private static final String SELECT_AUTHORIZATION_ID_FOR_UPDATE =
        "SELECT id FROM oauth2_authorization WHERE id = ? FOR UPDATE";

    private final OAuth2AuthorizationService authorizationService;
    private final Clock clock;
    private final AuthenticationProvider delegate;
    private final JdbcOperations jdbcOperations;
    private final TransactionOperations transactionOperations;

    @SuppressFBWarnings("EI2")
    public AppRefreshTokenReuseAuthenticationProvider(
        AuthenticationProvider delegate, OAuth2AuthorizationService authorizationService,
        JdbcOperations jdbcOperations, TransactionOperations transactionOperations, Clock clock) {

        this.authorizationService = authorizationService;
        this.clock = clock;
        this.delegate = delegate;
        this.jdbcOperations = jdbcOperations;
        this.transactionOperations = transactionOperations;
    }

    @Override
    @Nullable
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2RefreshTokenAuthenticationToken refreshTokenAuthentication =
            (OAuth2RefreshTokenAuthenticationToken) authentication;

        if (!(refreshTokenAuthentication.getPrincipal() instanceof OAuth2ClientAuthenticationToken clientPrincipal) ||
            !clientPrincipal.isAuthenticated()) {

            return delegate.authenticate(authentication);
        }

        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        if (registeredClient == null || !isRotatingAppClient(clientPrincipal, registeredClient)) {
            return delegate.authenticate(authentication);
        }

        Optional<String> authorizationId = TenantKeyedTokenValues.parseAuthorizationId(
            refreshTokenAuthentication.getRefreshToken());

        if (authorizationId.isEmpty()) {
            return delegate.authenticate(authentication);
        }

        return transactionOperations.execute(
            transactionStatus -> authenticateWithLock(
                refreshTokenAuthentication, clientPrincipal, registeredClient, authorizationId.get()));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2RefreshTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Nullable
    private Authentication authenticateWithLock(
        OAuth2RefreshTokenAuthenticationToken refreshTokenAuthentication,
        OAuth2ClientAuthenticationToken clientPrincipal, RegisteredClient registeredClient, String authorizationId) {

        List<String> lockedAuthorizationIds = jdbcOperations.queryForList(
            SELECT_AUTHORIZATION_ID_FOR_UPDATE, String.class, authorizationId);

        if (lockedAuthorizationIds.isEmpty()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }

        OAuth2Authorization authorization = authorizationService.findById(authorizationId);

        if (authorization == null || !registeredClient.getId()
            .equals(authorization.getRegisteredClientId())) {

            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }

        OAuth2Authorization.Token<OAuth2RefreshToken> currentRefreshToken = authorization.getRefreshToken();

        if (currentRefreshToken == null) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }

        String presentedRefreshTokenValue = refreshTokenAuthentication.getRefreshToken();
        OAuth2RefreshToken currentRefreshTokenValue = currentRefreshToken.getToken();

        if (isSameValue(currentRefreshTokenValue.getTokenValue(), presentedRefreshTokenValue)) {
            Authentication rotatedAuthentication = delegate.authenticate(refreshTokenAuthentication);

            recordRotation(authorizationId, presentedRefreshTokenValue);

            return rotatedAuthentication;
        }

        return reissueRotatedTokens(
            refreshTokenAuthentication, clientPrincipal, registeredClient, authorization, currentRefreshToken);
    }

    private OAuth2AccessTokenAuthenticationToken reissueRotatedTokens(
        OAuth2RefreshTokenAuthenticationToken refreshTokenAuthentication,
        OAuth2ClientAuthenticationToken clientPrincipal, RegisteredClient registeredClient,
        OAuth2Authorization authorization, OAuth2Authorization.Token<OAuth2RefreshToken> currentRefreshToken) {

        if (!isWithinReuseGrace(authorization, refreshTokenAuthentication.getRefreshToken()) ||
            hasDPoPProof(refreshTokenAuthentication) || !currentRefreshToken.isActive()) {

            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }

        OAuth2Authorization.Token<OAuth2AccessToken> currentAccessToken = authorization.getAccessToken();

        if (currentAccessToken == null || !currentAccessToken.isActive()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }

        OAuth2AccessToken accessToken = currentAccessToken.getToken();

        Set<String> requestedScopes = refreshTokenAuthentication.getScopes();

        if (!requestedScopes.isEmpty() && !requestedScopes.equals(accessToken.getScopes())) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }

        Map<String, Object> additionalParameters = Map.of();

        OAuth2Authorization.Token<OidcIdToken> idToken = authorization.getToken(OidcIdToken.class);

        if (idToken != null && idToken.isActive()) {
            OidcIdToken idTokenValue = idToken.getToken();

            additionalParameters = Map.of(OidcParameterNames.ID_TOKEN, idTokenValue.getTokenValue());
        }

        return new OAuth2AccessTokenAuthenticationToken(
            registeredClient, clientPrincipal, withRemainingLifetime(accessToken), currentRefreshToken.getToken(),
            additionalParameters);
    }

    private void recordRotation(String authorizationId, String previousRefreshTokenValue) {
        OAuth2Authorization rotatedAuthorization = authorizationService.findById(authorizationId);

        if (rotatedAuthorization == null) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }

        Instant rotatedAt = clock.instant();

        OAuth2Authorization recordedAuthorization = OAuth2Authorization.from(rotatedAuthorization)
            .attribute(PREVIOUS_REFRESH_TOKEN_HASH_ATTRIBUTE, hash(previousRefreshTokenValue))
            .attribute(REFRESH_TOKEN_ROTATED_AT_ATTRIBUTE, rotatedAt.toString())
            .build();

        authorizationService.save(recordedAuthorization);
    }

    private boolean isWithinReuseGrace(OAuth2Authorization authorization, String presentedRefreshTokenValue) {
        String previousRefreshTokenHash = authorization.getAttribute(PREVIOUS_REFRESH_TOKEN_HASH_ATTRIBUTE);
        String rotatedAtValue = authorization.getAttribute(REFRESH_TOKEN_ROTATED_AT_ATTRIBUTE);

        if (previousRefreshTokenHash == null || rotatedAtValue == null ||
            !isSameValue(previousRefreshTokenHash, hash(presentedRefreshTokenValue))) {

            return false;
        }

        Instant rotatedAt;

        try {
            rotatedAt = Instant.parse(rotatedAtValue);
        } catch (DateTimeParseException dateTimeParseException) {
            return false;
        }

        Instant now = clock.instant();

        return !now.isAfter(rotatedAt.plus(APP_REFRESH_TOKEN_REUSE_GRACE)) &&
            !now.isBefore(rotatedAt.minus(APP_REFRESH_TOKEN_REUSE_GRACE));
    }

    private OAuth2AccessToken withRemainingLifetime(OAuth2AccessToken accessToken) {
        Instant now = clock.instant();
        Instant issuedAt = accessToken.getIssuedAt();

        if (issuedAt == null || !now.isAfter(issuedAt)) {
            return accessToken;
        }

        return new OAuth2AccessToken(
            accessToken.getTokenType(), accessToken.getTokenValue(), now, accessToken.getExpiresAt(),
            accessToken.getScopes());
    }

    private static boolean hasDPoPProof(OAuth2RefreshTokenAuthenticationToken refreshTokenAuthentication) {
        Map<String, Object> additionalParameters = refreshTokenAuthentication.getAdditionalParameters();

        return additionalParameters.get(DPOP_PROOF_PARAMETER) != null;
    }

    private static String hash(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            return EncodingUtils.urlEncodeBase64ToString(
                messageDigest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }

    private static boolean isRotatingAppClient(
        OAuth2ClientAuthenticationToken clientPrincipal, RegisteredClient registeredClient) {

        TokenSettings tokenSettings = registeredClient.getTokenSettings();

        return ClientAuthenticationMethod.NONE.equals(clientPrincipal.getClientAuthenticationMethod()) &&
            AppClients.isAppClient(registeredClient) && !tokenSettings.isReuseRefreshTokens();
    }

    private static boolean isSameValue(String expectedValue, String presentedValue) {
        return MessageDigest.isEqual(
            expectedValue.getBytes(StandardCharsets.UTF_8), presentedValue.getBytes(StandardCharsets.UTF_8));
    }
}
