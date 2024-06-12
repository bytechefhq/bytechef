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

package com.bytechef.platform.component.registry.facade;

import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.platform.component.registry.domain.OAuth2AuthorizationParameters;
import java.util.Map;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface ConnectionDefinitionFacade {

    AuthorizationCallbackResponse executeAuthorizationCallback(
        @NonNull String componentName, @NonNull String authorizationName, @NonNull Map<String, ?> authorizationParms,
        @NonNull String redirectUri);

    OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        @NonNull String componentName, @NonNull String authorizationName, @NonNull Map<String, ?> authorizationParms);
}
