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

import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder pattern for managing validation context and parameters. Reduces parameter passing complexity and improves
 * code maintainability.
 *
 * @author Marko Kriskovic
 */
class ValidationContext {

    private final List<JsonNode> taskJsonNodes;
    private final Map<String, List<PropertyInfo>> taskDefinitionMap;
    private final Map<String, PropertyInfo> taskOutputMap;
    private final StringBuilder errors;
    private final StringBuilder warnings;
    private final List<String> taskNames = new ArrayList<>();
    private final Map<String, String> taskNameToTypeMap = new HashMap<>();
    private final Map<String, JsonNode> allTasksMap = new HashMap<>();

    private ValidationContext(
        List<JsonNode> taskJsonNodes, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, PropertyInfo> taskOutputMap, StringBuilder errors, StringBuilder warnings) {

        this.taskJsonNodes = taskJsonNodes;
        this.taskDefinitionMap = taskDefinitionMap;
        this.taskOutputMap = taskOutputMap;
        this.errors = errors;
        this.warnings = warnings;

        buildTaskMaps();
    }

    public static ValidationContext of(
        List<JsonNode> taskJsonNodes, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, PropertyInfo> taskOutputMap, StringBuilder errors, StringBuilder warnings) {

        return new ValidationContext(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);
    }

    private void buildTaskMaps() {
        for (JsonNode taskJsonNode : taskJsonNodes) {
            if (taskJsonNode.has("name")) {
                JsonNode nameJsonNode = taskJsonNode.get("name");

                String taskName = nameJsonNode.asText();

                taskNames.add(taskName);
                allTasksMap.put(taskName, taskJsonNode);

                if (taskJsonNode.has("type")) {
                    JsonNode typeJsonNode = taskJsonNode.get("type");

                    taskNameToTypeMap.put(taskName, typeJsonNode.asText());
                }
            }
        }
    }

    public List<JsonNode> getTasks() {
        return new ArrayList<>(taskJsonNodes);
    }

    public Map<String, List<PropertyInfo>> getTaskDefinitions() {
        return new HashMap<>(taskDefinitionMap);
    }

    public Map<String, PropertyInfo> getTaskOutputs() {
        return new HashMap<>(taskOutputMap);
    }

    public StringBuilder getErrors() {
        return errors;
    }

    public StringBuilder getWarnings() {
        return warnings;
    }

    public List<String> getTaskNames() {
        return new ArrayList<>(taskNames);
    }

    public Map<String, String> getTaskNameToTypeMap() {
        return taskNameToTypeMap;
    }

    public Map<String, JsonNode> getAllTasksMap() {
        return allTasksMap;
    }
}
