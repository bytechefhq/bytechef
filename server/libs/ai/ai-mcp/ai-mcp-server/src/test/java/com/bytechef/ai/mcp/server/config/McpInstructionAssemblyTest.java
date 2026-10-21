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

package com.bytechef.ai.mcp.server.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link ManagementMcpServerConfiguration#buildInstructions(Set)} assembles the {@code initialize}
 * result's {@code instructions} string from the generated fragment resource, always including the {@code ## always}
 * section and including a {@code ## tools: ...} section when ANY of the tools it names is present in the passed set of
 * registered tool names.
 *
 * @author Ivica Cardic
 */
class McpInstructionAssemblyTest {

    @Test
    void testASectionIsOmittedWhenItsToolIsNotRegistered() {
        Set<String> toolNames = Set.of("listProjects", "createProject", "createProjectWorkflow", "getWorkflow");

        String instructions = ManagementMcpServerConfiguration.buildInstructions(toolNames);

        assertFalse(instructions.contains("buildWorkflow"));
    }

    @Test
    void testASectionIsIncludedWhenItsToolIsRegistered() {
        Set<String> toolNames = Set.of("listProjects", "createProjectWorkflow", "buildWorkflow");

        String instructions = ManagementMcpServerConfiguration.buildInstructions(toolNames);

        assertTrue(instructions.contains("buildWorkflow"));
    }

    @Test
    void testASectionIsIncludedWhenAnyOfItsToolsIsRegistered() {
        Set<String> toolNames = Set.of("listProjects");

        String instructions = ManagementMcpServerConfiguration.buildInstructions(toolNames);

        assertTrue(instructions.contains("createProjectWorkflow"));
    }

    @Test
    void testTheAlwaysSectionIsAlwaysPresent() {
        String instructions = ManagementMcpServerConfiguration.buildInstructions(Set.of());

        assertTrue(instructions.contains("ByteChef management server"));
    }
}
