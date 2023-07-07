
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.definition.registry.service;

import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Authorization.ApplyResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ConnectionDefinitionService {

    boolean connectionExists(String componentName, int connectionVersion);

    ApplyResponse executeAuthorizationApply(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName);

    AuthorizationCallbackResponse executeAuthorizationCallback(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName,
        String redirectUri);

    Optional<String> fetchBaseUri(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters);

    AuthorizationType getAuthorizationType(String authorizationName, String componentName, int connectionVersion);

    ConnectionDefinitionDTO getConnectionDefinition(String componentName, int componentVersion);

    List<ConnectionDefinitionDTO> getConnectionDefinitions(String componentName, Integer componentVersion);

    List<ConnectionDefinitionDTO> getConnectionDefinitions();

    OAuth2AuthorizationParametersDTO getOAuth2Parameters(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName);
}
