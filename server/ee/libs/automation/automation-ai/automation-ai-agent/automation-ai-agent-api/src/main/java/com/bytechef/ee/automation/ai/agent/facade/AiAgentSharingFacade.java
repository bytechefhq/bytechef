/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.agent.facade;

import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;

/**
 * Visibility and named-user grants for AI agents — the agent-keyed face of {@code ProjectSharingFacade}, which it
 * delegates to entirely.
 *
 * <p>
 * An agent's reach is its hidden {@code __AI_AGENT__} project's reach; there is one record and it lives on the project.
 * These four methods exist so callers can say "this agent" rather than having to know that, and so the agent scopes can
 * gate the operation — not so that agents get a sharing implementation of their own. Nothing here reimplements a
 * sharing rule: the workspace-membership check on a grantee, the "same error for an unknown resource and someone
 * else's" collapse, the supported-rung check and the audit events are all the project facade's, and stay there.
 *
 * <p>
 * The operations govern MANAGEMENT surfaces only. Withholding an agent removes it from colleagues' agent and deployment
 * lists and denies their by-id reads; it does not touch a single channel. A PRIVATE agent's Slack, WhatsApp, webhook
 * and hosted-chat triggers keep answering everyone exactly as before.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiAgentSharingFacade {

    List<Long> getAgentGrants(long agentId);

    void grantAgentAccess(long agentId, long userId);

    void revokeAgentAccess(long agentId, long userId);

    void setAgentVisibility(long agentId, ResourceVisibility visibility);
}
