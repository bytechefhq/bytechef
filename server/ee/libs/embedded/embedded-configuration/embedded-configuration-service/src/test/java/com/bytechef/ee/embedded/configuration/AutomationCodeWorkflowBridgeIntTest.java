/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.codeworkflowbridge.AutomationCodeWorkflowBridgeIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflowConnection;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectCodeWorkflowFacade;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowConnectionRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * End-to-end integration test for the embedded automation code-workflow bridge, exercising Tasks 1-6 together against a
 * real Postgres (Testcontainers): deploy an artifact through {@link AutomationWorkflowProjectCodeWorkflowFacade}, wire
 * connected-user references through {@link ConnectedUserCodeWorkflowReferenceFacade}, and confirm the seams that only
 * show up when both meet -- uuid carry-forward across a redeploy, the dangling sweep it triggers, catalog-project
 * isolation, connection auto-wiring, and per-environment deployment isolation.
 *
 * <p>
 * Mock boundary: {@link ComponentConnectionFacade}, {@link ConnectionService}, {@link EnvironmentService}, and the
 * job/trigger/MCP/API-key/OAuth2 collaborators of the real {@code ProjectDeploymentFacadeImpl} are mocked -- they are
 * execution-time concerns (trigger enable/disable, job dispatch) that the deploy/redeploy/dangling/reference seam under
 * test never touches (our fixture workflows declare no triggers). {@link ComponentDefinitionService} is mocked and
 * driven directly per test to control whether the fixture's single task type ("codeWorkflow") is treated as
 * connection-required, which is what lets the connection round-trip test (priority 3) exercise
 * {@link MissingConnectionException} without a real component registry. Everything else --
 * {@code ProjectService}/{@code ProjectWorkflowService}/{@code ProjectDeploymentService}/
 * {@code ProjectDeploymentWorkflowService}, {@code ProjectCodeWorkflowService}, {@code CodeWorkflowContainerService}/
 * {@code CodeWorkflowContainerFacade}, {@code ConnectedUserService}, and the whole embedded-configuration facade layer
 * -- is real, backed by the Testcontainers Postgres instance. See
 * {@link AutomationCodeWorkflowBridgeIntTestConfiguration} for why its Spring config lives outside this package.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationCodeWorkflowBridgeIntTestConfiguration.class,
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    ClusterElementDefinitionService.class, ComponentConnectionFacade.class, ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionLifecycleFacade.class,
    ConnectionService.class, EmbeddedPermissionEvaluator.class, EnvironmentService.class, GitHubProxyClient.class,
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectFacade.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class AutomationCodeWorkflowBridgeIntTest {

    private static final String CODE_WORKFLOW_COMPONENT_NAME = "codeWorkflow";

    @Autowired
    private AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @Autowired
    private ConnectedUserProjectWorkflowConnectionRepository connectedUserProjectWorkflowConnectionRepository;

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private ConnectedUserProjectService connectedUserProjectService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        // The fixture's single task type always renders as "codeWorkflow/v1/perform" (CodeWorkflowContainerFacadeImpl
        // hardcodes it), so by default this component declares no connection requirement -- tests that are not about
        // connection wiring (priorities 1, 2, 4) never have to deal with MissingConnectionException. The connection
        // round-trip test (priority 3) overrides this to null to force the "unknown component, might need a
        // connection" branch in ConnectedUserWorkflowConnectionResolver.
        when(componentDefinitionService.getComponentDefinition(anyString(), anyInt()))
            .thenReturn(new ComponentDefinition(CODE_WORKFLOW_COMPONENT_NAME));
        when(connectionService.getConnections(PlatformType.EMBEDDED))
            .thenReturn(List.of());
    }

    /**
     * Priority 1: deploy v1 (workflows A+B) -> a user provisions a reference to A -> a second user provisions a
     * reference to B -> redeploy v2 (A unchanged, B removed, C added). A's reference must survive with the SAME
     * catalog_workflow_uuid, B's reference must be flagged dangling (redeploy's automatic
     * {@code markDanglingReferences} call), and C must be listed in the new published version.
     */
    @Test
    void testRedeployCarriesUuidForwardAndMarksRemovedWorkflowDangling() {
        connectedUserService.createConnectedUser("userLifecycleA", Environment.PRODUCTION);
        connectedUserService.createConnectedUser("userLifecycleB", Environment.PRODUCTION);

        automationWorkflowProjectCodeWorkflowFacade.save(
            projectSource("lifecycle-project", "workflowA", "workflowB"), Language.JAVASCRIPT);

        long projectId = automationWorkflowProjectFacade.fetchProjectIdByName("lifecycle-project")
            .orElseThrow();

        int publishedV1 = publishedVersion(projectId);

        ProjectWorkflow workflowARowV1 = findProjectWorkflowByLabel(projectId, publishedV1, "workflowA");
        ProjectWorkflow workflowBRowV1 = findProjectWorkflowByLabel(projectId, publishedV1, "workflowB");

        String workflowAUuid = workflowARowV1.getUuidAsString();
        String workflowBUuid = workflowBRowV1.getUuidAsString();

        ConnectedUserProjectWorkflow referenceA = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            "userLifecycleA", workflowAUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow referenceB = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            "userLifecycleB", workflowBUuid, Environment.PRODUCTION);

        assertThat(referenceA.isDangling()).isFalse();
        assertThat(referenceB.isDangling()).isFalse();

        // Redeploy: A unchanged, B removed, C added. save() triggers markDanglingReferences internally.
        automationWorkflowProjectCodeWorkflowFacade.save(
            projectSource("lifecycle-project", "workflowA", "workflowC"), Language.JAVASCRIPT);

        int publishedV2 = publishedVersion(projectId);

        ProjectWorkflow workflowARowV2 = findProjectWorkflowByLabel(projectId, publishedV2, "workflowA");
        ProjectWorkflow workflowCRowV2 = findProjectWorkflowByLabel(projectId, publishedV2, "workflowC");

        assertThat(workflowARowV2.getUuidAsString())
            .as("uuid carry-forward: workflowA must keep the same uuid across the redeploy")
            .isEqualTo(workflowAUuid);
        assertThat(workflowCRowV2)
            .as("workflowC must be listable in the redeployed catalog project")
            .isNotNull();

        ConnectedUserProjectWorkflow reloadedReferenceA = connectedUserProjectWorkflowRepository
            .findById(referenceA.getId())
            .orElseThrow();
        ConnectedUserProjectWorkflow reloadedReferenceB = connectedUserProjectWorkflowRepository
            .findById(referenceB.getId())
            .orElseThrow();

        assertThat(reloadedReferenceA.isDangling())
            .as("A's reference must survive the redeploy since its uuid was carried forward")
            .isFalse();
        assertThat(reloadedReferenceB.isDangling())
            .as("B's reference must be flagged dangling since workflowB was removed from the catalog")
            .isTrue();
        assertThat(reloadedReferenceB.getDanglingReason())
            .isEqualTo("Removed from the catalog project on redeploy");
    }

    /**
     * Priority 2: a second catalog project's references must be untouched by the first project's redeploy -- the
     * dangling sweep is scoped per catalog project, never a repository-wide scan.
     */
    @Test
    void testRedeployOfOneCatalogProjectNeverDanglesAnotherCatalogProjectsReferences() {
        connectedUserService.createConnectedUser("userCrossA", Environment.PRODUCTION);
        connectedUserService.createConnectedUser("userCrossB", Environment.PRODUCTION);

        automationWorkflowProjectCodeWorkflowFacade.save(
            projectSource("cross-project-a", "onlyWorkflow"), Language.JAVASCRIPT);
        automationWorkflowProjectCodeWorkflowFacade.save(
            projectSource("cross-project-b", "onlyWorkflow"), Language.JAVASCRIPT);

        long projectAId = automationWorkflowProjectFacade.fetchProjectIdByName("cross-project-a")
            .orElseThrow();
        long projectBId = automationWorkflowProjectFacade.fetchProjectIdByName("cross-project-b")
            .orElseThrow();

        String workflowAUuid = findProjectWorkflowByLabel(projectAId, publishedVersion(projectAId), "onlyWorkflow")
            .getUuidAsString();
        String workflowBUuid = findProjectWorkflowByLabel(projectBId, publishedVersion(projectBId), "onlyWorkflow")
            .getUuidAsString();

        ConnectedUserProjectWorkflow referenceToA = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            "userCrossA", workflowAUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow referenceToB = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            "userCrossB", workflowBUuid, Environment.PRODUCTION);

        // Redeploy project A only, with its workflow renamed away -- this dangles referenceToA, but must never touch
        // referenceToB, which belongs to an entirely different catalog project.
        automationWorkflowProjectCodeWorkflowFacade.save(
            projectSource("cross-project-a", "renamedWorkflow"), Language.JAVASCRIPT);

        ConnectedUserProjectWorkflow reloadedReferenceToA = connectedUserProjectWorkflowRepository
            .findById(referenceToA.getId())
            .orElseThrow();
        ConnectedUserProjectWorkflow reloadedReferenceToB = connectedUserProjectWorkflowRepository
            .findById(referenceToB.getId())
            .orElseThrow();

        assertThat(reloadedReferenceToA.isDangling()).isTrue();
        assertThat(reloadedReferenceToB.isDangling())
            .as("cross-project-b's reference must be untouched by cross-project-a's redeploy")
            .isFalse();
    }

    /**
     * Priority 3: provisioning a reference to a workflow whose component has no matching connection leaves the
     * reference disabled and rethrows {@link MissingConnectionException} naming the component. Once the connected user
     * creates the matching connection, enabling the reference re-resolves it, wires it into both the bookkeeping table
     * and the real {@link ProjectDeploymentWorkflow} connections, and succeeds.
     */
    @Test
    void testProvisionThenEnableRoundTripWithAConnection() {
        // Force the "unknown component, might require a connection" branch instead of the @BeforeEach default.
        when(componentDefinitionService.getComponentDefinition(anyString(), anyInt()))
            .thenReturn(null);

        connectedUserService.createConnectedUser("userConnection", Environment.PRODUCTION);

        automationWorkflowProjectCodeWorkflowFacade.save(
            projectSource("connection-project", "connectedWorkflow"), Language.JAVASCRIPT);

        long projectId = automationWorkflowProjectFacade.fetchProjectIdByName("connection-project")
            .orElseThrow();
        String workflowUuid = findProjectWorkflowByLabel(projectId, publishedVersion(projectId), "connectedWorkflow")
            .getUuidAsString();

        when(connectionService.getConnections(PlatformType.EMBEDDED))
            .thenReturn(List.of());

        assertThatThrownBy(
            () -> connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                "userConnection", workflowUuid, Environment.PRODUCTION))
                    .isInstanceOf(MissingConnectionException.class)
                    .satisfies(exception -> assertThat(((MissingConnectionException) exception).getComponentName())
                        .isEqualTo(CODE_WORKFLOW_COMPONENT_NAME));

        ConnectedUserProjectWorkflow disabledReference = connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(
                connectedUserProjectId("userConnection", Environment.PRODUCTION), workflowUuid)
            .orElseThrow();

        assertThat(disabledReference.isEnabled()).isFalse();

        // The connected user creates the missing connection.
        Connection connection = new Connection();

        connection.setId(777L);
        connection.setComponentName(CODE_WORKFLOW_COMPONENT_NAME);

        when(connectionService.getConnections(PlatformType.EMBEDDED))
            .thenReturn(List.of(connection));

        connectedUserCodeWorkflowReferenceFacade.enableReference(
            "userConnection", workflowUuid, true, Environment.PRODUCTION);

        ConnectedUserProjectWorkflow enabledReference = connectedUserProjectWorkflowRepository
            .findById(disabledReference.getId())
            .orElseThrow();

        assertThat(enabledReference.isEnabled()).isTrue();

        List<ConnectedUserProjectWorkflowConnection> wiredConnections =
            connectedUserProjectWorkflowConnectionRepository.findAllByConnectedUserProjectWorkflowId(
                enabledReference.getId());

        assertThat(wiredConnections)
            .extracting(ConnectedUserProjectWorkflowConnection::getConnectionId)
            .containsExactly(777L);

        String catalogWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(workflowUuid);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflow(enabledReference.getProjectDeploymentId(), catalogWorkflowId);

        assertThat(projectDeploymentWorkflow.getConnections())
            .as("enabling must wire the real ProjectDeploymentWorkflow connections, not just the bookkeeping table")
            .extracting(connectionEntry -> connectionEntry.getConnectionId())
            .containsExactly(777L);
    }

    /**
     * Priority 4: the same connected user provisioning the same catalog workflow in two different environments must get
     * two isolated {@code ProjectDeployment}s.
     */
    @Test
    void testSameUserInTwoEnvironmentsGetsIsolatedDeployments() {
        connectedUserService.createConnectedUser("userMultiEnv", Environment.PRODUCTION);
        connectedUserService.createConnectedUser("userMultiEnv", Environment.DEVELOPMENT);

        automationWorkflowProjectCodeWorkflowFacade.save(
            projectSource("multi-env-project", "envWorkflow"), Language.JAVASCRIPT);

        long projectId = automationWorkflowProjectFacade.fetchProjectIdByName("multi-env-project")
            .orElseThrow();
        String workflowUuid = findProjectWorkflowByLabel(projectId, publishedVersion(projectId), "envWorkflow")
            .getUuidAsString();

        ConnectedUserProjectWorkflow productionReference = connectedUserCodeWorkflowReferenceFacade
            .getOrCreateReference("userMultiEnv", workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow developmentReference = connectedUserCodeWorkflowReferenceFacade
            .getOrCreateReference("userMultiEnv", workflowUuid, Environment.DEVELOPMENT);

        assertThat(productionReference.getProjectDeploymentId())
            .isNotEqualTo(developmentReference.getProjectDeploymentId());
    }

    private long connectedUserProjectId(String externalUserId, Environment environment) {
        return connectedUserProjectService.getConnectUserProject(externalUserId, environment)
            .getId();
    }

    private int publishedVersion(long projectId) {
        Project project = projectService.getProject(projectId);

        return project.getLastPublishedProjectVersion()
            .getVersion();
    }

    private ProjectWorkflow findProjectWorkflowByLabel(long projectId, int projectVersion, String label) {
        List<ProjectWorkflow> workflows = projectWorkflowService.getProjectWorkflows(projectId, projectVersion);

        for (ProjectWorkflow projectWorkflow : workflows) {
            Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

            if (workflow.getDefinition()
                .contains("\"label\":\"" + label + "\"")) {

                return projectWorkflow;
            }
        }

        throw new AssertionError(
            "No project workflow found with label '" + label + "' in project=" + projectId + " version="
                + projectVersion);
    }

    // The newlines are embedded JavaScript source content, not console formatting; %n would corrupt the script.
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static byte[] projectSource(String projectName, String... workflowNames) {
        String workflowsJs = Arrays.stream(workflowNames)
            .map(AutomationCodeWorkflowBridgeIntTest::workflowJs)
            .collect(Collectors.joining(","));

        String source = """
            ({
                name: "%s",
                version: "1",
                description: "A code workflow.",
                workflows: [%s]
            })
            """.formatted(projectName, workflowsJs);

        return source.getBytes(StandardCharsets.UTF_8);
    }

    // The newlines are embedded JavaScript source content, not console formatting; %n would corrupt the script.
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static String workflowJs(String workflowName) {
        return """
            {
                name: "%s",
                label: "%s",
                tasks: [
                    {
                        name: "task1",
                        label: "Task 1",
                        perform: function () {
                            return "hello";
                        }
                    }
                ]
            }
            """.formatted(workflowName, workflowName);
    }
}
