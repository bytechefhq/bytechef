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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ConnectionRequirementTest {

    @Test
    void testTaskDeclaresConnectionsInBothArities() {
        TaskDefinition taskDefinition = WorkflowDsl.task("my-task")
            .connections(
                WorkflowDsl.connection("slack", "slack-prod"),
                WorkflowDsl.connection("httpClient", 2, "billing-api"))
            .perform(() -> "hello");

        List<? extends ConnectionRequirement> connections = taskDefinition.getConnections()
            .orElseThrow();

        assertEquals(2, connections.size());

        ConnectionRequirement first = connections.getFirst();

        assertEquals("slack", first.getComponentName());
        assertEquals(OptionalInt.empty(), first.getComponentVersion());
        assertEquals("slack-prod", first.getName());

        ConnectionRequirement second = connections.get(1);

        assertEquals("httpClient", second.getComponentName());
        assertEquals(OptionalInt.of(2), second.getComponentVersion());
        assertEquals("billing-api", second.getName());
    }

    @Test
    void testTaskWithoutConnectionsIsEmpty() {
        TaskDefinition taskDefinition = WorkflowDsl.task("my-task")
            .perform(() -> "hello");

        assertTrue(taskDefinition.getConnections()
            .isEmpty());
    }
}
