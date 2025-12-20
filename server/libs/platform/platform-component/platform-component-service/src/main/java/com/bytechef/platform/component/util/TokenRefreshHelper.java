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

package com.bytechef.platform.component.util;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.EXPIRES_IN;
import static com.bytechef.component.definition.Authorization.REFRESH_TOKEN;

import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ErrorType;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * @author Igor Beslic
 */
@Component
public class TokenRefreshHelper {

    private static final String CACHE = TokenRefreshHelper.class.getName() + ".reentrantLock";
    private static final Logger logger = LoggerFactory.getLogger(TokenRefreshHelper.class);

    private final CacheManager cacheManager;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public TokenRefreshHelper(
        CacheManager cacheManager, ConnectionDefinitionService connectionDefinitionService,
        ConnectionService connectionService) {

        this.cacheManager = cacheManager;
        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
    }

    public <V, C extends Context> V executeSingleConnectionFunction(
        String componentName, int componentVersion, ComponentConnection componentConnection,
        C context, ErrorType errorType, BiFunction<ComponentConnection, C, V> performFunction,
        Function<ComponentConnection, C> contextFunction) {

        try {
            return performFunction.apply(componentConnection, context);
        } catch (Exception exception) {
            if (Objects.isNull(componentConnection) || Objects.isNull(componentConnection.authorizationType())
                || Objects.isNull(exception.getMessage())) {

                throw exception;
            }

            List<Object> refreshOn = connectionDefinitionService.getAuthorizationRefreshOn(
                componentName, componentConnection.version(),
                Objects.requireNonNull(componentConnection.authorizationType()));

            if (componentConnection.canCredentialsBeRefreshed() &&
                RefreshCredentialsUtils.matches(refreshOn, exception)) {

                componentConnection = getRefreshedCredentialsComponentConnection(componentConnection, context);

                return performFunction.apply(componentConnection, contextFunction.apply(componentConnection));
            }

            if (exception instanceof ProviderException) {
                throw new ConfigurationException(exception, errorType);
            }

            if (exception instanceof ConfigurationException ||
                exception instanceof ExecutionException) {

                throw exception;
            }

            throw exception;
        }
    }

    private ComponentConnection getRefreshedCredentialsComponentConnection(
        ComponentConnection componentConnection, Context context) {

        Connection connection;
        Map<String, ?> parameters;

        try {
            if (logger.isTraceEnabled()) {
                logger.trace(
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

                if (logger.isTraceEnabled()) {
                    logger.trace("Refresh token execution executed");
                }
            } else {
                parameters = connectionDefinitionService.executeAcquire(
                    componentConnection.componentName(), componentConnection.version(),
                    Objects.requireNonNull(componentConnection.authorizationType()),
                    componentConnection.getParameters(), context);

                if (logger.isTraceEnabled()) {
                    logger.trace("Acquire executed");
                }
            }

            if (logger.isTraceEnabled()) {
                logger.trace("Refresh token execution executed");
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
            connectionService.updateConnectionCredentialStatus(
                componentConnection.connectionId(), Connection.CredentialStatus.INVALID);

            logger.error("Unable to complete refresh token procedure", exception);

            throw exception;
        }

        return new ComponentConnection(
            componentConnection.componentName(), connection.getConnectionVersion(), componentConnection.connectionId(),
            connection.getParameters(), connection.getAuthorizationType());
    }
}
