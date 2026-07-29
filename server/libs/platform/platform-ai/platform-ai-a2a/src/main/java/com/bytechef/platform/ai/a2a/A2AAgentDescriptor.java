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

package com.bytechef.platform.ai.a2a;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * A minimal, transport-agnostic description of an agent that is exposed over A2A. {@link A2AAgentCardFactory} turns
 * this into the wire-format {@code AgentCard} served at {@code /.well-known/agent-card.json}.
 *
 * @param name        the agent's display name
 * @param description a human-readable description of what the agent does
 * @param url         the absolute base URL at which this agent's A2A JSON-RPC endpoint is served
 * @param version     the agent version string
 * @param skills      the skills the agent advertises (at least one is recommended by the A2A spec)
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record A2AAgentDescriptor(
    String name, String description, String url, String version, List<A2ASkill> skills) {

    /**
     * A single advertised capability of the agent.
     *
     * @param id          a stable skill identifier
     * @param name        the skill's display name
     * @param description what the skill does
     * @param tags        free-form tags for discovery
     */
    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record A2ASkill(String id, String name, String description, List<String> tags) {
    }
}
