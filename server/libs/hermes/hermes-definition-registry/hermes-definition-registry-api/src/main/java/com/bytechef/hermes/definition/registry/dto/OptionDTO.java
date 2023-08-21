
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

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.definition.Option;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class OptionDTO {

    private final String description;
    private final String displayCondition;
    private final String name;
    private final Object value;

    public OptionDTO(Option<?> option) {
        this.description = OptionalUtils.orElse(option.getDescription(), null);
        this.displayCondition = option.getDisplayCondition();
        this.name = option.getName();
        this.value = option.getValue();
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public String getDisplayCondition() {
        return displayCondition;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OptionDTO optionDTO))
            return false;
        return Objects.equals(description, optionDTO.description)
            && Objects.equals(displayCondition, optionDTO.displayCondition) && Objects.equals(name, optionDTO.name)
            && Objects.equals(value, optionDTO.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, displayCondition, name, value);
    }

    @Override
    public String toString() {
        return "OptionDTO{" +
            "description='" + description + '\'' +
            ", displayCondition='" + displayCondition + '\'' +
            ", name='" + name + '\'' +
            ", value=" + value +
            '}';
    }
}
