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

package com.bytechef.automation.ai.tool.aiagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class SearchAiAgentReferencesToolCallbackTest {

    private final AiSkillFacade aiSkillFacade = mock(AiSkillFacade.class);
    private final JsonMapper jsonMapper = new JsonMapper();
    private final WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade =
        mock(WorkspaceKnowledgeBaseFacade.class);
    private final SearchAiAgentReferencesToolCallback searchAiAgentReferencesToolCallback =
        new SearchAiAgentReferencesToolCallback(aiSkillFacade, workspaceKnowledgeBaseFacade);

    @Test
    void testCallFindsASkillByName() {
        when(aiSkillFacade.getAiSkills()).thenReturn(
            List.of(skill(4L, "Release Notes", "Writes release notes"), skill(7L, "Brand Voice", "Product copy")));

        JsonNode result = jsonMapper.readTree(
            searchAiAgentReferencesToolCallback.call("{\"kind\": \"SKILL\", \"query\": \"brand voice\"}"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .get("referenceId")
            .asLong()).isEqualTo(7L);
        assertThat(result.get(0)
            .get("name")
            .asString()).isEqualTo("Brand Voice");
    }

    @Test
    void testCallFindsAKnowledgeBaseOfTheWorkspace() {
        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(3L, 0L)).thenReturn(
            List.of(knowledgeBase(11L, "Employee Handbook"), knowledgeBase(12L, "Pricing")));

        JsonNode result = jsonMapper.readTree(
            searchAiAgentReferencesToolCallback.call(
                "{\"kind\": \"KNOWLEDGE_BASE\", \"query\": \"handbook\"}", workspaceToolContext()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .get("referenceId")
            .asLong()).isEqualTo(11L);
    }

    @Test
    void testCallNeedsAWorkspaceForKnowledgeBases() {
        assertThat(searchAiAgentReferencesToolCallback.call("{\"kind\": \"KNOWLEDGE_BASE\"}"))
            .contains("Workspace context unavailable");
    }

    @Test
    void testCallReportsWhenSkillsAreNotAvailable() {
        SearchAiAgentReferencesToolCallback withoutSkills =
            new SearchAiAgentReferencesToolCallback(null, workspaceKnowledgeBaseFacade);

        assertThat(withoutSkills.call("{\"kind\": \"SKILL\"}")).contains("not available");
    }

    private static KnowledgeBase knowledgeBase(long id, String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setId(id);
        knowledgeBase.setName(name);

        return knowledgeBase;
    }

    private static AiSkill skill(long id, String name, String description) {
        AiSkill aiSkill = new AiSkill();

        aiSkill.setId(id);
        aiSkill.setName(name);
        aiSkill.setDescription(description);

        return aiSkill;
    }

    private static ToolContext workspaceToolContext() {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(3L)
                .environmentId(0L)
                .build()
                .toToolContext());
    }
}
