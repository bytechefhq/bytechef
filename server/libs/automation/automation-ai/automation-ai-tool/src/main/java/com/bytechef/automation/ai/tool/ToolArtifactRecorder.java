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

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Minimal CE abstraction that lets relocated tool callbacks record task artifacts without coupling to AI-Hub internals.
 * The only implementation is the AI-Hub task-artifact recorder, which populates the AI Hub task's artifacts sidebar
 * (keyed by the AG-UI {@code threadId}); it is injected into the callbacks only on the AI-Hub chat surface. On any
 * surface without an AI-Hub task (e.g. the management MCP server) the recorder is absent and callbacks skip recording —
 * so a {@code null} recorder is the normal off-AI-Hub case, not an error.
 *
 * @author Ivica Cardic
 */
public interface ToolArtifactRecorder {

    /**
     * Records that a file was created during the current task turn.
     *
     * @param threadId     the AG-UI thread id that identifies the active task
     * @param userId       the id of the user who owns the task (may be {@code null} — will be treated as 0)
     * @param artifactKind the kind string (matches {@code AiHubChatArtifactKind.name()})
     * @param artifactId   string representation of the created entity id
     * @param artifactName display name snapshot at creation time
     */
    void record(String threadId, Long userId, String artifactKind, String artifactId, String artifactName);

    /**
     * Same as {@link #record(String, Long, String, String, String)} but persists structured routing metadata as the
     * artifact's {@code metadata_json}.
     *
     * @param metadata routing metadata, or {@code null} for none
     */
    void record(
        String threadId, Long userId, String artifactKind, String artifactId, String artifactName,
        @Nullable Map<String, Object> metadata);

    /**
     * Records (or de-duplicates onto) a workflow artifact for a workflow the agent opened/referenced.
     */
    void recordWorkflowReference(
        String threadId, @Nullable Long userId, String workflowId, long projectId,
        @Nullable Long projectWorkflowId, String workflowName);

    /**
     * Server robustness layer for reference-kind artifacts. Resolves the task by (threadId, userId) and dedups on
     * (task, kind, artifactId).
     */
    void recordReference(
        String threadId, @Nullable Long userId, String artifactKind, String artifactId, String artifactName);
}
