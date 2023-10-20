
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

package com.bytechef.hermes.definition.registry.dto;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ConnectionDefinitionDTO(
    boolean authorizationRequired, List<AuthorizationDTO> authorizations, Optional<String> description, String name,
    List<? extends PropertyDTO> properties, String title, int version) {

    public ConnectionDefinitionDTO(ConnectionDefinition connectionDefinition, ComponentDefinition componentDefinition) {
        this(
            connectionDefinition.isAuthorizationRequired(),
            toAuthorizationDTOs(
                OptionalUtils.orElse(connectionDefinition.getAuthorizations(), Collections.emptyList())),
            componentDefinition.getDescription(), componentDefinition.getName(),
            CollectionUtils.map(
                OptionalUtils.orElse(connectionDefinition.getProperties(), Collections.emptyList()),
                PropertyDTO::toPropertyDTO),
            ComponentDefinitionDTO.getTitle(componentDefinition), connectionDefinition.getVersion());
    }

    private static List<AuthorizationDTO> toAuthorizationDTOs(List<? extends Authorization> authorizations) {
        return authorizations.stream()
            .map(authorization -> new AuthorizationDTO(
                authorization.getDescription(), authorization.getName(),
                CollectionUtils.map(
                    OptionalUtils.orElse(authorization.getProperties(), List.of()), PropertyDTO::toPropertyDTO),
                OptionalUtils.orElse(authorization.getTitle(), getDefaultTitle(authorization)),
                authorization.getType()))
            .toList();
    }

    private static String getDefaultTitle(Authorization authorization) {
        return authorization.getName();
    }
}
