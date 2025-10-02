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

package com.bytechef.platform.workflow.task.dispatcher.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class StringProperty extends ValueProperty<String> {

    private Integer maxLength;
    private Integer minLength;
    private String regex;
    private List<Option> options;
    private Boolean optionsLoadedDynamically;

    private StringProperty() {
    }

    public StringProperty(
        com.bytechef.platform.workflow.task.dispatcher.definition.Property.StringProperty stringProperty) {

        super(stringProperty);

        this.maxLength = OptionalUtils.orElse(stringProperty.getMaxLength(), null);
        this.minLength = OptionalUtils.orElse(stringProperty.getMinLength(), null);
        this.regex = OptionalUtils.orElse(stringProperty.getRegex(), null);
        this.options = CollectionUtils.map(OptionalUtils.orElse(stringProperty.getOptions(), List.of()), Option::new);
        this.optionsLoadedDynamically = OptionalUtils.orElse(stringProperty.getOptionsLoadedDynamically(), false);
    }

    @Override
    public Object accept(PropertyVisitor propertyVisitor) {
        return propertyVisitor.visit(this);
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public String getRegex() {
        return regex;
    }

    public List<Option> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public Boolean getOptionsLoadedDynamically() {
        return optionsLoadedDynamically;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof StringProperty that)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        return Objects.equals(regex, that.regex) &&
            Objects.equals(options, that.options) &&
            Objects.equals(optionsLoadedDynamically, that.optionsLoadedDynamically);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), regex, options, optionsLoadedDynamically);
    }

    @Override
    public String toString() {
        return "StringProperty{" +
            "name='" + name + '\'' +
            ", type=" + type +
            ", controlType=" + controlType +
            ", required=" + required +
            ", hidden=" + hidden +
            ", expressionEnabled=" + expressionEnabled +
            ", displayCondition='" + displayCondition + '\'' +
            ", description='" + description + '\'' +
            ", advancedOption=" + advancedOption +
            ", exampleValue=" + exampleValue +
            ", defaultValue=" + defaultValue +
            ", options=" + options +
            ", optionsLoadedDynamically=" + optionsLoadedDynamically +
            ", minLength=" + minLength +
            ", maxLength=" + maxLength +
            ", regex='" + regex + '\'' +
            "} " + super.toString();
    }
}
