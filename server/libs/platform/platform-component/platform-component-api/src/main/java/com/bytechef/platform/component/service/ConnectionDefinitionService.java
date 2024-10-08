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

package com.bytechef.platform.component.service;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.domain.ComponentConnection;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface ConnectionDefinitionService {

    Map<String, ?> executeAcquire(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName,
        @NonNull Map<String, ?> connectionParameters, @NonNull Context context);

    ApplyResponse executeAuthorizationApply(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName,
        @NonNull Map<String, ?> connectionParameters, @NonNull Context context);

    AuthorizationCallbackResponse executeAuthorizationCallback(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName,
        @NonNull Map<String, ?> connectionParameters, @NonNull Context context, @NonNull String redirectUri);

    Optional<String> executeBaseUri(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context);

    RefreshTokenResponse executeRefresh(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName,
        @NonNull Map<String, ?> connectionParameters, @NonNull Context context);

    OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName,
        @NonNull Map<String, ?> connectionParameters, @NonNull Context context);

    List<String> getAuthorizationDetectOn(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName);

    List<Object> getAuthorizationRefreshOn(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName);

    Authorization.AuthorizationType getAuthorizationType(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName);

    ConnectionDefinition getConnectionConnectionDefinition(@NonNull String componentName, int connectionVersion);

    ConnectionDefinition getConnectionDefinition(@NonNull String componentName, Integer componentVersion);

    List<ConnectionDefinition> getConnectionDefinitions();

    List<ConnectionDefinition> getConnectionDefinitions(@NonNull String componentName, Integer componentVersion);
}
