/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

/**
 * Discriminator for the two flavours of {@code ai_hub_task} rows. Drives both the client-side runtime provider
 * selection (standard tasks use {@code AiHubRuntimeProvider}, workflow chats use {@code ChatRuntimeProvider}) and the
 * per-row column population (workflow chats carry {@code workflow_execution_id} and {@code project_deployment_id};
 * standard tasks leave both null).
 *
 * <p>
 * Persisted as INT ordinal — append new values at the end to preserve ordinal stability of historical rows. Enforced by
 * {@code EnumOrdinalStabilityTest} in the same module.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubTaskKind {

    /**
     * Default kind. The user chats with the LLM agent; the runtime hits AG-UI's {@code runAgent} endpoint.
     * {@code workflow_execution_id} and {@code project_deployment_id} are null on these rows.
     */
    STANDARD,

    /**
     * The task is bound to a specific workflow execution. The runtime hits the webhook endpoints
     * ({@code /webhooks/<id>} or {@code /webhooks/<id>/sse}) instead of the agent. Carries the workflow's
     * {@code workflow_execution_id} and the parent {@code project_deployment_id} for sidebar grouping.
     */
    WORKFLOW_CHAT,

    /**
     * The task runs against a user-created personal agent (a per-user agent definition with its own name, description,
     * and instructions). Routed through the same LLM agent as {@link #STANDARD} but with the agent's instructions
     * appended to the system prompt. Carries {@code ai_hub_personal_agent_id} for the lookup and sidebar grouping;
     * {@code workflow_execution_id} and {@code project_deployment_id} are null.
     */
    PERSONAL_AGENT
}
