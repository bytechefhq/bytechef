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

import com.bytechef.platform.coordinator.job.JobSyncExecutor;
import com.bytechef.platform.workflow.test.dto.ExecutionErrorEventDTO;
import com.bytechef.platform.workflow.test.dto.JobStatusEventDTO;
import com.bytechef.platform.workflow.test.dto.TaskStatusEventDTO;
import com.bytechef.platform.workflow.test.dto.WorkflowTestExecutionDTO;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Ivica Cardic
 */
public interface WorkflowTestFacade {

    /**
     * Tests the execution of a workflow given its identifier, input parameters, and execution environment.
     *
     * @param workflowId    The unique identifier of the workflow to be tested.
     * @param inputs        A map of input parameters to be supplied to the workflow during execution.
     * @param environmentId The unique identifier of the environment in which the workflow will be executed.
     * @return A {@code WorkflowTestExecutionDTO} object that encapsulates the details of the test execution, including
     *         the executed job and any associated trigger execution details.
     */
    WorkflowTestExecutionDTO testWorkflow(String workflowId, Map<String, Object> inputs, long environmentId);

    /**
     * Initiates the test workflow execution for the specified workflow ID and environment.
     *
     * @param workflowId    The unique identifier of the workflow to be tested.
     * @param inputs        A map of input parameters required to execute the workflow.
     * @param environmentId The identifier of the environment in which the workflow is to be tested.
     * @return The unique job ID assigned to the test workflow execution.
     */
    long startTestWorkflow(String workflowId, Map<String, Object> inputs, long environmentId);

    /**
     * Waits for the result of a test workflow execution associated with a specific job ID.
     *
     * @param jobId The unique identifier of the job whose test result is being awaited.
     * @return A {@link WorkflowTestExecutionDTO} object containing details about the test execution, including job and
     *         trigger execution information.
     */
    WorkflowTestExecutionDTO awaitTestResult(long jobId);

    /**
     * Attempts to stop the running test job with the given job id.
     *
     * @param jobId The job identifier
     */
    void stopTest(long jobId);

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
     * @param bridge An instance of {@link JobSyncExecutor.SseStreamBridge} that handles the streaming of server-sent
     *               events for the specified job.
     * @return An {@link AutoCloseable} instance that can be used to unregister the SSE stream bridge and stop the
     *         streaming of events.
     */
    AutoCloseable addSseStreamBridge(long jobId, JobSyncExecutor.SseStreamBridge bridge);
}
