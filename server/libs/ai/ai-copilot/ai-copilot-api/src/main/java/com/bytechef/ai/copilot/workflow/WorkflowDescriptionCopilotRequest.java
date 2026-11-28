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

package com.bytechef.ai.copilot.workflow;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record WorkflowDescriptionCopilotRequest(
    String workflowId, @Nullable String workflowNodeName, long environmentId) {

    public WorkflowDescriptionCopilotRequest {
        Objects.requireNonNull(workflowId, "workflowId");
    }

    public WorkflowDescriptionCopilotRequest withEnvironmentId(long environmentId) {
        return new WorkflowDescriptionCopilotRequest(workflowId, workflowNodeName, environmentId);
    }
}
