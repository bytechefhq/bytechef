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

package com.bytechef.platform.ai.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.ai.facade.dto.ContextDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class AiCopilotFacadeImpl implements AiCopilotFacade {

    private final ChatClient chatClientWorkflow;
    private final ChatClient chatClientComponent;
    private final ChatClient chatClientScript;
    private final WorkflowService workflowService;
    private final OrchestratorWorkers orchestratorWorkers;

    @SuppressFBWarnings("EI")
    public AiCopilotFacadeImpl(
        ChatClient.Builder chatClientBuilder, VectorStore vectorStore, WorkflowService workflowService) {

        this.workflowService = workflowService;

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor
            .builder(
                MessageWindowChatMemory.builder()
                    .chatMemoryRepository(new InMemoryChatMemoryRepository())
                    .build())
            .build();

        QuestionAnswerAdvisor questionAnswerAdvisorComponents = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(
                SearchRequest.builder()
                    .filterExpression("category == 'components' or category == 'flows'")
                    .topK(15)
                    .build())
            .build();

        QuestionAnswerAdvisor questionAnswerAdvisorWorkflow = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(
                SearchRequest.builder()
                    .filterExpression("category == 'workflows'")
                    .build())
            .build();

        SimpleLoggerAdvisor qaRetrievedDocuments = new SimpleLoggerAdvisor(
            request -> "Retrieved documents: " + request.context()
                .get("qa_retrieved_documents"),
            response -> "Response: " + ModelOptionsUtils.toJsonStringPrettyPrinter(response),
            1 // Log level
        );

        this.chatClientComponent = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
//                messageChatMemoryAdvisor,
                questionAnswerAdvisorComponents, qaRetrievedDocuments)
            .build();

        this.chatClientWorkflow = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                questionAnswerAdvisorWorkflow
//                , questionAnswerAdvisorComponents
            )
            .build();

        this.chatClientScript = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                questionAnswerAdvisorComponents
            // add script advisor
            )
            .build();

        this.orchestratorWorkers = new OrchestratorWorkers(this.chatClientComponent);
    }

    @Override
    public Flux<Map<String, ?>> chat(String message, ContextDTO contextDTO, String conversationId) {
        Workflow workflow = workflowService.getWorkflow(contextDTO.workflowId());

        final String userPrompt = """
              Current workflow:
              {workflow}
              Instructions:
              {message}
            """;
        final String workflowString = "workflow";
        final String messageString = "message";

        return switch (contextDTO.source()) {
            case WORKFLOW_EDITOR, WORKFLOW_EDITOR_COMPONENTS_POPOVER_MENU -> {
                OrchestratorWorkers.FinalResponse process = orchestratorWorkers.process(message);

                yield chatClientWorkflow.prompt()
                    .system(
                        "Return your response in a JSON format in a similar structure to the Workflows in the context.")
                    .user(user -> user
                        .text(
                            """
                                Merge all the task.structure according to instructions. Only use tasks that are provided in this prompt. If the task.type is 'trigger' and task.structure.type is 'missing/v1/missing', don't put any trigger. If the task.type is 'action' and task.structure.type is 'missing/v1/missing', pass the 'missing' Component.

                                instructions:
                                {task_analysis}

                                subtasks:
                                {task_list}
                                """)
                        .param("task_analysis", process.analysis())
                        .param("task_list", process.workerResponses()))
                    .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .map(content -> Map.of("text", content));
            }
            case CODE_EDITOR -> {
                Map<String, ?> parameters = contextDTO.parameters();

                yield switch ((String) parameters.get("language")) {
                    case "javascript" -> chatClientScript.prompt()
                        .system("You are a javascript code generator, answer only with code.")
                        .user(user -> user.text(userPrompt)
                            .param(workflowString, workflow.getDefinition())
                            .param(messageString, message))
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .stream()
                        .content()
                        .map(content -> Map.of("text", content));
                    case "python" -> chatClientScript.prompt()
                        .system("You are a python code generator, answer only with code.")
                        .user(user -> user.text(userPrompt)
                            .param(workflowString, workflow.getDefinition())
                            .param(messageString, message))
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .stream()
                        .content()
                        .map(content -> Map.of("text", content));
                    case "ruby" -> chatClientScript.prompt()
                        .system("You are a ruby code generator, answer only with code.")
                        .user(user -> user.text(userPrompt)
                            .param(workflowString, workflow.getDefinition())
                            .param(messageString, message))
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .stream()
                        .content()
                        .map(content -> Map.of("text", content));
                    default -> throw new IllegalStateException("Unexpected value: " + parameters.get("language"));
                };
            }
            case null, default -> throw new IllegalStateException("Unexpected value: " + contextDTO.source());
        };
    }
}
