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
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The outcome of the calls a cluster root made to one of its cluster elements during a task execution.
 *
 * @author Ivica Cardic
 */
public record ClusterElementCall(
    String workflowNodeName, Status status, Instant startDate, @Nullable Instant endDate, @Nullable String error) {

    private static final String END_DATE = "endDate";
    private static final String ERROR = "error";
    private static final String START_DATE = "startDate";
    private static final String STATUS = "status";
    private static final String WORKFLOW_NODE_NAME = "workflowNodeName";

    public static ClusterElementCall fromMap(Map<String, ?> map) {
        Object endDate = map.get(END_DATE);
        Object error = map.get(ERROR);

        return new ClusterElementCall(
            String.valueOf(map.get(WORKFLOW_NODE_NAME)), Status.valueOf(String.valueOf(map.get(STATUS))),
            Instant.parse(String.valueOf(map.get(START_DATE))),
            endDate == null ? null : Instant.parse(String.valueOf(endDate)),
            error == null ? null : String.valueOf(error));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put(WORKFLOW_NODE_NAME, workflowNodeName);
        map.put(STATUS, status.name());
        map.put(START_DATE, startDate.toString());

        if (endDate != null) {
            map.put(END_DATE, endDate.toString());
        }

        if (error != null) {
            map.put(ERROR, error);
        }

        return map;
    }

    public enum Status {
        STARTED, COMPLETED, FAILED
    }
}
