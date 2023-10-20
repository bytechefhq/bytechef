
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

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.connection.domain.Connection;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ConnectionDefinitionService {

    void applyAuthorization(Connection connection, Authorization.AuthorizationContext authorizationContext);

    Optional<String> fetchBaseUri(Connection connection);

    Mono<ConnectionDefinition> getConnectionDefinitionMono(String componentName);

    Mono<List<ConnectionDefinition>> getConnectionDefinitionsMono();

    List<ConnectionDefinition> getConnectionDefinitions(String componentName);

    Mono<List<ConnectionDefinition>> getConnectionDefinitionsMono(String componentName);

}
