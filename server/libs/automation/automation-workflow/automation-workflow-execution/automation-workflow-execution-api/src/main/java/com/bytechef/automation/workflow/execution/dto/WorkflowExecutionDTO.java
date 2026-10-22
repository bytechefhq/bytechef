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

package com.bytechef.automation.workflow.execution.dto;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.platform.workflow.execution.dto.FocusPathElementDTO;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record WorkflowExecutionDTO(
    long id, Project project, ProjectDeployment projectDeployment, @Nullable Integer projectVersion,
    @Nullable JobDTO job, Workflow workflow, @Nullable TriggerExecutionDTO triggerExecution,
    @Nullable List<FocusPathElementDTO> focusPath, @Nullable Long taskExecutionCount) {

    public WorkflowExecutionDTO(
        long id, Project project, ProjectDeployment projectDeployment, @Nullable Integer projectVersion,
        @Nullable JobDTO job, Workflow workflow, @Nullable TriggerExecutionDTO triggerExecution) {

        this(id, project, projectDeployment, projectVersion, job, workflow, triggerExecution, null, null);
    }

    public boolean isTriggerOnly() {
        return job == null;
    }
}
