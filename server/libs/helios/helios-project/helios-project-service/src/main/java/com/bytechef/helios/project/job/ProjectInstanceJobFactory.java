
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

package com.bytechef.helios.project.job;

import com.bytechef.atlas.dto.JobParameters;
import com.bytechef.atlas.facade.JobFacade;
import com.bytechef.helios.project.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.project.service.ProjectInstanceWorkflowService;
import com.bytechef.hermes.job.InstanceJobFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ProjectInstanceJobFactory implements InstanceJobFactory {

    private final JobFacade jobFacade;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectInstanceJobFactory(
        JobFacade jobFacade, ProjectInstanceWorkflowService projectInstanceWorkflowService) {

        this.jobFacade = jobFacade;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public long createJob(String workflowId, long instanceId) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            workflowId, instanceId);

        long jobId = jobFacade.create(new JobParameters(projectInstanceWorkflow.getInputs(), workflowId));

        projectInstanceWorkflowService.addJob(Objects.requireNonNull(projectInstanceWorkflow.getId()), jobId);

        return jobId;
    }

    @Override
    public String getType() {
        return "PROJECT";
    }
}
