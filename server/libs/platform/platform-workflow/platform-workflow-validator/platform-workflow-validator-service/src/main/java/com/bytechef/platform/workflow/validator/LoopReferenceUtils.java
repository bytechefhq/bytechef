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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * @author Ivica Cardic
 */
final class LoopReferenceUtils {

    private static final String LOOP_TYPE_PREFIX = "loop/";

    private LoopReferenceUtils() {
    }

    static boolean isWithinSameLoopBody(
        String currentTaskName, String referencedTaskName, Map<String, JsonNode> allTasksMap) {

        for (JsonNode taskJsonNode : allTasksMap.values()) {
            if (!isLoopTask(taskJsonNode)) {
                continue;
            }

            JsonNode parametersJsonNode = taskJsonNode.path("parameters");

            Set<String> bodyTaskNames = new HashSet<>();

            collectTaskNames(parametersJsonNode.path("iteratee"), bodyTaskNames);

            if (bodyTaskNames.contains(currentTaskName) && bodyTaskNames.contains(referencedTaskName)) {
                return true;
            }
        }

        return false;
    }

    private static void collectTaskNames(JsonNode jsonNode, Set<String> taskNames) {
        if (jsonNode.isArray()) {
            for (JsonNode itemJsonNode : jsonNode) {
                collectTaskNames(itemJsonNode, taskNames);
            }

            return;
        }

        if (!jsonNode.isObject()) {
            return;
        }

        JsonNode nameJsonNode = jsonNode.path("name");
        JsonNode typeJsonNode = jsonNode.path("type");

        if (nameJsonNode.isString() && typeJsonNode.isString()) {
            taskNames.add(nameJsonNode.asString());
        }

        for (JsonNode valueJsonNode : jsonNode.values()) {
            collectTaskNames(valueJsonNode, taskNames);
        }
    }

    private static boolean isLoopTask(JsonNode taskJsonNode) {
        JsonNode typeJsonNode = taskJsonNode.path("type");

        return typeJsonNode.isString() && typeJsonNode.asString()
            .startsWith(LOOP_TYPE_PREFIX);
    }
}
