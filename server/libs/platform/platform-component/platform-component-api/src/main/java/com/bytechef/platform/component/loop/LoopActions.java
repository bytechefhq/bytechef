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

/**
 * Factory for synthesizing a looped sibling of an existing {@link ActionDefinition}.
 *
 * <p>
 * Usage from a component handler, mirroring the {@code action -&gt; tool} cluster-element pattern:
 *
 * <pre>{@code
 * import static com.bytechef.platform.component.loop.LoopActions.loopable;
 *
 * private static final ActionDefinition APPEND_ROW = action("appendRow")
 *     .properties(string("spreadsheetId"), string("range"), object("values"))
 *     .output(outputSchema(object().properties(string("updatedRange"))))
 *     .perform(GoogleSheetsAppendRowAction::perform);
 *
 * private static final ActionDefinition APPEND_ROW_LOOP = loopable(APPEND_ROW);
 *
 * // register both in the component:
 * component("googleSheets").actions(APPEND_ROW, APPEND_ROW_LOOP);
 * }</pre>
 *
 * <p>
 * The synthesized action:
 * <ul>
 * <li>Has the original action's name with {@value #LOOP_SUFFIX} appended (override via
 * {@link #loopable(ActionDefinition, String)}).
 * <li>Lifts every top-level scalar input property to an array; already-array properties pass through as broadcast
 * inputs reused per iteration.
 * <li>Wraps the output schema in an array.
 * <li>Has a {@link PerformFunction} that runs the wrapped action's perform once per array index (zip semantics — all
 * lifted-scalar inputs must have matching lengths) and returns a {@code List} of per-iteration outputs.
 * </ul>
 *
 * <p>
 * MVP constraints: only {@link PerformFunction} is supported (no streaming/webhook/resume variants). Failures abort the
 * loop and surface with the iteration index in the message.
 */
public final class LoopActions {

    public static final String LOOP_SUFFIX = "__loop";

    private LoopActions() {
    }

    /**
     * Returns a looped sibling of {@code wrapped} named {@code <wrapped.name>__loop}.
     */
    public static ActionDefinition loopable(ActionDefinition wrapped) {
        Objects.requireNonNull(wrapped, "wrapped");

        return loopable(wrapped, wrapped.getName() + LOOP_SUFFIX);
    }

    /**
     * Returns a looped sibling of {@code wrapped} with the given explicit name.
     */
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

    /**
     * Iterating perform: lifts the per-iteration scalar inputs out of array-valued {@link Parameters}, dispatches the
     * wrapped action's perform once per index, and aggregates outputs.
     */
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
