/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgent;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentResource;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentResourceKind;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentSchedule;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentScheduleService;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentScheduleService.ScheduleInput;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentTool;
import com.bytechef.ee.ai.hub.personalagent.ScheduleFrequencyKind;
import com.bytechef.ee.ai.hub.personalagent.ScheduleLifecycleKind;
import com.bytechef.ee.ai.hub.personalagent.WorkspaceAiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.security.WorkspaceAccessGuard;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for Personal Agents. The underlying {@link AiHubPersonalAgentService} owns slug normalisation and
 * ownership checks; this controller wires HTTP arguments into service calls and verifies workspace membership before
 * each operation.
 *
 * <p>
 * The {@link #createAiHubPersonalAgentTask} mutation delegates to {@link AiHubTaskService#createAiHubPersonalAgentChat}
 * — every call inserts a new task row (always-new semantics, May 2026). A double-click during a slow round-trip will
 * produce two aiHubTasks; the user can archive or delete the unwanted one from the aiHubTasks list.
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
public class AiHubPersonalAgentGraphQlController {

    private final AiHubPersonalAgentService aiHubPersonalAgentService;
    private final AiHubPersonalAgentScheduleService scheduleService;
    private final AiHubTaskService taskService;
    private final UserService userService;
    private final WorkspaceAiHubPersonalAgentService workspaceAiHubPersonalAgentService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public AiHubPersonalAgentGraphQlController(
        AiHubPersonalAgentService aiHubPersonalAgentService,
        AiHubPersonalAgentScheduleService scheduleService,
        AiHubTaskService taskService,
        UserService userService,
        WorkspaceAiHubPersonalAgentService workspaceAiHubPersonalAgentService,
        WorkspaceFacade workspaceFacade) {

        this.aiHubPersonalAgentService = aiHubPersonalAgentService;
        this.scheduleService = scheduleService;
        this.taskService = taskService;
        this.userService = userService;
        this.workspaceAiHubPersonalAgentService = workspaceAiHubPersonalAgentService;
        this.workspaceFacade = workspaceFacade;
    }

    @QueryMapping
    public List<AiHubPersonalAgent>
        aiHubPersonalAgents(@Argument long workspaceId, @Argument int environment) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return aiHubPersonalAgentService.list(workspaceId, userId, environment);
    }

    @QueryMapping
    @Nullable
    public AiHubPersonalAgent aiHubPersonalAgent(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        Optional<AiHubPersonalAgent> agent =
            aiHubPersonalAgentService.findOwned(id, workspaceId, userId);

        return agent.orElse(null);
    }

    @MutationMapping
    public AiHubPersonalAgent
        createAiHubPersonalAgent(@Argument CreateAiHubPersonalAgentInput input) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        return aiHubPersonalAgentService.create(
            input.workspaceId(), userId, input.environment(), input.name(), input.title(), input.description(),
            input.instructions(), input.llmProvider(), input.llmModel());
    }

    @MutationMapping
    public AiHubPersonalAgent
        updateAiHubPersonalAgent(@Argument UpdateAiHubPersonalAgentInput input) {
        if (input.title() == null && input.description() == null && input.instructions() == null
            && input.llmProvider() == null && input.llmModel() == null) {

            throw new IllegalArgumentException(
                "UpdateAiHubPersonalAgentInput requires at least one of title, description, instructions, " +
                    "llmProvider, llmModel");
        }

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        return aiHubPersonalAgentService.update(
            input.id(), input.workspaceId(), userId, input.title(), input.description(), input.instructions(),
            input.llmProvider(), input.llmModel());
    }

    @MutationMapping
    public boolean deleteAiHubPersonalAgent(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        aiHubPersonalAgentService.delete(id, workspaceId, userId);

        return true;
    }

    @MutationMapping
    public AiHubTask createAiHubPersonalAgentTask(
        @Argument CreateAiHubPersonalAgentTaskInput input) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        // Defense-in-depth: re-check that the personal agent belongs to (workspaceId, userId) before opening a
        // task against it. The task FK doesn't itself verify ownership; without this the user
        // could potentially construct a task that references an agent in a workspace they do not own.
        aiHubPersonalAgentService.findOwned(input.aiHubPersonalAgentId(), input.workspaceId(), userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Personal agent " + input.aiHubPersonalAgentId() + " not found in workspace "
                    + input.workspaceId()));

        return taskService.createAiHubPersonalAgentChat(
            input.workspaceId(), userId, input.environment(), input.aiHubPersonalAgentId(), input.title());
    }

    @MutationMapping
    public AiHubPersonalAgentTool
        addAiHubPersonalAgentTool(@Argument AddAiHubPersonalAgentToolInput input) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        return aiHubPersonalAgentService.addTool(
            input.aiHubPersonalAgentId(), input.workspaceId(), userId, input.componentName(),
            input.componentVersion(),
            input.operationName());
    }

    @MutationMapping
    public boolean removeAiHubPersonalAgentTool(@Argument long workspaceId, @Argument long toolId) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        aiHubPersonalAgentService.removeTool(toolId, workspaceId, userId);

        return true;
    }

    @MutationMapping
    public AiHubPersonalAgentResource
        addAiHubPersonalAgentResource(@Argument AddAiHubPersonalAgentResourceInput input) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        return aiHubPersonalAgentService.addResource(
            input.aiHubPersonalAgentId(), input.workspaceId(), userId, input.kind(), input.resourceId(),
            input.resourceName());
    }

    @MutationMapping
    public boolean removeAiHubPersonalAgentResource(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        aiHubPersonalAgentService.removeResource(id, workspaceId, userId);

        return true;
    }

    @MutationMapping
    public AiHubPersonalAgentTool updateAiHubPersonalAgentToolConfig(
        @Argument UpdateAiHubPersonalAgentToolConfigInput input) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        return aiHubPersonalAgentService.updateToolConfig(
            input.toolId(), input.workspaceId(), userId, input.connectionId(), input.parameters());
    }

    @MutationMapping
    public AiHubPersonalAgent setAiHubPersonalAgentSchedule(
        @Argument SetAiHubPersonalAgentScheduleInput input) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        AiHubPersonalAgent agent = aiHubPersonalAgentService
            .findOwned(input.aiHubPersonalAgentId(), input.workspaceId(), userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Personal agent " + input.aiHubPersonalAgentId() + " not found in workspace " + input.workspaceId()));

        var body = input.schedule();

        ScheduleInput serviceInput = body == null
            ? null
            : new ScheduleInput(
                body.enabled(),
                body.title(),
                body.prompt(),
                body.frequencyKind(),
                body.intervalMinutes(),
                body.minuteOfHour(),
                body.timeOfDay() == null ? null : LocalTime.parse(body.timeOfDay()),
                body.dayOfWeek(),
                body.dayOfMonth(),
                body.cronExpression(),
                body.zoneId(),
                body.startDate() == null ? null : LocalDateTime.parse(body.startDate()),
                body.lifecycleKind(),
                body.maxRuns());

        scheduleService.upsertOrDelete(
            input.aiHubPersonalAgentId(), input.workspaceId(), userId,
            agent.getEnvironment(), serviceInput);

        return agent;
    }

    /**
     * Resolves the owning workspace id for an agent. The agent entity is workspace-agnostic and lives on the platform
     * side; the (workspace, agent) link lives in the automation-side relation table, so the resolver consults
     * {@link WorkspaceAiHubPersonalAgentService} rather than the platform-side {@link AiHubPersonalAgentService}. One
     * round-trip per row is acceptable for the agent list (small N); switch to {@code @BatchMapping} if that stops
     * being true.
     */
    @SchemaMapping(typeName = "AiHubPersonalAgent", field = "workspaceId")
    public long workspaceId(AiHubPersonalAgent agent) {
        return workspaceAiHubPersonalAgentService.getWorkspaceId(agent.getId());
    }

    @SchemaMapping(typeName = "AiHubPersonalAgent", field = "environmentId")
    public Long environmentId(AiHubPersonalAgent agent) {
        return (long) agent.getEnvironmentOrdinal();
    }

    /**
     * Resolver for the {@code tools} field on {@code AiHubPersonalAgent}. Returning an empty list when the agent has no
     * tools rather than null keeps the schema's non-null array contract intact and saves the client from a per-call
     * null-check before iterating.
     */
    @SchemaMapping(typeName = "AiHubPersonalAgent", field = "tools")
    public List<AiHubPersonalAgentTool> tools(AiHubPersonalAgent agent) {
        return aiHubPersonalAgentService.listTools(agent.getId());
    }

    /**
     * Resolver for the {@code resources} field on {@code AiHubPersonalAgent}. Returns an empty list when the agent has
     * no resources rather than null, keeping the schema's non-null array contract intact.
     */
    @SchemaMapping(typeName = "AiHubPersonalAgent", field = "resources")
    public List<AiHubPersonalAgentResource> resources(AiHubPersonalAgent agent) {
        return aiHubPersonalAgentService.listResources(agent.getId());
    }

    @SchemaMapping(typeName = "AiHubPersonalAgent", field = "schedule")
    @Nullable
    public AiHubPersonalAgentSchedule schedule(AiHubPersonalAgent agent) {
        return scheduleService.findByAgentId(agent.getId())
            .orElse(null);
    }

    @SchemaMapping(typeName = "AiHubPersonalAgentTool", field = "createdAt")
    @Nullable
    public Long toolCreatedAt(AiHubPersonalAgentTool tool) {
        return tool.getCreatedAt() == null ? null
            : tool.getCreatedAt()
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiHubPersonalAgentResource", field = "createdAt")
    @Nullable
    public Long resourceCreatedAt(AiHubPersonalAgentResource resource) {
        return resource.getCreatedAt() == null ? null
            : resource.getCreatedAt()
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli();
    }

    /**
     * Resolver for {@code AiHubPersonalAgentTool.parameters}. The entity's {@code getParameters} returns an immutable
     * map (or empty when not configured); the GraphQL {@code Any} scalar accepts the map shape directly. Resolver lives
     * here rather than on a property accessor so the typing stays explicit at the schema boundary.
     */
    @SchemaMapping(typeName = "AiHubPersonalAgentTool", field = "parameters")
    public java.util.Map<String, ?> toolParameters(AiHubPersonalAgentTool tool) {
        return tool.getParameters();
    }

    @SchemaMapping(typeName = "AiHubPersonalAgent", field = "createdAt")
    @Nullable
    public Long createdAt(AiHubPersonalAgent agent) {
        return agent.getCreatedAt() == null ? null
            : agent.getCreatedAt()
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiHubPersonalAgent", field = "updatedAt")
    @Nullable
    public Long updatedAt(AiHubPersonalAgent agent) {
        return agent.getUpdatedAt() == null ? null
            : agent.getUpdatedAt()
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli();
    }

    public record CreateAiHubPersonalAgentInput(
        long workspaceId, int environment, String name, @Nullable String title, @Nullable String description,
        @Nullable String instructions, @Nullable String llmProvider, @Nullable String llmModel) {
    }

    public record UpdateAiHubPersonalAgentInput(
        long id, long workspaceId, @Nullable String title, @Nullable String description,
        @Nullable String instructions, @Nullable String llmProvider, @Nullable String llmModel) {
    }

    public record CreateAiHubPersonalAgentTaskInput(
        long workspaceId, int environment, long aiHubPersonalAgentId, @Nullable String title) {
    }

    public record AddAiHubPersonalAgentToolInput(
        long workspaceId, long aiHubPersonalAgentId, String componentName, int componentVersion,
        String operationName) {
    }

    public record AddAiHubPersonalAgentResourceInput(
        long workspaceId, long aiHubPersonalAgentId, AiHubPersonalAgentResourceKind kind, String resourceId,
        String resourceName) {
    }

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public record UpdateAiHubPersonalAgentToolConfigInput(
        long workspaceId, long toolId, @Nullable Long connectionId,
        java.util.@Nullable Map<String, Object> parameters) {
    }

    public record SetAiHubPersonalAgentScheduleInput(
        long workspaceId, long aiHubPersonalAgentId,
        @Nullable AiHubPersonalAgentScheduleInputBody schedule) {
    }

    public record AiHubPersonalAgentScheduleInputBody(
        boolean enabled, String title, String prompt,
        ScheduleFrequencyKind frequencyKind,
        @Nullable Integer intervalMinutes, @Nullable Integer minuteOfHour, @Nullable String timeOfDay,
        @Nullable Integer dayOfWeek, @Nullable Integer dayOfMonth, @Nullable String cronExpression,
        String zoneId, @Nullable String startDate,
        ScheduleLifecycleKind lifecycleKind, @Nullable Integer maxRuns) {
    }
}
