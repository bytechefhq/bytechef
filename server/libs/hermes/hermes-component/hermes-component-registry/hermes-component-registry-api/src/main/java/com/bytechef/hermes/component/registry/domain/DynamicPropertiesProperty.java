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

package com.bytechef.hermes.component.registry.domain;

import com.bytechef.commons.util.OptionalUtils;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class DynamicPropertiesProperty extends Property {

    private String header;
    private PropertiesDataSource propertiesDataSource;

    private DynamicPropertiesProperty() {
    }

    public DynamicPropertiesProperty(
        com.bytechef.hermes.component.definition.Property.DynamicPropertiesProperty property) {
        super(property);

        this.header = OptionalUtils.orElse(property.getHeader(), null);
        this.propertiesDataSource = new PropertiesDataSource(property.getDynamicPropertiesDataSource());
    }

    @Override
    public Object accept(PropertyVisitor propertyVisitor) {
        return propertyVisitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DynamicPropertiesProperty that))
            return false;
        return Objects.equals(propertiesDataSource, that.propertiesDataSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertiesDataSource);
    }

    public String getHeader() {
        return header;
    }

    public PropertiesDataSource getPropertiesDataSource() {
        return propertiesDataSource;
    }

    @Override
    public String toString() {
        return "DynamicPropertiesProperty{" +
            "header='" + header + '\'' +
            ", propertiesDataSource=" + propertiesDataSource +
            "} " + super.toString();
    }
}
