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

package com.bytechef.hermes.component.registry.service;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.ApplyResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.registry.domain.ComponentConnection;
import com.bytechef.hermes.component.registry.domain.ConnectionDefinition;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface ConnectionDefinitionService {

    ApplyResponse executeAuthorizationApply(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context);

    AuthorizationCallbackResponse executeAuthorizationCallback(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context,
        @NonNull String redirectUri);

    boolean connectionExists(String componentName);

    Optional<String> executeBaseUri(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context);

    OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context);

    Authorization.AuthorizationType getAuthorizationType(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName);

    ConnectionDefinition getConnectionDefinition(@NonNull String componentName, int componentVersion);

    List<ConnectionDefinition> getConnectionDefinitions();

    List<ConnectionDefinition> getConnectionDefinitions(
        @NonNull String componentName, @NonNull Integer componentVersion);
}
