/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgent;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentSchedule;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentScheduleService;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.personalagent.ScheduleFrequencyKind;
import com.bytechef.ee.ai.hub.personalagent.ScheduleLifecycleKind;
import com.bytechef.ee.ai.hub.personalagent.WorkspaceAiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskKind;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.web.graphql.AiHubPersonalAgentGraphQlController.AiHubPersonalAgentScheduleInputBody;
import com.bytechef.ee.ai.hub.web.graphql.AiHubPersonalAgentGraphQlController.CreateAiHubPersonalAgentInput;
import com.bytechef.ee.ai.hub.web.graphql.AiHubPersonalAgentGraphQlController.CreateAiHubPersonalAgentTaskInput;
import com.bytechef.ee.ai.hub.web.graphql.AiHubPersonalAgentGraphQlController.SetAiHubPersonalAgentScheduleInput;
import com.bytechef.ee.ai.hub.web.graphql.AiHubPersonalAgentGraphQlController.UpdateAiHubPersonalAgentInput;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiHubPersonalAgentGraphQlController}. The controller is mostly a thin pass-through to the
 * service, but two contracts ARE controller-owned and worth pinning:
 *
 * <ul>
 * <li>The {@link #updateAiHubPersonalAgent} guard rejects all-null patch inputs at the controller layer — the GraphQL
 * surface accepts optional fields, so the LLM/UI could submit an empty patch by mistake. Without the guard the
 * service-layer setter ladder runs three null-checks and saves an unchanged row, surprising the user with a
 * non-event.</li>
 * <li>The {@link #createAiHubPersonalAgentTask} mutation does a defense-in-depth ownership check on the agent before
 * opening a task against it. The task FK doesn't enforce agent ownership; without this the user could craft a task
 * referencing an agent in a workspace they don't own.</li>
 * </ul>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubPersonalAgentGraphQlControllerTest {

    @Test
    void testAiHubPersonalAgentsListsForCurrentUser() {
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);
        AiHubPersonalAgentService aiHubPersonalAgentService =
            mock(AiHubPersonalAgentService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        WorkspaceAiHubPersonalAgentService workspaceAiHubPersonalAgentService =
            mock(WorkspaceAiHubPersonalAgentService.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubPersonalAgent agent = new AiHubPersonalAgent(10L);

        agent.setId(42L);
        agent.setName("research-bot");

        when(aiHubPersonalAgentService.list(7L, 10L, 0)).thenReturn(List.of(agent));

        AiHubPersonalAgentGraphQlController controller = new AiHubPersonalAgentGraphQlController(
            aiHubPersonalAgentService, mock(AiHubPersonalAgentScheduleService.class), taskService, userService,
            workspaceAiHubPersonalAgentService, workspaceFacade);

        List<AiHubPersonalAgent> result = controller.aiHubPersonalAgents(7L, 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .getId()).isEqualTo(42L);
        // The userId on the service call is the controller-verified current user, not an argument — pin this so
        // a future refactor that accidentally accepts userId from the GraphQL input fails the test.
        verify(aiHubPersonalAgentService).list(7L, 10L, 0);
    }

    @Test
    void testCreateAiHubPersonalAgentPassesCurrentUserId() {
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);
        AiHubPersonalAgentService aiHubPersonalAgentService =
            mock(AiHubPersonalAgentService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        WorkspaceAiHubPersonalAgentService workspaceAiHubPersonalAgentService =
            mock(WorkspaceAiHubPersonalAgentService.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubPersonalAgent created = new AiHubPersonalAgent(10L);

        created.setId(42L);
        created.setName("research-bot");

        when(aiHubPersonalAgentService
            .create(
                eq(7L), eq(10L), eq(0), eq("research-bot"), anyString(), anyString(), anyString(),
                isNull(), isNull()))
                    .thenReturn(created);

        AiHubPersonalAgentGraphQlController controller = new AiHubPersonalAgentGraphQlController(
            aiHubPersonalAgentService, mock(AiHubPersonalAgentScheduleService.class), taskService, userService,
            workspaceAiHubPersonalAgentService, workspaceFacade);

        AiHubPersonalAgent result =
            controller.createAiHubPersonalAgent(new CreateAiHubPersonalAgentInput(
                7L, 0, "research-bot", "Research Bot", "Lit reviews", "Cite sources.", null, null));

        assertThat(result.getId()).isEqualTo(42L);
        // Same security pin as the list test — userId never comes from input.
        verify(aiHubPersonalAgentService)
            .create(
                eq(7L), eq(10L), eq(0), eq("research-bot"), eq("Research Bot"), eq("Lit reviews"),
                eq("Cite sources."), isNull(), isNull());
    }

    @Test
    void testUpdateAiHubPersonalAgentRejectsAllNullPatch() {
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);
        AiHubPersonalAgentService aiHubPersonalAgentService =
            mock(AiHubPersonalAgentService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        WorkspaceAiHubPersonalAgentService workspaceAiHubPersonalAgentService =
            mock(WorkspaceAiHubPersonalAgentService.class);

        AiHubPersonalAgentGraphQlController controller = new AiHubPersonalAgentGraphQlController(
            aiHubPersonalAgentService, mock(AiHubPersonalAgentScheduleService.class), taskService, userService,
            workspaceAiHubPersonalAgentService, workspaceFacade);

        // No fields supplied — controller refuses BEFORE calling currentUser/workspace/service. The order
        // matters: a no-op patch should be a 400 not a "the workspace doesn't exist" error.
        assertThatThrownBy(
            () -> controller
                .updateAiHubPersonalAgent(new UpdateAiHubPersonalAgentInput(42L, 7L, null, null, null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one");
    }

    @Test
    void testCreateAiHubPersonalAgentTaskRejectsForeignAgentId() {
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);
        AiHubPersonalAgentService aiHubPersonalAgentService =
            mock(AiHubPersonalAgentService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        WorkspaceAiHubPersonalAgentService workspaceAiHubPersonalAgentService =
            mock(WorkspaceAiHubPersonalAgentService.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        // Agent belongs to someone else / different workspace → service.findOwned returns empty. Controller
        // must refuse BEFORE calling createAiHubPersonalAgentChat — without this defense-in-depth check, the
        // task row's FK would point at a foreign agent, and the routing agent would silently apply
        // someone else's instructions to this user's chat.
        when(aiHubPersonalAgentService.findOwned(42L, 7L, 10L)).thenReturn(Optional.empty());

        AiHubPersonalAgentGraphQlController controller = new AiHubPersonalAgentGraphQlController(
            aiHubPersonalAgentService, mock(AiHubPersonalAgentScheduleService.class), taskService, userService,
            workspaceAiHubPersonalAgentService, workspaceFacade);

        assertThatThrownBy(() -> controller.createAiHubPersonalAgentTask(
            new CreateAiHubPersonalAgentTaskInput(7L, 0, 42L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");

        verify(taskService, never())
            .createAiHubPersonalAgentChat(anyLong(), anyLong(), anyInt(), anyLong(), isNull());
    }

    @Test
    void testCreateAiHubPersonalAgentTaskDelegatesToService() {
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);
        AiHubPersonalAgentService aiHubPersonalAgentService =
            mock(AiHubPersonalAgentService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        WorkspaceAiHubPersonalAgentService workspaceAiHubPersonalAgentService =
            mock(WorkspaceAiHubPersonalAgentService.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubPersonalAgent agent = new AiHubPersonalAgent(10L);

        agent.setId(42L);
        agent.setName("research-bot");

        when(aiHubPersonalAgentService.findOwned(42L, 7L, 10L)).thenReturn(Optional.of(agent));

        AiHubTask task = new AiHubTask(10L);

        task.setId(101L);
        task.setKind(AiHubTaskKind.PERSONAL_AGENT);
        task.setAiHubPersonalAgentId(42L);

        when(taskService.createAiHubPersonalAgentChat(eq(7L), eq(10L), eq(0), eq(42L),
            eq("Research Assistant")))
                .thenReturn(task);

        AiHubPersonalAgentGraphQlController controller = new AiHubPersonalAgentGraphQlController(
            aiHubPersonalAgentService, mock(AiHubPersonalAgentScheduleService.class), taskService, userService,
            workspaceAiHubPersonalAgentService, workspaceFacade);

        AiHubTask result = controller.createAiHubPersonalAgentTask(
            new CreateAiHubPersonalAgentTaskInput(7L, 0, 42L, "Research Assistant"));

        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getKind()).isEqualTo(AiHubTaskKind.PERSONAL_AGENT);
    }

    @Test
    void testWorkspaceIdResolvesViaWorkspaceRelationService() {
        // Schema declares AiHubPersonalAgent.workspaceId: Long!. The @SchemaMapping resolver must delegate to the
        // automation-side WorkspaceAiHubPersonalAgentService — not the platform-side AiHubPersonalAgentService, which
        // applies no workspace rules of its own. Otherwise GraphQL returns null and trips the non-null contract on
        // every list/create/update response.
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);
        AiHubPersonalAgentService aiHubPersonalAgentService =
            mock(AiHubPersonalAgentService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        WorkspaceAiHubPersonalAgentService workspaceAiHubPersonalAgentService =
            mock(WorkspaceAiHubPersonalAgentService.class);

        AiHubPersonalAgent agent = new AiHubPersonalAgent(10L);

        agent.setId(42L);

        when(workspaceAiHubPersonalAgentService.getWorkspaceId(42L)).thenReturn(7L);

        AiHubPersonalAgentGraphQlController controller =
            new AiHubPersonalAgentGraphQlController(
                aiHubPersonalAgentService, mock(AiHubPersonalAgentScheduleService.class), taskService, userService,
                workspaceAiHubPersonalAgentService, workspaceFacade);

        long workspaceId = controller.workspaceId(agent);

        assertThat(workspaceId).isEqualTo(7L);
        verify(workspaceAiHubPersonalAgentService).getWorkspaceId(42L);
    }

    @Test
    void testSetAiHubPersonalAgentScheduleUpsert() {
        long workspaceId = 1L;
        long userId = 10L;
        long agentId = 42L;

        AiHubPersonalAgentService aiHubPersonalAgentService = mock(AiHubPersonalAgentService.class);
        AiHubPersonalAgentScheduleService scheduleService = mock(AiHubPersonalAgentScheduleService.class);
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        AiHubPersonalAgent agent = sampleAgent(agentId, workspaceId, userId);
        User user = sampleUser(userId);
        Workspace workspace = buildWorkspace(workspaceId);

        when(userService.getCurrentUser()).thenReturn(user);
        when(workspaceFacade.getUserWorkspaces(userId)).thenReturn(List.of(workspace));
        when(aiHubPersonalAgentService.findOwned(agentId, workspaceId, userId)).thenReturn(Optional.of(agent));

        AiHubPersonalAgentScheduleInputBody body = new AiHubPersonalAgentScheduleInputBody(
            true, "Daily report", "Run morning report",
            ScheduleFrequencyKind.DAILY,
            null, null, "09:00", null, null, null,
            "UTC", null,
            ScheduleLifecycleKind.RECURRING, null);

        SetAiHubPersonalAgentScheduleInput input =
            new SetAiHubPersonalAgentScheduleInput(workspaceId, agentId, body);

        AiHubPersonalAgentGraphQlController controller = new AiHubPersonalAgentGraphQlController(
            aiHubPersonalAgentService, scheduleService, mock(AiHubTaskService.class), userService,
            mock(WorkspaceAiHubPersonalAgentService.class), workspaceFacade);

        AiHubPersonalAgent result = controller.setAiHubPersonalAgentSchedule(input);

        assertThat(result).isSameAs(agent);

        verify(scheduleService).upsertOrDelete(
            eq(agentId), eq(workspaceId), eq(userId), eq(agent.getEnvironment()),
            argThat(serviceInput -> serviceInput != null
                && serviceInput.enabled()
                && serviceInput.title()
                    .equals("Daily report")
                && serviceInput.frequencyKind() == ScheduleFrequencyKind.DAILY));
    }

    @Test
    void testSetAiHubPersonalAgentScheduleDelete() {
        long workspaceId = 1L;
        long userId = 10L;
        long agentId = 42L;

        AiHubPersonalAgentService aiHubPersonalAgentService = mock(AiHubPersonalAgentService.class);
        AiHubPersonalAgentScheduleService scheduleService = mock(AiHubPersonalAgentScheduleService.class);
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        AiHubPersonalAgent agent = sampleAgent(agentId, workspaceId, userId);
        User user = sampleUser(userId);
        Workspace workspace = buildWorkspace(workspaceId);

        when(userService.getCurrentUser()).thenReturn(user);
        when(workspaceFacade.getUserWorkspaces(userId)).thenReturn(List.of(workspace));
        when(aiHubPersonalAgentService.findOwned(agentId, workspaceId, userId)).thenReturn(Optional.of(agent));

        SetAiHubPersonalAgentScheduleInput input =
            new SetAiHubPersonalAgentScheduleInput(workspaceId, agentId, null);

        AiHubPersonalAgentGraphQlController controller = new AiHubPersonalAgentGraphQlController(
            aiHubPersonalAgentService, scheduleService, mock(AiHubTaskService.class), userService,
            mock(WorkspaceAiHubPersonalAgentService.class), workspaceFacade);

        controller.setAiHubPersonalAgentSchedule(input);

        verify(scheduleService).upsertOrDelete(
            eq(agentId), eq(workspaceId), eq(userId), eq(agent.getEnvironment()),
            isNull());
    }

    @Test
    void testSetAiHubPersonalAgentScheduleRejectsUnknownAgent() {
        long workspaceId = 1L;
        long userId = 10L;

        AiHubPersonalAgentService aiHubPersonalAgentService = mock(AiHubPersonalAgentService.class);
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = sampleUser(userId);
        Workspace workspace = buildWorkspace(workspaceId);

        when(userService.getCurrentUser()).thenReturn(user);
        when(workspaceFacade.getUserWorkspaces(userId)).thenReturn(List.of(workspace));
        when(aiHubPersonalAgentService.findOwned(999L, workspaceId, userId)).thenReturn(Optional.empty());

        SetAiHubPersonalAgentScheduleInput input =
            new SetAiHubPersonalAgentScheduleInput(workspaceId, 999L, null);

        AiHubPersonalAgentGraphQlController controller = new AiHubPersonalAgentGraphQlController(
            aiHubPersonalAgentService, mock(AiHubPersonalAgentScheduleService.class), mock(AiHubTaskService.class),
            userService, mock(WorkspaceAiHubPersonalAgentService.class), workspaceFacade);

        assertThatThrownBy(() -> controller.setAiHubPersonalAgentSchedule(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found in workspace");
    }

    @Test
    void testScheduleResolverReturnsRowWhenPresent() {
        AiHubPersonalAgentScheduleService scheduleService = mock(AiHubPersonalAgentScheduleService.class);

        AiHubPersonalAgent agent = sampleAgent(42L, 1L, 10L);
        AiHubPersonalAgentSchedule schedule = sampleSchedule(42L);

        when(scheduleService.findByAgentId(42L)).thenReturn(Optional.of(schedule));

        AiHubPersonalAgentGraphQlController controller = new AiHubPersonalAgentGraphQlController(
            mock(AiHubPersonalAgentService.class), scheduleService, mock(AiHubTaskService.class),
            mock(UserService.class), mock(WorkspaceAiHubPersonalAgentService.class), mock(WorkspaceFacade.class));

        AiHubPersonalAgentSchedule resolved = controller.schedule(agent);

        assertThat(resolved).isSameAs(schedule);
    }

    @Test
    void testScheduleResolverReturnsNullWhenAbsent() {
        AiHubPersonalAgentScheduleService scheduleService = mock(AiHubPersonalAgentScheduleService.class);

        AiHubPersonalAgent agent = sampleAgent(42L, 1L, 10L);

        when(scheduleService.findByAgentId(42L)).thenReturn(Optional.empty());

        AiHubPersonalAgentGraphQlController controller = new AiHubPersonalAgentGraphQlController(
            mock(AiHubPersonalAgentService.class), scheduleService, mock(AiHubTaskService.class),
            mock(UserService.class), mock(WorkspaceAiHubPersonalAgentService.class), mock(WorkspaceFacade.class));

        assertThat(controller.schedule(agent)).isNull();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static AiHubPersonalAgent sampleAgent(long id, long workspaceId, long userId) {
        AiHubPersonalAgent agent = new AiHubPersonalAgent(userId);

        agent.setId(id);
        agent.setName("sample-agent");
        agent.setTitle("Sample Agent");
        agent.setEnvironment(Environment.DEVELOPMENT);

        return agent;
    }

    private static User sampleUser(long userId) {
        User user = mock(User.class);

        when(user.getId()).thenReturn(userId);

        return user;
    }

    private static AiHubPersonalAgentSchedule sampleSchedule(long agentId) {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();

        schedule.setId(100L);
        schedule.setAiHubPersonalAgentId(agentId);

        return schedule;
    }

    private static Workspace buildWorkspace(long id) {
        Workspace workspace = mock(Workspace.class);

        when(workspace.getId()).thenReturn(id);

        return workspace;
    }
}
