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

package com.bytechef.platform.component.definition.ai.agent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * One call a cluster root made to a cluster element, with its input, output and the call it ran under.
 *
 * @author Ivica Cardic
 */
public record ClusterElementCallRecord(
    String id, @Nullable String parentId, @Nullable String workflowNodeName, @Nullable String componentName,
    @Nullable String toolName, Status status, @Nullable Instant startDate, @Nullable Instant endDate,
    @Nullable String input, @Nullable String output, @Nullable String error) {

    public static final int MAX_VALUE_LENGTH = 65_536;

    private static final String COMPONENT_NAME = "componentName";
    private static final String END_DATE = "endDate";
    private static final String ERROR = "error";
    private static final String ID = "id";
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";
    private static final String PARENT_ID = "parentId";
    private static final String START_DATE = "startDate";
    private static final String STATUS = "status";
    private static final String TOOL_NAME = "toolName";
    private static final String WORKFLOW_NODE_NAME = "workflowNodeName";

    public static @Nullable String cap(@Nullable String value) {
        if (value == null || value.length() <= MAX_VALUE_LENGTH) {
            return value;
        }

        return value.substring(0, MAX_VALUE_LENGTH) + "\n... [truncated, " + value.length() + " characters]";
    }

    public static ClusterElementCallRecord fromMap(Map<String, ?> map) {
        return new ClusterElementCallRecord(
            String.valueOf(map.get(ID)), string(map, PARENT_ID), string(map, WORKFLOW_NODE_NAME),
            string(map, COMPONENT_NAME), string(map, TOOL_NAME), Status.valueOf(String.valueOf(map.get(STATUS))),
            instant(map, START_DATE), instant(map, END_DATE), string(map, INPUT), string(map, OUTPUT),
            string(map, ERROR));
    }

    public static List<ClusterElementCallRecord> merge(
        List<ClusterElementCallRecord> existing, List<ClusterElementCallRecord> incoming) {

        Map<String, ClusterElementCallRecord> merged = new LinkedHashMap<>();

        for (ClusterElementCallRecord record : existing) {
            merged.put(record.id(), record);
        }

        for (ClusterElementCallRecord record : incoming) {
            merged.merge(record.id(), record, ClusterElementCallRecord::mergeRecord);
        }

        return new ArrayList<>(merged.values());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put(ID, id);
        map.put(STATUS, status.name());

        put(map, PARENT_ID, parentId);
        put(map, WORKFLOW_NODE_NAME, workflowNodeName);
        put(map, COMPONENT_NAME, componentName);
        put(map, TOOL_NAME, toolName);
        put(map, START_DATE, startDate == null ? null : startDate.toString());
        put(map, END_DATE, endDate == null ? null : endDate.toString());
        put(map, INPUT, input);
        put(map, OUTPUT, output);
        put(map, ERROR, error);

        return map;
    }

    private static ClusterElementCallRecord mergeRecord(
        ClusterElementCallRecord existing, ClusterElementCallRecord incoming) {

        return new ClusterElementCallRecord(
            existing.id, pick(incoming.parentId, existing.parentId),
            pick(incoming.workflowNodeName, existing.workflowNodeName),
            pick(incoming.componentName, existing.componentName), pick(incoming.toolName, existing.toolName),
            incoming.status, incoming.startDate == null ? existing.startDate : incoming.startDate,
            incoming.endDate == null ? existing.endDate : incoming.endDate, pick(incoming.input, existing.input),
            pick(incoming.output, existing.output), pick(incoming.error, existing.error));
    }

    private static @Nullable String pick(@Nullable String incoming, @Nullable String existing) {
        return incoming == null ? existing : incoming;
    }

    private static @Nullable Instant instant(Map<String, ?> map, String key) {
        Object value = map.get(key);

        return value == null ? null : Instant.parse(String.valueOf(value));
    }

    private static @Nullable String string(Map<String, ?> map, String key) {
        Object value = map.get(key);

        return value == null ? null : String.valueOf(value);
    }

    private static void put(Map<String, Object> map, String key, @Nullable String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    public enum Status {
        STARTED, SUSPENDED, COMPLETED, FAILED
    }
}
