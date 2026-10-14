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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class TaskRunnerConnectionTest {

    @Test
    public void testHoldsTheComponentNameAndVersion() {
        TaskRunnerConnection connection = new TaskRunnerConnection("kubernetes", 1);

        assertThat(connection.componentName()).isEqualTo("kubernetes");
        assertThat(connection.componentVersion()).isEqualTo(1);
    }

    @Test
    public void testRejectsABlankComponentName() {
        assertThatThrownBy(() -> new TaskRunnerConnection(" ", 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("componentName");
    }

    @Test
    public void testRejectsAVersionBelowOne() {
        assertThatThrownBy(() -> new TaskRunnerConnection("aws", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("componentVersion");
    }

    @Test
    public void testARunnerDeclaresNoConnectionByDefault() {
        TaskRunner taskRunner = new TaskRunner() {

            @Override
            public String getType() {
                return "x";
            }

            @Override
            public String getTitle() {
                return "X";
            }

            @Override
            public List<? extends ModifiableValueProperty<?, ?>> getProperties() {
                return List.of();
            }

            @Override
            public Set<TaskRunnerCapability> getCapabilities() {
                return Set.of();
            }

            @Override
            public void validate(TaskRunnerRequest request) {
            }

            @Override
            public TaskRunnerResult run(TaskRunnerRequest request) {
                return TaskRunnerResult.ofOutput(null);
            }
        };

        assertThat(taskRunner.getConnection()).isEmpty();
    }
}
