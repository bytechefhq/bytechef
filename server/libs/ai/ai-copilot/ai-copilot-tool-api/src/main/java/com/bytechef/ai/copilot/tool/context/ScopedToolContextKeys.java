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

package com.bytechef.ai.copilot.tool.context;

/**
 * @author Ivica Cardic
 */
public final class ScopedToolContextKeys {

    public static final String TOOL_CONTEXT_ENVIRONMENT_ID_KEY = "bytechef.automationTool.environmentId";
    public static final String TOOL_CONTEXT_PROJECT_OWNER_KEY = "bytechef.automationTool.projectOwner";
    public static final String TOOL_CONTEXT_WORKSPACE_ID_KEY = "bytechef.automationTool.workspaceId";

    private ScopedToolContextKeys() {
    }
}
