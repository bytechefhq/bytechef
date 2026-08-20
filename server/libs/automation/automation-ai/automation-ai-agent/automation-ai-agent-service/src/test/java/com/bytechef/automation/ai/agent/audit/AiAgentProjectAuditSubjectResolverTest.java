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

package com.bytechef.automation.ai.agent.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.configuration.audit.ProjectAuditSubjectResolver.AuditSubject;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiAgentProjectAuditSubjectResolverTest {

    private static final long AGENT_ID = 12L;
    private static final long AGENT_PROJECT_ID = 88L;
    private static final long PLAIN_PROJECT_ID = 89L;

    private final AiAgentRepository aiAgentRepository = mock(AiAgentRepository.class);
    private final AiAgentProjectAuditSubjectResolver resolver =
        new AiAgentProjectAuditSubjectResolver(aiAgentRepository);

    @Test
    void testNamesTheAgentBehindItsHiddenProject() {
        when(aiAgentRepository.findByProjectId(AGENT_PROJECT_ID)).thenReturn(Optional.of(agent("Support Bot")));

        assertThat(resolver.fetchSubject(AGENT_PROJECT_ID))
            .contains(new AuditSubject("AiAgent", AGENT_ID, "Support Bot"));
    }

    /**
     * The overwhelming majority of projects are projects. A resolver that claimed them would relabel every ordinary
     * project audit record as an agent.
     */
    @Test
    void testClaimsNothingForAProjectThatBacksNoAgent() {
        when(aiAgentRepository.findByProjectId(PLAIN_PROJECT_ID)).thenReturn(Optional.empty());

        assertThat(resolver.fetchSubject(PLAIN_PROJECT_ID)).isEmpty();
    }

    /**
     * This runs inside {@code ProjectAuditPublisher}'s failure boundary, which logs and continues — so a null title
     * would cost the record its subject entirely rather than throwing loudly. It falls back to the slug name instead.
     */
    @Test
    void testFallsBackToTheSlugNameRatherThanThrowingOnAnUntitledAgent() {
        when(aiAgentRepository.findByProjectId(AGENT_PROJECT_ID)).thenReturn(Optional.of(agent(null)));

        assertThat(resolver.fetchSubject(AGENT_PROJECT_ID))
            .contains(new AuditSubject("AiAgent", AGENT_ID, "support-bot"));
    }

    private static AiAgent agent(String title) {
        AiAgent agent = new AiAgent();

        agent.setId(AGENT_ID);
        agent.setName("support-bot");
        agent.setProjectId(AGENT_PROJECT_ID);
        agent.setTitle(title);

        return agent;
    }
}
