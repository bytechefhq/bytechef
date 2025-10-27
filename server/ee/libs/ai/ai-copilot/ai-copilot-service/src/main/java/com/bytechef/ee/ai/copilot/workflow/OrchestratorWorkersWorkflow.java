/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.workflow;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.Assert;

/**
 * The OrchestratorWorkers class manages the decomposition of a complex task into subtasks through the
 * orchestrator-workers pattern. It uses a ChatClient to interact with LLMs for task orchestration and execution. The
 * orchestrator analyzes and breaks down the task, and the workers process the resulting subtasks. The results from the
 * workers are then combined into a final response.
 *
 * @version ee
 *
 * @author Marko Kriskovic
 */
public class OrchestratorWorkersWorkflow {

    private static final String DEFAULT_ORCHESTRATOR_SYSTEM_PROMPT =
        """
            Analyze this task and break it down into subtasks so that the first subtask is always a single Trigger, and each subsequent subtask is either an Action or a Flow.

            Use the 'condition' flow for the 'if' function and 'loop' flow for the 'for each' function.
            If the component is contained by another flow component, the parent's name will be written in parentTaskName along with optional extra metadata.

            Return your response in this JSON format:
            \\{
                "analysis": "Explain your understanding of the task broken down into subtasks.",
                "tasks": [
                    \\{
                    "task_type": "trigger",
                    "name": "triggerComponentName1",
                    "description": "The function of the trigger",
                    "parentTaskName": "null"
                    \\},
                    \\{
                    "task_type": "flow",
                    "name": "flowComponentName1",
                    "description": "The function of the flow",
                    "parentTaskName": "null"
                    \\},
                    \\{
                    "task_type": "action",
                    "name": "actionComponentName",
                    "description": "The function of the action",
                    "parentTaskName": "flowComponentName1, optional metadata (ex. when condition = true)"
                    \\}
                ]
            \\}
            """;

    private static final String DEFAULT_ORCHESTRATOR_USER_PROMPT = """
        Current workflow:
        {workflow}

        Task:
        {task}
        """;

    private static final String DEFAULT_WORKER_SYSTEM_PROMPT =
        """
            Return your response in this JSON format:
            \\{
              "structure": \\{
                "label": "Name of action, trigger, or flow",
                "name": "componentName",
                "type": "componentType",
                "parameters": \\{\\}
              \\},
              "task_type": "action|trigger|flow",
              "parentTaskName": "Parent Flow Task Name",
              "output": \\{\\}
            \\}

            If no matching component exists, return the missing component:
            \\{
              "structure": \\{
                "label": "Name of missing component",
                "name": "missingComponentName",
                "type": "missing/v1/missing",
                "parameters": \\{\\}
              \\},
              "task_type": "action",
              "parentTaskName": "null",
              "output": \\{\\}
            \\}

            For Flow Types:
            - If task involves conditional logic, use condition/v1 flow
            - If task involves iteration/looping, use loop/v1 flow

            Flow Templates:
            Condition Flow:
            \\{
              "structure": \\{
                "label": "Condition description",
                "name": "condition1",
                "type": "condition/v1",
                "parameters": \\{
                  "rawExpression": true,
                  "expression": "condition_expression",
                  "caseTrue": [],
                  "caseFalse": []
                \\}
              \\},
              "task_type": "flow",
              "parentTaskName": "Parent Flow Task Name",
              "output": \\{\\}
            \\}

            Loop Flow:
            \\{
              "structure": \\{
                "label": "Loop description",
                "name": "loop1",
                "type": "loop/v1",
                "parameters": \\{
                  "items": "array_or_collection_reference",
                  "iteratee": []
                \\}
              \\},
              "task_type": "flow",
              "parentTaskName": "Parent Flow Task Name",
              "output": \\{"item": \\{\\}\\}
            \\}

            Output Rules:
            - If the component exists in context and has example output, paste the exact output structure in the "output" key
            - If the component exists but has no example output, use empty object \\{\\} for "output" (dynamic output)
            """;

    private static final String DEFAULT_WORKER_USER_PROMPT = """
        Search the context for an existing Component that best matches the task description. Generate content based on:
        - Task Type: {task_type}
        - Task Name: {task_name}
        - Task Description: {task_description}
        - Parent Flow Task Name: {parent_task_name}
        """;

    private final ChatClient chatClient;

    /**
     * Creates a new OrchestratorWorkers with default prompts.
     *
     * @param chatClient The ChatClient to use for LLM interactions
     */
    public OrchestratorWorkersWorkflow(ChatClient chatClient) {
        Assert.notNull(chatClient, "ChatClient must not be null");

        this.chatClient = chatClient;
    }

    /**
     * Processes a task using the orchestrator-workers pattern. First, the orchestrator analyzes the task and breaks it
     * down into subtasks. Then, workers execute each subtask in parallel. Finally, the results are combined into a
     * single response.
     *
     * @param taskDescription Description of the task to be processed
     * @return WorkerResponse containing the orchestrator's analysis and combined worker outputs
     * @throws IllegalArgumentException if a taskDescription is null or empty
     */
    @SuppressWarnings("null")
    public WorkerResponse process(String taskDescription, String currentWorkflow) {
        Assert.hasText(taskDescription, "Task description must not be empty");

        // 1. Orchestrator analyzes task and determines subtasks
        OrchestratorResponse orchestratorResponse = this.chatClient.prompt()
            .system(DEFAULT_ORCHESTRATOR_SYSTEM_PROMPT)
            .user(u -> u.text(DEFAULT_ORCHESTRATOR_USER_PROMPT)
                .param("task", taskDescription)
                .param("workflow", currentWorkflow))
            .call()
            .entity(OrchestratorResponse.class);

        Assert.notNull(orchestratorResponse, "Orchestrator response must not be null");

        // 2. Workers process subtasks in parallel
        List<String> workerResponses = orchestratorResponse.tasks()
            .parallelStream()
            .map(task -> this.chatClient.prompt()
                .system(DEFAULT_WORKER_SYSTEM_PROMPT)
                .user(u -> u.text(DEFAULT_WORKER_USER_PROMPT)
//                    .param("original_task", orchestratorResponse.analysis())
                    .param("task_type", task.task_type())
                    .param("task_name", task.name())
                    .param("task_description", task.description())
                    .param("parent_task_name", task.parentTaskName()))
                .call()
                .content())
            .toList();

        // 3. Results are combined into the final response
        return new WorkerResponse(orchestratorResponse.analysis(), workerResponses);
    }

    /**
     * Final response containing the orchestrator's analysis and combined worker outputs.
     *
     * @param analysis        The orchestrator's understanding and breakdown of the original task
     * @param workerResponses List of responses from workers, each handling a specific subtask
     */
    public record WorkerResponse(String analysis, List<String> workerResponses) {
        public WorkerResponse {
            workerResponses = List.copyOf(workerResponses);
        }
    }

    /**
     * Represents a subtask identified by the orchestrator that needs to be executed by a worker.
     *
     * @param task_type   The type or category of the task (e.g., "formal", "conversational")
     * @param description Detailed description of what the worker should accomplish
     */
    private record OrchestratorTask(String task_type, String name, String description, String parentTaskName) {
    }

    /**
     * Response from the orchestrator containing task analysis and breakdown into subtasks.
     *
     * @param analysis Detailed explanation of the task and how different approaches serve its aspects
     * @param tasks    List of subtasks identified by the orchestrator to be executed by workers
     */
    private record OrchestratorResponse(String analysis, List<OrchestratorTask> tasks) {
        public OrchestratorResponse {
            tasks = List.copyOf(tasks);
        }
    }
}
