
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

package com.bytechef.hermes.project.dto;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.domain.Workflow;
import com.bytechef.hermes.project.domain.Project;
import com.bytechef.hermes.project.domain.ProjectInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProjectExecutionDTO(
    ProjectInstance instance, Job job, Project project, List<TaskExecution> taskExecutions,
    Workflow workflow) {
}
