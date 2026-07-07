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

package com.bytechef.ai.copilot.constant;

/**
 * @author Ivica Cardic
 */
public final class CopilotConstants {

    public static final int ADDITIONAL_SYSTEM_PROMPT_MAX_LENGTH = 4000;
    public static final String STATE_AUTHENTICATED_USER_ID = "authenticatedUserId";
    public static final String STATE_AUTHENTICATION = "authentication";
    public static final String STATE_ADDITIONAL_SYSTEM_PROMPT = "additionalSystemPrompt";
    public static final String STATE_ENVIRONMENT_ID = "environmentId";
    public static final String STATE_TENANT_ID = "tenantId";
    public static final String STATE_USER_SELECTED_LLM_PROVIDER = "userSelectedLlmProvider";
    public static final String STATE_USER_SELECTED_LLM_MODEL = "userSelectedLlmModel";
    public static final String STATE_WORKSPACE_ID = "workspaceId";

    private CopilotConstants() {
    }
}
