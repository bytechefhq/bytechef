
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

package com.bytechef.atlas.facade;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.dto.TaskExecutionDTO;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.commons.util.CollectionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public class TaskExecutionFacadeImpl implements TaskExecutionFacade {

    private final ContextService contextService;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public TaskExecutionFacadeImpl(ContextService contextService, TaskExecutionService taskExecutionService) {
        this.contextService = contextService;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public TaskExecutionDTO getTaskExecution(long id) {
        return new TaskExecutionDTO(
            contextService.peek(id, Context.Classname.TASK_EXECUTION),
            taskExecutionService.getTaskExecution(id));
    }

    @Override
    @SuppressFBWarnings("NP")
    public List<TaskExecutionDTO> getJobTaskExecutions(long jobId) {
        return CollectionUtils.map(
            taskExecutionService.getJobTaskExecutions(jobId),
            taskExecution -> new TaskExecutionDTO(
                contextService.peek(taskExecution.getId(), Context.Classname.TASK_EXECUTION),
                taskExecutionService.getTaskExecution(taskExecution.getId())));
    }
}
