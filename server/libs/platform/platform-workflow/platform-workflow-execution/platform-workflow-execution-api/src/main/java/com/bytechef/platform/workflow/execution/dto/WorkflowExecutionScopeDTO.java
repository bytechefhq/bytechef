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

import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record WorkflowExecutionScopeDTO(
    PlatformType type, List<Long> principalIds, List<String> workflowIds,
    List<String> failedTriggerWorkflowExecutionIds) {

    public WorkflowExecutionScopeDTO(
        PlatformType type, @Nullable List<Long> principalIds, @Nullable List<String> workflowIds,
        @Nullable List<String> failedTriggerWorkflowExecutionIds) {

        this.type = type;
        this.principalIds = principalIds == null ? List.of() : List.copyOf(principalIds);
        this.workflowIds = workflowIds == null ? List.of() : List.copyOf(workflowIds);
        this.failedTriggerWorkflowExecutionIds =
            failedTriggerWorkflowExecutionIds == null ? List.of() : List.copyOf(failedTriggerWorkflowExecutionIds);
    }
}
