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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.ai.agent.tool.CoreAgentType;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.workflow.CallableWorkflowCheck;
import com.bytechef.automation.configuration.workflow.WorkflowKind;
import com.bytechef.automation.configuration.workflow.WorkflowKindChecker;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkspaceScopedSubAgentToolCallbackTest {

    private static final String CAPTURE_KEY = "bytechef.workflowEditor.persistedWorkflows";
    private static final long USER_ID = 1L;

    private ContextCapturingDelegate delegate;
    private WorkspaceFacade workspaceFacade;
    private WorkspaceScopeResolver workspaceScopeResolver;
    private WorkspaceScopedSubAgentToolCallback toolCallback;

    @BeforeEach
    void beforeEach() {
        delegate = new ContextCapturingDelegate();
        workspaceFacade = mock(WorkspaceFacade.class);

        UserService userService = mock(UserService.class);
        User user = new User();

        user.setId(USER_ID);

        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(
            List.of(workspace(42L, "Primary"), workspace(43L, "Secondary")));

        workspaceScopeResolver = new WorkspaceScopeResolver(userService, workspaceFacade);

        toolCallback = new WorkspaceScopedSubAgentToolCallback(delegate, workspaceScopeResolver);
    }

    @Test
    void testToolDefinitionKeepsDelegateNameAndExtendsSchema() {
        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("unknown");
        assertThat(toolCallback.getToolDefinition()
            .inputSchema()).contains("workspaceId")
                .contains("environment");
    }

    @Test
    void testBlankRequestReturnsError() {
        String result = toolCallback.call("{\"request\": \" \"}");

        assertThat(result)
            .contains("error")
            .contains("request is required");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testExplicitWorkspaceIdIsForwardedToDelegateContext() {
        String result = toolCallback.call("{\"request\": \"list servers\", \"workspaceId\": 42}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L);
        assertThat(delegate.capturedInput).contains("list servers");
    }

    @Test
    void testDefaultsEnvironmentToDevelopmentWhenOmitted() {
        toolCallback.call("{\"request\": \"list servers\", \"workspaceId\": 42}");

        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 0L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 0L);
    }

    @Test
    void testExplicitEnvironmentIsForwardedToDelegateContext() {
        String result = toolCallback.call(
            "{\"request\": \"list servers\", \"workspaceId\": 42, \"environment\": \"PRODUCTION\"}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L);
    }

    @Test
    void testExplicitEnvironmentIsCaseInsensitive() {
        String result = toolCallback.call(
            "{\"request\": \"list servers\", \"workspaceId\": 42, \"environment\": \"staging\"}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);
    }

    @Test
    void testUnknownEnvironmentReturnsError() {
        String result = toolCallback.call(
            "{\"request\": \"list servers\", \"workspaceId\": 42, \"environment\": \"NOPE\"}");

        assertThat(result).contains("error")
            .contains("Unknown environment")
            .contains("NOPE");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testForwardsWorkspaceIdUnderAgentToolInvocationContextKeyToo() {
        String result = toolCallback.call("{\"request\": \"list servers\", \"workspaceId\": 42}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L);
    }

    @Test
    void testExplicitForeignWorkspaceIdIsRejectedBeforeReachingDelegate() {
        String result = toolCallback.call("{\"request\": \"list servers\", \"workspaceId\": 99}");

        assertThat(result).contains("error")
            .contains("not accessible");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testSingleWorkspaceIsAutoSelected() {
        Workspace workspace = new Workspace();

        workspace.setId(7L);
        workspace.setName("Main");

        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace));

        String result = toolCallback.call("{\"request\": \"list servers\"}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L);
    }

    @Test
    void testMultipleWorkspacesReturnCandidateList() {
        Workspace firstWorkspace = new Workspace();

        firstWorkspace.setId(1L);
        firstWorkspace.setName("Alpha");

        Workspace secondWorkspace = new Workspace();

        secondWorkspace.setId(2L);
        secondWorkspace.setName("Beta");

        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(firstWorkspace, secondWorkspace));

        String result = toolCallback.call("{\"request\": \"list servers\"}");

        assertThat(result)
            .contains("workspace_required")
            .contains("Alpha")
            .contains("Beta");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testWrapsPlainToolCallbackDelegate() {
        PlainCopilotDelegate copilotDelegate = new PlainCopilotDelegate();

        WorkspaceScopedSubAgentToolCallback copilotToolCallback =
            new WorkspaceScopedSubAgentToolCallback(copilotDelegate, workspaceScopeResolver);

        String result = copilotToolCallback.call("{\"request\": \"list tables\", \"workspaceId\": 43}");

        assertThat(result).isEqualTo("delegated");
        assertThat(copilotToolCallback.getToolDefinition()
            .name()).isEqualTo("data_table_agent");
        assertThat(copilotToolCallback.getToolDefinition()
            .inputSchema()).contains("workspaceId");
        assertThat(copilotDelegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 43L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 43L);
    }

    @Nested
    class Kind {

        private WorkflowKindChecker appCallableChecker;
        private WorkflowKindChecker apiEndpointChecker;
        private FakeSpecialist fakeSpecialist;
        private WorkflowKindChecker scheduledChecker;

        @BeforeEach
        void beforeEach() {
            appCallableChecker = checker(WorkflowKind.APP_CALLABLE);
            apiEndpointChecker = checker(WorkflowKind.API_ENDPOINT);
            fakeSpecialist = new FakeSpecialist();
            scheduledChecker = checker(WorkflowKind.SCHEDULED);
        }

        @Test
        void testWithoutCheckersTheSchemaHasNoKind() {
            WorkspaceScopedSubAgentToolCallback toolCallback = new WorkspaceScopedSubAgentToolCallback(
                fakeSpecialist, workspaceScopeResolver);

            ToolDefinition toolDefinition = toolCallback.getToolDefinition();

            assertThat(toolDefinition.inputSchema())
                .doesNotContain("\"kind\"")
                .doesNotContain("appCallable");
        }

        @Test
        void testWithCheckersTheSchemaOffersOnlyTheirKinds() {
            WorkspaceScopedSubAgentToolCallback toolCallback = new WorkspaceScopedSubAgentToolCallback(
                fakeSpecialist, workspaceScopeResolver, List.of(scheduledChecker));

            ToolDefinition toolDefinition = toolCallback.getToolDefinition();

            assertThat(toolDefinition.inputSchema())
                .contains("\"kind\"")
                .contains("\"SCHEDULED\"")
                .doesNotContain("\"APP_CALLABLE\"")
                .doesNotContain("appCallable");
            assertThat(JsonUtils.readMap(toolDefinition.inputSchema())).containsKey("properties");
        }

        @Test
        void testAppCallableAppendsTheInstructionAndFlagsTheForwardedContextButForwardsOnlyTheRequest() {
            kindToolCallback().call(input("APP_CALLABLE"));

            assertThat(fakeSpecialist.capturedInput)
                .startsWith("{\"request\":\"Build a greeting workflow")
                .contains("workflow/v1/newWorkflowCall")
                .contains("workflow/v1/responseToWorkflowCall")
                .contains("an inputSchema parameter that is a string holding a JSON Schema")
                .contains("an outputSchema parameter that is a string holding a JSON Schema")
                .doesNotContain("APP_CALLABLE")
                .doesNotContain("workspaceId");
            assertThat(fakeSpecialist.capturedContext)
                .containsEntry(AppCallableDraftGuard.APP_CALLABLE_KEY, true)
                .containsKey(CAPTURE_KEY);
        }

        @Test
        void testNoKindSkipsCheck() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

            String result = kindToolCallback().call(
                "{\"request\": \"Build a greeting workflow\", \"workspaceId\": 42}");

            assertThat(result).isEqualTo("Built it.");
            assertThat(fakeSpecialist.capturedInput).isEqualTo("{\"request\":\"Build a greeting workflow\"}");
            assertThat(fakeSpecialist.capturedContext)
                .doesNotContainKey(AppCallableDraftGuard.APP_CALLABLE_KEY)
                .doesNotContainKey(CAPTURE_KEY);

            verify(appCallableChecker, never()).check(anyString());
            verify(apiEndpointChecker, never()).check(anyString());
            verify(scheduledChecker, never()).check(anyString());
        }

        @Test
        void testUnknownKindIsToolError() {
            String result = kindToolCallback().call(input("NOPE"));

            assertThat(result)
                .contains("\"error\"")
                .contains("Unknown kind 'NOPE'")
                .contains("APP_CALLABLE, API_ENDPOINT, SCHEDULED");
            assertThat(fakeSpecialist.capturedInput).isNull();
        }

        @Test
        void testAKindWithoutACheckerOnThisEditionIsToolError() {
            WorkspaceScopedSubAgentToolCallback toolCallback = new WorkspaceScopedSubAgentToolCallback(
                fakeSpecialist, workspaceScopeResolver, List.of(scheduledChecker));

            String result = toolCallback.call(input("APP_CALLABLE"));

            assertThat(result)
                .contains("\"error\"")
                .contains("Unknown kind 'APP_CALLABLE'")
                .contains("Supported: SCHEDULED");
            assertThat(fakeSpecialist.capturedInput).isNull();
        }

        @Test
        void testKindApiEndpointAppendsInstructionAndChecks() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

            when(apiEndpointChecker.check("wf-1")).thenReturn(new CallableWorkflowCheck(List.of(), null));
            when(apiEndpointChecker.fetchWorkflowUuid("wf-1")).thenReturn(Optional.of("uuid-1"));

            String result = kindToolCallback().call(input("API_ENDPOINT"));

            assertThat(fakeSpecialist.capturedInput)
                .contains("apiPlatform/v1/newApiRequest")
                .contains("apiPlatform/v1/responseToAPIRequest")
                .contains("responseType")
                .contains("headers, parameters and body")
                .contains("SUCCESS, INVALID_INPUT, INTERNAL_ERROR or FORBIDDEN")
                .contains("a response parameter")
                .contains("success, invalidInput, internalError or forbidden")
                .contains("JSON body")
                .doesNotContain("TIMEOUT")
                .contains("DRAFT");
            assertThat(fakeSpecialist.capturedContext)
                .containsEntry(AppCallableDraftGuard.APP_CALLABLE_KEY, true);
            assertThat(result)
                .startsWith("Built it.")
                .contains("API endpoint workflow: wf-1 (uuid uuid-1).")
                .contains("Publish the project, then createAppApiCollection / createAppApiEndpoint.");

            verify(apiEndpointChecker).check("wf-1");
            verify(appCallableChecker, never()).check(anyString());
            verify(scheduledChecker, never()).check(anyString());
        }

        @Test
        void testKindApiEndpointReportsTheCheckersProblems() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

            when(apiEndpointChecker.check("wf-1")).thenReturn(
                new CallableWorkflowCheck(
                    List.of("trigger is manual/v1/manual, not apiPlatform/v1/newApiRequest"), null));

            String result = kindToolCallback().call(input("API_ENDPOINT"));

            assertThat(result)
                .contains("\"error\"")
                .contains("The workflow was saved but it is not an API endpoint workflow")
                .contains("workflowId wf-1: trigger is manual/v1/manual, not apiPlatform/v1/newApiRequest")
                .contains("Call buildWorkflow again with kind API_ENDPOINT");
        }

        @Test
        void testKindScheduledReportsMissingDataTableWrite() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

            when(scheduledChecker.check("wf-1")).thenReturn(
                new CallableWorkflowCheck(List.of("no dataTable write task"), null));

            String result = kindToolCallback().call(input("SCHEDULED"));

            assertThat(fakeSpecialist.capturedInput)
                .contains("schedule/v1/cron")
                .contains("schedule/v1/interval")
                .contains("dataTable/v1/createRecords")
                .contains("dataTable/v1/updateRecord")
                .contains("baseName");
            assertThat(result)
                .contains("\"error\"")
                .contains("The workflow was saved but it is not a scheduled workflow")
                .contains("workflowId wf-1: no dataTable write task")
                .contains("Call buildWorkflow again with kind SCHEDULED");
        }

        @Test
        void testKindScheduledNamesDeployProjectWhereTheAppLoopIsRegistered() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

            when(scheduledChecker.check("wf-1")).thenReturn(new CallableWorkflowCheck(List.of(), null));
            when(scheduledChecker.fetchWorkflowUuid("wf-1")).thenReturn(Optional.of("uuid-1"));

            String result = kindToolCallback().call(input("SCHEDULED"));

            assertThat(result)
                .startsWith("Built it.")
                .contains(
                    "Scheduled workflow: wf-1 (uuid uuid-1). Publish the project, then call deployProject with this " +
                        "workflow uuid in enabledWorkflows.")
                .doesNotContain("promoteWorkflow");
        }

        @Test
        void testKindScheduledPointsToTheUiWithoutTheAppLoop() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

            when(scheduledChecker.check("wf-1")).thenReturn(new CallableWorkflowCheck(List.of(), null));
            when(scheduledChecker.fetchWorkflowUuid("wf-1")).thenReturn(Optional.of("uuid-1"));

            String result = new WorkspaceScopedSubAgentToolCallback(
                fakeSpecialist, workspaceScopeResolver, List.of(scheduledChecker)).call(input("SCHEDULED"));

            assertThat(result)
                .startsWith("Built it.")
                .contains(
                    "Scheduled workflow: wf-1 (uuid uuid-1). Publish the project, then deploy it in the ByteChef UI " +
                        "with this workflow enabled.")
                .doesNotContain("deployProject")
                .doesNotContain("promoteWorkflow");
        }

        @Test
        void testKindScheduledNamesSeveralUuidsInThePlural() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1", "wf-2");

            when(scheduledChecker.check(anyString())).thenReturn(new CallableWorkflowCheck(List.of(), null));
            when(scheduledChecker.fetchWorkflowUuid("wf-1")).thenReturn(Optional.of("uuid-1"));
            when(scheduledChecker.fetchWorkflowUuid("wf-2")).thenReturn(Optional.of("uuid-2"));

            String result = kindToolCallback().call(input("SCHEDULED"));

            assertThat(result)
                .contains("Scheduled workflows: wf-1 (uuid uuid-1), wf-2 (uuid uuid-2).")
                .contains("call deployProject with these workflow uuids in enabledWorkflows");
        }

        @Test
        void testKindScheduledDropsTheUuidPhraseWhenNoneResolves() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

            when(scheduledChecker.check("wf-1")).thenReturn(new CallableWorkflowCheck(List.of(), null));
            when(scheduledChecker.fetchWorkflowUuid("wf-1")).thenThrow(new IllegalStateException("db down"));

            String result = kindToolCallback().call(input("SCHEDULED"));

            assertThat(result)
                .contains(
                    "Scheduled workflow: wf-1. Publish the project, then call deployProject with the workflow enabled.")
                .doesNotContain("uuid")
                .doesNotContain("db down");
        }

        @Test
        void testTwoCheckersOfOneKindKeepTheFirstAndWarn() {
            WorkflowKindChecker otherScheduledChecker = checker(WorkflowKind.SCHEDULED);
            ch.qos.logback.classic.Logger callbackLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(WorkspaceScopedSubAgentToolCallback.class);
            ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

            listAppender.start();
            callbackLogger.addAppender(listAppender);

            try {
                WorkspaceScopedSubAgentToolCallback toolCallback = new WorkspaceScopedSubAgentToolCallback(
                    fakeSpecialist, workspaceScopeResolver, List.of(scheduledChecker, otherScheduledChecker));

                fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

                when(scheduledChecker.check("wf-1")).thenReturn(new CallableWorkflowCheck(List.of(), null));

                toolCallback.call(input("SCHEDULED"));
            } finally {
                callbackLogger.detachAppender(listAppender);
            }

            verify(scheduledChecker).check("wf-1");
            verify(otherScheduledChecker, never()).check(anyString());

            assertThat(listAppender.list)
                .anySatisfy(loggingEvent -> {
                    assertThat(loggingEvent.getLevel()).isEqualTo(Level.WARN);
                    assertThat(loggingEvent.getFormattedMessage())
                        .contains("Workflow kind SCHEDULED is claimed by both");
                });
        }

        @Test
        void testACallableWorkflowPassesAndIsMarked() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

            when(appCallableChecker.check("wf-1")).thenReturn(new CallableWorkflowCheck(List.of(), "{}"));

            String result = kindToolCallback().call(input("APP_CALLABLE"));

            assertThat(result)
                .startsWith("Built it.")
                .contains("App-callable: wf-1.")
                .contains("call deployProject");
        }

        @Test
        void testAManualTriggerFailsNamingTheWorkflowWhichStaysSaved() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

            when(appCallableChecker.check("wf-1")).thenReturn(
                new CallableWorkflowCheck(
                    List.of("no Call-workflow trigger: the trigger must be workflow/v1/newWorkflowCall"), null));

            String result = kindToolCallback().call(input("APP_CALLABLE"));

            assertThat(result)
                .contains("\"error\"")
                .contains("The workflow was saved but an app cannot call it")
                .contains("workflowId wf-1: no Call-workflow trigger")
                .contains("Call buildWorkflow again with kind APP_CALLABLE");
        }

        @Test
        void testAnUnreadableSchemaFailsNamingTheWorkflow() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

            when(appCallableChecker.check("wf-1")).thenReturn(
                new CallableWorkflowCheck(List.of("unreadable inputSchema: The schema is not valid JSON"),
                    "{not json"));

            assertThat(kindToolCallback().call(input("APP_CALLABLE")))
                .contains("workflowId wf-1: unreadable inputSchema");
        }

        @Test
        void testNothingPersistedPassesTheReplyThrough() {
            String result = kindToolCallback().call(input("APP_CALLABLE"));

            assertThat(result).isEqualTo("Built it.");
        }

        @Test
        void testEachDistinctCapturedWorkflowIsCheckedOnceAndOnlyFailuresAreNamed() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1", "wf-1", "wf-2");

            when(appCallableChecker.check("wf-1")).thenReturn(new CallableWorkflowCheck(List.of(), "{}"));
            when(appCallableChecker.check("wf-2")).thenReturn(
                new CallableWorkflowCheck(
                    List.of("no Call-workflow trigger: the trigger must be workflow/v1/newWorkflowCall"), null));

            String result = kindToolCallback().call(input("APP_CALLABLE"));

            assertThat(result)
                .contains("workflowId wf-2")
                .doesNotContain("workflowId wf-1");

            verify(appCallableChecker, times(1)).check("wf-1");
            verify(appCallableChecker, times(1)).check("wf-2");
        }

        @Test
        void testACheckerFailingWithAJacksonExceptionIsReportedAsNotCheckedWithoutItsText() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

            when(appCallableChecker.check("wf-1")).thenThrow(
                MismatchedInputException.from(
                    null, String.class, "Cannot deserialize value of type `java.lang.String` from Object value"));

            String result = kindToolCallback().call(input("APP_CALLABLE"));

            assertThat(result)
                .contains("The workflow was saved but an app cannot call it")
                .contains("workflowId wf-1: could not be checked")
                .contains("Call buildWorkflow again")
                .doesNotContain("Invalid tool input")
                .doesNotContain("Cannot deserialize");
        }

        @Test
        void testACheckerFailingForOneWorkflowStillChecksTheOthers() {
            fakeSpecialist.persistedWorkflowIds = List.of("wf-1", "wf-2");

            when(appCallableChecker.check("wf-1"))
                .thenThrow(new IllegalStateException("connection refused: db-7"));
            when(appCallableChecker.check("wf-2")).thenReturn(new CallableWorkflowCheck(List.of(), "{}"));

            String result = kindToolCallback().call(input("APP_CALLABLE"));

            assertThat(result)
                .contains("workflowId wf-1: could not be checked")
                .doesNotContain("workflowId wf-2")
                .doesNotContain("connection refused");

            verify(appCallableChecker, times(1)).check("wf-2");
        }

        private static WorkflowKindChecker checker(WorkflowKind workflowKind) {
            WorkflowKindChecker workflowKindChecker = mock(WorkflowKindChecker.class);

            when(workflowKindChecker.kind()).thenReturn(workflowKind);

            return workflowKindChecker;
        }

        private WorkspaceScopedSubAgentToolCallback kindToolCallback() {
            return new WorkspaceScopedSubAgentToolCallback(
                fakeSpecialist, workspaceScopeResolver,
                List.of(appCallableChecker, apiEndpointChecker, scheduledChecker));
        }

        private static String input(String kind) {
            return "{\"request\": \"Build a greeting workflow\", \"workspaceId\": 42, \"kind\": \"" + kind +
                "\"}";
        }
    }

    private static Workspace workspace(long id, String name) {
        Workspace workspace = new Workspace();

        workspace.setId(id);
        workspace.setName(name);

        return workspace;
    }

    private static final class ContextCapturingDelegate implements ToolCallback {

        private Map<String, Object> capturedContext;
        private String capturedInput;

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name(CoreAgentType.UNKNOWN.key())
                .description("Manages project deployments.")
                .inputSchema(
                    "{\"type\": \"object\", \"properties\": {\"request\": {\"type\": \"string\"}}, \"required\": [\"request\"]}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            capturedInput = toolInput;
            capturedContext = toolContext == null ? null : toolContext.getContext();

            return "done";
        }
    }

    private static final class PlainCopilotDelegate implements ToolCallback {

        private Map<String, Object> capturedContext;

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("data_table_agent")
                .description("Delegates data table work.")
                .inputSchema("{\"type\": \"object\", \"properties\": {\"request\": {\"type\": \"string\"}}}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            capturedContext = toolContext == null ? null : toolContext.getContext();

            return "delegated";
        }
    }

    private static final class FakeSpecialist implements ToolCallback {

        private Map<String, Object> capturedContext;
        private String capturedInput;
        private List<String> persistedWorkflowIds = List.of();

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
        @SuppressWarnings("unchecked")
        public String call(String toolInput, @Nullable ToolContext toolContext) {
            capturedInput = toolInput;
            capturedContext = toolContext == null ? Map.of() : toolContext.getContext();

            Object holder = capturedContext.get(CAPTURE_KEY);

            if (holder instanceof List<?> captures) {
                for (String workflowId : persistedWorkflowIds) {
                    Map<String, Object> entry = new HashMap<>();

                    entry.put("workflowId", workflowId);

                    ((List<Map<String, Object>>) captures).add(entry);
                }
            }

            return "Built it.";
        }
    }
}
