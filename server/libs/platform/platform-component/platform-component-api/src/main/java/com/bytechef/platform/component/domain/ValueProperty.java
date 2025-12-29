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

package com.bytechef.platform.component.domain;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.Property.ControlType;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public abstract class ValueProperty<V> extends Property {

    protected ControlType controlType;
    protected V defaultValue;
    protected V exampleValue;
    private String label;
    private String placeholder;

    protected ValueProperty() {
    }

    public ValueProperty(com.bytechef.component.definition.Property.ValueProperty<V> valueProperty) {
        super(valueProperty);

        this.controlType = valueProperty.getControlType();
        this.defaultValue = OptionalUtils.orElse(valueProperty.getDefaultValue(), null);
        this.exampleValue = OptionalUtils.orElse(valueProperty.getExampleValue(), null);
        this.label = OptionalUtils.orElse(valueProperty.getLabel(), StringUtils.capitalize(valueProperty.getName()));
        this.placeholder = OptionalUtils.orElse(valueProperty.getPlaceholder(), null);
    }

    public ControlType getControlType() {
        return controlType;
    }

    @Nullable
    public V getDefaultValue() {
        return defaultValue;
    }

    @Nullable
    public V getExampleValue() {
        return exampleValue;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }
}
