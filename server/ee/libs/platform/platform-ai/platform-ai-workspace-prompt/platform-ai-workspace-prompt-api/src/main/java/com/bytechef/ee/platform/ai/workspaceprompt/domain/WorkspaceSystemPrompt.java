/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt.domain;

/**
 * @version ee
 */
public record WorkspaceSystemPrompt(long workspaceId, String prompt) {

    public static final String PROPERTY_KEY = "workspace_system_prompt";

    public static final int MAX_LENGTH = 4000;
}
