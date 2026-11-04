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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ManagementMcpInstructionsTest {

    private static final Set<String> OVERVIEW_AND_SEARCH_TOOL_NAMES = Set.of(
        "listWorkspaces", "listProjects", "getProject", "createProject", "listWorkflows", "searchWorkflows",
        "getWorkflow");

    private static final Set<String> INTELLIGENT_TOOL_NAMES = Set.of(
        "buildWorkflow", "importWorkflow", "authorSkill", "debugWorkflowExecution", "configureMcpServer");

    private static final List<String> KNOWN_OPTIONAL_TOOL_NAMES = List.of(
        "buildWorkflow", "importWorkflow", "authorSkill", "debugWorkflowExecution", "configureMcpServer",
        "updateWorkflow", "listConnections", "saveWorkflowTestConnection", "createProjectWorkflow", "getWorkflow",
        "getWorkflowBuildInstructions", "searchComponents", "getActionDefinition", "searchTriggers",
        "getTriggerDefinition", "publishProject", "createMcpServer", "createMcpProject", "updateMcpServer",
        "listMcpServers", "updateMcpProjectWorkflowParameters");

    @Test
    void testOmitsIntelligentToolsWhenNotRegistered() {
        String instructions = ManagementMcpInstructions.build(deterministicToolNames());

        assertThat(instructions)
            .contains("updateWorkflow", "listConnections")
            .doesNotContain(INTELLIGENT_TOOL_NAMES);
    }

    @Test
    void testDescribesBuildWorkflowAndItsProviderRequirementWhenRegistered() {
        String instructions = ManagementMcpInstructions.build(allToolNames());

        assertThat(instructions)
            .contains("needs an AI provider", "author the workflow yourself");
    }

    @Test
    void testAuthoringLoopIsInOrder() {
        String instructions = ManagementMcpInstructions.build(deterministicToolNames());

        List<String> loop = List.of(
            "getWorkflowBuildInstructions", "searchComponents", "createProjectWorkflow", "getWorkflow",
            "updateWorkflow", "listConnections", "saveWorkflowTestConnection", "publishProject");

        int previous = -1;

        for (String name : loop) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");

            Matcher matcher = pattern.matcher(instructions);

            assertThat(matcher.find()).as(name)
                .isTrue();

            int index = matcher.start();

            assertThat(index).as(name)
                .isGreaterThan(previous);

            previous = index;
        }
    }

    @Test
    void testOmitsAuthoringWhenUpdateWorkflowIsNotRegistered() {
        String instructions = ManagementMcpInstructions.build(OVERVIEW_AND_SEARCH_TOOL_NAMES);

        assertThat(instructions).doesNotContain("updateWorkflow", "To author a workflow yourself");
    }

    @Test
    void testMcpServerParagraphWithoutConfigureMcpServerPointsToTheUi() {
        String instructions = ManagementMcpInstructions.build(deterministicToolNames());

        assertThat(instructions)
            .contains("in the ByteChef UI before enabling")
            .doesNotContain("configureMcpServer", "updateMcpProjectWorkflowParameters");
    }

    @Test
    void testMcpServerParagraphWithConfigureMcpServerNamesIt() {
        String instructions = ManagementMcpInstructions.build(allToolNames());

        assertThat(instructions)
            .contains("configureMcpServer(mcpServerId)")
            .doesNotContain("updateMcpProjectWorkflowParameters");
    }

    @Test
    void testNeverNamesAnUnregisteredTool() {
        assertNoAbsentToolNamed(deterministicToolNames());
        assertNoAbsentToolNamed(allToolNames());
        assertNoAbsentToolNamed(OVERVIEW_AND_SEARCH_TOOL_NAMES);
        assertNoAbsentToolNamed(partialAuthoringToolNames());
        assertNoAbsentToolNamed(buildWorkflowWithoutUpdateWorkflowToolNames());
    }

    @Test
    void testOmitsAuthoringLoopWhenItsToolsAreNotAllRegistered() {
        String instructions = ManagementMcpInstructions.build(partialAuthoringToolNames());

        assertThat(instructions).doesNotContain("To author a workflow yourself", "updateWorkflow");
    }

    @Test
    void testBuildWorkflowDoesNotPointToAuthoringWhenAuthoringIsNotServed() {
        String instructions = ManagementMcpInstructions.build(buildWorkflowWithoutUpdateWorkflowToolNames());

        assertThat(instructions)
            .contains("needs an AI provider enabled")
            .doesNotContain("author the workflow yourself");
    }

    @Test
    void testContainsNoEmDash() {
        assertThat(ManagementMcpInstructions.build(deterministicToolNames())).doesNotContain("\u2014");
        assertThat(ManagementMcpInstructions.build(allToolNames())).doesNotContain("\u2014");
        assertThat(ManagementMcpInstructions.build(OVERVIEW_AND_SEARCH_TOOL_NAMES)).doesNotContain("\u2014");
    }

    @Test
    void testInstructionsDescribeImportWorkflowWithAProjectIdOrAWorkflowId() {
        Set<String> toolNames = new HashSet<>(OVERVIEW_AND_SEARCH_TOOL_NAMES);

        toolNames.add("importWorkflow");
        toolNames.add("buildWorkflow");

        assertThat(ManagementMcpInstructions.build(toolNames))
            .contains("createProject (if needed) → importWorkflow with the source")
            .contains("the projectId for a new workflow or the workflowId for an existing one")
            .doesNotContain(
                "createProjectWorkflow", "searchProjects", "getProjectStatus", "cloneMcpProject",
                "listMcpProjectWorkflows", "\u2014");
    }

    private static void assertNoAbsentToolNamed(Set<String> toolNames) {
        String instructions = ManagementMcpInstructions.build(toolNames);

        List<String> mentionedAbsentNames = new ArrayList<>();

        for (String name : KNOWN_OPTIONAL_TOOL_NAMES) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");

            boolean mentioned = pattern.matcher(instructions)
                .find();

            if (mentioned && !toolNames.contains(name)) {
                mentionedAbsentNames.add(name);
            }
        }

        assertThat(mentionedAbsentNames).isEmpty();
    }

    private static Set<String> deterministicToolNames() {
        Set<String> toolNames = new HashSet<>(OVERVIEW_AND_SEARCH_TOOL_NAMES);

        toolNames.addAll(
            List.of(
                "updateWorkflow", "listConnections", "saveWorkflowTestConnection", "createProjectWorkflow",
                "getWorkflowBuildInstructions", "searchComponents", "getActionDefinition", "searchTriggers",
                "getTriggerDefinition", "publishProject", "createMcpServer", "createMcpProject", "updateMcpServer",
                "listMcpServers"));

        return toolNames;
    }

    private static Set<String> partialAuthoringToolNames() {
        Set<String> toolNames = deterministicToolNames();

        toolNames.removeAll(
            List.of(
                "createProjectWorkflow", "getWorkflowBuildInstructions", "searchComponents", "getActionDefinition",
                "searchTriggers", "getTriggerDefinition", "listConnections"));

        return toolNames;
    }

    private static Set<String> buildWorkflowWithoutUpdateWorkflowToolNames() {
        Set<String> toolNames = new HashSet<>(OVERVIEW_AND_SEARCH_TOOL_NAMES);

        toolNames.add("buildWorkflow");

        return toolNames;
    }

    private static Set<String> allToolNames() {
        Set<String> toolNames = deterministicToolNames();

        toolNames.addAll(INTELLIGENT_TOOL_NAMES);

        return toolNames;
    }
}
