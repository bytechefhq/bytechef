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

package com.bytechef.platform.configuration.dto;

import com.bytechef.atlas.configuration.domain.Workflow;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class VersionWorkflowChangeFactory {

    private VersionWorkflowChangeFactory() {
    }

    public static VersionWorkflowChange change(
        String workflowUuid, @Nullable Integer baseVersion, int version, @Nullable Workflow oldWorkflow,
        @Nullable Workflow newWorkflow, WorkflowDefinitionDiff diff) {

        return new VersionWorkflowChange(
            workflowUuid, label(oldWorkflow, newWorkflow), diff.changeType(), baseVersion, version, diff.changes(),
            diff.oldDefinition(), diff.newDefinition());
    }

    public static VersionWorkflowChangeSummary summary(
        String workflowUuid, @Nullable Workflow oldWorkflow, @Nullable Workflow newWorkflow,
        WorkflowDefinitionDiff diff) {

        int changeCount = diff.changes()
            .size();

        return new VersionWorkflowChangeSummary(
            workflowUuid, label(oldWorkflow, newWorkflow), diff.changeType(), changeCount);
    }

    private static @Nullable String label(@Nullable Workflow oldWorkflow, @Nullable Workflow newWorkflow) {
        Workflow workflow = newWorkflow != null ? newWorkflow : oldWorkflow;

        return workflow == null ? null : workflow.getLabel();
    }
}
