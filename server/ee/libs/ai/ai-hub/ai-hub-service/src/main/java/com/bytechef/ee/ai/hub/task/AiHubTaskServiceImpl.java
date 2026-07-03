/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.agent.WorkflowChatJobRegistry;
import com.bytechef.ee.ai.hub.audit.AiHubAuditEvent;
import com.bytechef.ee.ai.hub.audit.AiHubAuditPublisher;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentResource;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentTool;
import com.bytechef.ee.ai.hub.task.repository.AiHubTaskRepository;
import com.bytechef.ee.ai.hub.task.repository.WorkspaceAiHubTaskRepository;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder.TaskToolReference;
import com.bytechef.ee.ai.hub.util.EnumOrdinals;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Default implementation of {@link AiHubTaskService}.
 *
 * <p>
 * Message contents are stored and owned by Spring AI's {@code SPRING_AI_CHAT_MEMORY} table. This service reads them via
 * a raw {@link JdbcTemplate} query; it never writes to that table.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@Transactional
public class AiHubTaskServiceImpl implements AiHubTaskService {

    private static final Logger log = LoggerFactory.getLogger(AiHubTaskServiceImpl.class);

    private static final int LIST_LIMIT = 100;

    /**
     * Hard upper bound on the number of messages returned by {@link #loadMessages}. Without this cap a long-running
     * task could accumulate thousands of messages and force the controller to materialise them all in memory; the UI
     * never needs more than the most recent slice.
     */
    private static final int MESSAGE_LIMIT = 500;

    private static final String CHAT_MEMORY_QUERY =
        "SELECT type, content, \"timestamp\" FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ? " +
            "ORDER BY \"timestamp\" ASC LIMIT " + MESSAGE_LIMIT;

    private static final String CHAT_MEMORY_DELETE =
        "DELETE FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ?";

    private final AiHubTaskRepository taskRepository;
    private final WorkspaceAiHubTaskRepository workspaceTaskRepository;
    private final Clock clock;
    private final JdbcTemplate jdbcTemplate;
    private final JobFacade jobFacade;
    private final WorkflowChatJobRegistry jobRegistry;
    private final InFlightAiHubRunRegistry inFlightRunRegistry;
    private final ObjectProvider<AiHubPersonalAgentService> aiHubPersonalAgentServiceProvider;
    private final ObjectProvider<AiHubTaskToolFacade> taskToolFacadeProvider;
    private final ObjectProvider<ToolSearchCatalogFeeder> toolSearchCatalogFeederProvider;
    private final ObjectProvider<AiHubTaskArtifactService> taskArtifactServiceProvider;
    private final @Nullable AiHubAuditPublisher auditPublisher;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubTaskServiceImpl(
        AiHubTaskRepository taskRepository, WorkspaceAiHubTaskRepository workspaceTaskRepository,
        JdbcTemplate jdbcTemplate, JobFacade jobFacade, WorkflowChatJobRegistry jobRegistry,
        InFlightAiHubRunRegistry inFlightRunRegistry,
        ObjectProvider<AiHubPersonalAgentService> aiHubPersonalAgentServiceProvider,
        ObjectProvider<AiHubTaskToolFacade> taskToolFacadeProvider,
        ObjectProvider<ToolSearchCatalogFeeder> toolSearchCatalogFeederProvider,
        ObjectProvider<AiHubTaskArtifactService> taskArtifactServiceProvider,
        @Nullable AiHubAuditPublisher auditPublisher) {

        this.taskRepository = taskRepository;
        this.workspaceTaskRepository = workspaceTaskRepository;
        this.clock = Clock.systemUTC();
        this.jdbcTemplate = jdbcTemplate;
        this.jobFacade = jobFacade;
        this.jobRegistry = jobRegistry;
        this.inFlightRunRegistry = inFlightRunRegistry;
        // ObjectProvider so a future deployment that disables Personal Agents (or AiHubTask Tools)
        // doesn't fail
        // AiHubTaskService bean creation. The task flow degrades gracefully — without these
        // beans the
        // tool template copy below is a no-op and the task behaves like the pre-tools-picker era.
        this.aiHubPersonalAgentServiceProvider = aiHubPersonalAgentServiceProvider;
        this.taskToolFacadeProvider = taskToolFacadeProvider;
        // Same optional-dependency rationale: a deployment running with command center disabled (or running ahead of
        // the
        // tool-search advisor configuration) still gets a working AiHubTaskService — the
        // per-task tool
        // index just isn't refreshed, and search falls back to the workspace-wide catalog.
        this.toolSearchCatalogFeederProvider = toolSearchCatalogFeederProvider;
        // ObjectProvider so a deployment without the artifact service still gets a working AiHubTaskService —
        // the agent resource-template copy below is then a no-op, mirroring copyAgentToolTemplate.
        this.taskArtifactServiceProvider = taskArtifactServiceProvider;
        // Nullable so unit tests constructing this impl without an audit publisher (and any deployment that
        // doesn't supply the EE bean) degrade to a no-op; publishTaskCreated/publishTaskDeleted guard on null.
        this.auditPublisher = auditPublisher;
    }

