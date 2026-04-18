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

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
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
}
