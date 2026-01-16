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
import com.bytechef.platform.workflow.test.dto.WorkflowTestExecutionDTO;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An interface for managing and testing workflow executions. Provides functionality to monitor workflow execution
 * events, execute workflows in test environments, and manage lifecycle actions such as starting and stopping workflows.
 *
 * @author Ivica Cardic
 */
public interface TestWorkflowExecutor {

    /**
     * Executes a workflow asynchronously.
     *
     * @param workflowId           The unique identifier of the workflow to execute.
     * @param inputs               A map of input parameters required for the workflow execution.
     * @param environmentId        The ID of the environment in which the workflow runs.
     * @param afterStartCallback   A function that is invoked after the workflow starts, providing an SseStreamBridge.
     * @param afterFutureCallback  A bi-consumer called with the workflow ID and a CompletableFuture representing the
     *                             execution.
     * @param whenCompleteCallback A consumer that is called when the workflow execution is complete with the workflow
     *                             ID.
     */
    void executeAsync(
        String workflowId, Map<String, Object> inputs, long environmentId,
        Consumer<String> afterStartCallback, Function<String, SseStreamBridge> sseStreamBridgeFactory,
        BiConsumer<String, CompletableFuture<WorkflowTestExecutionDTO>> afterFutureCallback,
        Consumer<String> whenCompleteCallback);

    /**
     * Tests the execution of a workflow given its identifier, input parameters, and execution environment.
     *
     * @param workflowId    The unique identifier of the workflow to be tested.
     * @param inputs        A map of input parameters to be supplied to the workflow during execution.
     * @param environmentId The unique identifier of the environment in which the workflow will be executed.
     * @return A {@code WorkflowTestExecutionDTO} object that encapsulates the details of the execution, including the
     *         executed job and any associated trigger execution details.
     */
    WorkflowTestExecutionDTO executeSync(String workflowId, Map<String, Object> inputs, long environmentId);

    /**
     * Stops the execution of a workflow associated with the specified job ID.
     *
     * @param jobId The unique identifier of the job whose workflow execution is to be stopped.
     */
    void stop(long jobId);
}
