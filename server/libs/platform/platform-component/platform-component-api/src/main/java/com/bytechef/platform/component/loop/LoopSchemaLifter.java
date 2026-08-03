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

package com.bytechef.platform.component.loop;

import static com.bytechef.component.definition.ComponentDsl.array;

import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ValueProperty;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lifts an opted-in action's input/output schema into the looped-variant shape: every top-level scalar property is
 * wrapped in an array; already-array properties pass through as broadcast inputs; the output schema is wrapped in an
 * array.
 *
 * <p>
 * Lifting is intentionally shallow (top-level only). Nested object properties keep their inner shape unchanged — the
 * analogy is "run this action N times, each call gets one element of every lifted-scalar argument."
 */
public final class LoopSchemaLifter {

    private LoopSchemaLifter() {
    }

    /**
     * Returns a new property list where every top-level scalar property is wrapped in a {@link ModifiableArrayProperty}
     * carrying the original's label/description/required flags. Already-array properties pass through unchanged so
     * their value can be broadcast to every iteration.
     */
    public static List<ValueProperty<?>> liftInputProperties(List<? extends Property> original) {
        List<ValueProperty<?>> lifted = new ArrayList<>(original.size());

        for (Property property : original) {
            if (!(property instanceof ValueProperty<?> valueProperty)) {
                continue;
            }

            if (property.getType() == Property.Type.ARRAY) {
                lifted.add(valueProperty);
            } else {
                lifted.add(wrapInArray(valueProperty));
            }
        }

        return lifted;
    }

    /**
     * Returns the original {@code outputSchema} wrapped in an array property, or {@code null} when no static schema is
     * present (e.g. dynamic output function). The looped variant's output will still be a list at runtime — only the
     * static schema synthesis is skipped.
     */
    public static ValueProperty<?> liftOutputSchema(ValueProperty<?> originalSchema) {
        if (originalSchema == null) {
            return null;
        }

        return array().items(originalSchema);
    }

    /**
     * Returns the set of top-level scalar-property names that were lifted to arrays. Properties that were already
     * arrays in the original schema are NOT in this set — their values are broadcast across iterations.
     */
    public static Set<String> scalarPropertyNames(List<? extends Property> original) {
        Set<String> scalars = new LinkedHashSet<>();

        for (Property property : original) {
            if (property instanceof ValueProperty<?> && property.getType() != Property.Type.ARRAY) {
                scalars.add(property.getName());
            }
        }

        return scalars;
    }

    private static ModifiableArrayProperty wrapInArray(ValueProperty<?> property) {
        ModifiableArrayProperty wrapper = array(property.getName());

        property.getLabel()
            .ifPresent(wrapper::label);
        property.getDescription()
            .ifPresent(wrapper::description);

        wrapper.required(property.getRequired());

        return wrapper.items(property);
    }
}
