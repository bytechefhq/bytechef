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

package com.bytechef.platform.domain;

import com.bytechef.commons.util.OptionalUtils;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public abstract class BaseProperty {

    protected boolean advancedOption;
    protected String description;
    protected String displayCondition;
    protected boolean expressionEnabled; // Defaults to true
    protected boolean hidden;
    protected boolean required;
    protected String name;

    protected BaseProperty() {
    }

    public BaseProperty(com.bytechef.definition.BaseProperty property) {
        this.advancedOption = OptionalUtils.orElse(property.getAdvancedOption(), false);
        this.description = OptionalUtils.orElse(property.getDescription(), null);
        this.displayCondition = OptionalUtils.orElse(property.getDisplayCondition(), null);
        this.expressionEnabled = OptionalUtils.orElse(property.getExpressionEnabled(), true);
        this.hidden = OptionalUtils.orElse(property.getHidden(), false);
        this.required = OptionalUtils.orElse(property.getRequired(), false);
        this.name = property.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof BaseProperty that)) {
            return false;
        }

        return advancedOption == that.advancedOption && expressionEnabled == that.expressionEnabled
            && hidden == that.hidden && required == that.required
            && Objects.equals(displayCondition, that.displayCondition)
            && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(advancedOption, displayCondition, expressionEnabled, hidden, required, name);
    }

    public boolean getAdvancedOption() {
        return advancedOption;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getDisplayCondition() {
        return displayCondition;
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

    public String getName() {
        return name;
    }
}
