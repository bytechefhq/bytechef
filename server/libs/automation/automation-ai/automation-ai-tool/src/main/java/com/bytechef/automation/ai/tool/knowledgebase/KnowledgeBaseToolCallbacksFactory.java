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

import com.bytechef.automation.ai.tool.ToolArtifactRecorder;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * Builds the Knowledge Base tool-callback lists shared by the Copilot panel agents, the AI Hub ASK/BUILD agents (which
 * register the two reads flat and the five mutations via the searchable tool catalog rather than through a delegate —
 * the former {@code knowledge_base_agent} subagent was dissolved, ticket 732, CRUD-delegate-unwind Task 6), and the
 * management MCP server (which registers all seven flat — no schema-count pressure there). Read list feeds ASK; write
 * list feeds BUILD.
 *
 * @author Ivica Cardic
 */
public class KnowledgeBaseToolCallbacksFactory {

    private final WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;
    private final KnowledgeBaseFacade knowledgeBaseFacade;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final @Nullable ToolArtifactRecorder artifactRecorder;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public KnowledgeBaseToolCallbacksFactory(
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade,
        KnowledgeBaseFacade knowledgeBaseFacade,
        KnowledgeBaseService knowledgeBaseService,
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        @Nullable ToolArtifactRecorder artifactRecorder) {

        this.workspaceKnowledgeBaseFacade = workspaceKnowledgeBaseFacade;
        this.knowledgeBaseFacade = knowledgeBaseFacade;
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeBaseDocumentFacade = knowledgeBaseDocumentFacade;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.artifactRecorder = artifactRecorder;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListKnowledgeBasesToolCallback(workspaceKnowledgeBaseFacade));
        toolCallbacks.add(
            new QueryKnowledgeBaseToolCallback(knowledgeBaseFacade, knowledgeBaseService,
                knowledgeBaseDocumentService));

        return toolCallbacks;
    }

    public List<ToolCallback> writeToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(readToolCallbacks());

        toolCallbacks.add(new CreateKnowledgeBaseToolCallback(workspaceKnowledgeBaseFacade));
        toolCallbacks.add(
            new AddKnowledgeBaseDocumentToolCallback(
                knowledgeBaseDocumentFacade, workspaceKnowledgeBaseFacade, artifactRecorder));
        toolCallbacks.add(
            new DeleteKnowledgeBaseDocumentToolCallback(
                knowledgeBaseDocumentFacade, knowledgeBaseDocumentService, workspaceKnowledgeBaseFacade,
                artifactRecorder));
        toolCallbacks.add(new CloneKnowledgeBaseToolCallback(workspaceKnowledgeBaseFacade));
        toolCallbacks.add(new DeleteKnowledgeBaseToolCallback(workspaceKnowledgeBaseFacade));

        return toolCallbacks;
    }
}
