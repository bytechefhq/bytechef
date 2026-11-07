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

package com.bytechef.platform.workflow.task.dispatcher.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class GraphTransitionUtilsTest {

    private static final List<String> NODE_NAMES = List.of("classify", "draft", "review", "publish");

    @Test
    public void testIsDynamicTargetRecognisesFormulasAndDataPills() {
        assertTrue(GraphTransitionUtils.isDynamicTarget("=classify.decision"));
        assertTrue(GraphTransitionUtils.isDynamicTarget("  =classify.decision"));
        assertTrue(GraphTransitionUtils.isDynamicTarget("${classify.decision}"));
        assertFalse(GraphTransitionUtils.isDynamicTarget("review"));
        assertFalse(GraphTransitionUtils.isDynamicTarget(null));
    }

    @Test
    public void testGetPrecedingNodeNamesFollowsTransitionsBackwards() {
        List<Map<String, ?>> transitions = List.of(
            transition("classify", "draft"), transition("draft", "review"), transition("review", "publish"));

        assertEquals(
            Set.of("classify", "draft"), GraphTransitionUtils.getPrecedingNodeNames(transitions, "review", NODE_NAMES));
    }

    @Test
    public void testGetPrecedingNodeNamesIncludesTheNodeItselfOnALoopBack() {
        List<Map<String, ?>> transitions = List.of(
            transition("classify", "draft"), transition("draft", "review"), transition("review", "draft"));

        assertEquals(
            Set.of("classify", "draft", "review"),
            GraphTransitionUtils.getPrecedingNodeNames(transitions, "draft", NODE_NAMES));
    }

    @Test
    public void testGetPrecedingNodeNamesIsEmptyForANodeNothingLeadsTo() {
        List<Map<String, ?>> transitions = List.of(transition("classify", "draft"));

        assertEquals(Set.of(), GraphTransitionUtils.getPrecedingNodeNames(transitions, "publish", NODE_NAMES));
    }

    @Test
    public void testGetPrecedingNodeNamesIgnoresTransitionsFromUndeclaredNodes() {
        List<Map<String, ?>> transitions = List.of(transition("removed", "draft"), transition("classify", "draft"));

        assertEquals(Set.of("classify"), GraphTransitionUtils.getPrecedingNodeNames(transitions, "draft", NODE_NAMES));
    }

    @Test
    public void testGetPrecedingNodeNamesIsEveryNodeWhenATargetIsDynamic() {
        List<Map<String, ?>> transitions = List.of(
            transition("classify", "=classify.decision"), transition("draft", "review"));

        assertEquals(
            Set.copyOf(NODE_NAMES), GraphTransitionUtils.getPrecedingNodeNames(transitions, "classify", NODE_NAMES));
    }

    @Test
    public void testGetReachableNodeNamesFollowsTransitionsFromTheStartNode() {
        List<Map<String, ?>> transitions = List.of(transition("classify", "review"), transition("draft", "publish"));

        assertEquals(
            Set.of("classify", "review"),
            GraphTransitionUtils.getReachableNodeNames(transitions, "classify", NODE_NAMES));
    }

    @Test
    public void testGetReachableNodeNamesIsEveryNodeWhenATargetIsDynamic() {
        List<Map<String, ?>> transitions = List.of(transition("classify", "=classify.decision"));

        assertEquals(
            Set.copyOf(NODE_NAMES), GraphTransitionUtils.getReachableNodeNames(transitions, "classify", NODE_NAMES));
    }

    @Test
    public void testGetReachableNodeNamesIsEmptyWhenTheStartNodeIsNotDeclared() {
        List<Map<String, ?>> transitions = List.of(transition("classify", "review"));

        assertEquals(Set.of(), GraphTransitionUtils.getReachableNodeNames(transitions, "removed", NODE_NAMES));
    }

    private static Map<String, ?> transition(String from, String to) {
        return Map.of(GraphTransitionUtils.FROM, from, GraphTransitionUtils.TO, to);
    }
}
