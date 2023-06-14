
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

package com.bytechef.helios.coordinator.instance.manager;

import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.helios.execution.job.factory.ProjectInstanceWorkflowJobFactory;
import com.bytechef.hermes.coordinator.instance.manager.InstanceWorkflowManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectInstanceWorkflowManager implements InstanceWorkflowManager {

    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final ProjectInstanceWorkflowJobFactory projectInstanceWorkflowJobFactory;

    public static final String PROJECT = "PROJECT";

    @SuppressFBWarnings("EI")
    public ProjectInstanceWorkflowManager(
        ProjectInstanceWorkflowService projectInstanceWorkflowService,
        ProjectInstanceWorkflowJobFactory projectInstanceWorkflowJobFactory) {

        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.projectInstanceWorkflowJobFactory = projectInstanceWorkflowJobFactory;
    }

    @Override
    public long createJob(long instanceId, String workflowId) {
        return projectInstanceWorkflowJobFactory.createJob(instanceId, workflowId);
    }

    @Override
    public Map<String, Object> getInputs(long instanceId, String workflowId) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            instanceId, workflowId);

        return projectInstanceWorkflow.getInputs();
    }

    @Override
    public String getType() {
        return PROJECT;
    }
}
