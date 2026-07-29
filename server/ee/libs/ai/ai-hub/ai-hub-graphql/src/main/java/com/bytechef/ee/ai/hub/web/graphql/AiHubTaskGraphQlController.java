/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.security.WorkspaceAccessGuard;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskArtifact;
import com.bytechef.ee.ai.hub.task.AiHubTaskArtifactService;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskService.AiHubTaskMessage;
import com.bytechef.ee.ai.hub.task.AiHubTaskService.AiHubTaskPatch;
import com.bytechef.ee.ai.hub.task.AiHubTaskStatus;
import com.bytechef.ee.ai.hub.task.TitleGenerationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.ZoneOffset;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for AI Hub aiHubTasks. Replaces the prior REST {@code TaskApiController} — the
 * {@link AiHubTaskService} / {@link AiHubTaskArtifactService} / {@link TitleGenerationService} contracts are unchanged;
 * only the transport differs.
 *
 * <p>
 * Authorization mirrors the REST controller exactly: every operation requires {@code isAuthenticated()} plus the
 * {@link WorkspaceAccessGuard} membership gate. Service-layer ownership checks remain the authoritative defence — a
 * caller who is a workspace member but not the task owner still gets a 403-equivalent error from the service.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@PreAuthorize("isAuthenticated()")
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class AiHubTaskGraphQlController {

    private static final Logger log = LoggerFactory.getLogger(AiHubTaskGraphQlController.class);

    private final AiHubTaskArtifactService taskArtifactService;
    private final AiHubTaskService taskService;
    private final TitleGenerationService titleGenerationService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public AiHubTaskGraphQlController(
        AiHubTaskArtifactService taskArtifactService,
        AiHubTaskService taskService,
        TitleGenerationService titleGenerationService, UserService userService, WorkspaceFacade workspaceFacade) {

        this.taskArtifactService = taskArtifactService;
        this.taskService = taskService;
        this.titleGenerationService = titleGenerationService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @QueryMapping
    public List<AiHubTask> aiHubTasks(
        @Argument long workspaceId, @Argument int environment,
        @Argument @Nullable AiHubTaskStatus status) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        AiHubTaskStatus effectiveStatus =
            status == null ? AiHubTaskStatus.ACTIVE : status;

        return taskService.list(workspaceId, userId, environment, effectiveStatus);
    }

    @QueryMapping
    public List<AiHubTaskMessage>
        aiHubTaskMessages(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return taskService.loadMessages(id, workspaceId, userId);
    }

    @QueryMapping
    public List<AiHubTaskArtifact> aiHubTaskArtifactsByAiHubTask(
        @Argument long workspaceId, @Argument long id) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return taskArtifactService.listByTask(id, workspaceId, userId);
    }

    @MutationMapping
    public AiHubTask createAiHubTask(
        @Argument long workspaceId, @Argument int environment, @Argument String threadId) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return taskService.create(workspaceId, userId, environment, threadId);
    }

    @MutationMapping
    public AiHubTask createWorkflowChatAiHubTask(
        @Argument long workspaceId, @Argument int environment, @Argument String workflowExecutionId,
        @Argument long projectDeploymentId, @Argument @Nullable String title) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        // Always-new semantics: every call inserts a fresh task row with a UUID-suffixed threadId.
        // Past aiHubTasks bound to the same workflow remain reachable through the
        // aiHubTasks list rather
        // than being restored on re-click. The title is stamped onto the new row.
        return taskService.createWorkflowChat(
            workspaceId, userId, environment, workflowExecutionId, projectDeploymentId, title);
    }

    @MutationMapping
    public AiHubTask
        updateAiHubTask(@Argument AiHubTaskPatchInput input) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        AiHubTaskPatch patch = new AiHubTaskPatch(
            input.title(), input.lastPreview(), input.messageCount(), input.status());

        return taskService.patch(input.id(), input.workspaceId(), userId, patch);
    }

    @MutationMapping
    public AiHubTask
        generateAiHubTaskTitle(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        AiHubTask existing = taskService.getById(id, workspaceId, userId);

        if (!existing.isAutoTitled()) {
            // Locked — title was either set by the LLM on a previous regeneration or explicitly renamed by
            // the user. Short-circuit before loading messages or calling the LLM. The "auto-titled" flag
            // (versus the previous "title is empty" check) lets workflow chats — which start with a
            // label-based title that's still eligible for LLM regeneration — get a more meaningful title
            // after a few turns instead of being stuck on the project-name pattern forever.
            return existing;
        }

        List<AiHubTaskMessage> messages = taskService.loadMessages(id, workspaceId, userId);

        String title = generateTitleForEnvironment(existing.getEnvironment(), messages);

        if (title.isEmpty()) {
            // The model returned a blank or over-length title. Reuse the row we already loaded above
            // for the idempotency check rather than issuing a second getById round-trip.
            return existing;
        }

        try {
            return taskService.patch(
                id, workspaceId, userId, new AiHubTaskPatch(title, null, null, null));
        } catch (NotFoundException notFound) {
            // Race: the task was deleted between the getById/loadMessages above and this patch.
            // Title generation is fire-and-forget on the client (one per turn while untitled), so a quick
            // delete after sending a message lands here. Surface as a benign no-op — the deleted row's
            // pre-delete snapshot is still a valid AiHubTask shape for the GraphQL response, and the
            // client has already removed the task from its local state so this body is never
            // rendered. Logging at INFO so the race stays visible without spamming WARN/ERROR.
            log.info(
                "generateAiHubTaskTitle no-op: task {} was deleted during title generation",
                id);

            return existing;
        }
    }

    private String generateTitleForEnvironment(Environment environment, List<AiHubTaskMessage> messages) {
        Environment previousEnvironment = EnvironmentContext.fetchCurrentEnvironment();

        EnvironmentContext.set(environment);

        try {
            return titleGenerationService.generateTitle(messages);
        } finally {
            if (previousEnvironment == null) {
                EnvironmentContext.clear();
            } else {
                EnvironmentContext.set(previousEnvironment);
            }
        }
    }

    @MutationMapping
    public boolean deleteAiHubTask(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        taskService.delete(id, workspaceId, userId);

        return true;
    }

    @MutationMapping
    public int
        bulkArchiveWorkflowChatAiHubTasks(@Argument long workspaceId, @Argument int environment) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        // Service-layer per-row ownership re-check matches the rest of the task API; we double-gate on
        // the workspace-membership guard here AND the per-task ownership check inside the loop. Returning
        // the count rather than the affected row ids keeps the response small (no per-row latency the client has
        // to render); the sidebar refetches the aiHubTasks query after this fires anyway.
        return taskService.bulkArchiveWorkflowChatAiHubTasks(workspaceId, userId, environment);
    }

    @MutationMapping
    public boolean cancelWorkflowChatTurn(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return taskService.cancelWorkflowChatTurn(id, workspaceId, userId);
    }

    @MutationMapping
    public boolean cancelAiHubRun(@Argument long workspaceId, @Argument long id, @Argument String runId) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return taskService.cancelAiHubRun(id, workspaceId, userId, runId);
    }

    @MutationMapping
    public int truncateAiHubTaskMessages(
        @Argument long workspaceId, @Argument long id, @Argument int fromMessageIndex) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return taskService.truncateMessagesFrom(id, workspaceId, userId, fromMessageIndex);
    }

    @MutationMapping
    public boolean appendAiHubTaskAssistantMessage(
        @Argument long workspaceId, @Argument long id, @Argument String content) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        taskService.appendAssistantMessage(id, workspaceId, userId, content);

        return true;
    }

    /**
     * Resolves the owning workspace id for a task, read straight off the loaded row's {@code workspace_id} column, so
     * the sidebar listing costs no extra query per row. The schema declares the field non-null and a task with no
     * workspace is unreachable through every workspace-scoped path, so a null fails loudly here — the same contract
     * {@code AiHubTaskService.getWorkspaceId} enforces for callers that only have a task id.
     */
    @SchemaMapping(typeName = "AiHubTask", field = "workspaceId")
    public long taskWorkspaceId(AiHubTask task) {
        Long workspaceId = task.getWorkspaceId();

        if (workspaceId == null) {
            throw new NotFoundException("No workspace for ai_hub_task id=" + task.getId());
        }

        return workspaceId;
    }

    @SchemaMapping(typeName = "AiHubTask", field = "status")
    public String taskStatus(AiHubTask task) {
        return task.getStatus()
            .name();
    }

    @SchemaMapping(typeName = "AiHubTask", field = "kind")
    public String taskKind(AiHubTask task) {
        return task.getKind()
            .name();
    }

    @SchemaMapping(typeName = "AiHubTask", field = "createdAt")
    @Nullable
    public Long taskCreatedAt(AiHubTask task) {
        return task.getCreatedAt() == null ? null
            : task.getCreatedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiHubTask", field = "updatedAt")
    @Nullable
    public Long taskUpdatedAt(AiHubTask task) {
        return task.getUpdatedAt() == null ? null
            : task.getUpdatedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiHubTaskMessage", field = "timestamp")
    public long messageTimestamp(AiHubTaskMessage message) {
        return message.timestamp()
            .toEpochMilli();
    }

    public record AiHubTaskPatchInput(
        long id, long workspaceId, @Nullable String title, @Nullable String lastPreview,
        @Nullable Integer messageCount, @Nullable AiHubTaskStatus status) {
    }
}
