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

package com.bytechef.automation.ai.mcp.util;

import static com.bytechef.platform.component.constant.WorkflowConstants.NEW_WORKFLOW_CALL;
import static com.bytechef.platform.component.constant.WorkflowConstants.WORKFLOW;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Identifies the trigger through which a workflow can be called as an MCP tool.
 *
 * @author Ivica Cardic
 */
public final class McpWorkflowUtils {

    private McpWorkflowUtils() {
    }

    public static @Nullable WorkflowTrigger getCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), NEW_WORKFLOW_CALL)) {

                return workflowTrigger;
            }
        }

        return null;
    }
}
