/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.facade;

import com.bytechef.ai.mcp.tool.automation.ProjectTools;
import com.bytechef.ai.mcp.tool.automation.ProjectWorkflowTools;
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.ai.copilot.dto.ContextDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
public class AiCopilotFacadeImpl implements AiCopilotFacade {

    private static final String MESSAGE_ROUTE = "message";
    private static final MessageChatMemoryAdvisor MESSAGE_CHAT_MEMORY_ADVISOR = MessageChatMemoryAdvisor
        .builder(
            MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(500)
                .build())
        .build();
    private static final SimpleLoggerAdvisor QA_RETRIEVED_DOCUMENTS_SIMPLE_LOGGER_ADVISOR = new SimpleLoggerAdvisor(
        request -> {
            Map<String, Object> context = request.context();

            return "Retrieved documents: " + context.get("qa_retrieved_documents");
        },
        response -> "Response: " + ModelOptionsUtils.toJsonStringPrettyPrinter(response),
        1 // Log level
    );

    private static final String WORKFLOW = "workflow";
    private static final String USER_PROMPT = """
        Current workflow:
        {workflow}
        Instructions:
        {message}
        """;

    private final ChatClient chatClientWorkflow;
    private final ChatClient chatClientScript;
    private final WorkflowService workflowService;
    private final ProjectWorkflowTools projectWorkflowTools;
    private final ProjectTools projectTools;
    private final TaskTools taskTools;
    private final String systemPrompt;

    @SuppressFBWarnings("EI")
    public AiCopilotFacadeImpl(ChatClient.Builder chatClientBuilder,
        // TODO Remove dependency on WorkflowService, send the workflow definition and return the updated workflow in
        // the response
        @Autowired WorkflowService workflowService,
        ProjectTools projectTools, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools,
        @Value("classpath:system_prompt.txt") Resource systemPromptResource) {

        this.workflowService = workflowService;
        this.projectTools = projectTools;
        this.projectWorkflowTools = projectWorkflowTools;
        this.taskTools = taskTools;

        this.chatClientWorkflow = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                MESSAGE_CHAT_MEMORY_ADVISOR,
                QA_RETRIEVED_DOCUMENTS_SIMPLE_LOGGER_ADVISOR)
            .build();

        this.chatClientScript = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                MESSAGE_CHAT_MEMORY_ADVISOR
            // add script advisor
            )
            .build();
    }

    @Override
    public Flux<Map<String, ?>> chat(String message, String conversationId, ContextDTO context) {
        Workflow workflow = workflowService.getWorkflow(context.workflowId());

        String workflowDefinition = workflow.getDefinition();

        return switch (context.source()) {
            case WORKFLOW_EDITOR, WORKFLOW_EDITOR_COMPONENTS_POPOVER_MENU ->
                chatClientWorkflow.prompt()
                    .system(WORKFLOW_EDITOR_SYSTEM_PROMPT)
                    .user(user -> user.text(USER_PROMPT)
                        .param(WORKFLOW, workflowDefinition)
                        .param(MESSAGE_ROUTE, message))
                    .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID,
                        Objects.requireNonNull(workflow.getId()))) // conversationId
                    .tools(taskTools, projectWorkflowTools, projectTools)
                    .stream()
                    .content()
                    .map(content -> Map.of(
                        "text", content));
            case CODE_EDITOR -> {
                Map<String, ?> parameters = context.parameters();

                yield switch ((String) parameters.get("language")) {
                    case "javascript" -> chatClientScript.prompt()
                        .system("You are a javascript code generator, answer only with code.")
                        .user(user -> user.text(USER_PROMPT)
                            .param(WORKFLOW, workflowDefinition)
                            .param(MESSAGE_ROUTE, message))
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .stream()
                        .content()
                        .map(content -> Map.of("text", content));
                    case "python" -> chatClientScript.prompt()
                        .system("You are a python code generator, answer only with code.")
                        .user(user -> user.text(USER_PROMPT)
                            .param(WORKFLOW, workflowDefinition)
                            .param(MESSAGE_ROUTE, message))
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .stream()
                        .content()
                        .map(content -> Map.of("text", content));
                    case "ruby" -> chatClientScript.prompt()
                        .system("You are a ruby code generator, answer only with code.")
                        .user(user -> user.text(USER_PROMPT)
                            .param(WORKFLOW, workflowDefinition)
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
    }
}
