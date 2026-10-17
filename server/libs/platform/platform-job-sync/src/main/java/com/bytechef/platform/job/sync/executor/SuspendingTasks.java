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

package com.bytechef.platform.job.sync.executor;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Tells the workflow steps that can pause a run: approval and wait steps, and AI agents with an approval gate.
 *
 * @author Ivica Cardic
 */
public final class SuspendingTasks {

    private static final String APPROVAL_GATE = "approvalGate";
    private static final String CLUSTER_ELEMENTS = "clusterElements";
    private static final List<String> SUSPENDING_TASK_TYPE_PREFIXES = List.of("approval/", "wait/", "waitForApproval/");

    private SuspendingTasks() {
    }

    public static boolean isSuspending(WorkflowTask workflowTask) {
        String type = workflowTask.getType();

        if (type != null && SUSPENDING_TASK_TYPE_PREFIXES.stream()
            .anyMatch(type::startsWith)) {

            return true;
        }

        Map<String, ?> extensions = workflowTask.getExtensions();

        return containsApprovalGate(extensions.get(CLUSTER_ELEMENTS));
    }

    private static boolean containsApprovalGate(@Nullable Object clusterElements) {
        if (clusterElements instanceof Collection<?> clusterElementCollection) {
            return clusterElementCollection.stream()
                .anyMatch(SuspendingTasks::containsApprovalGate);
        }

        if (clusterElements instanceof Map<?, ?> clusterElementMap) {
            if (clusterElementMap.get(WorkflowConstants.TYPE) instanceof String type) {
                String[] typeParts = type.split("/");

                return (typeParts.length == 3 && APPROVAL_GATE.equals(typeParts[2])) ||
                    containsApprovalGate(clusterElementMap.get(CLUSTER_ELEMENTS));
            }

            return clusterElementMap.values()
                .stream()
                .anyMatch(SuspendingTasks::containsApprovalGate);
        }

        return false;
    }
}
