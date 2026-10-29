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

package com.bytechef.platform.configuration.map;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.map.MapDataSource;
import com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Delegates to the shared {@link TaskListOutputDataSource} fallback chain (test output sample -> static
 * component/dispatcher output -> live computation) used by every task dispatcher whose dynamic output mirrors the
 * output of the last task in a task list.
 *
 * @author Ivica Cardic
 */
@Component
class MapDataSourceImpl implements MapDataSource {

    private final TaskListOutputDataSource taskListOutputDataSource;

    MapDataSourceImpl(TaskListOutputDataSource taskListOutputDataSource) {
        this.taskListOutputDataSource = taskListOutputDataSource;
    }

    @Override
    public @Nullable OutputResponse getLastIterateeTaskOutput(
        String workflowId, String lastTaskName, String lastTaskType, long environmentId) {

        return taskListOutputDataSource.getLastTaskOutput(workflowId, lastTaskName, lastTaskType, environmentId);
    }
}
