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

package com.bytechef.automation.ai.agent.dto;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Read-model returned by {@link com.bytechef.automation.ai.agent.facade.AiAgentFacade} — the {@link AiAgent} row plus
 * its position-ordered {@link AiAgentChannel} and {@link AiAgentElement} rows, and two derived fields describing the
 * draft ↔ published relationship of the agent's hidden backing project.
 *
 * @param agent                the agent row
 * @param channels             the agent's channels, position-ordered
 * @param elements             the agent's elements, position-ordered
 * @param unpublishedChanges   {@code true} when the backing project has never been published, or when its current draft
 *                             workflow was modified after the most recently published {@code ProjectVersion}
 * @param lastPublishedVersion the backing project's most recently published version number, or {@code 0} if it has
 *                             never been published
 * @param publishedDate        when the backing project's most recently published version was published, or {@code null}
 *                             if it has never been published
 * @param tags                 the agent's tags, resolved from its {@code ai_agent_tag} rows
 * @param visibility           who may see the agent, read from the backing project rather than stored on
 *                             {@code ai_agent}: an agent's generated workflow is not reachable as a capability separate
 *                             from the agent, so the two reaches can never need to diverge and a second column could
 *                             only drift from this one. See {@code AiAgentVisibilityProvider}.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record AiAgentDTO(
    AiAgent agent, List<AiAgentChannel> channels, List<AiAgentElement> elements, boolean unpublishedChanges,
    int lastPublishedVersion, @Nullable Instant publishedDate, List<Tag> tags, ResourceVisibility visibility) {

    /**
     * Convenience accessor for {@link AiAgent#getSettings()} — not a separate stored field, just a flatter read path
     * for callers (e.g. the GraphQL payload) that otherwise only reach every other {@code AiAgent} column through
     * {@link #agent()}.
     */
    public Map<String, ?> settings() {
        return agent.getSettings();
    }
}
