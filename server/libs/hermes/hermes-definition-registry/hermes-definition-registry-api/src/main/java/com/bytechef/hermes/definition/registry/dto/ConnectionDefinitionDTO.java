
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
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ConnectionDefinitionDTO extends ConnectionDefinitionBasicDTO {

    private final List<AuthorizationDTO> authorizations;
    private final List<? extends PropertyDTO> properties;

    public ConnectionDefinitionDTO(ConnectionDefinition connectionDefinition) {
        super(connectionDefinition);

        this.authorizations = toAuthorizationDTOs(
            OptionalUtils.orElse(connectionDefinition.getAuthorizations(), Collections.emptyList()));
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(connectionDefinition.getProperties(), Collections.emptyList()),
            valueProperty -> (ValuePropertyDTO<?>) PropertyDTO.toPropertyDTO(valueProperty));
    }

    public List<AuthorizationDTO> getAuthorizations() {
        return authorizations;
    }

    public List<? extends PropertyDTO> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConnectionDefinitionDTO that))
            return false;
        if (!super.equals(o))
            return false;
        return Objects.equals(authorizations, that.authorizations) && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), authorizations, properties);
    }

    @Override
    public String toString() {
        return "ConnectionDefinitionDTO{" +
            "authorizations=" + authorizations +
            ", properties=" + properties +
            ", authorizationRequired=" + authorizationRequired +
            ", componentDescription='" + componentDescription + '\'' +
            ", componentName='" + componentName + '\'' +
            ", componentTitle='" + componentTitle + '\'' +
            ", version=" + version +
            "} ";
    }

    private static List<AuthorizationDTO> toAuthorizationDTOs(List<? extends Authorization> authorizations) {
        return authorizations.stream()
            .map(AuthorizationDTO::new)
            .toList();
    }
}
