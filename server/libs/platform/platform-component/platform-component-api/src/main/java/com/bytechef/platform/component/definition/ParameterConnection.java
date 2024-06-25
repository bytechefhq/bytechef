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

package com.bytechef.platform.component.definition;

import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public interface ParameterConnection {

    String getComponentName();

    int getVersion();

    Map<String, ?> getParameters();

    <T> T getParameter(String key);

    long getConnectionId();

    String getAuthorizationName();

    default boolean isAuthorizationOauth2AuthorizationCode() {
        return Objects.equals(AuthorizationType.OAUTH2_AUTHORIZATION_CODE.toLowerCase(), getAuthorizationName()) ||
            Objects.equals(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE.toLowerCase(), getAuthorizationName());
    }

    default boolean canCredentialsBeRefreshed() {
        return Objects.equals(getAuthorizationName(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE.toLowerCase()) ||
            Objects.equals(getAuthorizationName(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE.toLowerCase()) ||
            Objects.equals(CUSTOM.toLowerCase(), getAuthorizationName());
    }
}
