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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.Property;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class StringProperty extends ValueProperty<String> implements OptionsDataSourceAware {

    private String languageId;
    private Integer maxLength;
    private Integer minLength;
    private String regex;
    private List<Option> options;
    private OptionsDataSource optionsDataSource;
    private Boolean optionsLoadedDynamically;

    private StringProperty() {
    }

    public StringProperty(Property.StringProperty stringProperty) {
        super(stringProperty);

        this.languageId = OptionalUtils.orElse(stringProperty.getLanguageId(), null);
        this.maxLength = OptionalUtils.orElse(stringProperty.getMaxLength(), null);
        this.minLength = OptionalUtils.orElse(stringProperty.getMinLength(), null);
        this.regex = OptionalUtils.orElse(stringProperty.getRegex(), null);
        this.options = CollectionUtils.map(
            OptionalUtils.orElse(stringProperty.getOptions(), List.of()), Option::new);
        this.optionsDataSource = OptionalUtils.mapOrElse(
            stringProperty.getOptionsDataSource(), OptionsDataSource::new, null);
        this.optionsLoadedDynamically = OptionalUtils.orElse(stringProperty.getOptionsLoadedDynamically(), false);
    }

    @Override
    public Object accept(PropertyVisitor propertyVisitor) {
        return propertyVisitor.visit(this);
    }

    @Nullable
    public String getLanguageId() {
        return languageId;
    }

    @Nullable
    public Integer getMaxLength() {
        return maxLength;
    }

    @Nullable
    public Integer getMinLength() {
        return minLength;
    }

    @Nullable
    public String getRegex() {
        return regex;
    }

    public List<Option> getOptions() {
        return Collections.unmodifiableList(options);
    }

    @Nullable
    public OptionsDataSource getOptionsDataSource() {
        return optionsDataSource;
    }

    @Nullable
    public Boolean getOptionsLoadedDynamically() {
        return optionsLoadedDynamically;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof StringProperty that))
            return false;
        if (!super.equals(o))
            return false;
        return Objects.equals(regex, that.regex) && Objects.equals(options, that.options)
            && Objects.equals(optionsDataSource, that.optionsDataSource)
            && Objects.equals(optionsLoadedDynamically, that.optionsLoadedDynamically);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), regex, options, optionsDataSource, optionsLoadedDynamically);
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
            ", optionsDataSource=" + optionsDataSource +
            ", options=" + options +
            ", optionsLoadedDynamically=" + optionsLoadedDynamically +
            ", minLength=" + minLength +
            ", maxLength=" + maxLength +
            ", regex='" + regex + '\'' +
            ", languageId='" + languageId + '\'' +
            "} " + super.toString();
    }
}