    private void publishTaskCreated(AiHubTask task) {
        if (auditPublisher == null) {
            return;
        }

        Map<String, Object> data = new HashMap<>();

        data.put("taskId", task.getId());
        data.put("threadId", task.getThreadId());

        if (task.getKind() != null) {
            data.put("kind", task.getKind()
                .name());
        }

        if (task.getEnvironment() != null) {
            data.put("environment", task.getEnvironment()
                .name());
        }

        auditPublisher.publish(AiHubAuditEvent.AI_HUB_TASK_CREATED, data);
    }

    private void publishTaskDeleted(AiHubTask task) {
        if (auditPublisher == null) {
            return;
        }

        Map<String, Object> data = new HashMap<>();

        data.put("taskId", task.getId());
        data.put("threadId", task.getThreadId());

        auditPublisher.publish(AiHubAuditEvent.AI_HUB_TASK_DELETED, data);
    }

    /**
     * Saves a freshly-built task and its workspace membership in one transactional unit. Mirrors the Connection /
     * WorkspaceConnection model: the entity is workspace-agnostic, the membership row is the single place a workspace
     * claims a task.
     */
    private AiHubTask saveNew(AiHubTask task, long workspaceId) {
        AiHubTask saved = taskRepository.save(task);

        workspaceTaskRepository.save(new WorkspaceAiHubTask(workspaceId, saved.getId()));

        return saved;
    }

    /**
     * Returns true when {@code workspaceId} claims {@code taskId} via {@code workspace_ai_hub_task}. Used by
     * ownership-check paths that previously read {@code task.getWorkspaceId()} directly.
     */
    private boolean workspaceClaimsTask(long workspaceId, long taskId) {
        return workspaceTaskRepository.findByWorkspaceIdAndAiHubTaskId(workspaceId, taskId)
            .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public long getWorkspaceId(long taskId) {
        return workspaceTaskRepository.findByAiHubTaskId(taskId)
            .orElseThrow(() -> new NotFoundException(
                "No workspace membership row for ai_hub_task id=" + taskId))
            .getWorkspaceId();
    }

    @Override
    @Transactional(readOnly = true)
    public String getThreadId(long taskId) {
        return taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException("No ai_hub_task row for id=" + taskId))
            .getThreadId();
    }

    @Override
    public AiHubTask create(long workspaceId, long userId, int environment, String threadId) {
        // thread_id has a global UNIQUE constraint per the Liquibase migration. Look up by threadId only (NOT
        // (threadId, userId)) so a cross-user collision surfaces as an explicit ConflictException instead of falling
        // through to save() and triggering a DataIntegrityViolationException → default 500. The 500 path also leaks a
        // probe oracle: the constraint violation reveals that another user has claimed the threadId.
        Optional<AiHubTask> existing = taskRepository.findByThreadId(threadId);

        if (existing.isPresent()) {
            AiHubTask found = existing.get();

            if (found.getUserId() == userId && workspaceClaimsTask(workspaceId, found.getId())) {
                // Idempotent return — but we deliberately do NOT update `environment` on the existing row. A
                // task's environment is an immutable property of its origin: re-running create() from a
                // different environment must not retroactively reassign the row, otherwise audit + usage analytics
                // (which group by environment) would silently lose attribution for the task's earlier turns.
                return found;
            }

            // Same wording for cross-user and cross-workspace collisions so the response does not let the caller
            // distinguish "owned by another user" from "owned by you in another workspace".
            throw new ConflictException("AiHubTask %s already exists".formatted(threadId));
        }

        LocalDateTime now = LocalDateTime.now(clock);

        AiHubTask task = new AiHubTask(userId);

        task.setThreadId(threadId);
        task.setStatus(AiHubTaskStatus.ACTIVE);
        task.setMessageCount(0);
        task.setEnvironment(EnumOrdinals.fromOrdinal(environment, Environment.class));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        AiHubTask saved;

        try {
            saved = saveNew(task, workspaceId);
        } catch (DataIntegrityViolationException exception) {
            // TOCTOU race: another concurrent request inserted the same threadId between the findByThreadId() check
            // above and this save(). At READ_COMMITTED there is no SELECT FOR UPDATE in the lookup, so the unique
            // constraint is the only thing that catches the duplicate. Re-throw with the same generic ConflictException
            // wording the existing-row branch above uses so the response is indistinguishable from a normal collision —
            // does not leak whether the colliding row is owned by the caller or another user.
            throw new ConflictException("AiHubTask %s already exists".formatted(threadId), exception);
        }

        publishTaskCreated(saved);

        return saved;
    }

