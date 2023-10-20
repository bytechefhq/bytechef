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

package com.bytechef.hermes.registry.util;

import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Property.ArrayProperty;
import com.bytechef.hermes.definition.Property.InputProperty;
import com.bytechef.hermes.definition.Property.ObjectProperty;
import com.bytechef.hermes.definition.Property.OutputProperty;
import com.bytechef.hermes.definition.Property.ValueProperty;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class PropertyUtils {

    public static <P extends InputProperty> void checkInputProperties(List<P> properties) {
        if (properties == null) {
            return;
        }

        for (InputProperty property : properties) {
            String name = property.getName();

            if (name == null || name.isEmpty()) {
                throw new IllegalStateException("Defined properties cannot to have empty names");
            }

            if (property instanceof ValueProperty<?> valueProperty) {
                checkAnyAsInputProperty(List.of(valueProperty));
            }
        }
    }

    public static <P extends OutputProperty<?>> void checkOutputProperty(P property) {
        if (property == null) {
            return;
        }

        String name = property.getName();

        if (name != null && !name.isEmpty()) {
            throw new IllegalStateException("Defined properties must have empty names");
        }
    }

    private static void checkAnyAsInputProperty(List<? extends ValueProperty<?>> properties) {
        for (Property property : properties) {
            if (property instanceof Property.AnyProperty) {
                throw new IllegalStateException("Any type is not allowed as input property");
            }

            if (property instanceof ArrayProperty) {
                List<? extends ValueProperty<?>> items = ((ArrayProperty) property)
                    .getItems()
                    .orElse(List.of());

                checkAnyAsInputProperty(items);
            }

            if (property instanceof ObjectProperty) {
                List<? extends ValueProperty<?>> objectProperties = ((ObjectProperty) property)
                    .getProperties()
                    .orElse(List.of());

                checkAnyAsInputProperty(objectProperties);

                List<? extends ValueProperty<?>> additionalProperties = ((ObjectProperty) property)
                    .getAdditionalProperties()
                    .orElse(List.of());

                checkAnyAsInputProperty(additionalProperties);
            }
        }
    }
}
