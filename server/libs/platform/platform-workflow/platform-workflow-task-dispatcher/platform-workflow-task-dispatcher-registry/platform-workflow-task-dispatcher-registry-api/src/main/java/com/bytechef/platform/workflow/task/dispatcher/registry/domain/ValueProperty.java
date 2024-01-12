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

package com.bytechef.platform.workflow.task.dispatcher.registry.domain;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.definition.BaseProperty;
import com.bytechef.hermes.definition.BaseProperty.ControlType;
import java.util.Optional;

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

    public ValueProperty(BaseProperty.ValueProperty<V> valueProperty) {
        super(valueProperty);

        this.controlType = valueProperty.getControlType();
        this.defaultValue = OptionalUtils.orElse(valueProperty.getDefaultValue(), null);
        this.exampleValue = OptionalUtils.orElse(valueProperty.getExampleValue(), null);
        this.label = OptionalUtils.orElse(valueProperty.getLabel(), valueProperty.getName());
        this.placeholder = OptionalUtils.orElse(valueProperty.getPlaceholder(), null);
    }

    public ControlType getControlType() {
        return controlType;
    }

    public Optional<V> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public Optional<V> getExampleValue() {
        return Optional.ofNullable(exampleValue);
    }

    public Optional<String> getLabel() {
        return Optional.ofNullable(label);
    }

    public Optional<String> getPlaceholder() {
        return Optional.ofNullable(placeholder);
    }

    @Override
    public String toString() {
        return "ValueProperty{" +
            "controlType=" + controlType +
            ", defaultValue=" + defaultValue +
            ", exampleValue=" + exampleValue +
            ", label='" + label + '\'' +
            ", placeholder='" + placeholder + '\'' +
            "} " + super.toString();
    }
}
