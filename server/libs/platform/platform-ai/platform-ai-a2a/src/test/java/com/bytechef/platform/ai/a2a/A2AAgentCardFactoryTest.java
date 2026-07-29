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

import static org.assertj.core.api.Assertions.assertThat;

import io.a2a.spec.AgentCard;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class A2AAgentCardFactoryTest {

    private final A2AAgentCardFactory agentCardFactory = new A2AAgentCardFactory();

    @Test
    void testCreateProducesTextJsonRpcCard() {
        A2AAgentDescriptor descriptor = new A2AAgentDescriptor(
            "Support Agent", "Answers support questions", "https://example.com/a2a/agents/support", "1.0",
            List.of(new A2AAgentDescriptor.A2ASkill("qa", "Q&A", "Answers questions", List.of("support"))));

        AgentCard agentCard = agentCardFactory.create(descriptor);

        assertThat(agentCard.name()).isEqualTo("Support Agent");
        assertThat(agentCard.url()).isEqualTo("https://example.com/a2a/agents/support");
        assertThat(agentCard.preferredTransport()).isEqualTo("JSONRPC");
        assertThat(agentCard.defaultInputModes()).containsExactly("text");
        assertThat(agentCard.defaultOutputModes()).containsExactly("text");
        assertThat(agentCard.capabilities()
            .streaming()).isFalse();
        assertThat(agentCard.skills()).hasSize(1);
        assertThat(agentCard.skills()
            .get(0)
            .id()).isEqualTo("qa");
    }
}
