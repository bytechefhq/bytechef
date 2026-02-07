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

package com.bytechef.platform.component.aspect;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.EXPIRES_IN;
import static com.bytechef.component.definition.Authorization.REFRESH_TOKEN;

import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.util.RefreshCredentialsUtils;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Handler responsible for refreshing OAuth2 credentials and managing credential status. This handler is used by the
 * TokenRefreshAspect to perform the actual credential refresh operation.
 *
 * @author Ivica Cardic
 */
@Component
public class TokenRefreshHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenRefreshHandler.class);

    private static final String CACHE = TokenRefreshHandler.class.getName() + ".reentrantLock";

    private final CacheManager cacheManager;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public TokenRefreshHandler(
        CacheManager cacheManager, ConnectionDefinitionService connectionDefinitionService,
        ConnectionService connectionService) {

        this.cacheManager = cacheManager;
        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
    }

    /**
     * Checks if a token refresh should be attempted for the given exception.
     *
     * @param componentName       the name of the component
     * @param componentConnection the component connection containing authorization info
     * @param exception           the exception that occurred
     * @return true if refresh should be attempted, false otherwise
     */
    public boolean shouldRefresh(String componentName, ComponentConnection componentConnection, Exception exception) {
        if (componentConnection == null || componentConnection.authorizationType() == null ||
            exception.getMessage() == null) {

            return false;
        }

        if (!componentConnection.canCredentialsBeRefreshed()) {
            return false;
        }

        List<Object> refreshOn = connectionDefinitionService.getAuthorizationRefreshOn(
            componentName, componentConnection.version(),
            Objects.requireNonNull(componentConnection.authorizationType()));

        return RefreshCredentialsUtils.matches(refreshOn, exception);
    }

    /**
     * Refreshes the credentials for the given connection. For OAuth2 authorization code flows, this will refresh the
     * access token using the refresh token. For custom authorization types, this will acquire new credentials.
     *
     * @param componentConnection the component connection to refresh
     * @param context             the context for the refresh operation
     * @return a new ComponentConnection with the refreshed credentials
     * @throws RuntimeException if the refresh operation fails
     */
    public ComponentConnection refreshCredentials(ComponentConnection componentConnection, Context context) {
        Connection connection;
        Map<String, ?> parameters;

        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                    "Getting refreshed credentials with oAuth2AuthorizationCode set to {}",
                    componentConnection.isAuthorizationOauth2AuthorizationCode());
            }

            if (componentConnection.isAuthorizationOauth2AuthorizationCode()) {
                RefreshTokenResponse refreshTokenResponse = connectionDefinitionService.executeRefresh(
                    componentConnection.componentName(), componentConnection.version(),
                    Objects.requireNonNull(componentConnection.authorizationType()),
                    componentConnection.getParameters(), context);

                parameters = new HashMap<>() {
                    {
                        put(ACCESS_TOKEN, refreshTokenResponse.accessToken());

                        if (Objects.nonNull(refreshTokenResponse.refreshToken())) {
                            put(REFRESH_TOKEN, refreshTokenResponse.refreshToken());
                        }

                        if (Objects.nonNull(refreshTokenResponse.expiresIn())) {
                            put(EXPIRES_IN, refreshTokenResponse.expiresIn());
                        }
                    }
                };

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Refresh token execution executed");
                }
            } else {
                parameters = connectionDefinitionService.executeAcquire(
                    componentConnection.componentName(), componentConnection.version(),
                    Objects.requireNonNull(componentConnection.authorizationType()),
                    componentConnection.getParameters(), context);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Acquire executed");
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Credential refresh completed");
            }

            Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));

            ReentrantLock reentrantLock = Validate.notNull(
                cache.get(
                    TenantCacheKeyUtils.getKey(componentConnection.connectionId()), () -> new ReentrantLock(true)),
                "reentrantLock");

            reentrantLock.lock();

            try {
                connection = connectionService.updateConnectionParameters(
                    componentConnection.connectionId(), parameters);
            } finally {
                reentrantLock.unlock();
            }
        } catch (Exception exception) {
            markCredentialsInvalid(componentConnection.connectionId());

            LOGGER.error("Unable to complete refresh token procedure", exception);

            throw exception;
        }

        return new ComponentConnection(
            componentConnection.componentName(), connection.getConnectionVersion(), componentConnection.connectionId(),
            connection.getParameters(), connection.getAuthorizationType());
    }

    /**
     * Marks the credentials for the given connection as invalid.
     *
     * @param connectionId the ID of the connection to mark as invalid
     */
    public void markCredentialsInvalid(long connectionId) {
        connectionService.updateConnectionCredentialStatus(connectionId, Connection.CredentialStatus.INVALID);
    }
}
