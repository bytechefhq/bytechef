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

package com.bytechef.automation.ai.tool;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Surface-neutral seam that lets the workflow-persist tools record the persisted workflow as a task artifact, without
 * coupling the (community) tool module to any enterprise AI Hub type. The implementation is provided by the enterprise
 * service layer and is optional: when absent (community build, no AI Hub), the persist tools skip recording.
 *
 * <p>
 * The whole {@link ToolContext} is passed through so the implementation owns extraction of the AI Hub conversation id
 * and user id from it — the community module stays ignorant of the {@code bytechef.agentTool.*} context keys.
 *
 * @author Ivica Cardic
 */
public interface WorkflowArtifactRecorder {

    /**
     * Records the just-persisted workflow as a task artifact when the invocation carries an AI Hub conversation
     * context; a no-op otherwise. Must be best-effort: never throw out of this method in a way that fails the workflow
     * persist.
     *
     * @param toolContext       the tool invocation context (carries the optional conversation id)
     * @param created           {@code true} for a freshly created workflow, {@code false} for an update
     * @param workflowId        the workflow id (artifact id used to open the workflow)
     * @param projectId         owning project id (routing metadata)
     * @param projectWorkflowId project-workflow id (routing metadata), may be {@code null}
     * @param workflowName      display-name snapshot
     */
    void recordWorkflowArtifact(
        @Nullable ToolContext toolContext, boolean created, String workflowId, long projectId,
        @Nullable Long projectWorkflowId, String workflowName);
}
