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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class LoopActions {

    public static final String LOOP_SUFFIX = "__loop";

    private LoopActions() {
    }

    public static ActionDefinition loopable(ActionDefinition wrapped) {
        Objects.requireNonNull(wrapped, "wrapped");

        return loopable(wrapped, wrapped.getName() + LOOP_SUFFIX);
    }

    public static ActionDefinition loopable(ActionDefinition wrapped, String name) {
        Objects.requireNonNull(wrapped, "wrapped");
        Objects.requireNonNull(name, "name");

        List<? extends Property> originalProperties = wrapped.getProperties();

        List<ValueProperty<?>> liftedProperties = LoopSchemaLifter.liftInputProperties(originalProperties);
        Set<String> scalarPropertyNames = LoopSchemaLifter.scalarPropertyNames(originalProperties);

        PerformFunction wrappedPerform = wrapped.getPerform()
            .filter(PerformFunction.class::isInstance)
            .map(PerformFunction.class::cast)
            .orElseThrow(() -> new IllegalArgumentException(
                "loopable() requires a PerformFunction on the wrapped action; '" + wrapped.getName()
                    + "' has none or a non-Perform variant (streaming/webhook are unsupported)."));

        ModifiableActionDefinition built = action(name)
            .title(wrapped.getTitle()
                .map(title -> title + " (Loop)")
                .orElse(name))
            .description(
                "Iterates over arrays of inputs, calling " + wrapped.getName()
                    + " once per index. Each scalar input becomes an array; each scalar output becomes an array.")
            .properties(liftedProperties.toArray(new ValueProperty<?>[0]))
            .perform(new LoopPerform(wrappedPerform, scalarPropertyNames, wrapped.getName()));

        applyOutput(built, wrapped.getOutputDefinition()
            .orElse(null));

        return built;
    }

    private static void applyOutput(ModifiableActionDefinition built, OutputDefinition wrappedOutput) {
        if (wrappedOutput == null) {
            return;
        }

        OutputResponse outputResponse = wrappedOutput.getOutputResponse()
            .orElse(null);

        if (outputResponse == null) {
            return;
        }

        if (!(outputResponse.getOutputSchema() instanceof ValueProperty<?> valueProperty)) {
            return;
        }

        ValueProperty<?> liftedSchema = LoopSchemaLifter.liftOutputSchema(valueProperty);

        if (outputResponse.getSampleOutput() != null) {
            built.output(outputSchema(liftedSchema), sampleOutput(List.of(outputResponse.getSampleOutput())));
        } else {
            built.output(outputSchema(liftedSchema));
        }
    }

    private static final class LoopPerform implements PerformFunction {

        private final PerformFunction wrappedPerform;
        private final Set<String> scalarPropertyNames;
        private final String wrappedActionName;

        LoopPerform(PerformFunction wrappedPerform, Set<String> scalarPropertyNames, String wrappedActionName) {
            this.wrappedPerform = wrappedPerform;
            this.scalarPropertyNames = scalarPropertyNames;
            this.wrappedActionName = wrappedActionName;
        }

        @Override
        public Object apply(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
            throws Exception {

            int iterations = computeIterationCount(inputParameters);

            if (iterations == 0) {
                return List.of();
            }

            List<Object> outputs = new ArrayList<>(iterations);

            for (int i = 0; i < iterations; i++) {
                Parameters perIteration = ParametersFactory.create(buildIterationMap(inputParameters, i));

                try {
                    outputs.add(wrappedPerform.apply(perIteration, connectionParameters, context));
                } catch (Exception e) {
                    throw new IllegalStateException(
                        "Loop iteration " + i + " of '" + wrappedActionName + "' failed: " + e.getMessage(), e);
                }
            }

            return outputs;
        }

        private int computeIterationCount(Parameters inputParameters) {
            int length = -1;
            String firstSeenKey = null;

            for (String key : scalarPropertyNames) {
                Object value = inputParameters.get(key);

                if (value == null) {
                    continue;
                }

                int valueLength = lengthOf(value);

                if (length < 0) {
                    length = valueLength;
                    firstSeenKey = key;
                } else if (length != valueLength) {
                    throw new IllegalArgumentException(
                        "Loop inputs must have matching array lengths: '" + firstSeenKey + "'=" + length
                            + ", '" + key + "'=" + valueLength);
                }
            }

            return length < 0 ? 0 : length;
        }

        private Map<String, Object> buildIterationMap(Parameters inputParameters, int index) {
            Map<String, Object> map = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : inputParameters.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (scalarPropertyNames.contains(key) && value != null) {
                    map.put(key, elementAt(value, index));
                } else {
                    map.put(key, value);
                }
            }

            return map;
        }

        private static int lengthOf(Object value) {
            if (value instanceof List<?> list) {
                return list.size();
            }

            if (value instanceof Collection<?> collection) {
                return collection.size();
            }

            if (value instanceof Object[] array) {
                return array.length;
            }

            throw new IllegalArgumentException(
                "Loop expected an array value but got " + value.getClass()
                    .getName() + ".");
        }

        private static Object elementAt(Object value, int index) {
            if (value instanceof List<?> list) {
                return list.get(index);
            }

            if (value instanceof Object[] array) {
                return array[index];
            }

            if (value instanceof Collection<?> collection) {
                int i = 0;

                for (Object element : collection) {
                    if (i++ == index) {
                        return element;
                    }
                }

                throw new IndexOutOfBoundsException(index);
            }

            throw new IllegalArgumentException(
                "Loop cannot index into " + value.getClass()
                    .getName() + ".");
        }
    }

}