    @Override
    public AiHubTask createWorkflowChat(
        long workspaceId, long userId, int environment, String workflowExecutionId, long projectDeploymentId,
        @Nullable String title) {

        // Always-new semantics (per user request, May 2026): every click on a workflow row in the sidebar
        // starts a fresh task rather than restoring a prior thread. Past tasks remain
        // accessible via the tasks list — they're just not the default landing target. The threadId
        // is a plain UUID so chat-memory rows are isolated per task, not shared across a
        // (user, workflow) tuple, and fits the SPRING_AI_CHAT_MEMORY.conversation_id VARCHAR(36) budget.
        // The kind column on this row is the authoritative discriminator for routing; nothing parses the
        // threadId.
        LocalDateTime now = LocalDateTime.now(clock);

        AiHubTask task = new AiHubTask(userId);

        task.setThreadId(UUID.randomUUID()
            .toString());
        task.setStatus(AiHubTaskStatus.ACTIVE);
        task.setMessageCount(0);
        task.setKind(AiHubTaskKind.WORKFLOW_CHAT);
        task.setWorkflowExecutionId(workflowExecutionId);
        task.setProjectDeploymentId(projectDeploymentId);
        task.setEnvironment(EnumOrdinals.fromOrdinal(environment, Environment.class));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        // Workflow chats don't fire the LLM-driven generateAiHubTaskTitle loop (the bridge bypasses the
        // LLM
        // agent), so without an initial title the row would render as "New AiHubTask" forever. The
        // client
        // passes `${projectName} — ${workflowLabel}` here; null/blank skips this and falls back to the UI
        // default.
        if (title != null && !title.isBlank()) {
            task.setTitle(title);
        }

        AiHubTask saved = saveNew(task, workspaceId);

        publishTaskCreated(saved);

        return saved;
    }

    @Override
    public AiHubTask createAiHubPersonalAgentChat(
        long workspaceId, long userId, int environment, long aiHubPersonalAgentId, @Nullable String title) {

        // Always-new semantics: every click on a personal agent row in the sidebar starts a fresh task
        // rather than restoring a prior thread. Past tasks remain accessible via the tasks list
        // — they're just not the default landing target. The threadId is a plain UUID so chat-memory rows are
        // isolated per task, not shared across a (user, agent) tuple.
        LocalDateTime now = LocalDateTime.now(clock);

        AiHubTask task = new AiHubTask(userId);

        task.setThreadId(UUID.randomUUID()
            .toString());
        task.setStatus(AiHubTaskStatus.ACTIVE);
        task.setMessageCount(0);
        task.setKind(AiHubTaskKind.PERSONAL_AGENT);
        task.setAiHubPersonalAgentId(aiHubPersonalAgentId);
        task.setEnvironment(EnumOrdinals.fromOrdinal(environment, Environment.class));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        // Title default: the LLM regen loop runs against personal-agent tasks once enough turns
        // accumulate, so an initial title here is the placeholder. Blank/null leaves the row to fall back to the
        // UI's "New AiHubTask" default until regen kicks in.
        if (title != null && !title.isBlank()) {
            task.setTitle(title);
        }

        AiHubTask saved = saveNew(task, workspaceId);

        // Copy the agent's tool template into ai_hub_task_tool so the LLM sees these tools
        // attached on the very first turn — same UX as AI Agent simple mode where an agent declares its
        // capabilities once and every chat starts with those tools already available. The lookups go through
        // ObjectProviders so a deployment without Personal Agents or AiHubTask Tools degrades to a
        // no-op
        // rather than failing task creation. Guard against a null id — Spring Data JDBC populates id
        // post-save in production but unit tests with mocked repositories may return the unsaved instance.
        if (saved.getId() != null) {
            copyAgentToolTemplate(saved.getId(), aiHubPersonalAgentId, environment);
            copyAgentResourceTemplate(saved.getId(), workspaceId, userId, aiHubPersonalAgentId);
        }

        publishTaskCreated(saved);

        return saved;
    }

