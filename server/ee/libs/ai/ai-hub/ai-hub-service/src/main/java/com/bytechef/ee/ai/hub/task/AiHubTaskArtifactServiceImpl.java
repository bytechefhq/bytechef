/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.task.repository.AiHubTaskArtifactRepository;
import com.bytechef.ee.ai.hub.task.repository.AiHubTaskRepository;
import com.bytechef.ee.ai.hub.task.repository.WorkspaceAiHubTaskRepository;
import com.bytechef.ee.ai.hub.util.EnumOrdinals;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Default implementation of {@link AiHubTaskArtifactService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@Transactional
public class AiHubTaskArtifactServiceImpl implements AiHubTaskArtifactService {

    /**
     * Hard upper bound on the number of artifacts returned by {@link #listByTask}. Without this cap a long-running task
     * could accumulate hundreds of artifacts and force the controller to materialise them all in memory; the audit/log
     * UI never needs more than a single bounded page at a time.
     */
    private static final int LIST_LIMIT = 100;

    private static final List<Integer> WORKFLOW_KIND_ORDINALS = List.of(
        AiHubTaskArtifactKind.WORKFLOW_CREATED.ordinal(),
        AiHubTaskArtifactKind.WORKFLOW_UPDATED.ordinal(),
        AiHubTaskArtifactKind.WORKFLOW_REFERENCED.ordinal());

    private static final Logger log = LoggerFactory.getLogger(AiHubTaskArtifactServiceImpl.class);

    private static final RowMapper<AiHubTaskArtifact> ARTIFACT_ROW_MAPPER = (resultSet, rowNum) -> {
        AiHubTaskArtifact artifact = new AiHubTaskArtifact();

        artifact.setId(resultSet.getLong("id"));
        artifact.setTaskId(resultSet.getLong("task_id"));
        artifact
            .setKind(EnumOrdinals.fromOrdinal(resultSet.getInt("kind"), AiHubTaskArtifactKind.class));
        artifact.setArtifactId(resultSet.getString("artifact_id"));
        artifact.setArtifactName(resultSet.getString("artifact_name"));
        artifact.setMetadataJson(resultSet.getString("metadata_json"));
        artifact.setStatus(
            EnumOrdinals.fromOrdinal(resultSet.getInt("status"), AiHubTaskArtifactStatus.class));
        artifact.setEnvironment(EnumOrdinals.fromOrdinal(resultSet.getInt("environment"), Environment.class));
        artifact.setCreatedAt(resultSet.getObject("created_at", LocalDateTime.class));

        return artifact;
    };

    private final AiHubTaskArtifactRepository taskArtifactRepository;
    private final AiHubTaskRepository taskRepository;
    private final WorkspaceAiHubTaskRepository workspaceTaskRepository;
    private final Clock clock;
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    @SuppressFBWarnings({
        "EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"
    })
    public AiHubTaskArtifactServiceImpl(
        AiHubTaskArtifactRepository taskArtifactRepository,
        AiHubTaskRepository taskRepository,
        WorkspaceAiHubTaskRepository workspaceTaskRepository,
        JdbcTemplate jdbcTemplate,
        JsonMapper jsonMapper) {

        this.taskArtifactRepository = taskArtifactRepository;
        this.taskRepository = taskRepository;
        this.workspaceTaskRepository = workspaceTaskRepository;
        this.clock = Clock.systemUTC();
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void record(
        String threadId, long userId, AiHubTaskArtifactKind kind,
        String artifactId, String artifactName, @Nullable Map<String, Object> metadata) {

        Optional<AiHubTask> taskOptional =
            taskRepository.findByThreadIdAndUserId(threadId, userId);

        if (taskOptional.isEmpty()) {
            // The caller's mutation has already committed by the time this method runs, so dropping the artifact
            // means the user loses the audit/undo handle for it. Surface this loudly so it shows up in production
            // logs.
            log.warn(
                "No task found for threadId={} userId={} kind={} artifactId={} artifactName={} — " +
                    "DROPPING artifact record. The underlying mutation has already committed; the user will not " +
                    "see an undo entry for it.",
                threadId, userId, kind, artifactId, artifactName);

            return;
        }

        AiHubTask task = taskOptional.get();

        AiHubTaskArtifact artifact = new AiHubTaskArtifact();

        artifact.setTaskId(task.getId());
        artifact.setKind(kind);
        artifact.setArtifactId(artifactId);
        artifact.setArtifactName(artifactName);
        // Denormalise the parent task's environment onto the artifact row. The audit-listing UI filters by
        // environment per row and the JOIN cost on a multi-million-row table is non-trivial; carrying the value at
        // write time is cheaper than paying the join on every page load. The task's environment is itself
        // immutable post-create (see AiHubTaskServiceImpl.create), so this is a safe one-time copy.
        artifact.setEnvironment(task.getEnvironment());
        artifact.setCreatedAt(LocalDateTime.now(clock));

        // The artifact is an immutable audit-log entry — the underlying mutation has already committed.
        // The status enum is retained for ordinal stability (existing rows in production may still carry
        // legacy values) but every new row is recorded as APPLIED.
        if (metadata != null && !metadata.isEmpty()) {
            artifact.setMetadataJson(serializeMetadata(metadata, kind, artifactId));
        }

        artifact.setStatus(AiHubTaskArtifactStatus.APPLIED);

        taskArtifactRepository.save(artifact);
    }

    @Override
    public void recordWorkflowArtifact(
        String threadId, @Nullable Long userId, AiHubTaskArtifactKind kind,
        String workflowId, long projectId, @Nullable Long projectWorkflowId, String workflowName) {

        if (userId == null) {
            log.warn(
                "recordWorkflowArtifact called without a bound userId — skipping (threadId={}, workflowId={}). "
                    + "The persist has already committed; the user will not see an artifact for it.",
                threadId, workflowId);

            return;
        }

        Optional<AiHubTask> taskOptional = taskRepository.findByThreadIdAndUserId(threadId, userId);

        if (taskOptional.isEmpty()) {
            log.warn(
                "No task found for threadId={} userId={} — DROPPING workflow artifact (workflowId={}, name={}).",
                threadId, userId, workflowId, workflowName);

            return;
        }

        AiHubTask task = taskOptional.get();

        // Match the metadata shape of the legacy openWorkflowTab rows so the client sidebar quick-open keeps
        // working: projectId as a String, projectWorkflowId as a Long.
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("projectId", String.valueOf(projectId));

        if (projectWorkflowId != null) {
            metadata.put("projectWorkflowId", projectWorkflowId);
        }

        Optional<AiHubTaskArtifact> existingOptional =
            taskArtifactRepository.findFirstByTaskIdAndArtifactIdAndKindIn(
                task.getId(), workflowId, WORKFLOW_KIND_ORDINALS);

        AiHubTaskArtifact artifact;

        if (existingOptional.isPresent()) {
            // One workflow -> one row. Refresh the display name + routing metadata but preserve the existing
            // kind: a WORKFLOW_CREATED row must not be downgraded to WORKFLOW_REFERENCED by a later open.
            artifact = existingOptional.get();

            artifact.setArtifactName(workflowName);
            artifact.setMetadataJson(serializeMetadata(metadata, artifact.getKind(), workflowId));
        } else {
            artifact = new AiHubTaskArtifact();

            artifact.setTaskId(task.getId());
            artifact.setKind(kind);
            artifact.setArtifactId(workflowId);
            artifact.setArtifactName(workflowName);
            artifact.setEnvironment(task.getEnvironment());
            artifact.setCreatedAt(LocalDateTime.now(clock));
            artifact.setMetadataJson(serializeMetadata(metadata, kind, workflowId));
            artifact.setStatus(AiHubTaskArtifactStatus.APPLIED);
        }

        taskArtifactRepository.save(artifact);
    }

    @Override
    public AiHubTaskArtifact recordReference(
        long taskId, long requesterWorkspaceId, long requesterUserId,
        AiHubTaskArtifactKind kind,
        String artifactId, String artifactName,
        @Nullable Map<String, Object> metadata) {

        AiHubTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException("AiHubTask not found: " + taskId));

        if (task.getUserId() != requesterUserId) {
            throw new ForbiddenException(
                "User " + requesterUserId + " does not own task " + taskId);
        }

        if (!workspaceTaskRepository.findByWorkspaceIdAndAiHubTaskId(requesterWorkspaceId, task.getId())
            .isPresent()) {
            throw new ForbiddenException(
                "AiHubTask " + taskId + " does not live in workspace " + requesterWorkspaceId);
        }

        // Idempotency check. The user re-attaching the same file/workflow in the composer should hit the
        // existing row rather than proliferate sidebar entries.
        //
        // Workflows get special treatment: a single workflow may already have been recorded server-side as
        // WORKFLOW_CREATED / WORKFLOW_UPDATED (by createProjectWorkflow / updateWorkflow), and this
        // client-driven WORKFLOW_REFERENCED call must collapse onto that row instead of adding a second
        // sidebar entry for the same workflow. recordWorkflowArtifact already enforces this "one workflow ->
        // one row" rule across all workflow kinds; mirror it here by matching on (taskId, artifactId) across
        // the workflow kinds rather than scoping to WORKFLOW_REFERENCED alone. Non-workflow kinds keep the
        // exact (taskId, kind, artifactId) tuple, which matches the artifact-list rendering.
        Optional<AiHubTaskArtifact> existing =
            WORKFLOW_KIND_ORDINALS.contains(kind.ordinal())
                ? taskArtifactRepository.findFirstByTaskIdAndArtifactIdAndKindIn(
                    taskId, artifactId, WORKFLOW_KIND_ORDINALS)
                : taskArtifactRepository.findFirstByTaskIdAndKindAndArtifactId(
                    taskId, kind.ordinal(), artifactId);

        if (existing.isPresent()) {
            return existing.get();
        }

        AiHubTaskArtifact artifact = new AiHubTaskArtifact();

        artifact.setTaskId(taskId);
        artifact.setKind(kind);
        artifact.setArtifactId(artifactId);
        artifact.setArtifactName(artifactName);
        artifact.setEnvironment(task.getEnvironment());
        artifact.setCreatedAt(LocalDateTime.now(clock));

        if (metadata != null && !metadata.isEmpty()) {
            artifact.setMetadataJson(serializeMetadata(metadata, kind, artifactId));
        }

        // Reference-kind artifacts are immutable from the artifact-status perspective: there's no agent-driven
        // mutation to undo, so APPLIED is the natural fit. The kind itself carries reversible=false in the enum,
        // which the reverser registry uses to skip lookup of an undo-handler.
        artifact.setStatus(AiHubTaskArtifactStatus.APPLIED);

        return taskArtifactRepository.save(artifact);
    }

    @Override
    public void deleteReference(long artifactId, long requesterWorkspaceId, long requesterUserId) {
        Optional<AiHubTaskArtifact> artifactOptional = taskArtifactRepository.findById(artifactId);

        if (artifactOptional.isEmpty()) {
            // Idempotent: re-deleting a row that's already gone is a no-op so the UI can retry safely
            // (network blip, double-click) without surfacing a confusing "not found" toast.
            return;
        }

        AiHubTaskArtifact artifact = artifactOptional.get();

        AiHubTaskArtifactKind kind = artifact.getKind();

        if (kind != AiHubTaskArtifactKind.FILE_REFERENCED
            && kind != AiHubTaskArtifactKind.WORKFLOW_REFERENCED
            && kind != AiHubTaskArtifactKind.DATA_TABLE_REFERENCED
            && kind != AiHubTaskArtifactKind.KB_REFERENCED) {

            // Agent-driven audit-log rows (FILE_CREATED, WORKFLOW_EXECUTION_STARTED, etc.) are immutable
            // history. Allowing user-driven deletion of those would corrupt the undo chain and the
            // workspace-wide audit listing.
            throw new IllegalArgumentException(
                "Only reference-kind artifacts can be deleted via this surface; got kind=" + kind);
        }

        AiHubTask task = taskRepository.findById(artifact.getTaskId())
            .orElseThrow(() -> new NotFoundException("AiHubTask not found: " + artifact.getTaskId()));

        if (task.getUserId() != requesterUserId) {
            throw new ForbiddenException("User " + requesterUserId + " does not own task " + task.getId());
        }

        if (!workspaceTaskRepository.findByWorkspaceIdAndAiHubTaskId(requesterWorkspaceId, task.getId())
            .isPresent()) {

            throw new ForbiddenException(
                "AiHubTask " + task.getId() + " does not live in workspace " + requesterWorkspaceId);
        }

        taskArtifactRepository.deleteById(artifactId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubTaskArtifact> listByTask(
        long taskId, long requesterWorkspaceId, long requesterUserId) {

        AiHubTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException("AiHubTask not found: " + taskId));

        if (task.getUserId() != requesterUserId) {
            throw new ForbiddenException(
                "User " + requesterUserId + " does not own task " + taskId);
        }

        if (!workspaceTaskRepository.findByWorkspaceIdAndAiHubTaskId(requesterWorkspaceId, task.getId())
            .isPresent()) {
            throw new ForbiddenException(
                "AiHubTask " + taskId + " is not in workspace " + requesterWorkspaceId);
        }

        return taskArtifactRepository.findByTaskIdOrderByCreatedAtDesc(
            taskId, Limit.of(LIST_LIMIT));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByTask(long taskId) {
        return taskArtifactRepository.countByTaskId(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubTaskArtifact> listByWorkspace(
        long workspaceId, @Nullable Integer environment, @Nullable Long userId,
        @Nullable AiHubTaskArtifactKind kind, @Nullable LocalDateTime from, @Nullable LocalDateTime to,
        int page, int size) {

        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT a.id, a.task_id, a.kind, a.artifact_id, a.artifact_name, " +
                "a.metadata_json, a.status, a.environment, a.created_at " +
                "FROM ai_hub_task_artifact a " +
                "JOIN ai_hub_task c ON c.id = a.task_id " +
                "JOIN workspace_ai_hub_task w ON w.ai_hub_task_id = c.id " +
                "WHERE w.workspace_id = ?");

        List<Object> params = new ArrayList<>();

        params.add(workspaceId);

        appendWorkspaceFilters(sqlBuilder, params, environment, userId, kind, from, to);

        sqlBuilder.append(" ORDER BY a.created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add((long) page * size);

        return jdbcTemplate.query(sqlBuilder.toString(), ARTIFACT_ROW_MAPPER, params.toArray());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByWorkspace(
        long workspaceId, @Nullable Integer environment, @Nullable Long userId,
        @Nullable AiHubTaskArtifactKind kind, @Nullable LocalDateTime from,
        @Nullable LocalDateTime to) {

        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT COUNT(*) " +
                "FROM ai_hub_task_artifact a " +
                "JOIN ai_hub_task c ON c.id = a.task_id " +
                "JOIN workspace_ai_hub_task w ON w.ai_hub_task_id = c.id " +
                "WHERE w.workspace_id = ?");

        List<Object> params = new ArrayList<>();

        params.add(workspaceId);

        appendWorkspaceFilters(sqlBuilder, params, environment, userId, kind, from, to);

        Long count = jdbcTemplate.queryForObject(sqlBuilder.toString(), Long.class, params.toArray());

        return count != null ? count : 0L;
    }

    private void appendWorkspaceFilters(
        StringBuilder sqlBuilder, List<Object> params, @Nullable Integer environment,
        @Nullable Long userId, @Nullable AiHubTaskArtifactKind kind,
        @Nullable LocalDateTime from, @Nullable LocalDateTime to) {

        if (environment != null) {
            // Filter against the denormalised column on the artifact row, not c.environment, so the predicate runs
            // before the task table is materialised. Combined with the idx_cc_task_artifact_environment
            // index this turns the audit-list query from a hash join to a single index probe for the common path.
            sqlBuilder.append(" AND a.environment = ?");
            params.add(environment);
        }

        if (userId != null) {
            sqlBuilder.append(" AND c.user_id = ?");
            params.add(userId);
        }

        if (kind != null) {
            sqlBuilder.append(" AND a.kind = ?");
            params.add(kind.ordinal());
        }

        if (from != null) {
            sqlBuilder.append(" AND a.created_at >= ?");
            params.add(from);
        }

        if (to != null) {
            sqlBuilder.append(" AND a.created_at <= ?");
            params.add(to);
        }
    }

    /**
     * Returns the JSON payload, or {@code null} when serialization fails. The artifact is still recorded with a null
     * {@code metadataJson} when this returns null — losing the metadata does not invalidate the audit row. The kind and
     * artifactId are logged so ops can correlate the failure to the originating mutation.
     */
    private String
        serializeMetadata(Map<String, Object> metadata, AiHubTaskArtifactKind kind, String artifactId) {
        try {
            return jsonMapper.writeValueAsString(metadata);
        } catch (JacksonException exception) {
            log.warn(
                "Failed to serialize artifact metadata for kind={}, artifactId={} — recording artifact with "
                    + "null metadataJson.",
                kind, artifactId, exception);

            return null;
        }
    }
}
