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

package com.bytechef.platform.util;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseArrayProperty;
import com.bytechef.definition.BaseProperty.BaseObjectProperty;
import java.util.List;
import org.jspecify.annotations.Nullable;

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
            throw new IllegalArgumentException("Defined property: %s must have empty name".formatted(name));
        }

        if (property instanceof BaseArrayProperty<? extends BaseProperty> arrayProperty) {
            checkArrayProperty(arrayProperty, true);
        }

        if (property instanceof BaseObjectProperty<?> objectProperty) {
            checkObjectProperty(objectProperty);
        }
    }

    /**
     * Traverses {@code properties} along the dotted {@code path}, descending into {@link BaseObjectProperty} children
     * and {@link BaseArrayProperty} item types as needed. Returns {@code null} if {@code path} is null/blank or any
     * segment cannot be resolved.
     *
     * <p>
     * Path conventions:
     * <ul>
     * <li>{@code parent.child} - descend into an object property's children.</li>
     * <li>{@code arrayProp[].child} - explicit descent into the first item type of an array property.</li>
     * <li>{@code arrayProp.child} - implicit descent when {@code arrayProp} is an array whose single item type is an
     * object.</li>
     * <li>{@code propName} - top-level match by {@link BaseProperty#getName()}.</li>
     * </ul>
     */
    public static @Nullable BaseProperty findPropertyByPath(
        List<? extends BaseProperty> properties, @Nullable String path) {

        if (path == null || path.isBlank()) {
            return null;
        }

        String[] segments = path.split("\\.");

        List<? extends BaseProperty> currentProperties = properties;
        BaseProperty current = null;

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];

            boolean arrayDescent = segment.endsWith("[]");

            String name = arrayDescent ? segment.substring(0, segment.length() - 2) : segment;

            current = null;

            for (BaseProperty property : currentProperties) {
                if (name.equals(property.getName())) {
                    current = property;

                    break;
                }
            }

            if (current == null) {
                return null;
            }

            if (i < segments.length - 1) {
                if (arrayDescent) {
                    // explicit `arrayProp[].child` - descend into the array's first item type; if it's an
                    // object, expose its children so the next segment can match a field name
                    if (!(current instanceof BaseArrayProperty<? extends BaseProperty> arrayProperty)) {
                        return null;
                    }

                    List<? extends BaseProperty> items = arrayProperty.getItems()
                        .orElse(List.of());

                    if (items.isEmpty()) {
                        return null;
                    }

                    BaseProperty firstItem = items.getFirst();

                    if (firstItem instanceof BaseObjectProperty<? extends BaseProperty> objectItem) {
                        currentProperties = objectItem.getProperties()
                            .orElse(List.of());
                    } else {
                        // first item is itself a leaf - no further children to traverse
                        return null;
                    }
                } else {
                    currentProperties = childPropertiesOf(current);

                    if (currentProperties == null) {
                        return null;
                    }
                }
            } else if (arrayDescent) {
                // path ends in `[]` - caller wants the array's item type, not the array property itself
                if (!(current instanceof BaseArrayProperty<? extends BaseProperty> arrayProperty)) {
                    return null;
                }

                List<? extends BaseProperty> items = arrayProperty.getItems()
                    .orElse(List.of());

                if (items.isEmpty()) {
                    return null;
                }

                current = items.getFirst();
            }
        }

        return current;
    }

    private static @Nullable List<? extends BaseProperty> childPropertiesOf(BaseProperty property) {
        if (property instanceof BaseObjectProperty<? extends BaseProperty> objectProperty) {
            return objectProperty.getProperties()
                .orElse(List.of());
        }

        if (property instanceof BaseArrayProperty<? extends BaseProperty> arrayProperty) {
            // implicit descent into array items - accept `arrayProp.child` when parent is an array of objects
            List<? extends BaseProperty> items = arrayProperty.getItems()
                .orElse(List.of());

            if (items.size() == 1
                && items.getFirst() instanceof BaseObjectProperty<? extends BaseProperty> objectItem) {
                return objectItem.getProperties()
                    .orElse(List.of());
            }
        }

        return null;
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
