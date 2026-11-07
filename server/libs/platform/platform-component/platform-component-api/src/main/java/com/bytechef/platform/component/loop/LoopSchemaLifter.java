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

public final class LoopSchemaLifter {

    private LoopSchemaLifter() {
    }

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

    public static ValueProperty<?> liftOutputSchema(ValueProperty<?> originalSchema) {
        if (originalSchema == null) {
            return null;
        }

        return array().items(originalSchema);
    }

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
