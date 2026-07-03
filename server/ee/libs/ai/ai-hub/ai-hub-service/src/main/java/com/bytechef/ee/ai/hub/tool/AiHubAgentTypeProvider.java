/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.agent.tool.AgentTypeProvider;
import com.google.auto.service.AutoService;
import java.util.Set;

/**
 * Contributes the AI-hub {@link AiHubAgentType} values to the {@code AgentTypeRegistry}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoService(AgentTypeProvider.class)
public class AiHubAgentTypeProvider implements AgentTypeProvider {

    @Override
    public Set<AgentType> agentTypes() {
        return Set.of(AiHubAgentType.values());
    }
}
