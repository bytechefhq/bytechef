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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowTaskUtilsTest {

    @Test
    void testFlattenConditionCaseTrueStartingWithTaskWithoutParameters() {
        WorkflowTask condition = new WorkflowTask(Map.of(
            "name", "condition_1",
            "type", "condition/v1",
            "parameters", Map.of(
                "rawExpression", true,
                "caseTrue", List.of(
                    Map.of("name", "logger_1", "type", "logger/v1/info"),
                    Map.of(
                        "name", "dataStorage_1",
                        "type", "dataStorage/v1/setValue",
                        "parameters", Map.of("key", "k"))),
                "caseFalse", List.of(
                    Map.of(
                        "name", "logger_2",
                        "type", "logger/v1/info",
                        "parameters", Map.of("text", "t"))))));

        Set<String> names = WorkflowTaskUtils.getTasks(List.of(condition), null)
            .stream()
            .map(WorkflowTask::getName)
            .collect(Collectors.toSet());

        assertEquals(Set.of("condition_1", "logger_1", "dataStorage_1", "logger_2"), names);
    }

    @Test
    void testFlattenEachIterateeWithoutParameters() {
        WorkflowTask each = new WorkflowTask(Map.of(
            "name", "each_1",
            "type", "each/v1",
            "parameters", Map.of(
                "iteratee", Map.of("name", "logger_1", "type", "logger/v1/info"))));

        Set<String> names = WorkflowTaskUtils.getTasks(List.of(each), null)
            .stream()
            .map(WorkflowTask::getName)
            .collect(Collectors.toSet());

        assertEquals(Set.of("each_1", "logger_1"), names);
    }

    @Test
    void testFlattenConditionWithBothCasesTaskWithoutParameters() {
        WorkflowTask condition = new WorkflowTask(Map.of(
            "name", "condition_1",
            "type", "condition/v1",
            "parameters", Map.of(
                "rawExpression", true,
                "caseTrue", List.of(Map.of("name", "logger_1", "type", "logger/v1/info")),
                "caseFalse", List.of(Map.of("name", "logger_2", "type", "logger/v1/info")))));

        Set<String> names = WorkflowTaskUtils.getTasks(List.of(condition), null)
            .stream()
            .map(WorkflowTask::getName)
            .collect(Collectors.toSet());

        assertEquals(Set.of("condition_1", "logger_1", "logger_2"), names);
    }

    @Test
    void testFlattenForkJoinBranchStartingWithTaskWithoutParameters() {
        WorkflowTask forkJoin = new WorkflowTask(Map.of(
            "name", "forkJoin_1",
            "type", "fork-join/v1",
            "parameters", Map.of(
                "branches", List.of(
                    List.of(
                        Map.of("name", "logger_1", "type", "logger/v1/info"),
                        Map.of(
                            "name", "dataStorage_1",
                            "type", "dataStorage/v1/setValue",
                            "parameters", Map.of("key", "k"))),
                    List.of(
                        Map.of(
                            "name", "logger_2",
                            "type", "logger/v1/info",
                            "parameters", Map.of("text", "t")))))));

        Set<String> names = WorkflowTaskUtils.getTasks(List.of(forkJoin), null)
            .stream()
            .map(WorkflowTask::getName)
            .collect(Collectors.toSet());

        assertEquals(Set.of("forkJoin_1", "logger_1", "dataStorage_1", "logger_2"), names);
    }

    @Test
    void testNonTaskListWithTypeKeyIsIgnored() {
        // A parameter value that's List<Map> whose maps carry `type` but no `name`
        // must not be mistaken for tasks (e.g., condition-builder conditions,
        // schema descriptors).
        WorkflowTask task = new WorkflowTask(Map.of(
            "name", "dataStorage_1",
            "type", "dataStorage/v1/setValue",
            "parameters", Map.of(
                "descriptors", List.of(
                    Map.of("type", "STRING", "operation", "EQUALS", "value1", "a"),
                    Map.of("type", "NUMBER", "operation", "GREATER", "value1", "1")))));

        Set<String> names = WorkflowTaskUtils.getTasks(List.of(task), null)
            .stream()
            .map(WorkflowTask::getName)
            .collect(Collectors.toSet());

        assertEquals(Set.of("dataStorage_1"), names);
    }

    @Test
    void testNonTaskListWithNameAndTypeKeysIsIgnored() {
        // A parameter value that's List<Map> whose maps carry BOTH `name` and `type`
        // must not be mistaken for tasks if the `type` value isn't a valid workflow node
        // type (e.g., PostgreSQL column definitions use {"name": "col", "type": "STRING"}).
        WorkflowTask task = new WorkflowTask(Map.of(
            "name", "postgresql_1",
            "type", "postgresql/v1/insert",
            "parameters", Map.of(
                "schema", "public",
                "columns", List.of(
                    Map.of("type", "STRING", "name", "ime"),
                    Map.of("type", "NUMBER", "name", "age")))));

        Set<String> names = WorkflowTaskUtils.getTasks(List.of(task), null)
            .stream()
            .map(WorkflowTask::getName)
            .collect(Collectors.toSet());

        assertEquals(Set.of("postgresql_1"), names);
    }

    @Test
    void testFlattenLoopIterateeStartingWithTaskWithoutParameters() {
        WorkflowTask loop = new WorkflowTask(Map.of(
            "name", "loop_1",
            "type", "loop/v1",
            "parameters", Map.of(
                "iteratee", List.of(
                    Map.of("name", "logger_1", "type", "logger/v1/info"),
                    Map.of(
                        "name", "dataStorage_1",
                        "type", "dataStorage/v1/setValue",
                        "parameters", Map.of("key", "k"))))));

        Set<String> names = WorkflowTaskUtils.getTasks(List.of(loop), null)
            .stream()
            .map(WorkflowTask::getName)
            .collect(Collectors.toSet());

        assertEquals(Set.of("loop_1", "logger_1", "dataStorage_1"), names);
    }

    @Test
    void testFlattenBranchCaseTaskRetainsClusterElements() {
        WorkflowTask branch = new WorkflowTask(Map.of(
            "name", "branch_1",
            "type", "branch/v1",
            "parameters", Map.of(
                "expression", "1",
                "cases", List.of(
                    Map.of(
                        "key", "1",
                        "tasks", List.of(
                            Map.of(
                                "name", "aiAgent_1",
                                "type", "aiAgent/v1/chat",
                                "parameters", Map.of("messages", List.of()),
                                "clusterElements", Map.of(
                                    "model", Map.of("name", "openAi_1", "type", "openAi/v1/model")))))))));

        WorkflowTask aiAgentWorkflowTask = WorkflowTaskUtils.getTasks(List.of(branch), null)
            .stream()
            .filter(workflowTask -> Objects.equals(workflowTask.getName(), "aiAgent_1"))
            .findFirst()
            .orElseThrow();

        assertEquals(
            Map.of("model", Map.of("name", "openAi_1", "type", "openAi/v1/model")),
            aiAgentWorkflowTask.getExtensions()
                .get("clusterElements"));
    }

    @Test
    void testFlattenBranchCaseTaskRetainsMultipleElementClusterElements() {
        WorkflowTask branch = new WorkflowTask(Map.of(
            "name", "branch_1",
            "type", "branch/v1",
            "parameters", Map.of(
                "expression", "1",
                "cases", List.of(
                    Map.of(
                        "key", "1",
                        "tasks", List.of(
                            Map.of(
                                "name", "aiAgent_1",
                                "type", "aiAgent/v1/chat",
                                "parameters", Map.of("messages", List.of()),
                                "clusterElements", Map.of(
                                    "tools", List.of(
                                        Map.of("name", "dataTable_5", "type", "dataTable/v1/updateRecord"),
                                        Map.of("name", "dataTable_6", "type", "dataTable/v1/findRecords"))))))))));

        WorkflowTask aiAgentWorkflowTask = WorkflowTaskUtils.getTasks(List.of(branch), null)
            .stream()
            .filter(workflowTask -> Objects.equals(workflowTask.getName(), "aiAgent_1"))
            .findFirst()
            .orElseThrow();

        assertEquals(
            Map.of(
                "tools", List.of(
                    Map.of("name", "dataTable_5", "type", "dataTable/v1/updateRecord"),
                    Map.of("name", "dataTable_6", "type", "dataTable/v1/findRecords"))),
            aiAgentWorkflowTask.getExtensions()
                .get("clusterElements"));
    }

    @Test
    void testRemoveDisabledTasksTopLevel() {
        List<WorkflowTask> workflowTasks = List.of(
            new WorkflowTask(Map.of("name", "task1", "type", "component/v1/action")),
            new WorkflowTask(Map.of("name", "task2", "type", "component/v1/action", "disabled", true)),
            new WorkflowTask(Map.of("name", "task3", "type", "component/v1/action")));

        List<WorkflowTask> resultWorkflowTasks = WorkflowTaskUtils.removeDisabledTasks(workflowTasks);

        assertEquals(
            List.of("task1", "task3"),
            resultWorkflowTasks.stream()
                .map(WorkflowTask::getName)
                .toList());
    }

    @Test
    void testRemoveDisabledTasksFirstTaskShiftsIndexes() {
        List<WorkflowTask> workflowTasks = List.of(
            new WorkflowTask(Map.of("name", "task1", "type", "component/v1/action", "disabled", true)),
            new WorkflowTask(Map.of("name", "task2", "type", "component/v1/action")),
            new WorkflowTask(Map.of("name", "task3", "type", "component/v1/action")));

        List<WorkflowTask> resultWorkflowTasks = WorkflowTaskUtils.removeDisabledTasks(workflowTasks);

        assertEquals(
            List.of("task2", "task3"),
            resultWorkflowTasks.stream()
                .map(WorkflowTask::getName)
                .toList());
    }

    @Test
    void testRemoveDisabledTasksInsideConditionCases() {
        WorkflowTask condition = new WorkflowTask(Map.of(
            "name", "condition_1",
            "type", "condition/v1",
            "parameters", Map.of(
                "rawExpression", true,
                "caseTrue", List.of(
                    Map.of("name", "trueEnabled", "type", "component/v1/action"),
                    Map.of("name", "trueDisabled", "type", "component/v1/action", "disabled", true)),
                "caseFalse", List.of(
                    Map.of("name", "falseEnabled", "type", "component/v1/action"),
                    Map.of("name", "falseDisabled", "type", "component/v1/action", "disabled", true)))));

        List<WorkflowTask> resultWorkflowTasks = WorkflowTaskUtils.removeDisabledTasks(List.of(condition));

        WorkflowTask resultCondition = resultWorkflowTasks.getFirst();
        Map<String, ?> resultParameters = resultCondition.getParameters();

        List<?> resultCaseTrue = (List<?>) resultParameters.get("caseTrue");
        List<?> resultCaseFalse = (List<?>) resultParameters.get("caseFalse");

        assertEquals(1, resultCaseTrue.size());
        assertEquals(1, resultCaseFalse.size());
    }

    @Test
    void testRemoveDisabledTasksInsideBranchCases() {
        WorkflowTask branch = new WorkflowTask(Map.of(
            "name", "branch_1",
            "type", "branch/v1",
            "parameters", Map.of(
                "cases", List.of(
                    Map.of(
                        "key", "k1",
                        "tasks", List.of(
                            Map.of("name", "caseEnabled", "type", "component/v1/action"),
                            Map.of(
                                "name", "caseDisabled", "type", "component/v1/action", "disabled", true)))))));

        List<WorkflowTask> resultWorkflowTasks = WorkflowTaskUtils.removeDisabledTasks(List.of(branch));

        WorkflowTask resultBranch = resultWorkflowTasks.getFirst();
        Map<String, ?> resultParameters = resultBranch.getParameters();

        List<?> resultCases = (List<?>) resultParameters.get("cases");
        Map<?, ?> resultCase = (Map<?, ?>) resultCases.getFirst();
        List<?> resultTasks = (List<?>) resultCase.get("tasks");

        assertEquals(1, resultTasks.size());
    }

    @Test
    void testRemoveDisabledSingleMapSubtask() {
        WorkflowTask each = new WorkflowTask(Map.of(
            "name", "each_1",
            "type", "each/v1",
            "parameters", Map.of(
                "iteratee",
                Map.of("name", "iterateeTask", "type", "component/v1/action", "disabled", true))));

        List<WorkflowTask> resultWorkflowTasks = WorkflowTaskUtils.removeDisabledTasks(List.of(each));

        WorkflowTask resultEach = resultWorkflowTasks.getFirst();
        Map<String, ?> resultParameters = resultEach.getParameters();

        assertFalse(resultParameters.containsKey("iteratee"));
    }

    @Test
    void testRemoveDisabledTasksInsideForkJoinBranches() {
        WorkflowTask forkJoin = new WorkflowTask(Map.of(
            "name", "forkJoin_1",
            "type", "fork-join/v1",
            "parameters", Map.of(
                "branches", List.of(
                    List.of(
                        Map.of("name", "branch1Enabled", "type", "component/v1/action"),
                        Map.of(
                            "name", "branch1Disabled", "type", "component/v1/action", "disabled", true)),
                    List.of(
                        Map.of("name", "branch2Enabled", "type", "component/v1/action"))))));

        List<WorkflowTask> resultWorkflowTasks = WorkflowTaskUtils.removeDisabledTasks(List.of(forkJoin));

        WorkflowTask resultForkJoin = resultWorkflowTasks.getFirst();
        Map<String, ?> resultParameters = resultForkJoin.getParameters();

        List<?> resultBranches = (List<?>) resultParameters.get("branches");
        List<?> resultBranch1 = (List<?>) resultBranches.get(0);
        List<?> resultBranch2 = (List<?>) resultBranches.get(1);

        assertEquals(1, resultBranch1.size());
        assertEquals(1, resultBranch2.size());
    }

    @Test
    void testRemoveDisabledTasksReturnsSameListWhenNothingDisabled() {
        List<WorkflowTask> workflowTasks = List.of(
            new WorkflowTask(Map.of("name", "task1", "type", "component/v1/action")),
            new WorkflowTask(Map.of(
                "name", "condition_1",
                "type", "condition/v1",
                "parameters", Map.of(
                    "caseTrue", List.of(Map.of("name", "inner1", "type", "component/v1/action"))))));

        List<WorkflowTask> resultWorkflowTasks = WorkflowTaskUtils.removeDisabledTasks(workflowTasks);

        assertEquals(workflowTasks.size(), resultWorkflowTasks.size());
        assertSame(workflowTasks.get(0), resultWorkflowTasks.get(0));
        assertSame(workflowTasks.get(1), resultWorkflowTasks.get(1));
    }

    @Test
    void testGetDisabledTaskNamesIncludesDescendants() {
        WorkflowTask condition = new WorkflowTask(Map.of(
            "name", "condition_1",
            "type", "condition/v1",
            "disabled", true,
            "parameters", Map.of(
                "caseTrue", List.of(Map.of("name", "inner1", "type", "component/v1/action")))));

        List<String> disabledTaskNames = WorkflowTaskUtils.getDisabledTaskNames(List.of(condition));

        assertEquals(Set.of("condition_1", "inner1"), Set.copyOf(disabledTaskNames));
    }

    @Test
    void testGetDisabledTaskNamesForNestedDisabledUnderEnabledParent() {
        WorkflowTask loop = new WorkflowTask(Map.of(
            "name", "loop_1",
            "type", "loop/v1",
            "parameters", Map.of(
                "iteratee", List.of(
                    Map.of("name", "loopEnabled", "type", "component/v1/action"),
                    Map.of("name", "loopDisabled", "type", "component/v1/action", "disabled", true)))));

        List<String> disabledTaskNames = WorkflowTaskUtils.getDisabledTaskNames(List.of(loop));

        assertEquals(List.of("loopDisabled"), disabledTaskNames);
    }

    @Test
    void testRemoveDisabledTasksInsideFinalize() {
        WorkflowTask task = new WorkflowTask(Map.of(
            "name", "task_1",
            "type", "component/v1/action",
            "finalize", List.of(
                Map.of("name", "finalizeEnabled", "type", "component/v1/action"),
                Map.of("name", "finalizeDisabled", "type", "component/v1/action", "disabled", true))));

        List<WorkflowTask> resultWorkflowTasks = WorkflowTaskUtils.removeDisabledTasks(List.of(task));

        WorkflowTask resultTask = resultWorkflowTasks.getFirst();

        assertEquals(
            List.of("finalizeEnabled"),
            resultTask.getFinalize()
                .stream()
                .map(WorkflowTask::getName)
                .toList());
    }

    @Test
    void testRemoveDisabledTasksWithWorkflowTaskInstanceInParameters() {
        WorkflowTask iterateeTask = new WorkflowTask(
            Map.of("name", "iterateeInstance", "type", "component/v1/action", "disabled", true));

        WorkflowTask loop = new WorkflowTask(Map.of(
            "name", "loop_1",
            "type", "loop/v1",
            "parameters", Map.of("iteratee", iterateeTask)));

        List<WorkflowTask> resultWorkflowTasks = WorkflowTaskUtils.removeDisabledTasks(List.of(loop));

        WorkflowTask resultLoop = resultWorkflowTasks.getFirst();
        Map<String, ?> resultParameters = resultLoop.getParameters();

        assertFalse(resultParameters.containsKey("iteratee"));
    }

    @Test
    void testRemoveDisabledTasksPreservesMaxRetriesAndTaskNumber() {
        WorkflowTask condition = new WorkflowTask(Map.of(
            "name", "condition_1",
            "type", "condition/v1",
            "maxRetries", 3,
            "taskNumber", 2,
            "parameters", Map.of(
                "caseTrue", List.of(
                    Map.of("name", "trueEnabled", "type", "component/v1/action"),
                    Map.of("name", "trueDisabled", "type", "component/v1/action", "disabled", true)))));

        List<WorkflowTask> resultWorkflowTasks = WorkflowTaskUtils.removeDisabledTasks(List.of(condition));

        WorkflowTask resultCondition = resultWorkflowTasks.getFirst();

        assertEquals(3, resultCondition.getMaxRetries());
        assertEquals(2, resultCondition.getTaskNumber());
    }

    @Test
    void testRemoveDisabledTasksReturnsSameInstanceForEnabledNestedWorkflowTaskInstance() {
        WorkflowTask iterateeTask = new WorkflowTask(
            Map.of("name", "iterateeInstance", "type", "component/v1/action"));

        WorkflowTask loop = new WorkflowTask(Map.of(
            "name", "loop_1",
            "type", "loop/v1",
            "parameters", Map.of("iteratee", iterateeTask)));

        List<WorkflowTask> resultWorkflowTasks = WorkflowTaskUtils.removeDisabledTasks(List.of(loop));

        assertSame(loop, resultWorkflowTasks.getFirst());
    }
}
