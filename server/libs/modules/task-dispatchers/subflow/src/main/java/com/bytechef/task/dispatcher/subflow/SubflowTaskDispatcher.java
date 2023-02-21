
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.task.dispatcher.subflow;

import static com.bytechef.hermes.task.dispatcher.constant.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.subflow.constant.SubflowTaskDispatcherConstants.SUBFLOW;

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.dto.JobParameters;
import com.bytechef.atlas.facade.JobFacade;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.commons.util.MapValueUtils;
import java.util.Collections;
import java.util.Objects;

/**
 * a {@link TaskDispatcher} implementation which handles the 'subflow' task type. Subflows are essentially isolated job
 * instances started by the parent 'subflow' task which is the owner of the sub-flow.
 *
 * @author Arik Cohen
 * @since Sep 06, 2018
 */
public class SubflowTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final JobFacade jobFacade;

    public SubflowTaskDispatcher(JobFacade jobFacade) {
        this.jobFacade = jobFacade;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        JobParameters jobParameters = new JobParameters(
            MapValueUtils.getMap(taskExecution.getParameters(), WorkflowConstants.INPUTS, Collections.emptyMap()),
            taskExecution.getId(),
            MapValueUtils.getRequiredString(taskExecution.getParameters(), WorkflowConstants.WORKFLOW_ID));

        jobFacade.create(jobParameters);
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), SUBFLOW + "/v" + VERSION_1)) {
            return this;
        }

        return null;
    }
}
