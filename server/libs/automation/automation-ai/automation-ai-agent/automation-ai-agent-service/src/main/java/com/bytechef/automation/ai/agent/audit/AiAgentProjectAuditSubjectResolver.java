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

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.configuration.audit.ProjectAuditSubjectResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Names the agent behind a hidden {@code __AI_AGENT__} project, so that
 * {@code PROJECT_VISIBILITY_CHANGED}/{@code PROJECT_ACCESS_GRANTED}/{@code PROJECT_ACCESS_REVOKED} published by
 * {@code ProjectSharingFacadeImpl} on the agent sharing path identify the agent an auditor actually saw in the product
 * — {@code AiAgentSharingFacadeImpl} delegates to that facade, so the project id is the only key the record used to
 * carry.
 *
 * <p>
 * Reads {@link AiAgentRepository} rather than the {@code @PreAuthorize}-guarded facade, exactly as
 * {@code AiAgentVisibilityProvider} and {@code AiAgentOwnershipResolver} do: an audit record must be written for the
 * operation that just succeeded, whoever performed it, and a gate here could only suppress it.
 *
 * <p>
 * {@code ai_agent.project_id} is one-to-one with the backing project, so at most one agent is ever returned, and an
 * ordinary user-created project matches nothing. The title, not the name, because the title is what the product shows
 * and what the audit reader will have seen; it is carried on the record so the row stays legible after the agent is
 * deleted.
 *
 * @author Ivica Cardic
 */
@Component
public class AiAgentProjectAuditSubjectResolver implements ProjectAuditSubjectResolver {

    private final AiAgentRepository aiAgentRepository;

    @SuppressFBWarnings("EI")
    public AiAgentProjectAuditSubjectResolver(AiAgentRepository aiAgentRepository) {
        this.aiAgentRepository = aiAgentRepository;
    }

    @Override
    public Optional<AuditSubject> fetchSubject(long projectId) {
        return aiAgentRepository.findByProjectId(projectId)
            .map(agent -> new AuditSubject("AiAgent", agent.getId(), title(agent)));
    }

    /**
     * {@code ai_agent.title} is non-null in the schema, but this record is written on a path that must never throw, so
     * the fallback is the agent's slug name and then its id rather than a {@code NullPointerException} inside the
     * publisher's failure boundary.
     */
    private static String title(AiAgent agent) {
        String title = agent.getTitle();

        if (title != null) {
            return title;
        }

        String name = agent.getName();

        return name == null ? String.valueOf(agent.getId()) : name;
    }
}
