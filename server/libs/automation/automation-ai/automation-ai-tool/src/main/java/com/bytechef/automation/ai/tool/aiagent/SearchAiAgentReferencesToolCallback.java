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

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class SearchAiAgentReferencesToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "searchAiAgentReferences";

    private static final String KIND_KNOWLEDGE_BASE = "KNOWLEDGE_BASE";
    private static final String KIND_SKILL = "SKILL";
    private static final long DEFAULT_ENVIRONMENT_ID = 0L;

    private static final String DESCRIPTION = """
        Find the skills or knowledge bases an AI Agent can reference, by name. Call it before adding a SKILL
        or KNOWLEDGE_BASE element: kind is SKILL or KNOWLEDGE_BASE, query is the name the user gave (e.g.
        "Brand Voice"); omit query to list them all. Returns a JSON array of matches with referenceId, name
        and description. Pass the referenceId unchanged to addAiAgentElement. For a SUB_AGENT, find the
        other agent with listAiAgents instead.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "kind": {"type": "string", "enum": ["SKILL", "KNOWLEDGE_BASE"], "description": "What to find"},
                "query": {"type": "string", "description": "Words from the name or description"}
            },
            "required": ["kind"]
        }""";

    private final @Nullable AiSkillFacade aiSkillFacade;
    private final JsonMapper jsonMapper = new JsonMapper();
    private final @Nullable WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SearchAiAgentReferencesToolCallback(
        @Nullable AiSkillFacade aiSkillFacade, @Nullable WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade) {

        this.aiSkillFacade = aiSkillFacade;
        this.workspaceKnowledgeBaseFacade = workspaceKnowledgeBaseFacade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            SearchAiAgentReferencesInput input = jsonMapper.readValue(
                toolInput, SearchAiAgentReferencesInput.class);

            List<String> queryWords = queryWords(input.query());

            if (KIND_SKILL.equals(input.kind())) {
                return searchSkills(queryWords);
            }

            if (KIND_KNOWLEDGE_BASE.equals(input.kind())) {
                return searchKnowledgeBases(queryWords, toolContext);
            }

            return toolError("kind must be SKILL or KNOWLEDGE_BASE");
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, SearchAiAgentReferencesToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String searchSkills(List<String> queryWords) {
        if (aiSkillFacade == null) {
            return toolError("Skills are not available in this deployment");
        }

        List<AiSkill> aiSkills = aiSkillFacade.getAiSkills();

        List<AiAgentReference> references = aiSkills.stream()
            .map(aiSkill -> new AiAgentReference(aiSkill.getId(), aiSkill.getName(), aiSkill.getDescription()))
            .filter(reference -> matches(reference, queryWords))
            .toList();

        return jsonMapper.writeValueAsString(references);
    }

    private String searchKnowledgeBases(List<String> queryWords, @Nullable ToolContext toolContext) {
        if (workspaceKnowledgeBaseFacade == null) {
            return toolError("Knowledge bases are not available in this deployment");
        }

        AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

        Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

        if (workspaceId == null) {
            return toolError("Workspace context unavailable - open this chat from the AI Hub of a workspace.");
        }

        Long environmentId = invocationContext.environmentId();

        List<KnowledgeBase> knowledgeBases = workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(
            workspaceId, environmentId == null ? DEFAULT_ENVIRONMENT_ID : environmentId);

        List<AiAgentReference> references = knowledgeBases.stream()
            .map(knowledgeBase -> new AiAgentReference(
                knowledgeBase.getId(), knowledgeBase.getName(), knowledgeBase.getDescription()))
            .filter(reference -> matches(reference, queryWords))
            .toList();

        return jsonMapper.writeValueAsString(references);
    }

    private static boolean matches(AiAgentReference reference, List<String> queryWords) {
        String searchableText = (reference.name() + " " + Objects.toString(reference.description(), ""))
            .toLowerCase(Locale.ROOT);

        for (String queryWord : queryWords) {
            if (!searchableText.contains(queryWord)) {
                return false;
            }
        }

        return true;
    }

    private static List<String> queryWords(@Nullable String query) {
        if (query == null) {
            return List.of();
        }

        return Arrays.stream(query.toLowerCase(Locale.ROOT)
            .split("[^a-z0-9]+"))
            .filter(word -> word.length() > 1)
            .toList();
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record AiAgentReference(Long referenceId, String name, @Nullable String description) {
    }

    public record SearchAiAgentReferencesInput(@Nullable String kind, @Nullable String query) {
    }
}
