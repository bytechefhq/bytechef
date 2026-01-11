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

package com.bytechef.platform.workflow.test.facade;

import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.workflow.test.dto.ExecutionErrorEventDTO;
import com.bytechef.platform.workflow.test.dto.JobStatusEventDTO;
import com.bytechef.platform.workflow.test.dto.TaskStatusEventDTO;
import com.bytechef.platform.workflow.test.dto.WorkflowTestExecutionDTO;
import java.util.Map;
import java.util.function.Consumer;

/**
 * An interface for managing and testing workflow executions. Provides functionality to monitor workflow execution
 * events, execute workflows in test environments, and manage lifecycle actions such as starting and stopping workflows.
 *
 * @author Ivica Cardic
 */
public interface TestWorkflowExecutor {

    /**
     * Registers a listener to monitor job status updates for a specific job. The listener will be triggered with
     * {@link JobStatusEventDTO} objects whenever the job status changes.
     *
     * @param jobId    The unique identifier of the job whose status updates should be monitored.
     * @param listener The consumer that will process {@link JobStatusEventDTO} instances representing job status
     *                 changes.
     * @return An {@link AutoCloseable} instance that can be used to unregister the listener and stop receiving job
     *         status updates.
     */
    AutoCloseable addJobStatusListener(long jobId, Consumer<JobStatusEventDTO> listener);

    /**
     * Registers a listener to monitor task start events for a specific job. The listener will be triggered with
     * {@link TaskStatusEventDTO} objects whenever a task in the given job transitions to the "STARTED" state.
     *
     * @param jobId    The unique identifier of the job for which task start events should be monitored.
     * @param listener The consumer that will process {@link TaskStatusEventDTO} instances representing task start
     *                 events.
     * @return An {@link AutoCloseable} instance that can be used to unregister the listener and stop receiving task
     *         start notifications.
     */
    AutoCloseable addTaskStartedListener(long jobId, Consumer<TaskStatusEventDTO> listener);

    /**
     * Registers a listener to monitor task execution completion events for a specific job. The listener will be
     * triggered with {@link TaskStatusEventDTO} objects whenever a task in the given job transitions to the "COMPLETED"
     * state.
     *
     * @param jobId    The unique identifier of the job for which task completion events should be monitored.
     * @param listener The consumer that will process {@link TaskStatusEventDTO} instances representing task completion
     *                 events.
     * @return An {@link AutoCloseable} instance that can be used to unregister the listener and stop receiving task
     *         completion notifications.
     */
    AutoCloseable addTaskExecutionCompleteListener(long jobId, Consumer<TaskStatusEventDTO> listener);

    /**
     * Registers a listener to monitor error events for a specific job. The listener will be triggered with
     * {@link ExecutionErrorEventDTO} objects whenever an error occurs during the execution of the specified job.
     *
     * @param jobId    The unique identifier of the job for which error events should be monitored.
     * @param listener The consumer that will process {@link ExecutionErrorEventDTO} instances representing execution
     *                 error events.
     * @return An {@link AutoCloseable} instance that can be used to unregister the listener and stop receiving
     *         execution error notifications.
     */
    AutoCloseable addErrorListener(long jobId, Consumer<ExecutionErrorEventDTO> listener);

    /**
     * Registers an SSE (Server-Sent Events) stream bridge for a specific job. The bridge facilitates the real-time
     * streaming of events associated with the given job.
     *
     * @param jobId  The unique identifier of the job for which the SSE stream bridge is to be added.
     * @param bridge An instance of {@link SseStreamBridge} that handles the streaming of server-sent events for the
     *               specified job.
     * @return An {@link AutoCloseable} instance that can be used to unregister the SSE stream bridge and stop the
     *         streaming of events.
     */
    AutoCloseable addSseStreamBridge(long jobId, SseStreamBridge bridge);

    /**
     * Waits for the result of a workflow execution associated with a specific job ID.
     *
     * @param jobId The unique identifier of the job whose result is being awaited.
     * @return A {@link WorkflowTestExecutionDTO} object containing details about the execution, including job and
     *         trigger execution information.
     */
    WorkflowTestExecutionDTO awaitExecution(long jobId);

    /**
     * Tests the execution of a workflow given its identifier, input parameters, and execution environment.
     *
     * @param workflowId    The unique identifier of the workflow to be tested.
     * @param inputs        A map of input parameters to be supplied to the workflow during execution.
     * @param environmentId The unique identifier of the environment in which the workflow will be executed.
     * @return A {@code WorkflowTestExecutionDTO} object that encapsulates the details of the execution, including the
     *         executed job and any associated trigger execution details.
     */
    WorkflowTestExecutionDTO execute(String workflowId, Map<String, Object> inputs, long environmentId);

    /**
     * Initiates the execution of a workflow with the specified identifier, inputs, and execution environment.
     *
     * @param workflowId    The unique identifier of the workflow to be started.
     * @param inputs        A map of input parameters to be supplied to the workflow during its execution.
     * @param environmentId The unique identifier of the environment in which the workflow will execute.
     * @return The job ID associated with the started workflow execution.
     */
    long start(String workflowId, Map<String, Object> inputs, long environmentId);

    /**
     * Stops the execution of a workflow associated with the specified job ID.
     *
     * @param jobId The unique identifier of the job whose workflow execution is to be stopped.
     */
    void stop(long jobId);
}
