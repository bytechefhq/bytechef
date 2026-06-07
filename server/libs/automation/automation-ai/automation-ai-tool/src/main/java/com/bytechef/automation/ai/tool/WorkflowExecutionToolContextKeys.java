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

/**
 * Spring AI {@link org.springframework.ai.chat.model.ToolContext} keys read by {@link WorkflowExecutionTools} to scope
 * workspace-bounded queries.
 *
 * <p>
 * The literal values intentionally match the keys written by the EE {@code AiHubToolInvocationContext} so the AI Hub
 * runtime needs no change to populate them. The Copilot runtime populates the same keys via the specialist agent's
 * {@code toolContext()} override. If the EE key strings ever change, these must change in lockstep.
 * </p>
 *
 * @author Ivica Cardic
 */
public final class WorkflowExecutionToolContextKeys {

    public static final String WORKSPACE_ID = "bytechef.assetFile.workspaceId";
    public static final String ENVIRONMENT_ID = "bytechef.assetFile.environmentId";

    private WorkflowExecutionToolContextKeys() {
    }
}
