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

package com.bytechef.ai.mcp.tool.platform.validator;

import com.bytechef.ai.mcp.tool.platform.util.ToolUtils;
import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder pattern for managing validation context and parameters. Reduces parameter passing complexity and improves
 * code maintainability.
 */
public class ValidationContext {

    private final List<JsonNode> tasks;
    private final Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions;
    private final Map<String, ToolUtils.PropertyInfo> taskOutputs;
    private final StringBuilder errors;
    private final StringBuilder warnings;
    private final List<String> taskNames;
    private final Map<String, String> taskNameToTypeMap;
    private final Map<String, JsonNode> allTasksMap;

    private ValidationContext(Builder builder) {
        this.tasks = builder.tasks;
        this.taskDefinitions = builder.taskDefinitions;
        this.taskOutputs = builder.taskOutputs;
        this.errors = builder.errors;
        this.warnings = builder.warnings;
        this.taskNames = new ArrayList<>();
        this.taskNameToTypeMap = new HashMap<>();
        this.allTasksMap = new HashMap<>();

        buildTaskMaps();
    }

    private void buildTaskMaps() {
        for (JsonNode task : tasks) {
            if (task.has("name")) {
                String taskName = task.get("name")
                    .asText();
                taskNames.add(taskName);
                allTasksMap.put(taskName, task);
                if (task.has("type")) {
                    taskNameToTypeMap.put(taskName, task.get("type")
                        .asText());
                }
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<JsonNode> getTasks() {
        return new ArrayList<>(tasks);
    }

    public Map<String, List<ToolUtils.PropertyInfo>> getTaskDefinitions() {
        return new HashMap<>(taskDefinitions);
    }

    public Map<String, ToolUtils.PropertyInfo> getTaskOutputs() {
        return new HashMap<>(taskOutputs);
    }

    @SuppressFBWarnings("EI")
    public StringBuilder getErrors() {
        return errors;
    }

    @SuppressFBWarnings("EI")
    public StringBuilder getWarnings() {
        return warnings;
    }

    public List<String> getTaskNames() {
        return new ArrayList<>(taskNames);
    }

    @SuppressFBWarnings("EI")
    public Map<String, String> getTaskNameToTypeMap() {
        return taskNameToTypeMap;
    }

    @SuppressFBWarnings("EI")
    public Map<String, JsonNode> getAllTasksMap() {
        return allTasksMap;
    }

    public static class Builder {
        private List<JsonNode> tasks;
        private Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions;
        private Map<String, ToolUtils.PropertyInfo> taskOutputs;
        private StringBuilder errors;
        private StringBuilder warnings;

        public Builder withTasks(List<JsonNode> tasks) {
            this.tasks = new ArrayList<>(tasks);
            return this;
        }

        public Builder withTaskDefinitions(Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions) {
            this.taskDefinitions = new HashMap<>(taskDefinitions);
            return this;
        }

        public Builder withTaskOutputs(Map<String, ToolUtils.PropertyInfo> taskOutputs) {
            this.taskOutputs = new HashMap<>(taskOutputs);
            return this;
        }

        @SuppressFBWarnings("EI2")
        public Builder withErrors(StringBuilder errors) {
            this.errors = errors;
            return this;
        }

        @SuppressFBWarnings("EI2")
        public Builder withWarnings(StringBuilder warnings) {
            this.warnings = warnings;
            return this;
        }

        public ValidationContext build() {
            return new ValidationContext(this);
        }
    }
}
