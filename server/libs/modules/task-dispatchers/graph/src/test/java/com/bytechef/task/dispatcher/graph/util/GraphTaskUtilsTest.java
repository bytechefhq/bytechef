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

package com.bytechef.task.dispatcher.graph.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class GraphTaskUtilsTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    @Test
    public void testResolveTransitionTakesFirstTruthyConditionalInDeclaredOrder() {
        List<Map<String, ?>> transitions = List.of(
            transition("a", "b", "=score > 10"),
            transition("a", "c", "=score > 5"),
            transition("a", "d", null));

        Optional<String> target = GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of("score", 7));

        assertEquals(Optional.of("c"), target);
    }

    @Test
    public void testResolveTransitionFallsBackToUnconditionalWhenNoConditionMatches() {
        List<Map<String, ?>> transitions = List.of(
            transition("a", "d", null),
            transition("a", "b", "=score > 10"));

        Optional<String> target = GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of("score", 1));

        // the unconditional edge is the fallback even though it is declared first
        assertEquals(Optional.of("d"), target);
    }

    /**
     * The property editor writes a cleared formula field as the bare prefix rather than as an empty string. That is not
     * blank, so it counted as a condition, evaluated falsy for having no expression, and was then skipped by the
     * unconditional pass too -- the node stranded and the graph ended with nothing said.
     */
    @Test
    public void testResolveTransitionTreatsBareFormulaPrefixAsUnconditional() {
        List<Map<String, ?>> transitions = List.of(transition("a", "b", "="));

        Optional<String> target = GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of());

        assertEquals(Optional.of("b"), target);
    }

    @Test
    public void testResolveTransitionTreatsBlankConditionAsUnconditional() {
        List<Map<String, ?>> transitions = List.of(transition("a", "b", "   "));

        Optional<String> target = GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of());

        assertEquals(Optional.of("b"), target);
    }

    /**
     * A condition that does not evaluate to a boolean is an error, not a false. Reading it as false made a broken
     * expression indistinguishable from one that simply did not match, and the run ended silently.
     */
    @Test
    public void testResolveTransitionFailsOnAConditionThatIsNotBooleanValued() {
        List<Map<String, ?>> transitions = List.of(transition("a", "b", "=$"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of()));

        assertTrue(exception.getMessage()
            .contains("a"));
    }

    @Test
    public void testResolveTransitionPrefersConditionalOverEarlierUnconditional() {
        List<Map<String, ?>> transitions = List.of(
            transition("a", "d", null),
            transition("a", "b", "=score > 10"));

        Optional<String> target = GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of("score", 11));

        assertEquals(Optional.of("b"), target);
    }

    @Test
    public void testResolveTransitionIsTerminalWhenNothingMatches() {
        List<Map<String, ?>> transitions = List.of(transition("a", "b", "=score > 10"));

        assertTrue(GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of("score", 1))
            .isEmpty());
    }

    @Test
    public void testResolveTransitionIgnoresOtherNodesTransitions() {
        List<Map<String, ?>> transitions = List.of(transition("x", "b", null));

        assertTrue(GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of())
            .isEmpty());
    }

    @Test
    public void testResolveTransitionResolvesDynamicTarget() {
        List<Map<String, ?>> transitions = List.of(transition("a", "=nextNode", null));

        Optional<String> target = GraphTaskUtils.resolveTransition(
            EVALUATOR, transitions, "a", Map.of("nextNode", "review"));

        assertEquals(Optional.of("review"), target);
    }

    @Test
    public void testResolveTransitionSkipsDynamicTargetThatResolvesBlank() {
        List<Map<String, ?>> transitions = List.of(
            transition("a", "=nextNode", null),
            transition("a", "fallback", null));

        Optional<String> target = GraphTaskUtils.resolveTransition(
            EVALUATOR, transitions, "a", Map.of("nextNode", ""));

        assertEquals(Optional.of("fallback"), target);
    }

    @Test
    public void testResolveTransitionTakesFirstOfSeveralUnconditional() {
        List<Map<String, ?>> transitions = List.of(
            transition("a", "first", null),
            transition("a", "second", ""));

        assertEquals(Optional.of("first"), GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of()));
    }

    private static Map<String, ?> transition(String from, String to, String condition) {
        if (condition == null) {
            return Map.of("from", from, "to", to);
        }

        return Map.of("from", from, "to", to, "condition", condition);
    }
}
