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

package com.bytechef.platform.workflow.validator;

import com.bytechef.platform.workflow.task.dispatcher.graph.GraphTransitionUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * @author Ivica Cardic
 */
final class GraphReferenceUtils {

    private static final String GRAPH_TYPE_PREFIX = "graph/";
    private static final String NAME = "name";
    private static final String NODES = "nodes";
    private static final String PARAMETERS = "parameters";
    private static final String TRANSITIONS = "transitions";
    private static final String TYPE = "type";

    private GraphReferenceUtils() {
    }

    static Optional<Boolean> isReferenceAllowed(
        String currentTaskName, String referencedTaskName, Map<String, JsonNode> allTasksMap) {

        JsonNode graphTaskJsonNode = findGraphTaskDeclaring(referencedTaskName, allTasksMap);

        if (graphTaskJsonNode == null) {
            return Optional.empty();
        }

        JsonNode nameJsonNode = graphTaskJsonNode.path(NAME);

        if (currentTaskName.equals(nameJsonNode.asString(""))) {
            return Optional.of(true);
        }

        JsonNode parametersJsonNode = graphTaskJsonNode.path(PARAMETERS);

        JsonNode nodesJsonNode = parametersJsonNode.path(NODES);

        String enclosingMemberName = findEnclosingMemberName(nodesJsonNode, currentTaskName);

        if (enclosingMemberName == null) {
            return Optional.empty();
        }

        if (!enclosingMemberName.equals(currentTaskName) && enclosingMemberName.equals(referencedTaskName)) {
            return Optional.empty();
        }

        Set<String> precedingMemberNames = GraphTransitionUtils.getPrecedingNodeNames(
            getTransitions(parametersJsonNode.path(TRANSITIONS)), enclosingMemberName,
            getMemberNames(nodesJsonNode));

        return Optional.of(precedingMemberNames.contains(referencedTaskName));
    }

    private static @Nullable JsonNode findGraphTaskDeclaring(String memberName, Map<String, JsonNode> allTasksMap) {
        for (JsonNode taskJsonNode : allTasksMap.values()) {
            if (!isGraphTask(taskJsonNode)) {
                continue;
            }

            JsonNode parametersJsonNode = taskJsonNode.path(PARAMETERS);

            Set<String> memberNames = getMemberNames(parametersJsonNode.path(NODES));

            if (memberNames.contains(memberName)) {
                return taskJsonNode;
            }
        }

        return null;
    }

    private static Set<String> getMemberNames(JsonNode nodesJsonNode) {
        Set<String> memberNames = new LinkedHashSet<>();

        for (JsonNode memberJsonNode : nodesJsonNode) {
            JsonNode nameJsonNode = memberJsonNode.path(NAME);

            if (nameJsonNode.isString()) {
                memberNames.add(nameJsonNode.asString());
            }
        }

        return memberNames;
    }

    private static List<Map<String, String>> getTransitions(JsonNode transitionsJsonNode) {
        List<Map<String, String>> transitions = new ArrayList<>();

        for (JsonNode transitionJsonNode : transitionsJsonNode) {
            JsonNode fromJsonNode = transitionJsonNode.path(GraphTransitionUtils.FROM);
            JsonNode toJsonNode = transitionJsonNode.path(GraphTransitionUtils.TO);

            transitions.add(
                Map.of(
                    GraphTransitionUtils.FROM, fromJsonNode.asString(""), GraphTransitionUtils.TO,
                    toJsonNode.asString("")));
        }

        return transitions;
    }

    private static @Nullable String findEnclosingMemberName(JsonNode nodesJsonNode, String taskName) {
        for (JsonNode memberJsonNode : nodesJsonNode) {
            JsonNode nameJsonNode = memberJsonNode.path(NAME);

            if (!nameJsonNode.isString()) {
                continue;
            }

            if (taskName.equals(nameJsonNode.asString()) || containsTask(memberJsonNode.path(PARAMETERS), taskName)) {
                return nameJsonNode.asString();
            }
        }

        return null;
    }

    private static boolean containsTask(JsonNode jsonNode, String taskName) {
        if (jsonNode.isObject()) {
            JsonNode nameJsonNode = jsonNode.path(NAME);

            if (jsonNode.has(TYPE) && nameJsonNode.isString() && taskName.equals(nameJsonNode.asString())) {
                return true;
            }
        }

        if (!jsonNode.isContainer()) {
            return false;
        }

        for (JsonNode childJsonNode : jsonNode) {
            if (containsTask(childJsonNode, taskName)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isGraphTask(JsonNode taskJsonNode) {
        JsonNode typeJsonNode = taskJsonNode.path(TYPE);

        if (!typeJsonNode.isString()) {
            return false;
        }

        String type = typeJsonNode.asString();

        return type.startsWith(GRAPH_TYPE_PREFIX);
    }
}
