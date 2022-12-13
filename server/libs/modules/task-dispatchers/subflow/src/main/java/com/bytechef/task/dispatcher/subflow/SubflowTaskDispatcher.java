
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

import static com.bytechef.hermes.task.dispatcher.constants.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.subflow.constants.SubflowTaskDispatcherConstants.SUBFLOW;

import com.bytechef.atlas.constants.WorkflowConstants;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.commons.utils.MapUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * a {@link TaskDispatcher} implementation which handles the 'subflow' task type. Subflows are essentially isolated job
 * instances started by the parent 'subflow' task which is the owner of the sub-flow.
 *
 * @author Arik Cohen
 * @since Sep 06, 2018
 */
public class SubflowTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final MessageBroker messageBroker;

    public SubflowTaskDispatcher(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        Map<String, Object> params = new HashMap<>();

        params.put(
            WorkflowConstants.INPUTS,
            MapUtils.getMap(taskExecution.getParameters(), WorkflowConstants.INPUTS, Collections.emptyMap()));
        params.put(WorkflowConstants.PARENT_TASK_EXECUTION_ID, taskExecution.getId());
        params.put(
            WorkflowConstants.WORKFLOW_ID,
            MapUtils.getRequiredString(taskExecution.getParameters(), WorkflowConstants.WORKFLOW_ID));

        messageBroker.send(Queues.SUBFLOWS, params);
    }

    @Override
    public TaskDispatcher resolve(Task task) {
        if (task.getType()
            .equals(SUBFLOW + "/v" + VERSION_1)) {
            return this;
        }

        return null;
    }
}
