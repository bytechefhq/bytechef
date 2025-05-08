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

package com.bytechef.atlas.configuration.util;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.commons.util.MapUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for handling workflow tasks.
 *
 * @author Ivica Cardic
 */
public class WorkflowTaskUtils {

    @SuppressWarnings("unchecked")
    public static List<WorkflowTask> getTasks(List<WorkflowTask> workflowTasks, String lastWorkflowNodeName) {
        List<WorkflowTask> resultWorkflowTasks = new ArrayList<>();

        for (WorkflowTask workflowTask : workflowTasks) {
            List<WorkflowTask> returnedWorkflowTasks = new ArrayList<>();
            Map<String, ?> parameters = workflowTask.getParameters();

            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                if (entry.getValue() instanceof WorkflowTask curWorkflowTask) {
                    returnedWorkflowTasks.addAll(getTasks(List.of(curWorkflowTask), lastWorkflowNodeName));
                } else if (entry.getValue() instanceof List<?> curList) {
                    if (!curList.isEmpty()) {
                        Object firstItem = curList.getFirst();

                        if (firstItem instanceof WorkflowTask) {
                            List<WorkflowTask> curWorkflowTasks = curList.stream()
                                .map(item -> (WorkflowTask) item)
                                .toList();

                            returnedWorkflowTasks.addAll(getTasks(curWorkflowTasks, lastWorkflowNodeName));
                        }

                        if (firstItem instanceof Map<?, ?> map && map.containsKey(WorkflowConstants.PARAMETERS) &&
                            map.containsKey(WorkflowConstants.TYPE)) {

                            List<WorkflowTask> curWorkflowTasks = curList.stream()
                                .map(item -> new WorkflowTask((Map<String, ?>) item))
                                .toList();

                            returnedWorkflowTasks.addAll(getTasks(curWorkflowTasks, lastWorkflowNodeName));
                        } else if (firstItem instanceof Map<?, ?> map &&
                            map.containsKey(WorkflowConstants.TASKS)) {

                            for (Object curItem : curList) {
                                Map<String, ?> curMap = (Map<String, ?>) curItem;

                                List<WorkflowTask> curWorkflowTasks = MapUtils.getList(
                                    curMap, WorkflowConstants.TASKS, WorkflowTask.class, List.of());

                                returnedWorkflowTasks.addAll(getTasks(curWorkflowTasks, lastWorkflowNodeName));
                            }
                        }
                    }
                } else if (entry.getValue() instanceof Map<?, ?> curMap) {
                    for (Map.Entry<?, ?> curMapEntry : curMap.entrySet()) {
                        if (curMapEntry.getValue() instanceof WorkflowTask curWorkflowTask) {
                            returnedWorkflowTasks.addAll(getTasks(List.of(curWorkflowTask), lastWorkflowNodeName));
                        }
                    }
                }
            }

            if (lastWorkflowNodeName == null) {
                resultWorkflowTasks.add(workflowTask);
                resultWorkflowTasks.addAll(returnedWorkflowTasks);
            } else {
                if (!returnedWorkflowTasks.isEmpty() ||
                    Objects.equals(workflowTask.getName(), lastWorkflowNodeName)) {

                    resultWorkflowTasks.addAll(getPrevious(workflowTasks, workflowTask.getName()));
                    resultWorkflowTasks.addAll(returnedWorkflowTasks);
                }

                if (Objects.equals(workflowTask.getName(), lastWorkflowNodeName)) {
                    return resultWorkflowTasks;
                }
            }
        }

        return resultWorkflowTasks;
    }

    private static List<WorkflowTask> getPrevious(List<WorkflowTask> workflowTasks, String workflowTaskName) {
        List<WorkflowTask> previousWorkflowTasks = new ArrayList<>();

        for (WorkflowTask curWorkflowTask : workflowTasks) {
            previousWorkflowTasks.add(curWorkflowTask);

            if (Objects.equals(curWorkflowTask.getName(), workflowTaskName)) {
                break;
            }
        }

        return previousWorkflowTasks;
    }
}
