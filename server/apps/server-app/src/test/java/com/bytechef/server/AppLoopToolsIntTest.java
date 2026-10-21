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

package com.bytechef.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.WorkspaceScopeResolver;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
import com.bytechef.automation.configuration.workflow.WorkflowKindChecker;
import com.bytechef.ee.automation.ai.tool.AppLoopTools;
import com.bytechef.ee.automation.ai.tool.AppLoopTools.DeployProjectResult;
import com.bytechef.ee.automation.ai.tool.AppLoopTools.DeployedWorkflow;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.dto.AppWorkflowBindingDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    properties = "bytechef.ai.mcp.server.enabled=true", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
    PostgreSQLContainerConfiguration.class, ServerApplicationIntTest.ServerApplicationIntTestConfiguration.class
})
class AppLoopToolsIntTest {

    private static final String CALLABLE_DEFINITION =
        """
            {
              "label": "Greet",
              "inputs": [],
              "triggers": [
                {
                  "name": "trigger_1",
                  "type": "workflow/v1/newWorkflowCall",
                  "parameters": {
                    "inputSchema": "{\\"type\\": \\"object\\", \\"properties\\": {\\"name\\": {\\"type\\": \\"string\\"}}}"
                  }
                }
              ],
              "tasks": [
                {
                  "name": "responseToWorkflowCall_1",
                  "type": "workflow/v1/responseToWorkflowCall",
                  "parameters": {
                    "outputSchema": "{\\"type\\": \\"object\\", \\"properties\\": {\\"greeting\\": {\\"type\\": \\"string\\"}}}",
                    "response": {"greeting": "%s"}
                  }
                }
              ]
            }
            """;
    private static final String MANUAL_DEFINITION = """
        {"label": "Greet", "inputs": [],
         "triggers": [{"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}], "tasks": []}""";
    private static final String WORKFLOW_KEY = "greet";

    @Autowired
    private AppFacade appFacade;

    @Autowired
    private AppService appService;

    @Autowired
    @Qualifier("appLoopMcpContributor")
    private McpServerToolCallbackContributor appLoopMcpContributor;

    @Autowired
    private AppWorkflowFacade appWorkflowFacade;

    @Autowired
    private CallableWorkflowChecker callableWorkflowChecker;

    @Autowired
    private List<WorkflowKindChecker> workflowKindCheckers;

    private final JsonMapper jsonMapper = new JsonMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowTools projectWorkflowTools;

    @Autowired
    private WorkspaceScopeResolver workspaceScopeResolver;

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "admin", null, List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testBuildPublishDeployBindCallAndARebuildKeepsTheBinding() {
        long projectId = projectFacade.createProject(
            ProjectDTO.builder()
                .name("App loop " + UUID.randomUUID())
                .workspaceId(Workspace.DEFAULT_WORKSPACE_ID)
                .build());

        String workflowId = projectWorkflowFacade.addWorkflow(projectId, MANUAL_DEFINITION)
            .getWorkflowId();

        FakeWorkflowSpecialist fakeWorkflowSpecialist = new FakeWorkflowSpecialist(projectWorkflowTools);

        WorkspaceScopedSubAgentToolCallback buildWorkflow = new WorkspaceScopedSubAgentToolCallback(
            fakeWorkflowSpecialist, workspaceScopeResolver, workflowKindCheckers);

        fakeWorkflowSpecialist.target(workflowId, callableDefinition("v1"));

        assertThat(buildWorkflow.call(buildWorkflowInput(workflowId)))
            .contains("App-callable: " + workflowId);

        projectFacade.publishProject(projectId, "v1", false);

        AppLoopTools appLoopTools = new AppLoopTools(
            appFacade, appWorkflowFacade, callableWorkflowChecker, projectDeploymentFacade, projectFacade,
            projectWorkflowFacade);

        DeployProjectResult firstDeployment = appLoopTools.deployProject(
            projectId, null, null, null, null, developmentToolContext());

        assertThat(firstDeployment.status()).isEqualTo("deployed");
        assertThat(firstDeployment.enabled()).isTrue();

        DeployedWorkflow deployedWorkflow = firstDeployment.workflows()
            .getFirst();

        assertThat(deployedWorkflow.appCallable()).isTrue();
        assertThat(deployedWorkflow.bindCommand())
            .contains("--workflow-uuid " + deployedWorkflow.workflowUuid());

        long projectDeploymentId = Objects.requireNonNull(firstDeployment.projectDeploymentId());

        AppService.CreatedApp createdApp = appService.create(newApp());

        long appId = Objects.requireNonNull(
            createdApp.app()
                .getId());

        appWorkflowFacade.bindWorkflow(appId, WORKFLOW_KEY, projectDeploymentId, deployedWorkflow.workflowUuid());

        RestClient restClient = appRestClient(createdApp.secretKey());

        assertThat(isAvailable(restClient)).isTrue();
        assertThat(runGreeting(restClient)).isEqualTo("v1");

        String draftWorkflowId = Objects.requireNonNull(deployedWorkflow.draftWorkflowId());

        fakeWorkflowSpecialist.target(draftWorkflowId, callableDefinition("v2"));

        assertThat(buildWorkflow.call(buildWorkflowInput(draftWorkflowId)))
            .contains("App-callable: " + draftWorkflowId);

        projectFacade.publishProject(projectId, "v2", false);

        DeployProjectResult secondDeployment = appLoopTools.deployProject(
            projectId, null, null, null, null, developmentToolContext());

        assertThat(secondDeployment.status()).isEqualTo("deployed");
        assertThat(secondDeployment.projectDeploymentId()).isEqualTo(projectDeploymentId);
        assertThat(appWorkflowFacade.getWorkflowBindings(appId))
            .singleElement()
            .extracting(AppWorkflowBindingDTO::available)
            .isEqualTo(true);
        assertThat(isAvailable(restClient)).isTrue();
        assertThat(runGreeting(restClient)).isEqualTo("v2");
    }

