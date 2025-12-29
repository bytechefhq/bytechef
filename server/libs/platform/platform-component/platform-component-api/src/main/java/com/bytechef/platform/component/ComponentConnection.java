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

package com.bytechef.platform.component;

import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ComponentConnection(
    String componentName, int version, long connectionId, Map<String, ?> parameters,
    @Nullable AuthorizationType authorizationType) {

    public Map<String, ?> getConnectionParameters() {
        Map<String, Object> parameters = new HashMap<>(getParameters());

        if (authorizationType() != null) {
            parameters.put(Authorization.AUTHORIZATION_TYPE, authorizationType());
        }

        return parameters;
    }

    public String getComponentName() {
        return componentName;
    }

    public long getConnectionId() {
        return connectionId;
    }

    public int getVersion() {
        return version;
    }

    public Map<String, ?> getParameters() {
        return parameters;
    }

    public <T> @Nullable T getParameter(String key) {
        return MapUtils.get(parameters, key, new TypeReference<>() {});
    }

    public @Nullable AuthorizationType getAuthorizationType() {
        return authorizationType;
    }

    public boolean isAuthorizationOauth2AuthorizationCode() {
        return AuthorizationType.OAUTH2_AUTHORIZATION_CODE == authorizationType ||
            AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE == authorizationType;
    }

    public boolean canCredentialsBeRefreshed() {
        return authorizationType == AuthorizationType.OAUTH2_AUTHORIZATION_CODE ||
            authorizationType == AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE ||
            authorizationType == CUSTOM;
    }
}
