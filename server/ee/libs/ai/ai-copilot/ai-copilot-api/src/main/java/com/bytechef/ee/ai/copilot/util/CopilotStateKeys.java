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

    /**
     * Run-state key under which the controller injects the authenticated user id, resolved server-side from the request
     * security context. Tools must never read the user id from a client-supplied field — it drives security-context
     * rehydration in the shared picker tools, so it has to be trustworthy.
     */
    public static final String STATE_AUTHENTICATED_USER_ID = "bytechef.copilot.authenticatedUserId";

    /**
     * Run-state key under which the controller / generator injects the request's tenant id (captured on the request
     * thread). Drives tenant rehydration in the shared tool wrapper so tools persist to the correct schema on worker
     * threads. Server-populated, never client-supplied.
     */
    public static final String STATE_TENANT_ID = "bytechef.copilot.tenantId";

    /**
     * Run-state key under which the synchronous generator injects the request thread's Spring Security
     * {@code Authentication}, captured on the request thread. The tool wrapper restores it on the agent's worker
     * threads so {@code @PreAuthorize}-gated tools see the same principal. Used for surfaces whose principal has no
     * backing platform user (the embedded API-key principal), which therefore cannot be rehydrated from a user id.
     * Server-populated from the live security context, never client-supplied.
     */
    public static final String STATE_AUTHENTICATION = "bytechef.copilot.authentication";

    private CopilotStateKeys() {
    }
}
