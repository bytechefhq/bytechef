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

package com.bytechef.platform.workflow.task.dispatcher.output;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.map.MapDataSource;
import org.jspecify.annotations.Nullable;

/**
 * Design-time output resolution for task dispatchers whose dynamic output mirrors the output of the last task in a task
 * list (e.g. {@code condition}'s {@code caseTrue} list).
 *
 * <p>
 * {@code WORKFLOW_ID}/{@code ENVIRONMENT_ID} reuse {@link MapDataSource}'s constants: whoever computes dynamic output
 * for a task dispatcher (see {@code WorkflowNodeOutputFacadeImpl#getWorkflowTaskDispatcherDynamicOutputResponse})
 * injects those two keys into {@code inputParameters} generically, for every dispatcher type, not just {@code map} - so
 * any dispatcher's {@code .output(...)} function must read them back under the same keys.
 *
 * @author Ivica Cardic
 */
public interface TaskListOutputDataSource {

    String ENVIRONMENT_ID = MapDataSource.ENVIRONMENT_ID;
    String WORKFLOW_ID = MapDataSource.WORKFLOW_ID;

    @Nullable
    OutputResponse getLastTaskOutput(String workflowId, String lastTaskName, String lastTaskType, long environmentId);
}
