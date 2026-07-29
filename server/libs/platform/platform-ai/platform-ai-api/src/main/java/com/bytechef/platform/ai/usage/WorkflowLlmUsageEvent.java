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

package com.bytechef.platform.ai.usage;

import com.bytechef.platform.constant.PlatformType;
import org.jspecify.annotations.Nullable;

/**
 * Published (as a Spring application event) when a workflow task execution consumed LLM tokens — the CE emission seam
 * for per-job AI cost attribution. The CE side only emits; the EE metering listener resolves the workspace and persists
 * an {@code ai_llm_usage} row with {@code source = AI_AGENT} and {@code ownerId = jobId}. Best-effort by design: no
 * consumer means the event is dropped.
 *
 * @param jobId                  the Atlas job the task belongs to
 * @param taskExecutionId        the task execution that made the LLM call(s)
 * @param jobPrincipalId         the principal (project deployment / integration instance) id, when present
 * @param jobPrincipalWorkflowId the principal workflow id, when present
 * @param workflowId             the workflow definition id
 * @param environmentId          the execution environment id, when present
 * @param type                   the platform surface (AUTOMATION / EMBEDDED)
 * @param componentName          the component whose action consumed the tokens (e.g. aiAgent, openAi)
 * @param actionName             the executed action name
 * @param model                  the model reported by the chat response metadata, when available
 * @param promptTokens           accumulated prompt tokens for the task execution
 * @param completionTokens       accumulated completion tokens for the task execution
 * @param durationMs             wall-clock duration of the task execution
 *
 * @author Ivica Cardic
 */
public record WorkflowLlmUsageEvent(
    long jobId, @Nullable Long taskExecutionId, @Nullable Long jobPrincipalId, @Nullable Long jobPrincipalWorkflowId,
    @Nullable String workflowId, @Nullable Long environmentId, @Nullable PlatformType type, String componentName,
    String actionName, @Nullable String model, int promptTokens, int completionTokens, long durationMs) {
}
