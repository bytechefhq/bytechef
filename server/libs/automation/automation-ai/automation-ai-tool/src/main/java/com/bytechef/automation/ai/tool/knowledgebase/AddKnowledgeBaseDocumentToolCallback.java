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

package com.bytechef.automation.ai.tool.knowledgebase;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.tool.ToolArtifactRecorder;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that adds a text document to a knowledge base. The mutation is executed immediately —
 * every server-side mutation lands in real time and, when a {@link ToolArtifactRecorder} is supplied (AI Hub only), is
 * recorded as a task artifact for audit purposes.
 *
 * <p>
 * This callback is registered on {@code aiHubBuildSpringAIAgent} only — the ASK variant is read-only.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class AddKnowledgeBaseDocumentToolCallback implements ToolCallback {

    static final Set<String> ALLOWED_MIME_TYPES =
        Set.of("text/markdown", "text/plain", "text/html", "application/json");

    /**
     * Name of the artifact kind recorded on success, matching the {@code AiHubTaskArtifactKind.KB_DOCUMENT_ADDED} enum
     * constant on the AI Hub side. Carried as a plain string so this shared lib does not depend on ai-hub.
     */
    static final String ARTIFACT_KIND_KB_DOCUMENT_ADDED = "KB_DOCUMENT_ADDED";

    private static final long DEFAULT_ENVIRONMENT_ORDINAL = 0L;
    private static final String TOOL_NAME = "addKnowledgeBaseDocument";

    private static final String DESCRIPTION = """
        Add a text document to a knowledge base. Supply the knowledgeBaseId (from listKnowledgeBases),
        a document name, the text content, and a mimeType. Permitted mimeType values: text/markdown,
        text/plain, text/html, application/json. The document is added immediately. The
        knowledgeBaseId must belong to the current workspace.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "knowledgeBaseId": {"type": "string", "description": "Knowledge base id obtained from listKnowledgeBases"},
                    "name": {"type": "string", "description": "File name for the document (e.g. guide.md)"},
                    "content": {"type": "string", "description": "Text content of the document"},
                    "mimeType": {"type": "string", "description": "MIME type: text/markdown, text/plain, text/html, or application/json"}
                },
                "required": ["knowledgeBaseId", "name", "content", "mimeType"]
            }""";

    private final KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;
    private final WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;
    private final @Nullable ToolArtifactRecorder artifactRecorder;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AddKnowledgeBaseDocumentToolCallback(
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade,
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade,
        @Nullable ToolArtifactRecorder artifactRecorder) {

        this.knowledgeBaseDocumentFacade = knowledgeBaseDocumentFacade;
        this.workspaceKnowledgeBaseFacade = workspaceKnowledgeBaseFacade;
        this.artifactRecorder = artifactRecorder;
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
            AddKnowledgeBaseDocumentInput input =
                jsonMapper.readValue(toolInput, AddKnowledgeBaseDocumentInput.class);

            if (input.knowledgeBaseId() == null || input.knowledgeBaseId()
                .isBlank()) {
                return toolError("knowledgeBaseId is required");
            }

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            if (input.content() == null || input.content()
                .isBlank()) {
                return toolError("content is required and must not be blank");
            }

            if (input.mimeType() == null || input.mimeType()
                .isBlank()) {
                return toolError("mimeType is required");
            }

            if (!ALLOWED_MIME_TYPES.contains(input.mimeType())) {
                return toolError(
                    "Unsupported mimeType '" + input.mimeType() +
                        "'. Allowed values: text/markdown, text/plain, text/html, application/json");
            }

            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            long knowledgeBaseId;

            try {
                knowledgeBaseId = Long.parseLong(input.knowledgeBaseId());
            } catch (NumberFormatException exception) {
                return toolError(
                    "Invalid knowledgeBaseId - must be a numeric id obtained from listKnowledgeBases");
            }

            long environmentId = resolveEnvironmentId(invocationContext);

            KnowledgeBase knowledgeBase = resolveKnowledgeBaseInWorkspace(knowledgeBaseId, workspaceId, environmentId);

            if (knowledgeBase == null) {
                return toolError(
                    "Knowledge base " + input.knowledgeBaseId() + " not found in the current workspace.");
            }

            byte[] contentBytes = input.content()
                .getBytes(StandardCharsets.UTF_8);

            KnowledgeBaseDocument document = knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
                knowledgeBaseId, input.name(), input.mimeType(), contentBytes.length,
                new ByteArrayInputStream(contentBytes));

            recordArtifact(invocationContext, document);

            return jsonMapper.writeValueAsString(
                new AddKnowledgeBaseDocumentOutput(true, document.getId()
                    .toString(), document.getName()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, AddKnowledgeBaseDocumentToolCallback.class, TOOL_NAME, exception);
        }
    }

    private void recordArtifact(AgentToolInvocationContext invocationContext, KnowledgeBaseDocument document) {
        String conversationId = invocationContext.conversationId();
        Long userId = invocationContext.userId();

        if (artifactRecorder != null && conversationId != null && userId != null) {
            artifactRecorder.record(
                conversationId, userId, ARTIFACT_KIND_KB_DOCUMENT_ADDED,
                document.getId()
                    .toString(),
                document.getName(), null);
        }
    }

    private KnowledgeBase resolveKnowledgeBaseInWorkspace(long knowledgeBaseId, long workspaceId, long environmentId) {
        List<KnowledgeBase> workspaceKnowledgeBases =
            workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(workspaceId, environmentId);

        return workspaceKnowledgeBases.stream()
            .filter(knowledgeBase -> knowledgeBase.getId() != null && knowledgeBase.getId() == knowledgeBaseId)
            .findFirst()
            .orElse(null);
    }

    private long resolveEnvironmentId(AgentToolInvocationContext invocationContext) {
        Long environmentId = invocationContext.environmentId();

        return environmentId != null ? environmentId : DEFAULT_ENVIRONMENT_ORDINAL;
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record AddKnowledgeBaseDocumentInput(
        String knowledgeBaseId, String name, String content, String mimeType) {
    }

    public record AddKnowledgeBaseDocumentOutput(boolean added, String documentId, String name) {
    }
}
