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

import static com.bytechef.platform.component.constant.WorkflowConstants.NEW_WORKFLOW_CALL;
import static com.bytechef.task.dispatcher.callaiagent.constant.CallAiAgentTaskDispatcherConstants.AGENT_UUID;
import static com.bytechef.task.dispatcher.callaiagent.constant.CallAiAgentTaskDispatcherConstants.CALL_AI_AGENT;
import static com.bytechef.task.dispatcher.callaiagent.constant.CallAiAgentTaskDispatcherConstants.CONVERSATION_ID;
import static com.bytechef.task.dispatcher.callaiagent.constant.CallAiAgentTaskDispatcherConstants.MESSAGE;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource.ResolvedAiAgent;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import com.bytechef.task.dispatcher.subflow.SubflowChildJobLauncher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class CallAiAgentTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final @Nullable CallableAiAgentDataSource callableAiAgentDataSource;
    private final ChildJobPrincipalFactory childJobPrincipalFactory;
    private final JobService jobService;
    private final SubflowResolver subflowResolver;

    @SuppressFBWarnings("EI")
    public CallAiAgentTaskDispatcher(
        ChildJobPrincipalFactory childJobPrincipalFactory,
        @Nullable CallableAiAgentDataSource callableAiAgentDataSource,
        JobService jobService, SubflowResolver subflowResolver) {

        this.callableAiAgentDataSource = callableAiAgentDataSource;
        this.childJobPrincipalFactory = childJobPrincipalFactory;
        this.jobService = jobService;
        this.subflowResolver = subflowResolver;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        if (callableAiAgentDataSource == null) {
            throw new IllegalStateException("Call AI Agent is not available in this deployment");
        }

        Job job = jobService.getJob(Objects.requireNonNull(taskExecution.getJobId()));

        boolean editorEnvironment = MapUtils.getBoolean(job.getMetadata(), MetadataConstants.EDITOR_ENVIRONMENT, false);
        String agentUuid = MapUtils.getRequiredString(taskExecution.getParameters(), AGENT_UUID);

        ResolvedAiAgent resolvedAiAgent = callableAiAgentDataSource.resolveAgent(agentUuid, editorEnvironment);

        Subflow subflow = subflowResolver.resolveSubflow(
            resolvedAiAgent.workflowUuid(), NEW_WORKFLOW_CALL, editorEnvironment);

        Map<String, Object> inputValues = new HashMap<>();

        inputValues.put(MESSAGE, MapUtils.getRequiredString(taskExecution.getParameters(), MESSAGE));

        String conversationId = MapUtils.getString(taskExecution.getParameters(), CONVERSATION_ID);

        if (conversationId != null && !conversationId.isBlank()) {
            inputValues.put(CONVERSATION_ID, conversationId);
        }

        SubflowChildJobLauncher.launch(childJobPrincipalFactory, job, taskExecution, subflow, inputValues);
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), CALL_AI_AGENT + "/v1")) {
            return this;
        }

        return null;
    }
}
