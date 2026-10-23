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
public record ClusterElementCallDTO(
    String name, String status, Instant startDate, @Nullable Instant endDate, @Nullable String error) {

    public static List<ClusterElementCallDTO> fromMetadata(Map<String, ?> metadata) {
        if (!(metadata.get(TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS) instanceof List<?> calls)) {
            return List.of();
        }

        List<ClusterElementCallDTO> clusterElementCallDTOs = new ArrayList<>();

        for (Object call : calls) {
            if (call instanceof Map<?, ?> callMap) {
                Object endDate = callMap.get("endDate");
                Object error = callMap.get("error");

                clusterElementCallDTOs.add(
                    new ClusterElementCallDTO(
                        String.valueOf(callMap.get("workflowNodeName")), String.valueOf(callMap.get("status")),
                        Instant.parse(String.valueOf(callMap.get("startDate"))),
                        endDate == null ? null : Instant.parse(String.valueOf(endDate)),
                        error == null ? null : String.valueOf(error)));
            }
        }

        return List.copyOf(clusterElementCallDTOs);
    }
}
