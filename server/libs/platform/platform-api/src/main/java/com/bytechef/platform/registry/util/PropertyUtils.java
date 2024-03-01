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

package com.bytechef.platform.registry.util;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseArrayProperty;
import com.bytechef.definition.BaseProperty.BaseObjectProperty;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class PropertyUtils {

    public static void checkInputProperties(List<? extends BaseProperty> properties) {
        for (BaseProperty property : properties) {
            String name = property.getName();

            if (name == null || name.isEmpty()) {
                throw new IllegalStateException("Defined properties cannot have empty names");
            }

            if (property instanceof BaseArrayProperty<? extends BaseProperty> arrayProperty) {
                checkArrayProperty(arrayProperty, false);
            }

            if (property instanceof BaseObjectProperty<? extends BaseProperty> objectProperty) {
                checkObjectProperty(objectProperty);
            }
        }
    }

    public static void checkOutputProperty(BaseProperty property) {
        if (property == null) {
            return;
        }

        String name = property.getName();

        if (name != null) {
            throw new IllegalArgumentException("Defined property=%s must have empty name".formatted(name));
        }

        if (property instanceof BaseArrayProperty<? extends BaseProperty> arrayProperty) {
            checkArrayProperty(arrayProperty, true);
        }

        if (property instanceof BaseObjectProperty<?> objectProperty) {
            checkObjectProperty(objectProperty);
        }
    }

    private static void checkArrayProperty(BaseArrayProperty<? extends BaseProperty> arrayProperty, boolean checkName) {
        List<? extends BaseProperty> itemProperties = OptionalUtils.orElse(arrayProperty.getItems(), List.of());

        for (BaseProperty itemProperty : itemProperties) {
            String name = itemProperty.getName();

            if (checkName && name != null) {
                throw new IllegalStateException(
                    "Defined array property=%s must have empty name".formatted(arrayProperty.getName()));
            }

            if (itemProperty instanceof BaseArrayProperty<?> curArrayProperty) {
                checkArrayProperty(curArrayProperty, checkName);
            }

            if (itemProperty instanceof BaseObjectProperty<?> objectProperty) {
                checkObjectProperty(objectProperty);
            }
        }
    }

    private static void checkObjectProperty(BaseObjectProperty<? extends BaseProperty> objectProperty) {
        List<? extends BaseProperty> objectProperties = OptionalUtils.orElse(
            objectProperty.getProperties(), List.of());

        for (BaseProperty property : objectProperties) {
            String name = property.getName();

            if (name == null || name.isEmpty()) {
                throw new IllegalStateException(
                    "Defined object property=%s cannot have properties with empty name".formatted(
                        objectProperty.getName()));
            }

            if (property instanceof BaseArrayProperty<?> arrayProperty) {
                checkArrayProperty(arrayProperty, false);
            }

            if (property instanceof BaseObjectProperty<?> curObjectProperty) {
                checkObjectProperty(curObjectProperty);
            }
        }

        List<? extends BaseProperty> objectAdditionalProperties = OptionalUtils.orElse(
            objectProperty.getAdditionalProperties(), List.of());

        for (BaseProperty itemProperty : objectAdditionalProperties) {
            String name = itemProperty.getName();

            if (name != null) {
                throw new IllegalStateException("Defined additional property=%s must have empty name".formatted(name));
            }

            if (itemProperty instanceof BaseArrayProperty<?> curArrayProperty) {
                checkArrayProperty(curArrayProperty, true);
            }

            if (itemProperty instanceof BaseObjectProperty<?> curObjectProperty &&
                !CollectionUtils.isEmpty(OptionalUtils.orElse(curObjectProperty.getProperties(), List.of()))) {

                checkObjectProperty(curObjectProperty);
            }
        }
    }
}
