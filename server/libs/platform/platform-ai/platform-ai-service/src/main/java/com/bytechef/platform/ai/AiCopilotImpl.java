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

package com.bytechef.platform.ai;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.ai.dto.ContextDTO;
import com.bytechef.platform.ai.workflow.OrchestratorWorkersWorkflow;
import com.bytechef.platform.ai.workflow.RoutingWorkflow;
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
public class AiCopilotImpl implements AiCopilot {

    private static final String MESSAGE_ROUTE = "message";
    private static final Map<String, String> ROUTES = Map.of(
        "workflow",
        "The prompt contains some kind of workflow or the user asks you to create, add or modify something.",
        "other",
        "The user wants something else.");
    private static final String WORKFLOW_EDITOR_SYSTEM_PROMPT = """
        Return your response in a JSON format in a similar structure to the Workflows in the
        context.""";
    private static final String WORKFLOW_EDITOR_PROMPT = """
        Merge all the task.structure according to instructions. Only use tasks that are provided in this prompt.
        If the task.type is 'trigger' and task.structure.type is 'missing/v1/missing', don't put any trigger.
        If the task.type is 'action' and task.structure.type is 'missing/v1/missing', pass the 'missing' Component.

        instructions:
        {task_analysis}

        subtasks:
        {task_list}
        """;
    private static final String WORKFLOW_ROUTE = "workflow";
    private static final String USER_PROMPT = """
        Current workflow:
        {workflow}
        Instructions:
        {message}
        """;
    private static final String MESSAGE_SYSTEM_PROMPT = """
        You are a ByteChef workflow building assistant. Respond in a helpful manner,
        but professional tone. Offer helpful suggestions on what the user could ask you next.
        """;

    private final ChatClient chatClientWorkflow;
    private final ChatClient chatClientScript;
    private final WorkflowService workflowService;
    private final RoutingWorkflow routingWorkflow;
    private final OrchestratorWorkersWorkflow orchestratorWorkersWorkflow;

    @SuppressFBWarnings("EI")
    public AiCopilotImpl(
        ChatClient.Builder chatClientBuilder, VectorStore vectorStore, WorkflowService workflowService) {

        this.workflowService = workflowService;

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor
            .builder(
                MessageWindowChatMemory.builder()
                    .chatMemoryRepository(new InMemoryChatMemoryRepository())
                    .maxMessages(50)
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
            request -> {
                Map<String, Object> context = request.context();

                return "Retrieved documents: " + context.get("qa_retrieved_documents");
            },
            response -> "Response: " + ModelOptionsUtils.toJsonStringPrettyPrinter(response),
            1 // Log level
        );

        // TODO add multiuser, multitenant history
        // messageChatMemoryAdvisor,
        ChatClient chatClientComponent = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
//                messageChatMemoryAdvisor,
                questionAnswerAdvisorComponents,
                qaRetrievedDocuments)
            .build();

        this.chatClientWorkflow = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                questionAnswerAdvisorWorkflow,
                qaRetrievedDocuments
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

        this.routingWorkflow = new RoutingWorkflow(this.chatClientWorkflow);
        this.orchestratorWorkersWorkflow = new OrchestratorWorkersWorkflow(chatClientComponent);
    }

    @Override
    public Flux<Map<String, ?>> chat(String message, ContextDTO contextDTO, String conversationId) {
        Workflow workflow = workflowService.getWorkflow(contextDTO.workflowId());

        String currentWorkflow = workflow.getDefinition();

        String route = routingWorkflow.route(message, ROUTES);

        return switch (route) {
            case WORKFLOW_ROUTE -> switch (contextDTO.source()) {
                case WORKFLOW_EDITOR, WORKFLOW_EDITOR_COMPONENTS_POPOVER_MENU -> {
                    OrchestratorWorkersWorkflow.WorkerResponse process =
                        orchestratorWorkersWorkflow.process(message, currentWorkflow);

                    String definition = chatClientWorkflow.prompt()
                        .system(WORKFLOW_EDITOR_SYSTEM_PROMPT)
                        .user(user -> user
                            .text(WORKFLOW_EDITOR_PROMPT)
                            .param("task_analysis", process.analysis())
                            .param("task_list", process.workerResponses()))
                        .advisors(advisor -> advisor.param(
                            ChatMemory.CONVERSATION_ID, conversationId))
                        .call()
                        .content();

                    Map<String, ?> result;

                    if (definition == null) {
                        result = Map.of(
                            "workflowUpdated", false,
                            "text", "Unable to generate workflow, please try again");
                    } else {
                        definition = definition
                            .replace("```json", "")
                            .replace("```", "");

                        workflowService.update(workflow.getId(), definition, workflow.getVersion());

                        result = Map.of(
                            "workflowUpdated", true,
                            "text", "Workflow has been updated");
                    }

                    yield Flux.just(result);
                }
                case CODE_EDITOR -> {
                    Map<String, ?> parameters = contextDTO.parameters();

                    yield switch ((String) parameters.get("language")) {
                        case "javascript" -> chatClientScript.prompt()
                            .system("You are a javascript code generator, answer only with code.")
                            .user(user -> user.text(USER_PROMPT)
                                .param(WORKFLOW_ROUTE, currentWorkflow)
                                .param(MESSAGE_ROUTE, message))
                            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                            .stream()
                            .content()
                            .map(content -> Map.of("text", content));
                        case "python" -> chatClientScript.prompt()
                            .system("You are a python code generator, answer only with code.")
                            .user(user -> user.text(USER_PROMPT)
                                .param(WORKFLOW_ROUTE, currentWorkflow)
                                .param(MESSAGE_ROUTE, message))
                            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                            .stream()
                            .content()
                            .map(content -> Map.of("text", content));
                        case "ruby" -> chatClientScript.prompt()
                            .system("You are a ruby code generator, answer only with code.")
                            .user(user -> user.text(USER_PROMPT)
                                .param(WORKFLOW_ROUTE, currentWorkflow)
                                .param(MESSAGE_ROUTE, message))
                            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                            .stream()
                            .content()
                            .map(content -> Map.of("text", content));
                        default ->
                            throw new IllegalStateException("Unexpected value: " + parameters.get("language"));
                    };
                }
            };
            case MESSAGE_ROUTE ->
                chatClientWorkflow.prompt()
                    .system(MESSAGE_SYSTEM_PROMPT)
                    .user(user -> user.text(message))
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .map(content -> Map.of("text", content));
            default -> throw new IllegalStateException("Unexpected route: " + route);
        };
    }
}
