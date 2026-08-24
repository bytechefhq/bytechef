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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for handling workflow tasks.
 *
 * @author Ivica Cardic
 */
public class WorkflowTaskUtils {

    private static final Object REMOVED = new Object();

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

                        if (firstItem instanceof Map<?, ?> map && isWorkflowTaskMap(map)) {

                            List<WorkflowTask> curWorkflowTasks = curList.stream()
                                .map(item -> new WorkflowTask((Map<String, ?>) item))
                                .toList();

                            returnedWorkflowTasks.addAll(getTasks(curWorkflowTasks, lastWorkflowNodeName));
                        } else if (firstItem instanceof Map<?, ?> map &&
                            map.containsKey(WorkflowConstants.TASKS)) {

                            for (Object curItem : curList) {
                                Map<String, ?> curMap = (Map<String, ?>) curItem;

                                List<WorkflowTask> curWorkflowTasks = MapUtils
                                    .getList(curMap, WorkflowConstants.TASKS, List.of())
                                    .stream()
                                    .map(WorkflowTaskUtils::toWorkflowTask)
                                    .toList();

                                returnedWorkflowTasks.addAll(getTasks(curWorkflowTasks, lastWorkflowNodeName));
                            }
                            // Fork/join support
                        } else if (firstItem instanceof List<?> list && !list.isEmpty() &&
                            list.getFirst() instanceof Map<?, ?> map && isWorkflowTaskMap(map)) {

                            for (Object curItem : curList) {
                                List<?> curSubList = (List<?>) curItem;

                                List<WorkflowTask> curWorkflowTasks = curSubList.stream()
                                    .map(item -> new WorkflowTask((Map<String, ?>) item))
                                    .toList();

                                returnedWorkflowTasks.addAll(getTasks(curWorkflowTasks, lastWorkflowNodeName));
                            }
                        }
                    }
                    // Each support
                } else if (entry.getValue() instanceof Map<?, ?> curMap && isWorkflowTaskMap(curMap)) {

                    returnedWorkflowTasks.addAll(
                        getTasks(List.of(new WorkflowTask((Map<String, ?>) curMap)), lastWorkflowNodeName));
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

    @SuppressWarnings("unchecked")
    private static WorkflowTask toWorkflowTask(Object item) {
        if (item instanceof WorkflowTask workflowTask) {
            return workflowTask;
        }

        return new WorkflowTask((Map<String, ?>) item);
    }

    public static List<WorkflowTask> removeDisabledTasks(List<WorkflowTask> workflowTasks) {
        List<WorkflowTask> resultWorkflowTasks = new ArrayList<>();

        for (WorkflowTask workflowTask : workflowTasks) {
            if (workflowTask.isDisabled()) {
                continue;
            }

            Map<String, ?> taskMap = workflowTask.toMap();
            Map<String, Object> strippedTaskMap = removeDisabledTasksFromMap(taskMap);

            if (strippedTaskMap.equals(taskMap)) {
                resultWorkflowTasks.add(workflowTask);
            } else {
                resultWorkflowTasks.add(
                    new WorkflowTask(preserveNonMapFields(workflowTask, strippedTaskMap)));
            }
        }

        return resultWorkflowTasks;
    }

    public static List<String> getDisabledTaskNames(List<WorkflowTask> workflowTasks) {
        List<String> disabledTaskNames = new ArrayList<>();

        for (WorkflowTask workflowTask : workflowTasks) {
            collectDisabledTaskNames(workflowTask.toMap(), workflowTask.isDisabled(), disabledTaskNames);
        }

        return disabledTaskNames;
    }

    public static boolean isEmptiedByDisabledTasks(WorkflowTask workflowTask, String parameterKey) {
        List<String> parameterKeys = MapUtils.getList(
            workflowTask.getMetadata(), WorkflowConstants.DISABLED_TASKS_REMOVED_FROM, String.class, List.of());

        return parameterKeys.contains(parameterKey);
    }

    private static Map<String, Object> removeDisabledTasksFromMap(Map<String, ?> taskMap) {
        Map<String, Object> resultMap = new LinkedHashMap<>();

        for (Map.Entry<String, ?> entry : taskMap.entrySet()) {
            Object strippedValue = removeDisabledTasksFromValue(entry.getValue());

            if (strippedValue == REMOVED) {
                continue;
            }

            resultMap.put(entry.getKey(), strippedValue);
        }

        List<String> changedParameterKeys = getChangedParameterKeys(
            taskMap.get(WorkflowConstants.PARAMETERS), resultMap.get(WorkflowConstants.PARAMETERS));

        if (!changedParameterKeys.isEmpty()) {
            Map<String, Object> metadata = new LinkedHashMap<>();

            if (taskMap.get(WorkflowConstants.METADATA) instanceof Map<?, ?> existingMetadata) {
                for (Map.Entry<?, ?> entry : existingMetadata.entrySet()) {
                    metadata.put((String) entry.getKey(), entry.getValue());
                }
            }

            metadata.put(WorkflowConstants.DISABLED_TASKS_REMOVED_FROM, changedParameterKeys);

            resultMap.put(WorkflowConstants.METADATA, metadata);
        }

        return resultMap;
    }

    private static List<String> getChangedParameterKeys(Object parameters, Object strippedParameters) {
        List<String> changedParameterKeys = new ArrayList<>();

        if (!(parameters instanceof Map<?, ?> parametersMap) ||
            !(strippedParameters instanceof Map<?, ?> strippedParametersMap)) {

            return changedParameterKeys;
        }

        for (Map.Entry<?, ?> entry : parametersMap.entrySet()) {
            if (!Objects.equals(entry.getValue(), strippedParametersMap.get(entry.getKey()))) {
                changedParameterKeys.add((String) entry.getKey());
            }
        }

        return changedParameterKeys;
    }

    @SuppressWarnings("unchecked")
    private static Object removeDisabledTasksFromValue(Object value) {
        if (value instanceof WorkflowTask workflowTask) {
            if (workflowTask.isDisabled()) {
                return REMOVED;
            }

            Map<String, ?> taskMap = workflowTask.toMap();
            Map<String, Object> strippedTaskMap = removeDisabledTasksFromMap(taskMap);

            if (strippedTaskMap.equals(taskMap)) {
                return workflowTask;
            }

            return preserveNonMapFields(workflowTask, strippedTaskMap);
        } else if (value instanceof List<?> list) {
            List<Object> resultList = new ArrayList<>();

            for (Object item : list) {
                Object strippedItem = removeDisabledTasksFromValue(item);

                if (strippedItem != REMOVED) {
                    resultList.add(strippedItem);
                }
            }

            return resultList;
        } else if (value instanceof Map<?, ?> map) {
            if (isWorkflowTaskMap(map)) {
                if (Boolean.TRUE.equals(map.get(WorkflowConstants.DISABLED))) {
                    return REMOVED;
                }

                return removeDisabledTasksFromMap((Map<String, ?>) map);
            }

            Map<Object, Object> resultMap = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object strippedValue = removeDisabledTasksFromValue(entry.getValue());

                if (strippedValue != REMOVED) {
                    resultMap.put(entry.getKey(), strippedValue);
                }
            }

            return resultMap;
        }

        return value;
    }

    private static Map<String, Object> preserveNonMapFields(WorkflowTask workflowTask, Map<String, Object> taskMap) {
        if (workflowTask.getMaxRetries() != 0) {
            taskMap.put(WorkflowConstants.MAX_RETRIES, workflowTask.getMaxRetries());
        }

        if (workflowTask.getTaskNumber() != 0) {
            taskMap.put(WorkflowConstants.TASK_NUMBER, workflowTask.getTaskNumber());
        }

        return taskMap;
    }

    private static void collectDisabledTaskNames(
        Map<?, ?> taskMap, boolean ancestorDisabled, List<String> disabledTaskNames) {

        boolean disabled = ancestorDisabled || Boolean.TRUE.equals(taskMap.get(WorkflowConstants.DISABLED));

        if (disabled && taskMap.get(WorkflowConstants.NAME) instanceof String name) {
            disabledTaskNames.add(name);
        }

        for (Object value : taskMap.values()) {
            collectDisabledTaskNamesFromValue(value, disabled, disabledTaskNames);
        }
    }

    private static void collectDisabledTaskNamesFromValue(
        Object value, boolean ancestorDisabled, List<String> disabledTaskNames) {

        if (value instanceof WorkflowTask workflowTask) {
            collectDisabledTaskNames(workflowTask.toMap(), ancestorDisabled, disabledTaskNames);
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                collectDisabledTaskNamesFromValue(item, ancestorDisabled, disabledTaskNames);
            }
        } else if (value instanceof Map<?, ?> map) {
            if (isWorkflowTaskMap(map)) {
                collectDisabledTaskNames(map, ancestorDisabled, disabledTaskNames);
            } else {
                for (Object mapValue : map.values()) {
                    collectDisabledTaskNamesFromValue(mapValue, ancestorDisabled, disabledTaskNames);
                }
            }
        }
    }

    private static boolean isWorkflowTaskMap(Map<?, ?> map) {
        if (!map.containsKey(WorkflowConstants.NAME) || !map.containsKey(WorkflowConstants.TYPE)) {
            return false;
        }

        // Workflow task types always follow the format: componentName/vVersion[/operation].
        // This distinguishes real tasks from user-defined data structures that happen to have
        // name/type keys (e.g., PostgreSQL column definitions like {"name": "ime", "type": "STRING"}).
        return map.get(WorkflowConstants.TYPE) instanceof String type && isWorkflowNodeType(type);
    }

    private static boolean isWorkflowNodeType(String type) {
        String[] parts = type.split("/", -1);

        if (parts.length < 2 || parts.length > 3) {
            return false;
        }

        if (parts[0].isEmpty() || !parts[1].matches("v\\d+")) {
            return false;
        }

        return parts.length == 2 || !parts[2].isEmpty();
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
