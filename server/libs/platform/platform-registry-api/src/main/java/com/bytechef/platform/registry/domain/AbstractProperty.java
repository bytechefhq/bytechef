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

package com.bytechef.platform.registry.domain;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.definition.BaseProperty;
import com.bytechef.hermes.definition.BaseProperty.Type;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractProperty {

    private boolean advancedOption;
    private String description;
    private String displayCondition;
    private boolean expressionEnabled; // Defaults to true
    private boolean hidden;
    private boolean required;
    private String name;
    private Type type;

    protected AbstractProperty() {
    }

    public AbstractProperty(BaseProperty property) {
        this.advancedOption = OptionalUtils.orElse(property.getAdvancedOption(), false);
        this.description = OptionalUtils.orElse(property.getDescription(), null);
        this.displayCondition = OptionalUtils.orElse(property.getDisplayCondition(), null);
        this.expressionEnabled = OptionalUtils.orElse(property.getExpressionEnabled(), true);
        this.hidden = OptionalUtils.orElse(property.getHidden(), false);
        this.required = OptionalUtils.orElse(property.getRequired(), false);
        this.name = property.getName();
        this.type = property.getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AbstractProperty that)) {
            return false;
        }

        return advancedOption == that.advancedOption && expressionEnabled == that.expressionEnabled
            && hidden == that.hidden && required == that.required
            && Objects.equals(displayCondition, that.displayCondition)
            && Objects.equals(name, that.name) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            advancedOption, displayCondition, expressionEnabled, hidden, required, name, type);
    }

    public boolean getAdvancedOption() {
        return advancedOption;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getDisplayCondition() {
        return Optional.ofNullable(displayCondition);
    }

    public boolean getExpressionEnabled() {
        return expressionEnabled;
    }

    public boolean getHidden() {
        return hidden;
    }

    public boolean getRequired() {
        return required;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Property" +
            "advancedOption=" + advancedOption +
            ", description='" + description + '\'' +
            ", displayCondition='" + displayCondition + '\'' +
            ", expressionEnabled=" + expressionEnabled +
            ", hidden=" + hidden +
            ", required=" + required +
            ", name='" + name + '\'' +
            ", type=" + type +
            '}';
    }
}
