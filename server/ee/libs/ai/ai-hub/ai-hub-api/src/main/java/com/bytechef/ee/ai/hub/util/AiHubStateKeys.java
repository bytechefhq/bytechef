/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.util;

/**
 * Reserved keys placed by {@code AiHubApiController} into the AG-UI {@code State} map before the agent runs. These keys
 * are <em>server-controlled</em>: the controller overwrites whatever the client may have sent so the agent and tool
 * callbacks can trust the values.
 *
 * <p>
 * Why a dedicated namespace: the agent's {@code buildInvocationContext} originally read {@code workspaceId} and
 * {@code userId} directly from the request-supplied state. That made the SSE chat path the one place in the system
 * where workspace membership and chat ownership were not gated server-side; any authenticated user could submit
 * {@code state.workspaceId=&lt;victim_ws&gt;} and have the LLM tools operate against that workspace. The controller now
 * verifies these values and rewrites them into the server-controlled keys defined here. Anything else the client sent
 * under the same names is irrelevant; the agent reads only these keys.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubStateKeys {

    public static final String AUTHENTICATED_USER_ID = "__authenticatedUserId";

    public static final String VERIFIED_WORKSPACE_ID = "__verifiedWorkspaceId";

    public static final String VERIFIED_THREAD_ID = "__verifiedThreadId";

    public static final String VERIFIED_ENVIRONMENT_ID = "__verifiedEnvironmentId";

    /**
     * The id of the chat's owner, placed by the controller alongside the other verified keys whenever a
     * {@link #VERIFIED_THREAD_ID} resolves to an existing chat. Distinct from {@link #AUTHENTICATED_USER_ID}: once a
     * chat can be shared, the two diverge for a participant's turn; {@code AUTHENTICATED_USER_ID} is always the current
     * sender, this key is always the chat's owner. Tool callbacks that must not leak the owner's user-global resources
     * (connectors, MCP servers, skills) to a participant compare the two rather than assuming the sender is always the
     * owner. Absent on the very first turn, before any chat row exists.
     */
    public static final String VERIFIED_OWNER_USER_ID = "__verifiedOwnerUserId";

    /**
     * The current tenant id captured from {@link com.bytechef.tenant.TenantContext} by the controller at request time
     * and written into the server-controlled state. Downstream worker threads that execute tool callbacks outside the
     * original request thread can restore the tenant via
     * {@code TenantContext.callWithTenantId(state.get(VERIFIED_TENANT_ID))} so multi-tenant data isolation is preserved
     * across thread hops.
     */
    public static final String VERIFIED_TENANT_ID = "bytechef.aiHub.verifiedTenantId";

    /**
     * User-selected LLM provider for the current conversation, supplied by the client via the AG-UI request
     * {@code state.userSelectedLlmProvider}. Takes precedence over the workspace default when honored by
     * {@code AiHubChatClientResolver}. Always paired with {@link #USER_SELECTED_LLM_MODEL_KEY}: half-set states (only
     * provider or only model present) fall back to the workspace default with a warning log rather than 400-ing the
     * request, since a half-set state is a transient client artifact rather than malicious input.
     *
     * <p>
     * Unlike {@link #VERIFIED_WORKSPACE_ID} and friends, this key is <em>not</em> rewritten by the controller; the
     * client value is the authoritative source. The resolver constrains the resulting (provider, model) pair to the
     * workspace's enabled AI Gateway providers, so a malicious client cannot escape the workspace's allowed set.
     * </p>
     */
    public static final String USER_SELECTED_LLM_PROVIDER_KEY = "userSelectedLlmProvider";

    public static final String USER_SELECTED_LLM_MODEL_KEY = "userSelectedLlmModel";

    public static final String ENVIRONMENT_ID = "environmentId";

    public static final String WORKSPACE_ID = "workspaceId";

    public static final String USER_ID = "userId";

    public static final String THREAD_ID = "threadId";

    private AiHubStateKeys() {
    }
}
