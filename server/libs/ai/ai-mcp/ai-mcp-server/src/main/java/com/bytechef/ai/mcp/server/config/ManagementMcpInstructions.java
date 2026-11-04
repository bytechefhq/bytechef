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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
final class ManagementMcpInstructions {

    private static final List<String> INTELLIGENT_TOOL_NAMES = List.of(
        "buildWorkflow", "importWorkflow", "authorSkill", "debugWorkflowExecution", "configureMcpServer");

    private static final List<String> MCP_SERVER_TOOL_NAMES = List.of(
        "createMcpServer", "createMcpProject", "updateMcpServer", "listMcpServers");

    private static final List<String> AUTHORING_TOOL_NAMES = List.of(
        "updateWorkflow", "createProjectWorkflow", "getWorkflowBuildInstructions");

    private ManagementMcpInstructions() {
    }

    static String build(Collection<String> toolNames) {
        Set<String> names = Set.copyOf(toolNames);

        List<String> paragraphs = new ArrayList<>();

        paragraphs.add(overview());

        boolean authoringServed = names.containsAll(AUTHORING_TOOL_NAMES);

        if (authoringServed) {
            paragraphs.add(authoring(names));
        }

        List<String> intelligentNames = new ArrayList<>();

        for (String intelligentToolName : INTELLIGENT_TOOL_NAMES) {
            if (names.contains(intelligentToolName)) {
                intelligentNames.add(intelligentToolName);
            }
        }

        if (!intelligentNames.isEmpty()) {
            paragraphs.add(intelligent(intelligentNames));
        }

        if (names.contains("buildWorkflow")) {
            paragraphs.add(buildWorkflow(authoringServed));
        }

        if (names.contains("importWorkflow")) {
            paragraphs.add(importWorkflow());
        }

        if (names.contains("authorSkill")) {
            paragraphs.add(authorSkill());
        }

        paragraphs.add(search());

        if (names.containsAll(MCP_SERVER_TOOL_NAMES)) {
            paragraphs.add(mcpServers(names));
        }

        return String.join("\n", paragraphs);
    }

    private static String overview() {
        return """
            ByteChef management server. Ordinary tools are deterministic CRUD; call them directly. \
            Tools that work within a workspace (listProjects, createProject, searchWorkflows without a \
            projectId, and the MCP server, deployment and knowledge-base tools) take a workspaceId, and several \
            also take an environment (DEVELOPMENT, STAGING or PRODUCTION, default DEVELOPMENT). Call \
            listWorkspaces first to find the workspace id. Tools that take a project or workflow id act on \
            that object directly.""";
    }

    private static String authoring(Set<String> names) {
        List<String> steps = new ArrayList<>();

        steps.add("getWorkflowBuildInstructions for the workflow definition format");

        List<String> lookups = new ArrayList<>();

        for (String lookup : List.of("searchComponents", "getActionDefinition", "searchTriggers",
            "getTriggerDefinition")) {

            if (names.contains(lookup)) {
                lookups.add(lookup);
            }
        }

        if (!lookups.isEmpty()) {
            steps.add("look up components, actions and triggers with " + String.join(", ", lookups));
        }

        steps.add("createProjectWorkflow to create an empty workflow in a project");

        steps.add("getWorkflow for the current version");

        steps.add(
            "updateWorkflow to save the definition; it is the authoritative check, so errors block the save: fix "
                + "every reported error and save again; warnings such as a missing connection are fixed after "
                + "saving");

        if (names.contains("listConnections") && names.contains("saveWorkflowTestConnection")) {
            steps.add("listConnections and saveWorkflowTestConnection to bind a connection to the workflow");
        }

        if (names.contains("publishProject")) {
            steps.add("publishProject");
        }

        return "To author a workflow yourself: createProject (if needed) → " + String.join(" → ", steps) + ".";
    }

    private static String intelligent(List<String> intelligentNames) {
        return "Intelligent tools (" + String.join(", ", intelligentNames) + ") run an inner AI agent and may take "
            + "minutes; call them for judgment work, not for CRUD. Each call is independent and re-reads current "
            + "state (e.g. the workflow) rather than remembering earlier calls, so iterate by calling the tool "
            + "again with the next instruction and restate any context it still needs.";
    }

    private static String buildWorkflow(boolean authoringServed) {
        String text =
            """
                To have the server build a workflow: createProject (if needed) → buildWorkflow with a \
                plain-language instruction and the projectId for a new workflow or the workflowId for an \
                existing one. buildWorkflow runs a server-side AI model and needs an AI provider enabled""";

        if (authoringServed) {
            return text + "; if it reports none, author the workflow yourself.";
        }

        return text + ".";
    }

    private static String importWorkflow() {
        return """
            To import an external workflow (n8n, Make, Zapier, Workato): createProject (if needed) → \
            importWorkflow with the source definition and the projectId for a new workflow or the workflowId for \
            an existing one.""";
    }

    private static String authorSkill() {
        return """
            Skills are read-only through the ordinary tools; author or change a skill with authorSkill.""";
    }

    private static String search() {
        return """
            To find something, listProjects and listWorkflows take an optional query that matches names and \
            descriptions; searchWorkflows without a projectId searches the workflows of every project in a \
            workspace. getProject also reports publication state, the last version and deployments.""";
    }

    private static String mcpServers(Set<String> names) {
        StringBuilder sb = new StringBuilder();

        sb.append(
            """
                To expose workflows over MCP: createMcpServer(name, environment, enabled?), leaving enabled \
                unset or false, since a server with any unmapped attached workflow cannot be enabled; then \
                createMcpProject(mcpServerId, projectId, projectVersion, workflowIds) to attach a published \
                project version's workflows (the server must already exist); then\s""");

        if (names.contains("configureMcpServer")) {
            sb.append(
                """
                    configureMcpServer(mcpServerId) to synthesize each attached workflow's tool mapping (tool \
                    name, tool description, fromAi(...) input expressions); then updateMcpServer(mcpServerId, \
                    enabled=true) to bring it online. That last call FAILS with a typed error naming the \
                    still-unmapped workflows if any attached workflow lacks a toolName or a required fromAi \
                    mapping; that is the enable-guard working as intended, not a bug, so call \
                    configureMcpServer again and retry. listMcpServers resolves a user-named server to its \
                    numeric id. configureMcpServer never creates the server, attaches workflows to it, or \
                    enables it; those are the flat tools above.""");
        } else {
            sb.append(
                """
                    set each attached workflow's tool mapping (tool name, tool description, fromAi(...) input \
                    expressions) in the ByteChef UI before enabling; then updateMcpServer(mcpServerId, \
                    enabled=true) to bring it online. That last call FAILS with a typed error naming the \
                    still-unmapped workflows if any attached workflow lacks a toolName or a required fromAi \
                    mapping; that is the enable-guard working as intended, not a bug. listMcpServers resolves a \
                    user-named server to its numeric id.""");
        }

        return sb.toString();
    }
}
