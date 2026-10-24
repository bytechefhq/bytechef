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

package com.bytechef.platform.workflow.execution.dto;

import com.bytechef.platform.workflow.execution.constant.TaskExecutionMetadataConstants;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record ClusterElementCallRecordDTO(
    String id, @Nullable String parentId, @Nullable String workflowNodeName, @Nullable String componentName,
    @Nullable String toolName, String status, @Nullable Instant startDate, @Nullable Instant endDate,
    @Nullable String input, @Nullable String output, @Nullable String error) {

    public static List<ClusterElementCallRecordDTO> fromMetadata(Map<String, ?> metadata) {
        if (!(metadata.get(
            TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALL_RECORDS) instanceof List<?> callRecords)) {

            return List.of();
        }

        List<ClusterElementCallRecordDTO> clusterElementCallRecordDTOs = new ArrayList<>();

        for (Object callRecord : callRecords) {
            if (callRecord instanceof Map<?, ?> callRecordMap) {
                clusterElementCallRecordDTOs.add(
                    new ClusterElementCallRecordDTO(
                        String.valueOf(callRecordMap.get("id")), getString(callRecordMap, "parentId"),
                        getString(callRecordMap, "workflowNodeName"), getString(callRecordMap, "componentName"),
                        getString(callRecordMap, "toolName"), String.valueOf(callRecordMap.get("status")),
                        getInstant(callRecordMap, "startDate"), getInstant(callRecordMap, "endDate"),
                        getString(callRecordMap, "input"), getString(callRecordMap, "output"),
                        getString(callRecordMap, "error")));
            }
        }

        return List.copyOf(clusterElementCallRecordDTOs);
    }

    private static @Nullable Instant getInstant(Map<?, ?> map, String key) {
        Object value = map.get(key);

        return value == null ? null : Instant.parse(String.valueOf(value));
    }

    private static @Nullable String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);

        return value == null ? null : String.valueOf(value);
    }
}
