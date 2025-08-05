/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ai.copilot;

import com.bytechef.ai.copilot.dto.ContextDTO;
import com.bytechef.ai.copilot.workflow.OrchestratorWorkersWorkflow;
import com.bytechef.ai.copilot.workflow.RoutingWorkflow;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class AiCopilotImpl implements AiCopilot {

    private static final String MESSAGE_ROUTE = "other";
    private static final Map<String, String> ROUTES = Map.of(
        "workflow",
        "The prompt contains some kind of workflow or the user asks you to create, add or modify something.",
        "other",
        "The user wants something else.");
    private static final String WORKFLOW_EDITOR_SYSTEM_PROMPT =
        """
            Return your response in a JSON format in a similar structure to the Workflows in the context.

            Workflow Building Rules:
            - Use the structure inside "structure" to build the workflow according to the Workflow Prompt
            - Use the attributes inside "output" to understand available variables from previous components
            - Put tasks where "parentTaskName" is not null into a component with "task_type"=flow, according to their "parentTaskName" and according to the given Workflow Prompt
            - Do not put "parentTaskName" or "task_type" as attributes into the workflow

            Using Flow Task Type Components:
            - If task involves conditional logic, use condition/v1 flow
            - If task involves iteration/looping, use loop/v1 flow
            - If a flow task type component already exists in components that matches this task's requirements, do NOT create a new one

            Using References:
            - When referencing previous component outputs in parameters, use the format: $\\{componentName.outputProperty\\}
            - For array access, use: $\\{componentName.outputProperty[index]\\}
            - For nested objects, use: $\\{componentName.outputProperty.nestedProperty\\}

            Handling Missing Components:
            - If component.type is "trigger" and component.structure.type is "missing/v1/missing", don't put any trigger
            - If component.type is "action" and component.structure.type is "missing/v1/missing", pass the missing Component

            Return your response in this JSON format:
            \\{
              "label": "Workflow label",
              "description": "Workflow description",
              "inputs": [],
              "triggers": [],
              "tasks": []
            \\}
            """;
    private static final String WORKFLOW_EDITOR_USER_PROMPT =
        """
            components:
            {task_list}

            Workflow Prompt:
            {task_analysis}

            Build a workflow using component.structure using the instructions from the Workflow Prompt and component.parentTaskName.
            Provide it in a JSON format similar structure to the Workflows in the context.
            Only use components that are provided in this prompt.
            """;
    private static final String WORKFLOW_CHECKER_USER_PROMPT = """
            Workflow:
            {workflow}
            Prompt:
            {message}

            Check the provided workflow and correct any mistakes:
            1. Check if the Workflow displays what the prompt describes
            2. Check if the Workflow has the correct structure (similar to the one in the context)

            If the Workflow correctly follows these guidelines, return it unmodified. If it doesn't, modify it.
            Return only the JSON.
        """;

    private static final String WORKFLOW_ROUTE = "workflow";
    private static final String USER_PROMPT = """
        Current workflow:
        {workflow}
        Instructions:
        {message}
        """;
    private static final String MESSAGE_SYSTEM_PROMPT =
        """
            You are a ByteChef workflow building assistant. Respond in a helpful manner, but professional tone. Offer helpful suggestions on what the user could ask you next.
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
                    .similarityThreshold(0.7)
                    .build())
            .build();

        QuestionAnswerAdvisor questionAnswerAdvisorWorkflow = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(
                SearchRequest.builder()
                    .filterExpression("category == 'workflows'")
                    .topK(6)
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
                            .text(WORKFLOW_EDITOR_USER_PROMPT)
                            .param("task_analysis", process.analysis())
                            .param("task_list", process.workerResponses()))
                        .advisors(advisor -> advisor.param(
                            ChatMemory.CONVERSATION_ID, workflow.getId())) // conversationId
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

                        String finalDefinition = definition;

                        definition = chatClientWorkflow.prompt()
                            .system(WORKFLOW_EDITOR_SYSTEM_PROMPT)
                            .user(user -> user
                                .text(WORKFLOW_CHECKER_USER_PROMPT)
                                .param("message", process.analysis())
                                .param("workflow", finalDefinition))
                            .call()
                            .content();

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
