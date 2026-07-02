/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.constant;

/**
 * @version ee
 *
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
