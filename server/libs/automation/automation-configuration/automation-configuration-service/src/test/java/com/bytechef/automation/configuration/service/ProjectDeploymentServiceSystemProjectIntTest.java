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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Pins that the workspace deployment listing hides system projects, against real rows and the real SQL.
 *
 * <p>
 * The exclusion is one {@code SystemProjects.projectNameNotLikePredicates("project.name")} call inside
 * {@code CustomProjectDeploymentRepositoryImpl}, and until this test it was asserted in three places and verified in
 * none: {@code CLAUDE.md}, {@code AiAgentFacade#getWorkspaceChatAgents}'s javadoc, and a comment in the client's
 * {@code useAiHubChatLaunchers} all state that hidden {@code __AI_AGENT__} projects cannot reach a deployment listing.
 * The AI Hub launcher's two cascades are built on that being true — the agent cascade and the workflow cascade would
 * otherwise offer the same chat twice — and so is the promise that a hidden project stays hidden.
 *
 * <p>
 * A hand-built SQL string is the reason this is an IntTest rather than a unit test over the repository: the predicate
 * has to be spliced into the right clause of the right query against the right column, and a test that inspected the
 * generated SQL would pass on a string that Postgres rejects or silently mismatches. Only real rows distinguish "the
 * predicate is present" from "the predicate excludes the row".
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
public class ProjectDeploymentServiceSystemProjectIntTest {

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace workspace;

    @BeforeEach
    public void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));
    }

    @AfterEach
    public void afterEach() {
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    public void testWorkspaceListingHidesEveryKindOfSystemProject() {
        long userDeploymentId = createDeployment("A user project");

        createDeployment(SystemProjects.AI_AGENT_NAME_PREFIX + "b8c1");
        createDeployment(SystemProjects.KNOWLEDGE_BASE_NAME_PREFIX + workspace.getId());
        createDeployment(SystemProjects.CONTEXT_STORE_NAME_PREFIX + workspace.getId());
        createDeployment(SystemProjects.EMBEDDED_AUTOMATION_NAME_PREFIX + "catalog");

        // Asserted at embedded=false AND embedded=null. The embedded=false call adds a second exclusion predicate
        // for the __EMBEDDED__ deployment marker, and that predicate -- not the one under test -- could be what
        // drops the __EMBEDDED_AUTOMATION__ row. It used to be exactly that: the marker's underscores were
        // unescaped LIKE wildcards, so '__EMBEDDED__%' matched '__EMBEDDED_AUTOMATION__catalog' and this case
        // passed with EMBEDDED_AUTOMATION_NAME_PREFIX deleted from NAME_PREFIXES. The escaping is fixed, and the
        // embedded=null pass keeps the case honest regardless: with no embedded flag there is no second predicate,
        // so SystemProjects is the only thing that can hide any of these four rows.
        for (Boolean embedded : new Boolean[] {
            false, null
        }) {
            List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
                embedded, Environment.DEVELOPMENT, null, null, workspace.getId());

            // By id rather than by size: a listing that returned the wrong single row would satisfy a count.
            assertThat(projectDeployments)
                .as("embedded=%s", embedded)
                .extracting(ProjectDeployment::getId)
                .containsExactly(userDeploymentId);
        }
    }

    /**
     * The mirror image of the exclusions above: a project whose name merely <em>resembles</em> the {@code __EMBEDDED__}
     * marker under LIKE wildcard rules must still be listed. {@code _} is a single-character wildcard, so the unescaped
     * {@code '__EMBEDDED__%'} this repository used to emit matched any name shaped <em>&lt;2
     * chars&gt;</em>{@code EMBEDDED}<em>&lt;2 chars&gt;…</em> — hiding an ordinarily-named user project from every
     * non-embedded listing, with no marker involved and nothing to explain it.
     */
    @Test
    public void testWorkspaceListingKeepsAProjectWhoseNameOnlyResemblesTheEmbeddedMarker() {
        long userDeploymentId = createDeployment("A user project");
        long lookalikeDeploymentId = createDeployment("MyEMBEDDEDxyz");

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
            false, Environment.DEVELOPMENT, null, null, workspace.getId());

        assertThat(projectDeployments)
            .extracting(ProjectDeployment::getId)
            .containsExactlyInAnyOrder(userDeploymentId, lookalikeDeploymentId);
    }

    /**
     * The exclusion keys on {@code project.name}, so a system-named <em>deployment</em> of an ordinary project is a
     * different question from the test above and has to be asked separately — the two namespaces are excluded by
     * different predicates, and only this one would survive a change that filtered the wrong column.
     */
    @Test
    public void testWorkspaceListingHidesApiCollectionAndMcpServerDeployments() {
        long userDeploymentId = createDeployment("A user project");

        createDeployment(
            "An API collection's project", SystemProjects.API_COLLECTION_DEPLOYMENT_NAME_PREFIX + "orders");
        createDeployment("An MCP server's project", SystemProjects.MCP_SERVER_DEPLOYMENT_NAME_PREFIX + "tools");

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
            false, Environment.DEVELOPMENT, null, null, workspace.getId());

        assertThat(projectDeployments)
            .extracting(ProjectDeployment::getId)
            .containsExactly(userDeploymentId);
    }

    private long createDeployment(String projectName) {
        return createDeployment(projectName, projectName + " deployment");
    }

    private long createDeployment(String projectName, String deploymentName) {
        Project project = projectRepository.save(
            Project.builder()
                .name(projectName)
                .workspaceId(workspace.getId())
                .build());

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(true);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName(deploymentName);
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);
        // Saved through the repository rather than the service, so nothing mints the lineage uuid for us and the
        // NOT NULL column would reject the insert.
        projectDeployment.setUuid(UUID.randomUUID());

        return Validate.notNull(
            projectDeploymentRepository.save(projectDeployment)
                .getId(),
            "id");
    }
}
