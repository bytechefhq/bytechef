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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Routes lookups of authorization codes and refresh tokens to the tenant encoded in the token value, and refuses to
 * re-create an authorization that was revoked while a code exchange or token refresh for it was in flight.
 *
 * @author Ivica Cardic
 */
public class TenantRoutingOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private static final String SELECT_AUTHORIZATION_ID_FOR_UPDATE =
        "SELECT id FROM oauth2_authorization WHERE id = ? FOR UPDATE";

    private static final Set<AuthorizationGrantType> REVOCABLE_GRANT_TYPES = Set.of(
        AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.DEVICE_CODE);

    private static final Set<String> TENANT_KEYED_TOKEN_TYPES = Set.of(
        OAuth2ParameterNames.CODE, OAuth2TokenType.REFRESH_TOKEN.getValue());

    private final OAuth2AuthorizationService delegate;
    private final JdbcOperations jdbcOperations;
    private final TransactionOperations transactionOperations;

    @SuppressFBWarnings("EI2")
    public TenantRoutingOAuth2AuthorizationService(
        OAuth2AuthorizationService delegate, JdbcOperations jdbcOperations,
        TransactionOperations transactionOperations) {

        this.delegate = delegate;
        this.jdbcOperations = jdbcOperations;
        this.transactionOperations = transactionOperations;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        if (!isPastCodeIssuance(authorization)) {
            delegate.save(authorization);

            return;
        }

        transactionOperations.executeWithoutResult(transactionStatus -> {
            List<String> authorizationIds = jdbcOperations.queryForList(
                SELECT_AUTHORIZATION_ID_FOR_UPDATE, String.class, authorization.getId());

            if (authorizationIds.isEmpty()) {
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
            }

            delegate.save(authorization);
        });
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        delegate.remove(authorization);
    }

    @Override
    @Nullable
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    @Override
    @Nullable
    public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        if (tokenType != null && !TENANT_KEYED_TOKEN_TYPES.contains(tokenType.getValue())) {
            return delegate.findByToken(token, tokenType);
        }

        Optional<String> tenantId = TenantKeyedTokenValues.parseTenantId(token);

        if (tenantId.isPresent()) {
            if (tokenType != null && !Objects.equals(tenantId.get(), TenantContext.getCurrentTenantId())) {
                return null;
            }

            return TenantScope.call(tenantId.get(), () -> delegate.findByToken(token, tokenType));
        }

        if (tokenType == null) {
            return delegate.findByToken(token, null);
        }

        return null;
    }

    @Nullable
    public OAuth2Authorization findByAccessToken(String tokenValue, String tenantId) {
        return TenantScope.call(tenantId, () -> delegate.findByToken(tokenValue, OAuth2TokenType.ACCESS_TOKEN));
    }

    private static boolean isPastCodeIssuance(OAuth2Authorization authorization) {
        if (!REVOCABLE_GRANT_TYPES.contains(authorization.getAuthorizationGrantType())) {
            return false;
        }

        if (authorization.getAccessToken() != null || authorization.getRefreshToken() != null) {
            return true;
        }

        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
            authorization.getToken(OAuth2AuthorizationCode.class);

        return authorizationCode != null && authorizationCode.isInvalidated();
    }
}
