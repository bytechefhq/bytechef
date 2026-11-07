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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class GraphTransitionUtils {

    public static final String FROM = "from";
    public static final String TO = "to";

    private static final String FORMULA_PREFIX = "=";
    private static final String DATA_PILL_PREFIX = "${";

    private GraphTransitionUtils() {
    }

    public static boolean isDynamicTarget(@Nullable String target) {
        if (target == null) {
            return false;
        }

        String trimmedTarget = target.trim();

        return trimmedTarget.startsWith(FORMULA_PREFIX) || trimmedTarget.contains(DATA_PILL_PREFIX);
    }

    public static boolean hasDynamicTarget(List<? extends Map<String, ?>> transitions) {
        return transitions.stream()
            .anyMatch(transition -> isDynamicTarget(getString(transition, TO)));
    }

    public static Set<String> getPrecedingNodeNames(
        List<? extends Map<String, ?>> transitions, String nodeName, Collection<String> nodeNames) {

        if (hasDynamicTarget(transitions)) {
            return new LinkedHashSet<>(nodeNames);
        }

        Set<String> precedingNodeNames = new LinkedHashSet<>();
        Deque<String> pendingNodeNames = new ArrayDeque<>();

        pendingNodeNames.push(nodeName);

        while (!pendingNodeNames.isEmpty()) {
            String targetNodeName = pendingNodeNames.pop();

            for (Map<String, ?> transition : transitions) {
                String fromNodeName = getString(transition, FROM);

                if (Objects.equals(targetNodeName, getTrimmedString(transition, TO)) && fromNodeName != null &&
                    nodeNames.contains(fromNodeName) && precedingNodeNames.add(fromNodeName)) {

                    pendingNodeNames.push(fromNodeName);
                }
            }
        }

        return precedingNodeNames;
    }

    public static Set<String> getReachableNodeNames(
        List<? extends Map<String, ?>> transitions, @Nullable String startNodeName, Collection<String> nodeNames) {

        if (startNodeName == null || hasDynamicTarget(transitions)) {
            return new LinkedHashSet<>(nodeNames);
        }

        Set<String> reachableNodeNames = new LinkedHashSet<>();
        Deque<String> pendingNodeNames = new ArrayDeque<>();

        pendingNodeNames.push(startNodeName);

        while (!pendingNodeNames.isEmpty()) {
            String sourceNodeName = pendingNodeNames.pop();

            if (!nodeNames.contains(sourceNodeName) || !reachableNodeNames.add(sourceNodeName)) {
                continue;
            }

            for (Map<String, ?> transition : transitions) {
                String toNodeName = getTrimmedString(transition, TO);

                if (Objects.equals(sourceNodeName, getString(transition, FROM)) && toNodeName != null) {
                    pendingNodeNames.push(toNodeName);
                }
            }
        }

        return reachableNodeNames;
    }

    private static @Nullable String getTrimmedString(Map<String, ?> transition, String key) {
        String value = getString(transition, key);

        return value == null ? null : value.trim();
    }

    private static @Nullable String getString(Map<String, ?> transition, String key) {
        Object value = transition.get(key);

        return value == null ? null : String.valueOf(value);
    }
}
