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

import java.util.List;
import java.util.Map;

/**
 * Facade for executing AI agent test actions. Resolves workflow task parameters, evaluates expressions, resolves
 * connections and extensions, and delegates to the action definition for execution.
 *
 * @author Ivica Cardic
 */
public interface AiAgentTestFacade {

    /**
     * Executes an AI agent action for testing purposes.
     *
     * @param workflowId       the workflow identifier
     * @param workflowNodeName the name of the workflow node (task) to execute
     * @param environmentId    the environment identifier
     * @param conversationId   the conversation identifier for the AI agent
     * @param message          the user prompt message
     * @param attachments      the list of attachments
     * @return the result of the action execution
     */
    Object executeAiAgentAction(
        String workflowId, String workflowNodeName, long environmentId, String conversationId, String message,
        List<Object> attachments);

    /**
     * Executes an AI agent action for testing purposes with tool simulations.
     *
     * @param workflowId       the workflow identifier
     * @param workflowNodeName the name of the workflow node (task) to execute
     * @param environmentId    the environment identifier
     * @param conversationId   the conversation identifier for the AI agent
     * @param message          the user prompt message
     * @param attachments      the list of attachments
     * @param toolSimulations  map of tool name to simulated response prompt
     * @return the result of the action execution
     */
    Object executeAiAgentAction(
        String workflowId, String workflowNodeName, long environmentId, String conversationId, String message,
        List<Object> attachments, Map<String, String> toolSimulations);
}
