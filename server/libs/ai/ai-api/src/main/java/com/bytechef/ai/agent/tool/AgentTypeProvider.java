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

import java.util.Set;

/**
 * SPI contributed once per module to declare the {@link AgentType}s that module owns. Discovered via
 * {@link java.util.ServiceLoader} and aggregated by {@link AgentTypeRegistry}, so a new module adds its agent types by
 * dropping in a provider (registered with {@code @AutoService(AgentTypeProvider.class)}) rather than editing a central
 * enum. Mirrors the {@code @AutoService(ComponentHandler.class)} discovery used for components.
 *
 * @author Ivica Cardic
 */
public interface AgentTypeProvider {

    /**
     * The agent types this module owns. Keys must be unique across all providers.
     */
    Set<AgentType> agentTypes();
}
