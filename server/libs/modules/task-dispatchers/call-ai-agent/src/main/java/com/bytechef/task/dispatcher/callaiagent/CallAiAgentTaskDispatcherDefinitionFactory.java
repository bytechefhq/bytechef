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

package com.bytechef.task.dispatcher.callaiagent;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.option;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static com.bytechef.task.dispatcher.callaiagent.constant.CallAiAgentTaskDispatcherConstants.AGENT_UUID;
import static com.bytechef.task.dispatcher.callaiagent.constant.CallAiAgentTaskDispatcherConstants.CALL_AI_AGENT;
import static com.bytechef.task.dispatcher.callaiagent.constant.CallAiAgentTaskDispatcherConstants.CONVERSATION_ID;
import static com.bytechef.task.dispatcher.callaiagent.constant.CallAiAgentTaskDispatcherConstants.MESSAGE;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.definition.BaseProperty;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.Option;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource.ResolvedAiAgent;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class CallAiAgentTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private final TaskDispatcherDefinition taskDispatcherDefinition;

    @Autowired
    public CallAiAgentTaskDispatcherDefinitionFactory(
        ObjectProvider<CallableAiAgentDataSource> callableAiAgentDataSourceProvider,
        SubflowDataSource subflowDataSource) {

        this(callableAiAgentDataSourceProvider.getIfAvailable(), subflowDataSource);
    }

    CallAiAgentTaskDispatcherDefinitionFactory(
        @Nullable CallableAiAgentDataSource callableAiAgentDataSource, SubflowDataSource subflowDataSource) {

        this.taskDispatcherDefinition = taskDispatcher(CALL_AI_AGENT)
            .title("Call AI Agent")
            .description("Sends a message to a published AI agent and continues with its reply.")
            .icon("path:assets/callAiAgent.svg")
            .properties(
                string(AGENT_UUID)
                    .label("Agent")
                    .description("The published agent to call.")
                    .optionsFunction(search -> getAgentOptions(callableAiAgentDataSource, search))
                    .required(true),
                string(MESSAGE)
                    .label("Message")
                    .description("The message sent to the agent.")
                    .controlType(Property.ControlType.TEXT_AREA)
                    .required(true),
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description(
                        "Memory-thread key passed to the agent. Left blank, the agent starts or continues whatever "
                            + "thread its own conversationId input resolves to."))
            .output(inputParameters -> output(inputParameters, callableAiAgentDataSource, subflowDataSource));
    }

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return taskDispatcherDefinition;
    }

    private static List<? extends Option<String>> getAgentOptions(
        @Nullable CallableAiAgentDataSource callableAiAgentDataSource, String search) {

        if (callableAiAgentDataSource == null) {
            return List.of();
        }

        return callableAiAgentDataSource.getCallableAgents(search)
            .stream()
            .map(entry -> option(entry.title(), entry.agentUuid()))
            .toList();
    }

    private static @Nullable OutputResponse output(
        Map<String, ?> inputParameters, @Nullable CallableAiAgentDataSource callableAiAgentDataSource,
        SubflowDataSource subflowDataSource) {

        String agentUuid = MapUtils.getString(inputParameters, AGENT_UUID);

        if (callableAiAgentDataSource == null || agentUuid == null || agentUuid.isEmpty()) {
            return null;
        }

        ResolvedAiAgent resolvedAiAgent = callableAiAgentDataSource.resolveAgent(agentUuid, true);

        BaseProperty.BaseValueProperty<?> outputSchema =
            subflowDataSource.getSubWorkflowOutputSchema(resolvedAiAgent.workflowUuid());

        if (outputSchema == null) {
            return null;
        }

        return OutputResponse.of(outputSchema);
    }
}
