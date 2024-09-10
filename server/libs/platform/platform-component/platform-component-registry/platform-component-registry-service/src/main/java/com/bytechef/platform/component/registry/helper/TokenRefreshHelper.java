/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.registry.helper;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.EXPIRES_IN;
import static com.bytechef.component.definition.Authorization.REFRESH_TOKEN;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Context;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.exception.ComponentConfigurationException;
import com.bytechef.platform.component.exception.ComponentExecutionException;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.service.ConnectionDefinitionService;
import com.bytechef.platform.component.registry.util.RefreshCredentialsUtils;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.exception.ErrorType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * @author Igor Beslic
 */
@Component
public class TokenRefreshHelper {

    private static final Cache<Long, ReentrantLock> REENTRANT_LOCK_CACHE =
        Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .maximumSize(100000)
            .build();

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public TokenRefreshHelper(ConnectionDefinitionService connectionDefinitionService,
        ConnectionService connectionService) {
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
            if (componentConnection == null || componentConnection.authorizationName() == null) {
                throw exception;
            }

            List<Object> refreshOn = connectionDefinitionService.getAuthorizationRefreshOn(
                componentName, componentConnection.version(),
                Objects.requireNonNull(componentConnection.authorizationName()));

            if (componentConnection.canCredentialsBeRefreshed() &&
                RefreshCredentialsUtils.matches(refreshOn, exception)) {

                componentConnection = getRefreshedCredentialsComponentConnection(componentConnection, context);

                return performFunction.apply(componentConnection, contextFunction.apply(componentConnection));
            }

            if (exception instanceof ProviderException) {
                throw new ComponentConfigurationException(exception, errorType);
            }

            if (exception instanceof ComponentConfigurationException ||
                exception instanceof ComponentExecutionException) {

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
            if (componentConnection.isAuthorizationOauth2AuthorizationCode()) {
                Authorization.RefreshTokenResponse refreshTokenResponse =
                    connectionDefinitionService.executeRefresh(
                        componentConnection.componentName(), componentConnection.version(),
                        Objects.requireNonNull(componentConnection.authorizationName()),
                        componentConnection.getParameters(), context);

                parameters = new HashMap<>() {
                    {
                        put(ACCESS_TOKEN, refreshTokenResponse.accessToken());

                        if (refreshTokenResponse.refreshToken() != null) {
                            put(REFRESH_TOKEN, refreshTokenResponse.refreshToken());
                        }

                        if (refreshTokenResponse.expiresIn() != null) {
                            put(EXPIRES_IN, refreshTokenResponse.expiresIn());
                        }
                    }
                };
            } else {
                parameters = connectionDefinitionService.executeAcquire(
                    componentConnection.componentName(), componentConnection.version(),
                    Objects.requireNonNull(componentConnection.authorizationName()),
                    componentConnection.getParameters(), context);
            }

            ReentrantLock reentrantLock = REENTRANT_LOCK_CACHE.get(
                componentConnection.connectionId(), (key) -> new ReentrantLock(true));

            reentrantLock.lock();

            try {
                connection = connectionService.updateConnectionParameters(
                    componentConnection.connectionId(), parameters);
            } finally {
                reentrantLock.unlock();
            }
        } catch (Exception e) {
            connectionService.updateConnectionCredentialStatus(
                componentConnection.connectionId(), Connection.CredentialStatus.INVALID);

            throw e;
        }

        return new ComponentConnection(
            componentConnection.componentName(), connection.getConnectionVersion(), componentConnection.connectionId(),
            connection.getParameters(), connection.getAuthorizationName());
    }
}
