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

package com.bytechef.platform.component.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class TaskRunnerConnectionWorkflowPreprocessorTest {

    private final TaskRunnerRegistry taskRunnerRegistry = mock(TaskRunnerRegistry.class);
    private final TaskRunnerConnectionWorkflowPreprocessor preprocessor =
        new TaskRunnerConnectionWorkflowPreprocessor(taskRunnerRegistry);

    @Test
    public void testARunnerWithAConnectionGetsTheEntry() {
        registerRunner("kubernetes", new TaskRunnerConnection("kubernetes", 1));

        Map<String, Object> task = task("kubernetes");
        Map<String, Object> definition = definition(task);

        assertThat(preprocessor.process(definition)).isTrue();
        assertThat(connections(task)).containsEntry(
            "taskRunner", Map.of("componentName", "kubernetes", "componentVersion", 1, "authorizationRequired", true));
    }

    @Test
    public void testAnUnchangedEntryReportsNoChange() {
        registerRunner("kubernetes", new TaskRunnerConnection("kubernetes", 1));

        Map<String, Object> task = task("kubernetes");

        connections(task).put(
            "taskRunner",
            new LinkedHashMap<>(
                Map.of("componentName", "kubernetes", "componentVersion", 1, "authorizationRequired", true)));

        assertThat(preprocessor.process(definition(task))).isFalse();
    }

    @Test
    public void testARunnerWithoutAConnectionRemovesTheEntry() {
        registerRunner("docker", null);

        Map<String, Object> task = task("docker");

        connections(task).put("taskRunner", Map.of("componentName", "kubernetes", "componentVersion", 1));

        assertThat(preprocessor.process(definition(task))).isTrue();
        assertThat(task).doesNotContainKey("connections");
    }

    @Test
    public void testOtherConnectionsSurviveTheRemoval() {
        registerRunner("docker", null);

        Map<String, Object> task = task("docker");

        connections(task).put("taskRunner", Map.of("componentName", "kubernetes", "componentVersion", 1));
        connections(task).put("slack", Map.of("componentName", "slack", "componentVersion", 1));

        preprocessor.process(definition(task));

        assertThat(connections(task)).containsOnlyKeys("slack");
    }

    @Test
    public void testADisabledRunnerLeavesTheConnectionAlone() {
        when(taskRunnerRegistry.getTaskRunner(anyString())).thenThrow(new TaskRunnerNotEnabledException("kubernetes"));

        Map<String, Object> task = task("kubernetes");

        Map<String, Object> existing = Map.of("componentName", "kubernetes", "componentVersion", 1);

        connections(task).put("taskRunner", existing);

        assertThat(preprocessor.process(definition(task))).isFalse();
        assertThat(connections(task)).containsEntry("taskRunner", existing);
    }

    @Test
    public void testATaskWithoutARunnerIsLeftAlone() {
        Map<String, Object> task = new LinkedHashMap<>(Map.of("name", "t", "type", "slack/v1/send"));

        assertThat(preprocessor.process(definition(task))).isFalse();
        assertThat(task).doesNotContainKey("connections");
    }

    @Test
    public void testNestedTasksAreVisitedWhereverTheySit() {
        registerRunner("kubernetes", new TaskRunnerConnection("kubernetes", 1));

        Map<String, Object> inner = task("kubernetes");

        Map<String, Object> loop = new LinkedHashMap<>();

        loop.put("name", "loop");
        loop.put("type", "loop/v1");
        loop.put("parameters", new LinkedHashMap<>(Map.of("iteratee", new ArrayList<>(List.of(inner)))));

        assertThat(preprocessor.process(definition(loop))).isTrue();
        assertThat(connections(inner)).containsKey("taskRunner");
    }

    @Test
    public void testAColumnDefinitionThatLooksLikeATaskIsNotATask() {
        registerRunner("kubernetes", new TaskRunnerConnection("kubernetes", 1));

        Map<String, Object> column = new LinkedHashMap<>(
            Map.of("name", "col", "type", "STRING", "parameters", Map.of("taskRunner", Map.of("type", "kubernetes"))));

        Map<String, Object> task = new LinkedHashMap<>(
            Map.of("name", "t", "type", "postgres/v1/create", "parameters", Map.of("columns", List.of(column))));

        assertThat(preprocessor.process(definition(task))).isFalse();
    }

    private void registerRunner(String type, TaskRunnerConnection connection) {
        TaskRunner taskRunner = mock(TaskRunner.class);

        when(taskRunner.getType()).thenReturn(type);
        when(taskRunner.getConnection()).thenReturn(Optional.ofNullable(connection));
        when(taskRunnerRegistry.getTaskRunner(type)).thenReturn(taskRunner);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> connections(Map<String, Object> task) {
        return (Map<String, Object>) task.computeIfAbsent("connections", key -> new LinkedHashMap<String, Object>());
    }

    private static Map<String, Object> definition(Map<String, Object> task) {
        Map<String, Object> definition = new LinkedHashMap<>();

        definition.put("label", "L");
        definition.put("tasks", new ArrayList<>(List.of(task)));

        return definition;
    }

    private static Map<String, Object> task(String runnerType) {
        Map<String, Object> task = new LinkedHashMap<>();

        task.put("name", "script");
        task.put("type", "script/v1/python");
        task.put("parameters",
            new LinkedHashMap<>(Map.of("taskRunner", new LinkedHashMap<>(Map.of("type", runnerType)))));

        return task;
    }
}
