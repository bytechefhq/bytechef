
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
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class AuthorizationDTO {

    private final String description;
    private final String name;
    private final List<? extends PropertyDTO> properties;
    private final String title;
    private final AuthorizationType type;

    public AuthorizationDTO(Authorization authorization) {
        this.description = OptionalUtils.orElse(authorization.getDescription(), null);
        this.name = authorization.getName();
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(authorization.getProperties(), List.of()),
            valueProperty -> (ValuePropertyDTO<?>) PropertyDTO.toPropertyDTO(valueProperty));
        this.title = OptionalUtils.orElse(authorization.getTitle(), authorization.getName());
        this.type = authorization.getType();
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public String getName() {
        return name;
    }

    public List<? extends PropertyDTO> getProperties() {
        return properties;
    }

    public String getTitle() {
        return title;
    }

    public AuthorizationType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AuthorizationDTO that))
            return false;
        return Objects.equals(description, that.description) && Objects.equals(name, that.name)
            && Objects.equals(properties, that.properties) && Objects.equals(title, that.title) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, name, properties, title, type);
    }

    @Override
    public String toString() {
        return "AuthorizationDTO{" +
            "description='" + description + '\'' +
            ", name='" + name + '\'' +
            ", properties=" + properties +
            ", title='" + title + '\'' +
            ", type=" + type +
            '}';
    }
}
