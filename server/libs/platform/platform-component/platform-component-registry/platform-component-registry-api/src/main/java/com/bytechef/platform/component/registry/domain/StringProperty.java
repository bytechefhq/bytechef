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

package com.bytechef.platform.component.registry.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.Property;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public class StringProperty extends ValueProperty<String> implements OptionsDataSourceAware {

    private String languageId;
    private Integer maxLength;
    private Integer minLength;
    private List<Option> options;
    private OptionsDataSource optionsDataSource;

    private StringProperty() {
    }

    public StringProperty(Property.StringProperty stringProperty) {
        super(stringProperty);

        this.languageId = OptionalUtils.orElse(stringProperty.getLanguageId(), null);
        this.maxLength = OptionalUtils.orElse(stringProperty.getMaxLength(), null);
        this.minLength = OptionalUtils.orElse(stringProperty.getMinLength(), null);
        this.options = CollectionUtils.map(
            OptionalUtils.orElse(stringProperty.getOptions(), List.of()), Option::new);
        this.optionsDataSource = OptionalUtils.mapOrElse(
            stringProperty.getOptionsDataSource(), OptionsDataSource::new, null);
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

    public List<Option> getOptions() {
        return Collections.unmodifiableList(options);
    }

    @Nullable
    public OptionsDataSource getOptionsDataSource() {
        return optionsDataSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof StringProperty that))
            return false;
        if (!super.equals(o))
            return false;
        return Objects.equals(options, that.options) && Objects.equals(optionsDataSource, that.optionsDataSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), options, optionsDataSource);
    }

    @Override
    public String toString() {
        return "StringProperty{" +
            "maxLength=" + maxLength +
            ", minLength=" + minLength +
            ", options=" + options +
            ", optionsDataSource=" + optionsDataSource +
            ", controlType=" + controlType +
            ", defaultValue=" + defaultValue +
            ", exampleValue=" + exampleValue +
            "} ";
    }
}
