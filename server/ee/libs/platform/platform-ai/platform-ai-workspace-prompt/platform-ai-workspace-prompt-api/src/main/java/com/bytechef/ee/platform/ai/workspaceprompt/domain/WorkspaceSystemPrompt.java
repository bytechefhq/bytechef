/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt.domain;

/**
 * A workspace administrator's standing instructions for every AI agent operating in the workspace. Persisted as a
 * single {@link com.bytechef.platform.configuration.domain.Property} row per workspace rather than a dedicated table —
 * the platform property store already handles scope/audit/versioning and this is plain config data (the same storage
 * decision as {@code AiGuardrailsWorkspaceSettings}). Workspace-only by design: there is no tenant-default row; a
 * {@code null} workspace resolution means "no prompt applies".
 *
 * @version ee
 */
public record WorkspaceSystemPrompt(long workspaceId, String prompt) {

    public static final String PROPERTY_KEY = "workspace_system_prompt";

    public static final int MAX_LENGTH = 4000;
}
