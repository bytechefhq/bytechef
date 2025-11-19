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
import com.bytechef.ee.ai.copilot.workflow.RoutingWorkflow;
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
import org.springframework.ai.vectorstore.VectorStore;
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
    private static final String OTHER_ROUTE = "other";
    private static final Map<String, String> ROUTES = Map.of(
        "workflow",
        "The prompt contains some kind of workflow or the user asks you to create, add or modify something.",
        "other",
        "The user wants something else.");
    private static final String WORKFLOW_EDITOR_SYSTEM_PROMPT =
        """
            You are an expert in Bytechef automation software using tools. Your role is to design, build, and validate Bytechef workflows with maximum accuracy and efficiency.

            ## Core Workflow Building Process

            1. **Discovery Phase** - Find the right tasks:
               - Think deeply about user request and the logic you are going to build to fulfill it. Ask follow-up questions to clarify the user's intent, if something is unclear. Then, proceed with the rest of your instructions.
               - `searchTask(query, type)` - Search triggers, actions or task dispatchers by keywords
               - `listTasks(type, limit)` - list all tasks of certain type: action, trigger or taskDispatcher

            2. **Configuration Phase** - Get task details efficiently:
               - `getTaskDefinition(type, name, componentName, version)` - Structure of the task
               - `getTaskProperties(type, name, componentName, version)` - Optional. Get a more detailed descriptions of its properties
               - `getTaskOutputProperty(type, name, componentName, version)` - Output properties of the task.
            - If you get an error on getTaskOutputProperty() that says that the user needs to make a connection, warn the user that the final workflow might not complete if he doesn't have a connection. Repeat getTaskOutputProperty() if he makes a connection

            3. **Pre-Validation Phase** - Validate BEFORE building:
               - `validateTask(task, type,  name, conponentName, version)` - Task related validation
               - Fix any errors before proceeding. Repeat task validation until all errors are gone
               - It is good common practice to show a visual representation of the workflow architecture to the user and asking for opinion, before moving forward.

            4. **Building Phase** - Create the workflow:
               - `buildingInstructions()` - Start here!
               - `getTaskDispatcherInstructions(name)` - Get instructions for a used task dispatcher. Call this for every task dispatcher that's being used
               - Use validated configurations from step 3
               - Connect nodes with proper structure
               - Build the workflow in an artifact for easy editing downstream (unless the user asked to create in Bytechef instance)

            5. **Workflow Validation Phase** - Validate complete workflow:
               - `validateWorkflow(workflow)` - Complete validation
               - Fix any errors found. Repeat workflow validation until all errors are gone

            6. **Deployment** - Deploy it on ByteChef:
            - `searchWorkflows(query)` - search for a specific workflow
            - `updateWorkflow(workflowId, workflow)` -
            - `searchProjects(query)` - search for a specific project if asked to create a new one
            - `createProject(name)` - create a new project if asked to create a new one
            - `createProjectWorkflow(projectId, definition)` - create a new workflow if asked

            ## Key Insights

            - **VALIDATE EARLY AND OFTEN** - Catch errors before they reach deployment
            - **Pre-validate configurations** - Use validateTask before building
            - **Post-validate workflows** - Always validate complete workflows before deployment

            ## Validation Strategy

            ### Before Building:
            1. validateTask() - Full configuration validation
            2. If there are errors, fix them and validateTask() again before proceeding

            ### After Building:
            1. validateWorkflow() - Complete workflow validation
            2. If there are errors, fix them and validateWorkflow() again before proceeding

            ## Example Workflow

            ### 1. Discovery & Configuration
            searchTask('slack', 'action')
            // Ask questions if something is unclear

            ### 2. Configuration Phase
            getTaskDefinition('action', 'slackActionName', 'slack', 1)
            getTaskOutputProperty('action', 'slackActionName', 'slack', 1)
            // If getTaskOutputProperty() throws an error, remind them to make a connection before you proceed
            // Ask questions if something is unclear

            ### 3. Pre-Validation
            validateTask()

            ### 4. Build Workflow
            buildingInstructions()
            // Create workflow JSON with validated configs

            ### 5. Workflow Validation
            validateWorkflow()

            ### 5. Deploy Workflow
            // Ask the user what project and workflow you should deploy it to
            // If provided:
            searchWorkflows()
            updateWorkflow()
            // If asked to create a new one:
            createProject()
            createProjectWorkflow()

            ## Important Rules

            - Every workflow must only have one trigger, but as many actions or task dispatchers as needed
            - Display condition is located in metadata if the property is an object or in between @@ if it's not. If the condition is false: the object is not part of the JSON definition. Otherwise it is a part of it.
            - Every task must have a unique name in format: componentName_{number}
            - Required fields must be filled, the other fields are optional
            - If an array in taskDefinition contains multiple objects, you can use any of the objects for that array
            - ALWAYS validate before building
            - ALWAYS validate after building
            - FIX all errors before proceeding
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
    private final ProjectWorkflowTools projectWorkflowTools;
    private final ProjectTools projectTools;
    private final TaskTools taskTools;

    @SuppressFBWarnings("EI")
    public AiCopilotFacadeImpl(
        ChatClient.Builder chatClientBuilder, VectorStore vectorStore,
        // TODO Remove dependency on WorkflowService, send the workflow definition and return the updated workflow in
        // the response
        @Autowired(required = false) WorkflowService workflowService,
        ProjectTools projectTools, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools) {

        this.workflowService = workflowService;
        this.projectTools = projectTools;
        this.projectWorkflowTools = projectWorkflowTools;
        this.taskTools = taskTools;

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor
            .builder(
                MessageWindowChatMemory.builder()
                    .chatMemoryRepository(new InMemoryChatMemoryRepository())
                    .maxMessages(500)
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

        this.chatClientWorkflow = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                qaRetrievedDocuments
//                , questionAnswerAdvisorComponents
            )
            .build();

        this.chatClientScript = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor
            // add script advisor
            )
            .build();

        this.routingWorkflow = new RoutingWorkflow(this.chatClientWorkflow);
    }

    @Override
    public Flux<Map<String, ?>> chat(String message, String conversationId, ContextDTO context) {
        Workflow workflow;
        String currentWorkflow;

        if (workflowService != null) {
            workflow = workflowService.getWorkflow(context.workflowId());

            currentWorkflow = workflow.getDefinition();
        } else {
            currentWorkflow = null;
            workflow = null;
        }

        String route = routingWorkflow.route(message, ROUTES);

        return switch (route) {
            case WORKFLOW_ROUTE -> switch (context.source()) {
                case WORKFLOW_EDITOR, WORKFLOW_EDITOR_COMPONENTS_POPOVER_MENU ->
                    chatClientWorkflow.prompt()
                        .system(WORKFLOW_EDITOR_SYSTEM_PROMPT)
                        .user(user -> user.text(USER_PROMPT)
                            .param(WORKFLOW_ROUTE, Objects.requireNonNull(currentWorkflow))
                            .param(MESSAGE_ROUTE, message))
                        .advisors(advisor -> advisor.param(
                            ChatMemory.CONVERSATION_ID,
                            Objects.requireNonNull(Objects.requireNonNull(workflow)
                                .getId()))) // conversationId
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
                                .param(WORKFLOW_ROUTE, Objects.requireNonNull(currentWorkflow))
                                .param(MESSAGE_ROUTE, message))
                            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                            .stream()
                            .content()
                            .map(content -> Map.of("text", content));
                        case "python" -> chatClientScript.prompt()
                            .system("You are a python code generator, answer only with code.")
                            .user(user -> user.text(USER_PROMPT)
                                .param(WORKFLOW_ROUTE, Objects.requireNonNull(currentWorkflow))
                                .param(MESSAGE_ROUTE, message))
                            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                            .stream()
                            .content()
                            .map(content -> Map.of("text", content));
                        case "ruby" -> chatClientScript.prompt()
                            .system("You are a ruby code generator, answer only with code.")
                            .user(user -> user.text(USER_PROMPT)
                                .param(WORKFLOW_ROUTE, Objects.requireNonNull(currentWorkflow))
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
            case OTHER_ROUTE ->
                chatClientWorkflow.prompt()
                    .system(MESSAGE_SYSTEM_PROMPT)
                    .user(user -> user.text(message))
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .tools(projectTools, projectWorkflowTools)
                    .stream()
                    .content()
                    .map(content -> Map.of("text", content));
            default -> throw new IllegalStateException("Unexpected route: " + route);
        };
    }
}