    /**
     * Copies the personal agent's tool template rows into {@code ai_hub_task_tool} for the new task. Each
     * (componentName, componentVersion) pair becomes a single attached component (with no connection — the LLM will
     * pick a connection at first invocation), and each operation under that component becomes one tool row with empty
     * parameters. Failures are logged and swallowed so a misbehaving tool template (e.g. a referenced component that no
     * longer exists) does not block the task from being created.
     */
    private void copyAgentToolTemplate(long taskId, long aiHubPersonalAgentId, int environment) {
        // Defensive against unit-test wiring where @InjectMocks doesn't supply ObjectProvider mocks for
        // newly-added dependencies. Production wiring always provides them; if either provider is null we
        // silently skip the template copy — same behavior as a missing Personal Agents bean would produce.
        if (aiHubPersonalAgentServiceProvider == null || taskToolFacadeProvider == null) {
            return;
        }

        AiHubPersonalAgentService aiHubPersonalAgentService =
            aiHubPersonalAgentServiceProvider.getIfAvailable();
        AiHubTaskToolFacade taskToolFacade = taskToolFacadeProvider.getIfAvailable();

        if (aiHubPersonalAgentService == null || taskToolFacade == null) {
            return;
        }

        List<AiHubPersonalAgentTool> templateTools;

        try {
            templateTools = aiHubPersonalAgentService.listTools(aiHubPersonalAgentId);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to read tool template for personal agent {} on task {}; task continues "
                    + "with no attached tools.",
                aiHubPersonalAgentId, taskId, exception);

            return;
        }

        if (templateTools.isEmpty()) {
            return;
        }

        for (AiHubPersonalAgentTool templateTool : templateTools) {
            try {
                // Carry the agent's pre-set per-tool config (connection + parameters) into the task
                // binding. The component-attach uses the pinned connection so subsequent invocations don't have
                // to re-pick; the tool-attach forwards the parameters map verbatim so LLM-supplied invocation
                // args merge on top of the user's defaults at dispatch time. Both fall back to neutral values
                // (null connection, empty parameters) when the agent didn't configure them, matching the
                // pre-config behaviour exactly.
                long taskComponentId = taskToolFacade.attachComponent(
                    taskId, templateTool.getComponentName(), templateTool.getComponentVersion(),
                    templateTool.getConnectionId(), environment);

                taskToolFacade.addTool(
                    taskComponentId, templateTool.getOperationName(), templateTool.getParameters());
            } catch (RuntimeException exception) {
                // Per-tool failure is logged but doesn't abort the loop — partial-attach is preferable to a fully
                // empty task when one of the agent's tools references a stale component.
                log.warn(
                    "Failed to attach template tool {}:{}:{} to task {} (agent {}); skipping.",
                    templateTool.getComponentName(), templateTool.getComponentVersion(),
                    templateTool.getOperationName(), taskId, aiHubPersonalAgentId, exception);
            }
        }

