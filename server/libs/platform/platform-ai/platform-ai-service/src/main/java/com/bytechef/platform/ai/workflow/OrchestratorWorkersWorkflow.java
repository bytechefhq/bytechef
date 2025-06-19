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

package com.bytechef.platform.ai.workflow;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.Assert;

/**
 * The OrchestratorWorkers class manages the decomposition of a complex task into subtasks through the
 * orchestrator-workers pattern. It uses a ChatClient to interact with LLMs for task orchestration and execution. The
 * orchestrator analyzes and breaks down the task, and the workers process the resulting subtasks. The results from the
 * workers are then combined into a final response.
 *
 * @author Marko Kriskovic
 */
public class OrchestratorWorkersWorkflow {

    private static final String DEFAULT_ORCHESTRATOR_PROMPT = """
        Analyze this task and break it down into subtasks so that the first subtask is always a single Trigger, and
        each subsequent subtask is either an Action or a Flow. Use the 'condition' flow for the 'if' function and
        'loop' flow for the 'for each' function.

        Current workflow:
        {workflow}

        Task:
        {task}

        Return your response in this JSON format:
        \\{
            "analysis": "Explain your understanding of the task broken down into subtasks.",
            "tasks": [
                \\{
                "type": "trigger",
                "description": "The function of the trigger",
                \\},
                \\{
                "type": "flow",
                "description": "The function of the flow",
                \\},
                \\{
                "type": "action",
                "description": "The function of the action",
                \\}
            ]
        \\}
        """;

    private static final String DEFAULT_WORKER_PROMPT = """
        Search the context for an existing Component that best matches the tasks description.

        Generate content based on:
        Original task: {original_task}
        Task Type: {task_type}
        Task Description: {task_description}

        Return your response in a JSON format like the one in the Component's context: 'Example JSON
        structure' in 'structure', 'Output JSON' in 'output' if it exists and task type into 'type'.
        Only use the properties and parameters described in the context with the task type and name.
        If there is no such task, return the 'missing' Component with a custom label.

        Return your response in this JSON format:
        \\{
            "structure": \\{
                "label": "Name of missing action or trigger",
                "name": "missingComponentName",
                "type": "missing/v1/missing",
                "parameters": \\{\\}
            \\},
            "type": "action",
            "output": \\{\\}
        \\}
        """;

    private final ChatClient chatClient;
    private final String orchestratorPrompt;
    private final String workerPrompt;

    /**
     * Creates a new OrchestratorWorkers with default prompts.
     *
     * @param chatClient The ChatClient to use for LLM interactions
     */
    public OrchestratorWorkersWorkflow(ChatClient chatClient) {
        this(chatClient, DEFAULT_ORCHESTRATOR_PROMPT, DEFAULT_WORKER_PROMPT);
    }

    /**
     * Creates a new OrchestratorWorkers with custom prompts.
     *
     * @param chatClient         The ChatClient to use for LLM interactions
     * @param orchestratorPrompt Custom prompt for the orchestrator LLM
     * @param workerPrompt       Custom prompt for the worker LLMs
     */
    public OrchestratorWorkersWorkflow(ChatClient chatClient, String orchestratorPrompt, String workerPrompt) {
        Assert.notNull(chatClient, "ChatClient must not be null");
        Assert.hasText(orchestratorPrompt, "Orchestrator prompt must not be empty");
        Assert.hasText(workerPrompt, "Worker prompt must not be empty");

        this.chatClient = chatClient;
        this.orchestratorPrompt = orchestratorPrompt;
        this.workerPrompt = workerPrompt;
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
            .user(u -> u.text(this.orchestratorPrompt)
                .param("task", taskDescription)
                .param("workflow", currentWorkflow))
            .call()
            .entity(OrchestratorResponse.class);

        Assert.notNull(orchestratorResponse, "Orchestrator response must not be null");

        // 2. Workers process subtasks in parallel
        List<String> workerResponses = orchestratorResponse.tasks()
            .parallelStream()
            .map(task -> this.chatClient.prompt()
                .user(u -> u.text(this.workerPrompt)
                    .param("original_task", orchestratorResponse.analysis())
                    .param("task_type", task.type())
                    .param("task_description", task.description()))
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
     * @param type        The type or category of the task (e.g., "formal", "conversational")
     * @param description Detailed description of what the worker should accomplish
     */
    private record OrchestratorTask(String type, String description) {
    }

    /**
     * Response from the orchestrator containing task analysis and breakdown into subtasks.
     *
     * @param analysis Detailed explanation of the task and how different approaches serve its aspects
     * @param tasks    List of subtasks identified by the orchestrator to be executed by workers
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    private record OrchestratorResponse(String analysis, List<OrchestratorTask> tasks) {
        public OrchestratorResponse {
            tasks = List.copyOf(tasks);
        }
    }
}
