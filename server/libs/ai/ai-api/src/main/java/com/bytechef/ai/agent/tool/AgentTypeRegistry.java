/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.agent.tool;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Central view of every {@link AgentType} contributed by the per-module {@link AgentTypeProvider}s. The providers are
 * discovered once via {@link ServiceLoader}, so the catalog is assembled from the modules that own the types rather
 * than from a central enum. The built-in {@link CoreAgentType} values (currently the {@link CoreAgentType#UNKNOWN}
 * fallback) are always registered.
 *
 * @author Ivica Cardic
 */
public final class AgentTypeRegistry {

    private static final Map<String, AgentType> AGENT_TYPES_BY_KEY = load();

    private AgentTypeRegistry() {
    }

    public static AgentType fromKey(String key) {
        if (key == null) {
            return CoreAgentType.UNKNOWN;
        }

        return AGENT_TYPES_BY_KEY.getOrDefault(key, CoreAgentType.UNKNOWN);
    }

    private static Map<String, AgentType> load() {
        Map<String, AgentType> agentTypesByKey = new HashMap<>();

        for (CoreAgentType coreAgentType : CoreAgentType.values()) {
            agentTypesByKey.put(coreAgentType.key(), coreAgentType);
        }

        for (AgentTypeProvider agentTypeProvider : ServiceLoader.load(
            AgentTypeProvider.class, AgentTypeRegistry.class.getClassLoader())) {

            for (AgentType agentType : agentTypeProvider.agentTypes()) {
                AgentType existing = agentTypesByKey.putIfAbsent(agentType.key(), agentType);

                if (existing != null && existing != agentType) {
                    throw new IllegalStateException(
                        "Agent type key '" + agentType.key() + "' declared by more than one provider (" +
                            existing.name() + " and " + agentType.name() + ")");
                }
            }
        }

        return agentTypesByKey;
    }
}
