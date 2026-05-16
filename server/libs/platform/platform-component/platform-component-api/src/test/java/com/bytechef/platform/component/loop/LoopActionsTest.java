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

import static com.bytechef.commons.util.MapUtils.setObjectMapper;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class LoopActionsTest {

    @BeforeAll
    static void initMapUtilsObjectMapper() {
        setObjectMapper(new ObjectMapper());
    }

    @Test
    void testLoopableProducesArrayLiftedInputProperties() {
        ActionDefinition original = action("appendRow")
            .title("Append Row")
            .properties(
                string("spreadsheetId").required(true),
                integer("count"),
                array("rows"))
            .perform((PerformFunction) (input, conn, ctx) -> Map.of());

        ActionDefinition looped = LoopActions.loopable(original);

        assertEquals("appendRow__loop", looped.getName());

        List<? extends Property> liftedProperties = looped.getProperties()
            .orElseThrow();

        assertEquals(3, liftedProperties.size());
        assertEquals(Property.Type.ARRAY, liftedProperties.get(0)
            .getType());
        assertEquals(Property.Type.ARRAY, liftedProperties.get(1)
            .getType());
        assertEquals(Property.Type.ARRAY, liftedProperties.get(2)
            .getType(),
            "already-array properties should remain arrays (broadcast)");
        assertEquals(
            Optional.of(true),
            liftedProperties.get(0)
                .getRequired(),
            "required flag on lifted scalar must propagate to the array wrapper");
    }

    @Test
    void testLoopableLiftsOutputSchemaToArray() {
        ActionDefinition original = action("identity")
            .properties(string("value"))
            .output(outputSchema(string("echoed")))
            .perform((PerformFunction) (input, conn, ctx) -> Map.of("echoed", input.getString("value")));

        ActionDefinition looped = LoopActions.loopable(original);

        ValueProperty<?> outputSchemaProperty = (ValueProperty<?>) looped.getOutputDefinition()
            .orElseThrow()
            .getOutputResponse()
            .orElseThrow()
            .getOutputSchema();

        assertEquals(Property.Type.ARRAY, outputSchemaProperty.getType());
    }

    @Test
    void testLoopableIteratesScalarInputsAndAggregatesOutputs() throws Exception {
        ActionDefinition original = action("double")
            .properties(integer("value"))
            .perform((PerformFunction) (input, conn, ctx) -> input.getInteger("value") * 2);

        ActionDefinition looped = LoopActions.loopable(original);
        PerformFunction performLoop = (PerformFunction) looped.getPerform()
            .orElseThrow();

        Object result = performLoop.apply(
            ParametersFactory.create(Map.of("value", List.of(1, 2, 3))), ParametersFactory.create(Map.of()), null);

        assertEquals(List.of(2, 4, 6), result);
    }

    @Test
    void testLoopableEmptyInputsProduceEmptyOutput() throws Exception {
        ActionDefinition original = action("double")
            .properties(integer("value"))
            .perform((PerformFunction) (input, conn, ctx) -> input.getInteger("value") * 2);

        ActionDefinition looped = LoopActions.loopable(original);
        PerformFunction performLoop = (PerformFunction) looped.getPerform()
            .orElseThrow();

        Object result = performLoop.apply(
            ParametersFactory.create(Map.of("value", List.of())), ParametersFactory.create(Map.of()), null);

        assertEquals(List.of(), result);
    }

    @Test
    void testLoopableMismatchedArrayLengthsRejected() {
        ActionDefinition original = action("concat")
            .properties(string("a"), string("b"))
            .perform((PerformFunction) (input, conn, ctx) -> input.getString("a") + input.getString("b"));

        ActionDefinition looped = LoopActions.loopable(original);
        PerformFunction performLoop = (PerformFunction) looped.getPerform()
            .orElseThrow();

        Throwable thrown = assertThrows(
            Exception.class,
            () -> performLoop.apply(
                ParametersFactory.create(Map.of("a", List.of("x", "y"), "b", List.of("z"))),
                ParametersFactory.create(Map.of()),
                null));

        // The mismatch is wrapped in IllegalStateException-style iteration failure OR raised directly when validating
        // lengths up-front. Either form must clearly reference both keys.
        String message = thrown.getMessage();

        assertTrue(
            message != null && message.contains("a") && message.contains("b"),
            "mismatch message should name both inputs, was: " + message);
    }

    @Test
    void testLoopableBroadcastsArrayInputUnchanged() throws Exception {
        ActionDefinition original = action("pickFirst")
            .properties(integer("index"), array("options"))
            .perform((PerformFunction) (input, conn, ctx) -> {
                List<?> options = input.getList("options");

                return options.get(input.getInteger("index"));
            });

        ActionDefinition looped = LoopActions.loopable(original);
        PerformFunction performLoop = (PerformFunction) looped.getPerform()
            .orElseThrow();

        Object result = performLoop.apply(
            ParametersFactory.create(Map.of("index", List.of(0, 1, 2), "options", List.of("a", "b", "c"))),
            ParametersFactory.create(Map.of()),
            null);

        assertEquals(List.of("a", "b", "c"), result);
    }

    @Test
    void testLoopableIterationFailureSurfacesIndex() {
        ActionDefinition original = action("crashAtTwo")
            .properties(integer("value"))
            .perform((PerformFunction) (input, conn, ctx) -> {
                int value = input.getInteger("value");

                if (value == 2) {
                    throw new RuntimeException("boom-at-2");
                }

                return value;
            });

        ActionDefinition looped = LoopActions.loopable(original);
        PerformFunction performLoop = (PerformFunction) looped.getPerform()
            .orElseThrow();

        Exception thrown = assertThrows(
            Exception.class,
            () -> performLoop.apply(
                ParametersFactory.create(Map.of("value", List.of(1, 2, 3))),
                ParametersFactory.create(Map.of()),
                null));

        assertTrue(
            thrown.getMessage()
                .contains("iteration 1"),
            "failure message should include the failing iteration index, was: " + thrown.getMessage());
        assertEquals("boom-at-2", thrown.getCause()
            .getMessage());
    }

    @Test
    void testLoopableRejectsActionWithoutPerformFunction() {
        ActionDefinition original = action("schemaOnly")
            .properties(string("value"));

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> LoopActions.loopable(original));

        assertTrue(
            thrown.getMessage()
                .contains("PerformFunction"),
            thrown.getMessage());
    }

    @Test
    void testLoopableWithoutOutputDefinitionLeavesNoStaticSchema() {
        ActionDefinition original = action("noOutput")
            .properties(integer("value"))
            .perform((PerformFunction) (input, conn, ctx) -> input.getInteger("value"));

        ActionDefinition looped = LoopActions.loopable(original);

        assertNull(
            looped.getOutputDefinition()
                .orElse(null),
            "no output definition on the original means no static schema on the loop variant");
    }
}
