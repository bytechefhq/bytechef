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

package com.bytechef.task.dispatcher.subflow;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds and launches the child job a parent task waits on.
 *
 * @author Ivica Cardic
 */
public final class SubflowChildJobLauncher {

    private SubflowChildJobLauncher() {
    }

    public static void launch(
        ChildJobPrincipalFactory childJobPrincipalFactory, Job job, TaskExecution taskExecution, Subflow subflow,
        Map<String, ?> inputValues) {

        String workflowId = subflow.workflowId();

        if (workflowId == null || workflowId.isEmpty()) {
            throw new IllegalStateException(
                "Resolved subflow has an empty workflow ID for parent job id '%s' and parent task execution id '%s'"
                    .formatted(job.getId(), taskExecution.getId()));
        }

        Map<String, Object> inputs = new HashMap<>();

        inputs.put(subflow.inputsName(), inputValues);
        inputs.put(JobInputConstants.TRIGGER_NAME_INPUT, subflow.inputsName());

        Map<String, Object> childMetadata = new HashMap<>(job.getMetadata());

        SubflowRequestConstants.AGENT_LINK_KEYS.forEach(childMetadata::remove);

        JobParametersDTO jobParametersDTO = new JobParametersDTO(
            workflowId, taskExecution.getId(), inputs, null, null, List.of(), childMetadata);

        childJobPrincipalFactory.createChildJob(Objects.requireNonNull(job.getId()), jobParametersDTO);
    }
}
