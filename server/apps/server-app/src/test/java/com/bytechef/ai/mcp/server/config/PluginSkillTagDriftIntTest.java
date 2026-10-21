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

import com.bytechef.ai.copilot.config.ToolCallbackContributorConfiguration;
import com.bytechef.plugintools.Edition;
import com.bytechef.plugintools.SkillDocument;
import com.bytechef.plugintools.SkillDocumentParser;
import com.bytechef.plugintools.Transport;
import com.bytechef.plugintools.TransportFence;
import com.bytechef.server.ServerApplication;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifies that every transport fence tagged onto a plugin skill names something that actually exists: an {@code mcp}
 * fence must name a tool registered on the management MCP server, and a {@code cli} fence must name a command
 * implemented under {@code cli/commands}.
 * <p>
 * Built before any skill carries a transport fence, so with no tags present the first three tests pass vacuously — that
 * is expected, and {@link #testTheCliInventoryIsNotEmpty()} exists specifically so the suite cannot pass forever by
 * finding nothing: it proves the CLI command extractor itself works.
 * <p>
 * {@link #testEditionMarkingIsNotVerifiedMechanically()} checks only that an {@code edition: ee} fence names a tool
 * that exists in the registry — the same existence check as any other fence. Whether that tool is genuinely EE-only is
 * not mechanically checked here: "contributed vs core" is derivable but means something different (buildWorkflow is
 * contributed AND CE), and the embedded tools share names with the automation ones (getWorkflow, listWorkflows,
 * updateWorkflow, deleteWorkflow exist in both ProjectWorkflowTools and IntegrationWorkflowTools) while reaching a
 * different MCP server, so there is no mechanical source of truth for an edition claim. This comment exists so a future
 * reader does not mistake an unchecked declaration for a checked one.
 * <p>
 * {@code registeredMcpToolNames()} alone under-approximates the real registry: Copilot-contributed intelligent tools
 * (e.g. {@code buildWorkflow}, {@code importWorkflow}) only resolve there when {@code CopilotConfiguration} is active,
 * which needs {@code bytechef.ai.copilot.enabled=true}, which this test's context deliberately runs without (see
 * {@code ServerApplicationIntTest.testCopilotBeansNotPresentWhenFeatureDisabled}) — and turning it on here pulls in a
 * pgvector-backed vector store ({@code CopilotPgVectorConfiguration}) that runs {@code CREATE EXTENSION IF NOT
 * EXISTS vector} against a real datasource at context-refresh time, which the plain Postgres Testcontainer this suite
 * uses cannot satisfy. So {@link #mcpInventory()} widens {@link #registeredMcpToolNames()} with
 * {@link ToolCallbackContributorConfiguration#INTELLIGENT_TOOL_NAMES} — the exact, hand-curated CE partition of
 * intelligent-tool names the management MCP surface owns, already a public constant rather than something this test
 * needs to derive. It admits only the CE names ({@code buildWorkflow}, {@code importWorkflow},
 * {@code configureClusterElement}, {@code writeScript}, {@code authorSkill}, {@code debugWorkflowExecution},
 * {@code configureMcpServer}) — a fence naming an EE-contributed intelligent tool ({@code buildCustomComponent},
 * {@code buildCodeWorkflow}, {@code buildIntegrationWorkflow}) still needs the real registry (or a deliberate, separate
 * widening) to pass.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "bytechef.ai.mcp.server.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
class PluginSkillTagDriftIntTest {

    @Autowired
    private ManagementMcpServerConfiguration managementMcpServerConfiguration;

    private Set<String> registeredMcpToolNames() {
        ToolCallbackProvider toolCallbackProvider = managementMcpServerConfiguration.toolCallbackProvider();

        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .collect(Collectors.toSet());
    }

    private Set<String> mcpInventory() {
        Set<String> names = new HashSet<>(registeredMcpToolNames());

        names.addAll(ToolCallbackContributorConfiguration.INTELLIGENT_TOOL_NAMES);

        return names;
    }

    private static List<SkillDocument> skillDocuments() throws IOException {
        return SkillDocumentParser.parseAll(CliCommandInventory.repositoryRoot()
            .resolve("claude-code-plugin/bytechef-dev/skills"));
    }

    @Test
    void testEveryMcpTagNamesARegisteredTool() throws IOException {
        Set<String> registeredToolNames = mcpInventory();
        List<String> failures = new ArrayList<>();
        int mcpFenceCount = 0;

        for (SkillDocument skillDocument : skillDocuments()) {
            for (TransportFence transportFence : skillDocument.fences()) {
                if (transportFence.transport() != Transport.MCP) {
                    continue;
                }

                mcpFenceCount++;

                for (String use : transportFence.uses()) {
                    if (use.equals(TransportFence.GENERAL_MARKER)) {
                        continue;
                    }

                    if (!registeredToolNames.contains(use)) {
                        failures.add(skillDocument.name() + ": mcp tool '" + use + "' is not registered");
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
        assertTrue(mcpFenceCount > 0, "Expected at least one mcp transport fence across all skills");
    }

    @Test
    void testEveryCliTagNamesAnExistingCommand() throws IOException {
        Set<String> existingCommandNames = CliCommandInventory.commandNames(CliCommandInventory.repositoryRoot());
        List<String> failures = new ArrayList<>();

        for (SkillDocument skillDocument : skillDocuments()) {
            for (TransportFence transportFence : skillDocument.fences()) {
                if (transportFence.transport() != Transport.CLI) {
                    continue;
                }

                for (String use : transportFence.uses()) {
                    if (!existingCommandNames.contains(use)) {
                        failures.add(skillDocument.name() + ": cli command '" + use + "' does not exist");
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void testEditionMarkingIsNotVerifiedMechanically() throws IOException {
        Set<String> registeredToolNames = mcpInventory();
        List<String> failures = new ArrayList<>();

        for (SkillDocument skillDocument : skillDocuments()) {
            for (TransportFence transportFence : skillDocument.fences()) {
                if (transportFence.transport() != Transport.MCP || transportFence.edition() != Edition.EE) {
                    continue;
                }

                for (String use : transportFence.uses()) {
                    if (use.equals(TransportFence.GENERAL_MARKER)) {
                        continue;
                    }

                    if (!registeredToolNames.contains(use)) {
                        failures.add(skillDocument.name() + ": mcp tool '" + use + "' tagged edition "
                            + transportFence.edition() + " is not registered");
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void testTheCliInventoryIsNotEmpty() throws IOException {
        Set<String> commandNames = CliCommandInventory.commandNames(CliCommandInventory.repositoryRoot());

        assertFalse(commandNames.isEmpty());
        assertTrue(commandNames.contains("configure"));
    }

    @Test
    void testTheMcpInventoryIsNotEmpty() {
        Set<String> toolNames = registeredMcpToolNames();

        assertFalse(toolNames.isEmpty());
        assertTrue(toolNames.contains("createProjectWorkflow"));
    }

    @Test
    void testTheIntelligentToolNamesAreNotEmpty() {
        Set<String> names = ToolCallbackContributorConfiguration.INTELLIGENT_TOOL_NAMES;

        assertFalse(names.isEmpty());
        assertTrue(names.contains("buildWorkflow"));
    }

    @Test
    void testEveryFenceTransportIsDeclaredInSkillFrontmatter() throws IOException {
        List<String> failures = new ArrayList<>();

        for (SkillDocument skillDocument : skillDocuments()) {
            if (!isSkillFile(skillDocument)) {
                continue;
            }

            Set<Transport> declaredTransports = new HashSet<>(skillDocument.requiredTransports());

            declaredTransports.addAll(skillDocument.optionalTransports());

            for (TransportFence transportFence : skillDocument.fences()) {
                if (!declaredTransports.contains(transportFence.transport())) {
                    failures.add(skillDocument.name() + ": fence transport '" + transportFence.transport()
                        + "' is not declared in the frontmatter's required/optional transports");
                }
            }
        }

        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void testEveryRequiredTransportIsExercisedByAFence() throws IOException {
        List<String> failures = new ArrayList<>();

        for (SkillDocument skillDocument : skillDocuments()) {
            if (!isSkillFile(skillDocument)) {
                continue;
            }

            Set<Transport> exercisedTransports = new HashSet<>();

            for (TransportFence transportFence : skillDocument.fences()) {
                exercisedTransports.add(transportFence.transport());
            }

            for (Transport requiredTransport : skillDocument.requiredTransports()) {
                if (!exercisedTransports.contains(requiredTransport)) {
                    failures.add(skillDocument.name() + ": required transport '" + requiredTransport
                        + "' is not exercised by any fence");
                }
            }
        }

        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    private static boolean isSkillFile(SkillDocument skillDocument) {
        Path fileNamePath = skillDocument.path()
            .getFileName();

        return fileNamePath != null && fileNamePath.toString()
            .equals("SKILL.md");
    }
}
