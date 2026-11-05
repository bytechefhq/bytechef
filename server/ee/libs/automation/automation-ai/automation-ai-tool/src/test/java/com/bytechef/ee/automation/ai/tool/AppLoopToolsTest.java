/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.dto.DeployProjectResultDTO;
import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.Candidate;
import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.DeployedConnection;
import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.RemovedWorkflow;
import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.UnresolvedConnection;
import com.bytechef.automation.configuration.dto.DeploymentConnectionChoiceDTO;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.exception.ProjectDeploymentErrorType;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.workflow.CallableWorkflowCheck;
import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
import com.bytechef.ee.automation.ai.tool.AppLoopTools.ConnectionChoice;
import com.bytechef.ee.automation.ai.tool.AppLoopTools.DeployProjectResult;
import com.bytechef.ee.automation.ai.tool.AppLoopTools.DeployedWorkflow;
import com.bytechef.ee.automation.app.dto.AppCallableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppLoopToolsTest {

    private static final String CALLABLE_UUID = "11111111-1111-4111-8111-111111111111";
    private static final long PROJECT_ID = 5L;
    private static final String SCHEDULED_UUID = "22222222-2222-4222-8222-222222222222";
    private static final long WORKSPACE_ID = 1049L;

    private AppFacade appFacade;
    private AppLoopTools appLoopTools;
    private AppWorkflowFacade appWorkflowFacade;
    private CallableWorkflowChecker callableWorkflowChecker;
    private ProjectDeploymentFacade projectDeploymentFacade;
    private ProjectFacade projectFacade;
    private ProjectWorkflowFacade projectWorkflowFacade;

    @BeforeEach
    void beforeEach() {
        appFacade = mock(AppFacade.class);
        appWorkflowFacade = mock(AppWorkflowFacade.class);
        callableWorkflowChecker = mock(CallableWorkflowChecker.class);
        projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
        projectFacade = mock(ProjectFacade.class);
        projectWorkflowFacade = mock(ProjectWorkflowFacade.class);

        appLoopTools = new AppLoopTools(
            appFacade, appWorkflowFacade, callableWorkflowChecker, projectDeploymentFacade, projectFacade,
            projectWorkflowFacade);

        when(callableWorkflowChecker.check("wf-callable")).thenReturn(new CallableWorkflowCheck(List.of(), "{}"));
        when(callableWorkflowChecker.check("wf-scheduled"))
            .thenReturn(new CallableWorkflowCheck(List.of("no Call-workflow trigger"), null));
    }

    @Test
    void testNonDevelopmentEnvironmentIsRefused() {
        DeployProjectResult deployProjectResult = appLoopTools.deployProject(
            PROJECT_ID, null, null, null, null, toolContext(Environment.PRODUCTION));

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.message()).isEqualTo("The app loop runs in DEVELOPMENT only");
        assertThat(appLoopTools.listConnections(null, toolContext(Environment.STAGING))
            .error()).isEqualTo("The app loop runs in DEVELOPMENT only");
        assertThat(appLoopTools.listAppCallableWorkflows(null, toolContext(Environment.PRODUCTION))
            .error()).isEqualTo("The app loop runs in DEVELOPMENT only");

        verifyNoInteractions(
            appFacade, appWorkflowFacade, callableWorkflowChecker, projectDeploymentFacade, projectFacade,
            projectWorkflowFacade);
    }

    @Test
    void testProjectOfAnotherWorkspaceIsRefused() {
        when(projectFacade.getProject(PROJECT_ID)).thenReturn(project(7L));

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.message()).contains("does not belong to workspace 1049");

        verify(projectDeploymentFacade, never()).deployProject(anyLong(), any(), anyList(), anyList(), anyMap());
    }

    @Test
    void testSeveralDevelopmentDeploymentsRequireAnId() {
        when(projectFacade.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));
        when(projectDeploymentFacade.getWorkspaceProjectDeployments(WORKSPACE_ID, 0L, PROJECT_ID, null, false))
            .thenReturn(List.of(projectDeployment(11L, "Orders"), projectDeployment(12L, "Orders copy")));

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.deployments())
            .extracting(AppLoopTools.DeploymentChoice::projectDeploymentId)
            .containsExactly(11L, 12L);

        verify(projectDeploymentFacade, never()).deployProject(anyLong(), any(), anyList(), anyList(), anyMap());
    }

    @Test
    void testDefaultEnabledWorkflowsAreTheAppCallableOnesAndConnectionsArePickedByTheFacade() {
        stubPublishedProject();

        when(projectDeploymentFacade.deployProject(
            PROJECT_ID, null, List.of(CALLABLE_UUID), List.of(), Map.of())).thenReturn(written(11L));

        deploy(null, null, null);

        verify(projectDeploymentFacade).deployProject(PROJECT_ID, null, List.of(CALLABLE_UUID), List.of(), Map.of());
        verify(projectDeploymentFacade).enableProjectDeployment(11L, true);
    }

    @Test
    void testExplicitWorkflowsConnectionsAndInputsArePassedThrough() {
        stubPublishedProject();

        List<DeploymentConnectionChoiceDTO> connectionChoices = List.of(
            new DeploymentConnectionChoiceDTO(SCHEDULED_UUID, "sendMessage_1", "slack", 101L));
        Map<String, Map<String, ?>> inputs = Map.of(SCHEDULED_UUID, Map.of("region", "eu"));

        when(projectDeploymentFacade.deployProject(
            PROJECT_ID, null, List.of(SCHEDULED_UUID), connectionChoices, inputs)).thenReturn(written(11L));

        deploy(
            List.of(SCHEDULED_UUID), List.of(new ConnectionChoice(SCHEDULED_UUID, "sendMessage_1", "slack", 101L)),
            Map.of(SCHEDULED_UUID, Map.of("region", "eu")));

        verify(projectDeploymentFacade).deployProject(
            PROJECT_ID, null, List.of(SCHEDULED_UUID), connectionChoices, inputs);
    }

    @Test
    void testUnresolvedConnectionsWriteNothingAndAreReported() {
        stubPublishedProject();

        UnresolvedConnection unresolvedConnection = new UnresolvedConnection(
            CALLABLE_UUID, "Greet", "sendMessage_1", "slack", "slack", true,
            List.of(new Candidate(101L, "Slack A"), new Candidate(102L, "Slack B")));

        when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
            .thenReturn(new DeployProjectResultDTO(
                null, 1, List.of(), List.of(), List.of(unresolvedConnection), List.of()));

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("unresolved");
        assertThat(deployProjectResult.unresolvedConnections()).containsExactly(unresolvedConnection);
        assertThat(deployProjectResult.message()).contains("Nothing was written");

        verify(projectDeploymentFacade, never()).enableProjectDeployment(anyLong(), anyBoolean());
    }

    @Test
    void testUnpublishedChangesAreReturnedAsARefusalWithTheFacadesMessage() {
        stubPublishedProject();

        when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
            .thenThrow(new ConfigurationException(
                "Project 5 has unpublished changes; call publishProject first",
                ProjectDeploymentErrorType.UNPUBLISHED_CHANGES));

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.message()).contains("call publishProject first");

        verify(projectDeploymentFacade, never()).enableProjectDeployment(anyLong(), anyBoolean());
    }

    @Test
    void testArmingFailureLeavesTheDeploymentDisabledAndIsReported() {
        stubPublishedProject();

        when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
            .thenReturn(written(11L));
        doThrow(new ConfigurationException("trigger rejected", ProjectDeploymentErrorType.INVALID_DEPLOYMENT))
            .when(projectDeploymentFacade)
            .enableProjectDeployment(11L, true);

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("arming failed");
        assertThat(deployProjectResult.enabled()).isFalse();
        assertThat(deployProjectResult.projectDeploymentId()).isEqualTo(11L);
        assertThat(deployProjectResult.message())
            .contains("trigger rejected")
            .contains("call deployProject again");
        assertThat(deployProjectResult.workflows())
            .extracting(DeployedWorkflow::bindCommand)
            .containsOnlyNulls();
    }

    @Test
    void testArmingFailureOfAForeignTypeReportsOnlyItsClass() {
        stubPublishedProject();

        when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
            .thenReturn(written(11L));
        doThrow(new IllegalStateException("JDBC URL=jdbc:postgresql://prod-db/secret key=sk-live-123"))
            .when(projectDeploymentFacade)
            .enableProjectDeployment(11L, true);

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("arming failed");
        assertThat(deployProjectResult.enabled()).isFalse();
        assertThat(deployProjectResult.message())
            .contains("IllegalStateException")
            .contains("call deployProject again")
            .doesNotContain("jdbc:postgresql")
            .doesNotContain("secret")
            .doesNotContain("sk-live-123");
    }

    @Test
    void testArmingFailureOfAnExecutionExceptionReportsOnlyItsClass() {
        stubPublishedProject();

        when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
            .thenReturn(written(11L));
        doThrow(
            new ExecutionException(
                "POST https://api.example.com/hooks?api_key=sk-live-456 returned 401",
                ProjectDeploymentErrorType.INVALID_DEPLOYMENT))
                    .when(projectDeploymentFacade)
                    .enableProjectDeployment(11L, true);

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("arming failed");
        assertThat(deployProjectResult.enabled()).isFalse();
        assertThat(deployProjectResult.message())
            .contains("ExecutionException")
            .contains("call deployProject again")
            .doesNotContain("api.example.com")
            .doesNotContain("sk-live-456");
    }

    @Test
    void testArmingDeniedReportsTheMissingPermission() {
        stubPublishedProject();

        when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
            .thenReturn(written(11L));
        doThrow(new AccessDeniedException("Access Denied"))
            .when(projectDeploymentFacade)
            .enableProjectDeployment(11L, true);

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("arming failed");
        assertThat(deployProjectResult.enabled()).isFalse();
        assertThat(deployProjectResult.projectDeploymentId()).isEqualTo(11L);
        assertThat(deployProjectResult.message())
            .contains("DEPLOYMENT_EDIT")
            .doesNotContain("call deployProject again");
    }

    @Test
    void testIllegalArgumentFromTheDeployReportsOnlyItsClass() {
        stubPublishedProject();

        when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
            .thenThrow(new IllegalArgumentException("https://internal.example/api?token=abc123"));

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.message())
            .contains("IllegalArgumentException")
            .doesNotContain("internal.example")
            .doesNotContain("abc123");
    }

    @Test
    void testExplicitDeploymentIdSkipsTheDeploymentListing() {
        stubPublishedProject();

        when(projectDeploymentFacade.deployProject(eq(PROJECT_ID), eq(11L), anyList(), anyList(), anyMap()))
            .thenReturn(written(11L));

        DeployProjectResult deployProjectResult = appLoopTools.deployProject(
            PROJECT_ID, 11L, null, null, null, toolContext(Environment.DEVELOPMENT));

        assertThat(deployProjectResult.status()).isEqualTo("deployed");

        verify(projectDeploymentFacade, never()).getWorkspaceProjectDeployments(
            anyLong(), anyLong(), any(), any(), anyBoolean());
    }

    @Test
    void testDeployedResultMarksAppCallableWorkflowsWithTheirBindCommand() {
        stubPublishedProject();

        when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
            .thenReturn(written(11L));

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("deployed");
        assertThat(deployProjectResult.enabled()).isTrue();
        assertThat(deployProjectResult.workflows())
            .extracting(
                DeployedWorkflow::workflowUuid, DeployedWorkflow::appCallable, DeployedWorkflow::inputSchema,
                DeployedWorkflow::draftWorkflowId, DeployedWorkflow::bindCommand)
            .containsExactly(
                tuple(
                    CALLABLE_UUID, true, "{}", "wf-callable-draft",
                    "bytechef automation app bind workflow --workflow-key greet_customers --deployment-id 11 " +
                        "--workflow-uuid " + CALLABLE_UUID),
                tuple(
                    SCHEDULED_UUID, false, null, "wf-scheduled-draft", null));
    }

    @Test
    void testRemovedWorkflowsPointToTheAppStatusCommand() {
        stubPublishedProject();

        DeployProjectResultDTO writtenWithRemoval = new DeployProjectResultDTO(
            11L, 1, List.of(), List.of(new RemovedWorkflow("33333333-3333-4333-8333-333333333333", "Old")),
            List.of(), List.of());

        when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
            .thenReturn(writtenWithRemoval);

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.removedWorkflows()).hasSize(1);
        assertThat(deployProjectResult.message()).contains("bytechef automation app status");
    }

    @Test
    void testListConnectionsPassesTheFacadeAnswerThrough() {
        List<AppWorkspaceConnectionDTO> connections = List.of(
            new AppWorkspaceConnectionDTO(101L, "Slack A", "slack", 1, "OAUTH2_AUTHORIZATION_CODE", "VALID", false,
                true),
            new AppWorkspaceConnectionDTO(-300L, "OpenAI", "openai", 1, null, "VALID", true, false));

        when(appFacade.getConnections(WORKSPACE_ID, Environment.DEVELOPMENT, "slack")).thenReturn(connections);

        AppLoopTools.ConnectionListResult connectionListResult = appLoopTools.listConnections(
            "slack", toolContext(Environment.DEVELOPMENT));

        assertThat(connectionListResult.error()).isNull();
        assertThat(connectionListResult.connections()).isEqualTo(connections);
    }

    @Test
    void testListAppCallableWorkflowsUsesTheWorkspaceAndProject() {
        List<AppCallableWorkflowDTO> workflows = List.of(
            new AppCallableWorkflowDTO(11L, CALLABLE_UUID, "Greet", PROJECT_ID, "Orders", "{}"));

        when(appWorkflowFacade.getCallableWorkflows(WORKSPACE_ID, PROJECT_ID)).thenReturn(workflows);

        assertThat(appLoopTools.listAppCallableWorkflows(PROJECT_ID, toolContext(Environment.DEVELOPMENT))
            .workflows()).isEqualTo(workflows);
    }

    @Test
    void testMissingWorkspaceContextIsRefused() {
        DeployProjectResult deployProjectResult = appLoopTools.deployProject(
            PROJECT_ID, null, null, null, null, new ToolContext(Map.of()));

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.message()).contains("workspaceId");

        verifyNoInteractions(
            appFacade, appWorkflowFacade, callableWorkflowChecker, projectDeploymentFacade, projectFacade,
            projectWorkflowFacade);
    }

    @Test
    void testMissingProjectIsARefusal() {
        when(projectFacade.getProject(PROJECT_ID)).thenThrow(new NoSuchElementException());

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.message()).isEqualTo("Project 5 does not exist");

        verify(projectDeploymentFacade, never()).deployProject(anyLong(), any(), anyList(), anyList(), anyMap());
    }

    @Test
    void testMissingElementWhileDeployingIsARefusal() {
        stubPublishedProject();

        when(projectDeploymentFacade.deployProject(anyLong(), any(), anyList(), anyList(), anyMap()))
            .thenThrow(new NoSuchElementException());

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.message())
            .isEqualTo("Project 5, or the deployment named, does not exist or cannot be deployed");
    }

    @Test
    void testAccessDeniedByTheDeployIsARefusal() {
        stubPublishedProject();

        when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
            .thenThrow(new AccessDeniedException("Access Denied"));

        DeployProjectResult deployProjectResult = deploy(null, null, null);

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.message()).contains("Access Denied");

        verify(projectDeploymentFacade, never()).enableProjectDeployment(anyLong(), anyBoolean());
    }

    @Test
    void testAccessDeniedByAListingIsReturnedAsItsError() {
        when(appFacade.getConnections(WORKSPACE_ID, Environment.DEVELOPMENT, null))
            .thenThrow(new AccessDeniedException("Access Denied"));
        when(appWorkflowFacade.getCallableWorkflows(WORKSPACE_ID, null))
            .thenThrow(new AccessDeniedException("Access Denied"));

        AppLoopTools.ConnectionListResult connectionListResult = appLoopTools.listConnections(
            null, toolContext(Environment.DEVELOPMENT));
        AppLoopTools.CallableWorkflowListResult callableWorkflowListResult = appLoopTools.listAppCallableWorkflows(
            null, toolContext(Environment.DEVELOPMENT));

        assertThat(connectionListResult.error()).contains("Access Denied");
        assertThat(connectionListResult.connections()).isEmpty();
        assertThat(callableWorkflowListResult.error()).contains("Access Denied");
        assertThat(callableWorkflowListResult.workflows()).isEmpty();
    }

    @Test
    void testConnectionChoiceWithAMissingFieldIsRefusedBeforeTheFacade() {
        stubPublishedProject();

        DeployProjectResult deployProjectResult = deploy(
            List.of(SCHEDULED_UUID), List.of(new ConnectionChoice(SCHEDULED_UUID, null, "slack", 101L)), null);

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.message()).contains("workflowNodeName");

        verify(projectDeploymentFacade, never()).deployProject(anyLong(), any(), anyList(), anyList(), anyMap());
    }

    @Test
    void testConnectionChoiceWithoutAConnectionIdIsRefusedNamingItsIndex() {
        stubPublishedProject();

        DeployProjectResult deployProjectResult = deploy(
            List.of(SCHEDULED_UUID),
            List.of(
                new ConnectionChoice(SCHEDULED_UUID, "sendMessage_1", "slack", 101L),
                new ConnectionChoice(SCHEDULED_UUID, "sendMessage_2", "slack", null)),
            null);

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.message())
            .contains("connections[1]")
            .contains("connectionId");

        verify(projectDeploymentFacade, never()).deployProject(anyLong(), any(), anyList(), anyList(), anyMap());
    }

    @Test
    void testNullEnabledWorkflowIsRefusedBeforeTheFacade() {
        stubPublishedProject();

        DeployProjectResult deployProjectResult = deploy(Arrays.asList(SCHEDULED_UUID, null), null, null);

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.message()).contains("enabledWorkflows");

        verify(projectDeploymentFacade, never()).deployProject(anyLong(), any(), anyList(), anyList(), anyMap());
    }

    @Test
    void testNullInputsOfAWorkflowAreRefusedBeforeTheFacade() {
        stubPublishedProject();

        Map<String, Map<String, Object>> inputs = new HashMap<>();

        inputs.put(SCHEDULED_UUID, null);

        DeployProjectResult deployProjectResult = deploy(List.of(SCHEDULED_UUID), null, inputs);

        assertThat(deployProjectResult.status()).isEqualTo("refused");
        assertThat(deployProjectResult.message()).contains("inputs of workflow " + SCHEDULED_UUID);

        verify(projectDeploymentFacade, never()).deployProject(anyLong(), any(), anyList(), anyList(), anyMap());
    }

    @ParameterizedTest
    @CsvSource({
        "Greet customers,greet_customers",
        "2024 Report!,workflow_2024_report",
        "!!!,workflow",
        "Überweisung prüfen,berweisung_pr_fen"
    })
    void testSuggestedWorkflowKeysAreValidBindingKeys(String workflowName, String expectedWorkflowKey) {
        assertThat(AppLoopTools.suggestWorkflowKey(workflowName))
            .isEqualTo(expectedWorkflowKey)
            .matches("^[a-z][a-z0-9_-]{0,63}$");
    }

    @Test
    void testSuggestedWorkflowKeyIsCutToSixtyFourCharacters() {
        assertThat(AppLoopTools.suggestWorkflowKey("a".repeat(80))).hasSize(64);
    }

    private DeployProjectResult deploy(
        List<String> enabledWorkflows, List<ConnectionChoice> connections, Map<String, Map<String, Object>> inputs) {

        return appLoopTools.deployProject(
            PROJECT_ID, null, enabledWorkflows, connections, inputs, toolContext(Environment.DEVELOPMENT));
    }

    private void stubPublishedProject() {
        ProjectWorkflowDTO callableWorkflow = mock(ProjectWorkflowDTO.class);
        ProjectWorkflowDTO scheduledWorkflow = mock(ProjectWorkflowDTO.class);

        when(callableWorkflow.getId()).thenReturn("wf-callable");
        when(callableWorkflow.getWorkflowUuid()).thenReturn(CALLABLE_UUID);
        when(scheduledWorkflow.getId()).thenReturn("wf-scheduled");
        when(scheduledWorkflow.getWorkflowUuid()).thenReturn(SCHEDULED_UUID);
        when(projectFacade.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));
        when(projectWorkflowFacade.getProjectVersionWorkflows(PROJECT_ID, 1, false))
            .thenReturn(List.of(callableWorkflow, scheduledWorkflow));
    }

    private static ProjectDTO project(long workspaceId) {
        return ProjectDTO.builder()
            .id(PROJECT_ID)
            .name("Orders")
            .projectVersions(
                List.of(
                    new ProjectVersion(1, ProjectVersion.Status.PUBLISHED.ordinal(), Instant.now(), "v1"),
                    new ProjectVersion(2)))
            .workspaceId(workspaceId)
            .build();
    }

    private static ProjectDeploymentDTO projectDeployment(long id, String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setId(id);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(PROJECT_ID);
        projectDeployment.setProjectVersion(1);

        return new ProjectDeploymentDTO(projectDeployment);
    }

    private static ToolContext toolContext(Environment environment) {
        return new ToolContext(
            Map.of(
                AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID,
                AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, (long) environment.ordinal()));
    }

    private static DeployProjectResultDTO written(long projectDeploymentId) {
        return new DeployProjectResultDTO(
            projectDeploymentId, 1,
            List.of(
                new DeployProjectResultDTO.DeployedWorkflow(
                    CALLABLE_UUID, "wf-callable", "Greet customers", true, "wf-callable-draft",
                    List.of(new DeployedConnection("sendMessage_1", "slack", 101L, "Slack A"))),
                new DeployProjectResultDTO.DeployedWorkflow(
                    SCHEDULED_UUID, "wf-scheduled", "Nightly sync", true, "wf-scheduled-draft", List.of())),
            List.of(), List.of(), List.of());
    }
}