        // Build the per-task tool-search subset from the same template tool list. Done here (not in the
        // catch branch above) so the subset reflects whatever subset of tools actually attached — a stale-component
        // skip means that tool isn't searchable for this task, which is the correct behaviour.
        scheduleTaskToolIndexRefresh(taskId, toReferences(templateTools));
    }

    /**
     * Copies the personal agent's resource template rows into {@code ai_hub_task_artifact} for the new task. Each
     * {@link AiHubPersonalAgentResource} becomes one {@code *_REFERENCED} artifact via
     * {@link AiHubTaskArtifactService#recordReference} (idempotent on {@code (taskId, kind, artifactId)}), so the
     * spawned task surfaces the agent's pre-declared resources in the right-panel artifact list — mirroring how
     * {@code copyAgentToolTemplate} seeds {@code ai_hub_task_tool}. Failures are logged and swallowed per-row so a
     * stale resource (e.g. a workflow that no longer exists) does not block task creation.
     */
    private void copyAgentResourceTemplate(
        long taskId, long workspaceId, long userId, long aiHubPersonalAgentId) {

        // Defensive against unit-test wiring where @InjectMocks doesn't supply ObjectProvider mocks. Production
        // wiring always provides them; a null provider means we silently skip the copy.
        if (aiHubPersonalAgentServiceProvider == null || taskArtifactServiceProvider == null) {
            return;
        }

        AiHubPersonalAgentService aiHubPersonalAgentService = aiHubPersonalAgentServiceProvider.getIfAvailable();
        AiHubTaskArtifactService taskArtifactService = taskArtifactServiceProvider.getIfAvailable();

        if (aiHubPersonalAgentService == null || taskArtifactService == null) {
            return;
        }

        List<AiHubPersonalAgentResource> templateResources;

        try {
            templateResources = aiHubPersonalAgentService.listResources(aiHubPersonalAgentId);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to read resource template for personal agent {} on task {}; task continues with no "
                    + "attached resources.",
                aiHubPersonalAgentId, taskId, exception);

            return;
        }

        if (templateResources.isEmpty()) {
            return;
        }

        for (AiHubPersonalAgentResource templateResource : templateResources) {
            try {
                AiHubTaskArtifactKind artifactKind = templateResource.getKind()
                    .toArtifactKind();

                // Personal-agent template resources don't currently carry the projectId / projectWorkflowId
                // needed for sidebar quick-open of workflow rows. Pass null so the artifact is recorded
                // without metadata; the row stays clickable for files/data-tables/knowledge-bases (which
                // route by their artifactId alone), and workflow rows will gracefully fall through to
                // non-clickable until the template plumbing learns to pass the parent project context.
                taskArtifactService.recordReference(
                    taskId, workspaceId, userId, artifactKind, templateResource.getResourceId(),
                    templateResource.getResourceName(), null);
            } catch (RuntimeException exception) {
                // Per-resource failure is logged but doesn't abort the loop — partial-attach is preferable to a
                // fully empty task when one of the agent's resources references a stale entity.
                log.warn(
                    "Failed to record template resource {}:{} to task {} (agent {}); skipping.",
                    templateResource.getKind(), templateResource.getResourceId(), taskId, aiHubPersonalAgentId,
                    exception);
            }
        }
    }

    /**
     * Translates the persisted {@link AiHubPersonalAgentTool} list into the lighter {@link TaskToolReference} shape
     * that {@link ToolSearchCatalogFeeder#populateForTask} consumes. Decouples the feeder from the personal-agent
     * persistence layer — the feeder only needs the (component, version, operation) triple, not the surrounding row
     * metadata.
     */
    private static List<TaskToolReference> toReferences(List<AiHubPersonalAgentTool> templateTools) {
        return templateTools.stream()
            .map(tool -> new TaskToolReference(
                tool.getComponentName(), tool.getComponentVersion(), tool.getOperationName()))
            .toList();
    }

    /**
     * Defers the per-task tool-search index refresh until after the surrounding transaction commits. Reasons:
     * <ul>
     * <li><b>External I/O.</b> The feeder calls the embeddings API and writes to pgvector — neither participates in the
     * task table's transaction. Doing the work inside the transaction would extend the lock window for no correctness
     * gain.</li>
     * <li><b>Rollback safety.</b> If the task row insert fails late in the transaction, the index is never touched —
     * versus calling the feeder synchronously, which would leave a populated session for a task that then never
     * persisted.</li>
     * </ul>
     *
     * <p>
     * No-op when the feeder bean isn't present (deployment without the tool-search advisor configuration). Failures
     * during the deferred populate are logged at WARN and swallowed so the user-visible task flow is unaffected.
     * </p>
     */
    private void scheduleTaskToolIndexRefresh(
        long taskId, List<TaskToolReference> toolReferences) {

        if (toolSearchCatalogFeederProvider == null) {
            return;
        }

        ToolSearchCatalogFeeder feeder = toolSearchCatalogFeederProvider.getIfAvailable();

        if (feeder == null) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    refreshIndex(feeder, taskId, toolReferences);
                }
            });
        } else {
            // Defensive — production call sites all run within a transaction, but tests or future call sites
            // without one shouldn't silently skip the refresh. Run inline.
            refreshIndex(feeder, taskId, toolReferences);
        }
    }

    private static void refreshIndex(
        ToolSearchCatalogFeeder feeder, long taskId, List<TaskToolReference> toolReferences) {
        try {
            feeder.populateForTask(taskId, toolReferences);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to refresh per-task tool search index for task {}; LLM will fall back "
                    + "to the workspace catalog.",
                taskId, exception);
        }
    }

    private void scheduleTaskToolSessionClear(long taskId) {
        if (toolSearchCatalogFeederProvider == null) {
            return;
        }

        ToolSearchCatalogFeeder feeder = toolSearchCatalogFeederProvider.getIfAvailable();

        if (feeder == null) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    clearIndex(feeder, taskId);
                }
            });
        } else {
            clearIndex(feeder, taskId);
        }
    }

    private static void clearIndex(ToolSearchCatalogFeeder feeder, long taskId) {
        try {
            feeder.clearTaskSession(taskId);
        } catch (RuntimeException exception) {
            // The task row is already gone at this point — a stale per-task session in the vector
            // store is benign (it just keeps a few orphaned rows that never match a future search). Log and move
            // on rather than propagating to the caller, who has no recovery path anyway.
            log.warn("Failed to clear tool search session for deleted task {}", taskId, exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubTask>
        list(long workspaceId, long userId, int environment, @Nullable AiHubTaskStatus status) {
        // The query already filters by workspaceId via the JOIN; the entity itself is workspace-agnostic and
        // callers needing the workspace for a task ID query the relation explicitly.
        //
        // A null status means "all statuses" — the tool-binding lookup (removeAiHubTaskTool) scans every task
        // regardless of active/archived state. Without this branch the status.ordinal() below NPEs.
        if (status == null) {
            return taskRepository.findByWorkspaceIdAndUserIdAndEnvironmentOrderByUpdatedAtDesc(
                workspaceId, userId, environment, LIST_LIMIT);
        }

        return taskRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
            workspaceId, userId, environment, status.ordinal(), LIST_LIMIT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubTaskMessage> loadMessages(
        long taskId, long requesterWorkspaceId, long requesterUserId) {

        AiHubTask task =
            loadAndCheckOwnership(taskId, requesterWorkspaceId, requesterUserId);

        return jdbcTemplate.query(
            CHAT_MEMORY_QUERY,
            (resultSet, rowNum) -> new AiHubTaskMessage(
                resultSet.getString("type"),
                resultSet.getString("content"),
                resultSet.getTimestamp("timestamp")
                    .toInstant()),
            task.getThreadId());
    }

    @Override
    public AiHubTask patch(
        long taskId, long requesterWorkspaceId, long requesterUserId,
        AiHubTaskPatch taskPatch) {

        AiHubTask task =
            loadAndCheckOwnership(taskId, requesterWorkspaceId, requesterUserId);

        if (taskPatch.title() != null) {
            task.setTitle(taskPatch.title());

            // Lock the title against further automatic regeneration. The patch path is invoked from both the
            // user-rename surface AND the LLM-generated title write — both should result in the title sticking,
            // so the regeneration fire-on-every-turn loop in generateAiHubTaskTitle stops calling the
            // LLM
            // for this row. Distinguishing user vs LLM at this layer would require a flag on
            // AiHubTaskPatch
            // and per-caller propagation; not worth the surface for the same end-state.
            task.setAutoTitled(false);
        }

        if (taskPatch.lastPreview() != null) {
            task.setLastPreview(taskPatch.lastPreview());
        }

        Integer messageCount = taskPatch.messageCount();

        if (messageCount != null) {
            task.setMessageCount(messageCount);
        }

        if (taskPatch.status() != null) {
            task.setStatus(taskPatch.status());
        }

        task.setUpdatedAt(LocalDateTime.now(clock));

        return taskRepository.save(task);
    }

    @Override
    public int bulkArchiveWorkflowChatAiHubTasks(long workspaceId, long userId, int environment) {
        // Cap at LIST_LIMIT to bound memory + write traffic in pathological cases (a workspace with thousands of
        // stale workflow chats). Re-running the bulk archive picks up the overflow on the next call. The same cap
        // is used by the sidebar listing query so anything visible to the user is reachable in one bulk run.
        List<AiHubTask> activeWorkflowChats =
            taskRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndKindAndStatus(
                workspaceId, userId, environment, AiHubTaskKind.WORKFLOW_CHAT.ordinal(),
                AiHubTaskStatus.ACTIVE.ordinal(), LIST_LIMIT);

        if (activeWorkflowChats.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        int archived = 0;

        for (AiHubTask task : activeWorkflowChats) {
            // Defensive: re-check ownership in the loop. The repository query already filters by userId AND joins on
            // workspace; if a future change relaxes that filter (or someone discovers a query-level bypass), the
            // ownership check here is the authoritative gate. Membership lookup is cheap (PK-indexed on the
            // (workspace_id, ai_hub_task_id) unique constraint).
            if (task.getUserId() != userId || !workspaceClaimsTask(workspaceId, task.getId())) {
                continue;
            }

            task.setStatus(AiHubTaskStatus.ARCHIVED);
            task.setUpdatedAt(now);

            taskRepository.save(task);

            archived++;
        }

        return archived;
    }

    @Override
    @Transactional
    public int truncateMessagesFrom(
        long taskId, long requesterWorkspaceId, long requesterUserId, int fromMessageIndex) {

        AiHubTask task =
            loadAndCheckOwnership(taskId, requesterWorkspaceId, requesterUserId);

        if (fromMessageIndex < 0) {
            // Defensive — a negative index would delete every row. The GraphQL surface should reject this earlier
            // but the service-layer check makes the contract explicit at the boundary.
            throw new IllegalArgumentException("fromMessageIndex must be non-negative; got " + fromMessageIndex);
        }

        // Read the timestamps of the rows from the same ORDER BY the loadMessages path uses, then delete by
        // exact-timestamp match. We can't use OFFSET in the DELETE because postgres doesn't support
        // DELETE ... ORDER BY ... OFFSET. The timestamp list is bounded to MESSAGE_LIMIT (500), so the IN-clause
        // delete is fine in practice — applies uniformly to STANDARD, WORKFLOW_CHAT, and PERSONAL_AGENT
        // tasks (any task kind that uses chat-memory). A task that has accumulated more
        // than MESSAGE_LIMIT messages can re-truncate after the first 500 land outside the window, or be deleted
        // entirely if the user wants a hard reset.
        List<Long> timestamps = jdbcTemplate.query(
            "SELECT \"timestamp\" FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ? "
                + "ORDER BY \"timestamp\" ASC LIMIT " + MESSAGE_LIMIT,
            (resultSet, rowNum) -> resultSet.getTimestamp("timestamp")
                .getTime(),
            task.getThreadId());

        if (fromMessageIndex >= timestamps.size()) {
            // Index past the end is a no-op rather than an error. Lets the client send "truncate from N" without
            // racing the server on history length — the worst case is no rows deleted.
            return 0;
        }

        // We delete by timestamp >= the captured cutoff. Two messages at the same millisecond would both fall in
        // the deletion window, which is the right semantic — they're indistinguishable for ordering purposes.
        long cutoffMillis = timestamps.get(fromMessageIndex);

        int deleted = jdbcTemplate.update(
            "DELETE FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ? AND \"timestamp\" >= ?",
            task.getThreadId(), new java.sql.Timestamp(cutoffMillis));

        // Bump the task's updatedAt so the sidebar re-sorts to the top — same convention every other
        // mutation here uses. messageCount is not authoritative for chat-memory rows (it tracks user-perceived
        // turns, not chat-memory entries) so we leave it untouched.
        task.setUpdatedAt(LocalDateTime.now(clock));
        taskRepository.save(task);

        return deleted;
    }

    @Override
    public boolean cancelWorkflowChatTurn(long taskId, long requesterWorkspaceId, long requesterUserId) {
        // Ownership check is the same gate as patch/delete — verifies the task belongs to the requester.
        // We don't restrict to kind=WORKFLOW_CHAT here: the cancel-turn surface is workflow-chat-specific by name
        // and the GraphQL mutation is the only entry point, but if a future caller wires this against a standard
        // task by mistake, the registry lookup returns null and we surface a clean false rather than
        // accidentally cancelling something else.
        AiHubTask task =
            loadAndCheckOwnership(taskId, requesterWorkspaceId, requesterUserId);

        Long jobId = jobRegistry.get(task.getId());

        if (jobId == null) {
            // No in-flight job for this task — either the turn finished between the user's stop click
            // and this handler firing, or there was never one running. Idempotent no-op; the client can render
            // "nothing to cancel" if it cares to distinguish.
            return false;
        }

        try {
            jobFacade.stopJob(jobId);
        } finally {
            // Clear the registry slot regardless of stopJob outcome. Even if the cancel failed (e.g. the job
            // already finished server-side between our get() and stopJob() calls), the entry is now stale and
            // a duplicate cancel attempt for this task should be a no-op rather than reissuing
            // stopJob against the same id.
            jobRegistry.clear(task.getId());
        }

        return true;
    }

    @Override
    public boolean cancelAiHubRun(long taskId, long requesterWorkspaceId, long requesterUserId, String runId) {
        // Sibling of cancelWorkflowChatTurn for the LLM-agent kinds (STANDARD / PERSONAL_AGENT). The agent
        // run is tracked in InFlightAiHubRunRegistry rather than the WorkflowChatJobRegistry, so cancellation
        // is a sink-completion signal — the AGUI agent.runAgent call doesn't expose a Disposable we can
        // dispose, but flipping the registry entry to terminated and completing the sink stops fan-out to
        // SSE subscribers and makes subsequent isInFlight probes return false. That's the contract the
        // client needs: a re-mount after the cancel sees a not-in-flight task and stops showing the
        // streaming UI. Passing runId lets the registry tombstone a run that hasn't registered yet.
        AiHubTask task = loadAndCheckOwnership(taskId, requesterWorkspaceId, requesterUserId);

        return inFlightRunRegistry.cancel(task.getThreadId(), runId);
    }

    @Override
    public void delete(long taskId, long requesterWorkspaceId, long requesterUserId) {
        AiHubTask task =
            loadAndCheckOwnership(taskId, requesterWorkspaceId, requesterUserId);

        // Delete the task row first inside the @Transactional boundary; the chat-memory rows are deleted
        // afterCommit. Mirrors AssetFileFacadeImpl.scheduleBlobDeleteAfterCommit: a rollback restores the task
        // row consistently with no orphan messages possible from a rolled-back delete, and a post-commit chat-memory
        // failure leaves orphan messages (best-effort cleanup) instead of zombie tasks the user can still see.
        taskRepository.delete(task);

        publishTaskDeleted(task);

        scheduleChatMemoryDeleteAfterCommit(task.getThreadId());

        // Same after-commit semantics for the per-task tool-search session. If the task delete rolls
        // back, the session stays populated and remains queryable by the agent — correct, since the task
        // row is still alive. On commit, the session entries are dropped so the vector store doesn't accumulate
        // orphans for every deleted task.
        scheduleTaskToolSessionClear(taskId);
    }

    private void scheduleChatMemoryDeleteAfterCommit(String threadId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            try {
                jdbcTemplate.update(CHAT_MEMORY_DELETE, threadId);
            } catch (DataAccessException exception) {
                log.warn(
                    "Failed to delete chat memory rows for threadId {} (task row already removed); leaving "
                        + "orphan messages for background cleanup",
                    threadId, exception);
            }

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                try {
                    jdbcTemplate.update(CHAT_MEMORY_DELETE, threadId);
                } catch (DataAccessException exception) {
                    log.warn(
                        "Failed to delete chat memory rows for threadId {} (task row already committed); "
                            + "leaving orphan messages for background cleanup",
                        threadId, exception);
                }
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiHubTask> findByThreadId(String threadId) {
        return taskRepository.findByThreadId(threadId);
    }

    @Override
    @Transactional(readOnly = true)
    public AiHubTask getById(long taskId, long requesterWorkspaceId, long requesterUserId) {
        return loadAndCheckOwnership(taskId, requesterWorkspaceId, requesterUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public AiHubTask getByThreadId(String threadId, long requesterWorkspaceId, long requesterUserId) {
        Optional<AiHubTask> taskOptional = taskRepository.findByThreadId(threadId);

        if (taskOptional.isEmpty()) {
            throw new NotFoundException("AiHubTask not found");
        }

        AiHubTask task = taskOptional.get();

        if (task.getUserId() != requesterUserId || !workspaceClaimsTask(requesterWorkspaceId, task.getId())) {
            log.warn(
                "AiHubTask ownership mismatch: requester userId={} workspaceId={} attempted to access "
                    + "threadId={} owned by userId={}. Returning 404 to avoid leaking existence.",
                requesterUserId, requesterWorkspaceId, threadId, task.getUserId());

            throw new NotFoundException("AiHubTask not found");
        }

        return task;
    }

    /**
     * Loads {@code taskId} and verifies it belongs to {@code requesterUserId} AND lives in
     * {@code requesterWorkspaceId}. The workspace check is required because the controller-level workspaceId query
     * parameter would otherwise be a decorative no-op — a caller could read or mutate any task they own from any
     * workspace.
     *
     * <p>
     * Probe-oracle defense: "does not exist" and "exists in another workspace/user" both return 404 with an opaque
     * message so an authenticated attacker cannot enumerate task ids across the install. Server-side logging at WARN
     * preserves the audit trail for security review.
     */
    private AiHubTask loadAndCheckOwnership(
        long taskId, long requesterWorkspaceId, long requesterUserId) {

        Optional<AiHubTask> taskOptional = taskRepository.findById(taskId);

        if (taskOptional.isEmpty()) {
            throw new NotFoundException("AiHubTask not found");
        }

        AiHubTask task = taskOptional.get();

        if (task.getUserId() != requesterUserId || !workspaceClaimsTask(requesterWorkspaceId, task.getId())) {
            log.warn(
                "AiHubTask ownership mismatch: requester userId={} workspaceId={} attempted to access "
                    + "taskId={} owned by userId={}. Returning 404 to avoid leaking "
                    + "existence.",
                requesterUserId, requesterWorkspaceId, taskId, task.getUserId());

            throw new NotFoundException("AiHubTask not found");
        }

        return task;
    }
}