    @Test
    void testTheRegisteredContributorOffersTheAppLoopTools() {
        assertThat(appLoopMcpContributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder(
                "deployProject", "listConnections", "listAppCallableWorkflows", "createAppApiCollection",
                "createAppApiEndpoint", "updateAppApiCollectionVersion", "listAppApiCollections",
                "createAppDataTable");
    }

    private static String callableDefinition(String greeting) {
        return CALLABLE_DEFINITION.replace("%s", greeting);
    }

    private RestClient appRestClient(String secretKey) {
        return RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
            .build();
    }

    private static String buildWorkflowInput(String workflowId) {
        return """
            {"request": "Make workflow %s greet the caller", "workspaceId": %d, "kind": "APP_CALLABLE"}"""
            .formatted(workflowId, Workspace.DEFAULT_WORKSPACE_ID);
    }

    private static ToolContext developmentToolContext() {
        return new ToolContext(
            Map.of(
                AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, Workspace.DEFAULT_WORKSPACE_ID,
                AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY,
                (long) Environment.DEVELOPMENT.ordinal()));
    }

    private boolean isAvailable(RestClient restClient) {
        String body = restClient.get()
            .uri("/api/app/v1/workflows")
            .retrieve()
            .body(String.class);

        for (JsonNode workflowNode : jsonMapper.readTree(body)) {
            if (WORKFLOW_KEY.equals(
                workflowNode.get("workflowKey")
                    .asString())) {

                return workflowNode.get("available")
                    .asBoolean();
            }
        }

        return false;
    }

    private static App newApp() {
        App app = new App();

        app.setEnvironment(Environment.DEVELOPMENT);
        app.setHostingTarget(AppHostingTarget.VERCEL);
        app.setName("agent-loop-" + UUID.randomUUID());
        app.setOwnerUserId(1L);
        app.setVisibility(ResourceVisibility.WORKSPACE);
        app.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);

        return app;
    }

    private String runGreeting(RestClient restClient) {
        String body = restClient.post()
            .uri("/api/app/v1/workflows/" + WORKFLOW_KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"input\": {\"name\": \"Ada\"}, \"waitSeconds\": 20}")
            .retrieve()
            .body(String.class);

        JsonNode workflowRunNode = jsonMapper.readTree(body);

        assertThat(
            workflowRunNode.get("status")
                .asString()).isEqualTo("COMPLETED");

        return workflowRunNode.get("result")
            .get("greeting")
            .asString();
    }

    private static final class FakeWorkflowSpecialist implements ToolCallback {

        private final ProjectWorkflowTools projectWorkflowTools;

        private String definition;
        private String workflowId;

        private FakeWorkflowSpecialist(ProjectWorkflowTools projectWorkflowTools) {
            this.projectWorkflowTools = projectWorkflowTools;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("buildWorkflow")
                .description("Builds workflows.")
                .inputSchema(
                    "{\"type\": \"object\", \"properties\": {\"request\": {\"type\": \"string\"}}, " +
                        "\"required\": [\"request\"]}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            projectWorkflowTools.updateWorkflow(workflowId, definition, null, toolContext);

            return "Built the workflow.";
        }

        private void target(String workflowId, String definition) {
            this.definition = definition;
            this.workflowId = workflowId;
        }
    }
}
