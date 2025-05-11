package com.bytechef.platform.ai.facade;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.Assert;

import java.util.List;

public class OrchestratorWorkers {

    private final ChatClient chatClient;
    private final String orchestratorPrompt;
    private final String workerPrompt;

    public static final String DEFAULT_ORCHESTRATOR_PROMPT = """
			Analyze this task and break it down into subtasks so that the first subtask is always a single Trigger, and each subsequent subtask is either an Action or a Flow. Use the 'condition' flow for the 'if' function and 'loop' flow for the 'for each' function.

			Task: {task}

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

    public static final String DEFAULT_WORKER_PROMPT = """
			Search the context for an existing Component that best matches the tasks description.

			Generate content based on:
			Original task: {original_task}
			Task Type: {task_type}
			Task Description: {task_description}

            Return your response in a JSON format like the one in the Component's context: 'Example JSON structure' in 'structure', 'Output JSON' in 'output' if it exists and task type into 'type'. Only use the properties and parameters described in the context with the task type and name.
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


    /**
     * Represents a subtask identified by the orchestrator that needs to be executed
     * by a worker.
     *
     * @param type        The type or category of the task (e.g., "formal",
     *                    "conversational")
     * @param description Detailed description of what the worker should accomplish
     */
    public static record OrchestratorTask(String type, String description) {
    }

    /**
     * Response from the orchestrator containing task analysis and breakdown into
     * subtasks.
     *
     * @param analysis Detailed explanation of the task and how different approaches
     *                 serve its aspects
     * @param tasks    List of subtasks identified by the orchestrator to be
     *                 executed by workers
     */
    public static record OrchestratorResponse(String analysis, List<OrchestratorTask> tasks) {
    }

    /**
     * Final response containing the orchestrator's analysis and combined worker
     * outputs.
     *
     * @param analysis        The orchestrator's understanding and breakdown of the
     *                        original task
     * @param workerResponses List of responses from workers, each handling a
     *                        specific subtask
     */
    public static record FinalResponse(String analysis, List<String> workerResponses) {
    }

    /**
     * Creates a new OrchestratorWorkers with default prompts.
     *
     * @param chatClient The ChatClient to use for LLM interactions
     */
    public OrchestratorWorkers(ChatClient chatClient) {
        this(chatClient, DEFAULT_ORCHESTRATOR_PROMPT, DEFAULT_WORKER_PROMPT);
    }

    /**
     * Creates a new OrchestratorWorkers with custom prompts.
     *
     * @param chatClient         The ChatClient to use for LLM interactions
     * @param orchestratorPrompt Custom prompt for the orchestrator LLM
     * @param workerPrompt       Custom prompt for the worker LLMs
     */
    public OrchestratorWorkers(ChatClient chatClient, String orchestratorPrompt, String workerPrompt) {
        Assert.notNull(chatClient, "ChatClient must not be null");
        Assert.hasText(orchestratorPrompt, "Orchestrator prompt must not be empty");
        Assert.hasText(workerPrompt, "Worker prompt must not be empty");

        this.chatClient = chatClient;
        this.orchestratorPrompt = orchestratorPrompt;
        this.workerPrompt = workerPrompt;
    }

    /**
     * Processes a task using the orchestrator-workers pattern.
     * First, the orchestrator analyzes the task and breaks it down into subtasks.
     * Then, workers execute each subtask in parallel.
     * Finally, the results are combined into a single response.
     *
     * @param taskDescription Description of the task to be processed
     * @return WorkerResponse containing the orchestrator's analysis and combined
     *         worker outputs
     * @throws IllegalArgumentException if taskDescription is null or empty
     */
    @SuppressWarnings("null")
    public FinalResponse process(String taskDescription) {
        Assert.hasText(taskDescription, "Task description must not be empty");

        // Step 1: Get orchestrator response
        OrchestratorResponse orchestratorResponse = this.chatClient.prompt()
            .user(u -> u.text(this.orchestratorPrompt)
                .param("task", taskDescription))
            .call()
            .entity(OrchestratorResponse.class);

        System.out.println(String.format("\n=== ORCHESTRATOR OUTPUT ===\nANALYSIS: %s\n\nTASKS: %s\n",
            orchestratorResponse.analysis(), orchestratorResponse.tasks()));

        // Step 2: Process each task
        List<String> workerResponses = orchestratorResponse.tasks()
            .stream()
            .map(task -> this.chatClient.prompt()
            .user(u -> u.text(this.workerPrompt)
                .param("original_task", orchestratorResponse.analysis())
                .param("task_type", task.type())
                .param("task_description", task.description()))
            .call()
            .content())
            .toList();

        System.out.println("\n=== WORKER OUTPUT ===\n" + workerResponses);

        return new FinalResponse(orchestratorResponse.analysis(), workerResponses);
    }

}
