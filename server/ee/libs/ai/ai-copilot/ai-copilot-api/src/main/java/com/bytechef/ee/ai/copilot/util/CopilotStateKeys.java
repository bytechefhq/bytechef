/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.util;

/**
 * Run-state keys shared between the Copilot REST controller (which populates them on the authenticated request thread)
 * and the service-layer tool-context mapper (which reads them off the agent run state). Kept in the api module so both
 * the rest and service modules reference one source of truth.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class CopilotStateKeys {

    public static final String STATE_AUTHENTICATED_USER_ID = "authenticatedUserId";
    public static final String STATE_TENANT_ID = "tenantId";
    public static final String STATE_AUTHENTICATION = "authentication";
    public static final String WORKSPACE_ID = "workspaceId";
    public static final String ENVIRONMENT_ID = "environmentId";

    private CopilotStateKeys() {
    }
}
