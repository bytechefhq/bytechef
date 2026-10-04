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

package com.bytechef.automation.ai.a2a.util;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public final class A2aWorkflowTriggerUtils {

    private A2aWorkflowTriggerUtils() {
    }

    public static Optional<WorkflowTrigger> fetchNewWorkflowCallTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), WorkflowConstants.NEW_WORKFLOW_CALL)) {

                return Optional.of(workflowTrigger);
            }
        }

        return Optional.empty();
    }
}
