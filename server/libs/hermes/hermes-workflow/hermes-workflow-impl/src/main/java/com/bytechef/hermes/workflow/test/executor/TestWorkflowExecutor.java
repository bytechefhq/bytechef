
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.workflow.test.executor;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.job.JobParameters;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.sync.executor.JobSyncExecutor;
import com.bytechef.hermes.workflow.test.dto.WorkflowResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class TestWorkflowExecutor implements WorkflowExecutor {

    private final JobSyncExecutor jobSyncExecutor;
    private final TaskExecutionService taskExecutionService;

    public TestWorkflowExecutor(JobSyncExecutor jobSyncExecutor, TaskExecutionService taskExecutionService) {
        this.jobSyncExecutor = jobSyncExecutor;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public WorkflowResponse execute(String workflowId) {
        return execute(workflowId, Map.of());
    }

    @Override
    @SuppressFBWarnings("NP")
    public WorkflowResponse execute(String workflowId, Map<String, Object> inputs) {
        Job job = jobSyncExecutor.execute(new JobParameters(inputs, workflowId));

        return new WorkflowResponse(job, taskExecutionService.getJobTaskExecutions(job.getId()));
    }
}
