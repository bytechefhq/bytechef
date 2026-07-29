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

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import java.util.List;

/**
 * Builds the wire-format A2A {@link AgentCard} (served at {@code /.well-known/agent-card.json}) from a
 * {@link A2AAgentDescriptor}. The card advertises text input/output over the JSON-RPC transport; streaming is reported
 * as unsupported until the {@code message/stream} surface is added.
 *
 * @author Ivica Cardic
 */
public class A2AAgentCardFactory {

    private static final List<String> TEXT_MODES = List.of("text");
    private static final String PROTOCOL_VERSION = "0.3.0";
    private static final String JSONRPC_TRANSPORT = "JSONRPC";

    public AgentCard create(A2AAgentDescriptor descriptor) {
        AgentCapabilities capabilities = new AgentCapabilities.Builder()
            .streaming(false)
            .pushNotifications(false)
            .stateTransitionHistory(false)
            .build();

        List<AgentSkill> skills = descriptor.skills()
            .stream()
            .map(A2AAgentCardFactory::toAgentSkill)
            .toList();

        return new AgentCard.Builder()
            .name(descriptor.name())
            .description(descriptor.description())
            .url(descriptor.url())
            .version(descriptor.version())
            .protocolVersion(PROTOCOL_VERSION)
            .preferredTransport(JSONRPC_TRANSPORT)
            .capabilities(capabilities)
            .defaultInputModes(TEXT_MODES)
            .defaultOutputModes(TEXT_MODES)
            .skills(skills)
            .build();
    }

    private static AgentSkill toAgentSkill(A2AAgentDescriptor.A2ASkill skill) {
        return new AgentSkill.Builder()
            .id(skill.id())
            .name(skill.name())
            .description(skill.description())
            .tags(skill.tags() == null ? List.of() : skill.tags())
            .build();
    }
}
