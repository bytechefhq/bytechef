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

import com.bytechef.atlas.Constants;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * a {@link TaskDispatcher} implementation which handles the 'subflow' task type. Subflows are
 * essentially isolated job instances started by the parent 'subflow' task which is the owner of the
 * sub-flow.
 *
 * @author Arik Cohen
 * @since Sep 06, 2018
 */
public class SubflowTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final MessageBroker messageBroker;

    public SubflowTaskDispatcher(MessageBroker aMessageBroker) {
        messageBroker = aMessageBroker;
    }

    @Override
    public void dispatch(TaskExecution aTask) {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.INPUTS, aTask.getMap(Constants.INPUTS, Collections.emptyMap()));
        params.put(Constants.PARENT_TASK_EXECUTION_ID, aTask.getId());
        params.put(Constants.WORKFLOW_ID, aTask.getRequiredString(Constants.WORKFLOW_ID));
        messageBroker.send(Queues.SUBFLOWS, params);
    }

    @Override
    public TaskDispatcher resolve(Task aTask) {
        if (aTask.getType().equals(Constants.SUBFLOW)) {
            return this;
        }
        return null;
    }
}
