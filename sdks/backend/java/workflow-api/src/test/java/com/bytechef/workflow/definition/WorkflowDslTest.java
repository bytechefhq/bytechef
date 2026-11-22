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

package com.bytechef.workflow.definition;

import static com.bytechef.workflow.definition.WorkflowDsl.branch;
import static com.bytechef.workflow.definition.WorkflowDsl.componentTask;
import static com.bytechef.workflow.definition.WorkflowDsl.forkJoin;
import static com.bytechef.workflow.definition.WorkflowDsl.input;
import static com.bytechef.workflow.definition.WorkflowDsl.parallel;
import static com.bytechef.workflow.definition.WorkflowDsl.task;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.workflow.definition.Input.ComponentInputReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowDslTest {

    @Test
    void testInputReferencesNoComponentInputByDefault() {
        assertThat(input("channel").getComponentInput()).isEmpty();
    }

    @Test
    void testInputReferencesTheComponentInputItIsGiven() {
        assertThat(
            input("channel")
                .component("slack", 1, "channel")
                .getComponentInput())
                    .contains(new ComponentInputReference("slack", 1, "channel"));
    }

    @Test
    void testComponentTaskCarriesTypeAndParameters() {
        ComponentTaskDefinition componentTaskDefinition = componentTask(
            "respond", "apiPlatform/v1/responseToAPIRequest")
                .label("Respond")
                .parameters(Map.of("response", Map.of("statusCode", 200)));

        assertThat(componentTaskDefinition.getName()).isEqualTo("respond");
        assertThat(componentTaskDefinition.getType()).isEqualTo("apiPlatform/v1/responseToAPIRequest");
        assertThat(componentTaskDefinition.getLabel()).contains("Respond");
        Map<String, ?> parameters = componentTaskDefinition.getParameters();

        assertThat(parameters.get("response")).isEqualTo(Map.of("statusCode", 200));
    }

    @Test
    void testComponentTaskHasNoParametersByDefault() {
        assertThat(componentTask("bare", "x/v1/y").getParameters()).isEmpty();
    }

    @Test
    void testComponentTaskKeepsNullParameterValues() {
        Map<String, Object> declaredParameters = new HashMap<>();

        declaredParameters.put("text", null);

        ComponentTaskDefinition componentTaskDefinition = componentTask("log", "logger/v1/info")
            .parameters(declaredParameters);

        Map<String, ?> parameters = componentTaskDefinition.getParameters();

        assertThat(parameters).containsKey("text");
        assertThat(parameters.get("text")).isNull();
    }

    @Test
    void testComponentTaskWithNullParametersHasNoParameters() {
        ComponentTaskDefinition componentTaskDefinition = componentTask("log", "logger/v1/info").parameters(null);

        assertThat(componentTaskDefinition.getParameters()).isEmpty();
    }

    @Test
    void testBranchKeepsTheTypeOfItsTasks() {
        List<WorkflowDsl.ModifiableTaskDefinition> branchTasks = branch(task("a"), task("b"));

        assertThat(branchTasks).hasSize(2);
    }

    @Test
    void testParallelAcceptsAComponentTask() {
        CompositeTaskDefinition compositeTaskDefinition = parallel("p")
            .tasks(task("a"), componentTask("b", "x/v1/y"));

        List<? extends WorkflowTaskDefinition> tasks = compositeTaskDefinition.getTasks();

        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(1)).isInstanceOf(ComponentTaskDefinition.class);
    }

    @Test
    void testForkJoinBranchAcceptsAComponentTask() {
        CompositeTaskDefinition compositeTaskDefinition = forkJoin("f")
            .branches(branch(task("a")), branch(componentTask("b", "x/v1/y")));

        List<? extends List<? extends WorkflowTaskDefinition>> branches = compositeTaskDefinition.getBranches();

        assertThat(branches.get(1)
            .getFirst()).isInstanceOf(ComponentTaskDefinition.class);
    }
}
