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
 * where workspace membership and chat ownership were not gated server-side — any authenticated user could submit
 * {@code state.workspaceId=&lt;victim_ws&gt;} and have the LLM tools operate against that workspace. The controller now
 * verifies these values and rewrites them into the server-controlled keys defined here. Anything else the client sent
 * under the same names is irrelevant — the agent reads only these keys.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubStateKeys {

    /**
     * The authenticated user id placed by the controller after the workspace membership check passes. Always present
     * once the SSE chat dispatched, never client-controllable.
     */
    public static final String AUTHENTICATED_USER_ID = "__authenticatedUserId";

    /**
     * The verified workspace id placed by the controller after the workspace membership check passes. The client also
     * sends {@code workspaceId} in the original request body — that value is read once by the controller for the
     * membership check and then rewritten under this key for downstream agents/tools.
     */
    public static final String VERIFIED_WORKSPACE_ID = "__verifiedWorkspaceId";

    /**
     * The verified thread id placed by the controller after the chat-ownership check passes. Optional — only present
     * when the client sent a {@code threadId} that resolves to a chat owned by the authenticated user.
     */
    public static final String VERIFIED_THREAD_ID = "__verifiedThreadId";

    /**
     * The server-validated environment ordinal, injected by the controller after range-checking the client-supplied
     * {@code environmentId}. The catalog runtime resolver reads THIS key (never the raw client value) so a forged or
     * out-of-range ordinal can't drive platform-API-key selection. Environment is a deployment-wide enum
     * (DEVELOPMENT/STAGING/PRODUCTION), not a per-user ACL, so this is range-validation + verified-state consistency,
     * not a membership check.
     */
    public static final String VERIFIED_ENVIRONMENT_ID = "__verifiedEnvironmentId";

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
     * Unlike {@link #VERIFIED_WORKSPACE_ID} and friends, this key is <em>not</em> rewritten by the controller — the
     * client value is the authoritative source. The resolver constrains the resulting (provider, model) pair to the
     * workspace's enabled AI Gateway providers, so a malicious client cannot escape the workspace's allowed set.
     * </p>
     */
    public static final String USER_SELECTED_LLM_PROVIDER_KEY = "userSelectedLlmProvider";

    /**
     * User-selected LLM model id for the current conversation. Paired with {@link #USER_SELECTED_LLM_PROVIDER_KEY}; see
     * that field's Javadoc for precedence and validation behavior.
     */
    public static final String USER_SELECTED_LLM_MODEL_KEY = "userSelectedLlmModel";

    /**
     * AG-UI state key for the active environment id (client-supplied). Used to resolve the platform AI provider catalog
     * API key for the chosen provider.
     */
    public static final String ENVIRONMENT_ID = "environmentId";

    /**
     * Plain AG-UI state alias for the workspace id. The controller overwrites this with the server-verified value (see
     * {@link #VERIFIED_WORKSPACE_ID}) so code reading either key form sees consistent, server-controlled data.
     */
    public static final String WORKSPACE_ID = "workspaceId";

    /**
     * Plain AG-UI state alias for the user id. The controller overwrites this with the server-verified value (see
     * {@link #AUTHENTICATED_USER_ID}) so code reading either key form sees consistent, server-controlled data.
     */
    public static final String USER_ID = "userId";

    /**
     * Plain AG-UI state alias for the thread id. The controller overwrites this with the server-verified value (see
     * {@link #VERIFIED_THREAD_ID}) so code reading either key form sees consistent, server-controlled data.
     */
    public static final String THREAD_ID = "threadId";

    private AiHubStateKeys() {
    }
}
