/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.ee.ai.hub.util.EnumOrdinals;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistent record of a single side-effect artifact produced during a AI Hub chat turn. Each row links to the parent
 * {@code ai_hub_chat} via {@link #chatId}.
 *
 * <p>
 * <b>Two write paths, by design.</b> This entity has two — and only two — legitimate ways to mutate the {@code status}
 * column:
 * </p>
 * <ol>
 * <li><b>Initial construction / hydration</b> via {@link #setStatus(AiHubChatArtifactStatus)}. The setter is
 * package-private and only callable from the {@code chat} package. {@code AiHubChatArtifactServiceImpl} uses it to set
 * {@code APPLIED} (or {@code IRREVERSIBLE}) on a fresh row before the first save, and the row-mapper uses it to bind
 * the column into a new instance during hydration. Neither use is a state <em>transition</em>.</li>
 * <li><b>Lifecycle transitions</b> ({@code APPLIED → EXPIRED} / {@code IRREVERSIBLE}) go exclusively through
 * {@code AiHubChatArtifactService#updateStatus}, which issues a raw {@code UPDATE} that deliberately bypasses the
 * setter. Co-locating the transition logic with the SQL keeps the audit-path single-source.</li>
 * </ol>
 * <p>
 * <b>Why not route transitions through the setter?</b> The transition path needs row-level locking semantics tied to
 * the SQL statement (the {@code UPDATE ... WHERE} predicate is the gate), and a load-then-save pattern would either
 * race or require an explicit {@code SELECT FOR UPDATE} that the raw {@code UPDATE} provides for free. A setter-side
 * guard would also create the misleading impression of defense-in-depth on a path that the transition layer does not
 * traverse.
 * </p>
 * <p>
 * <b>What this means for new code:</b> never call {@link #setStatus(AiHubChatArtifactStatus)} on a loaded row to try to
 * transition status — the call would silently appear to succeed in memory but never reach the database via the
 * raw-{@code UPDATE} contract. Use {@code AiHubChatArtifactService#updateStatus} instead.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_chat_artifact")
public class AiHubChatArtifact {

    @Id
    private Long id;

    @Column("chat_id")
    private long chatId;

    @Column("kind")
    private int kind;

    @Column("artifact_id")
    private String artifactId;

    @Column("artifact_name")
    private String artifactName;

    @Column("metadata_json")
    private String metadataJson;

    @Column("status")
    private int status = AiHubChatArtifactStatus.APPLIED.ordinal();

    @Column("environment")
    private int environment;

    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * Wall-clock time of the last {@code status} transition (APPLIED → EXPIRED / IRREVERSIBLE). Nullable for rows whose
     * status was set on insert and never transitioned — callers should treat {@code null} here as "same as
     * {@link #createdAt}". Populated by {@code AiHubChatArtifactService#updateStatus}'s raw UPDATE; do not write
     * through {@link #setStatusChangedAt} for transitions (the SQL UPDATE is the audit-path source of truth, mirroring
     * the {@code status} two-write-path discipline). Without this column, "when was this artifact undone?" requires a
     * MutationLog join, which is expensive for the audit listing UI's per-row badge.
     */
    @Column("status_changed_at")
    private LocalDateTime statusChangedAt;

    public AiHubChatArtifact() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public AiHubChatArtifactKind getKind() {
        return EnumOrdinals.fromOrdinal(kind, AiHubChatArtifactKind.class);
    }

    public void setKind(AiHubChatArtifactKind kind) {
        this.kind = kind.ordinal();
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public AiHubChatArtifactStatus getStatus() {
        return EnumOrdinals.fromOrdinal(status, AiHubChatArtifactStatus.class);
    }

    /**
     * Used only for initial construction (before first save) and JDBC row-mapper hydration. Lifecycle
     * <em>transitions</em> ({@code APPLIED → EXPIRED} / {@code IRREVERSIBLE}) go through
     * {@code AiHubChatArtifactService#updateStatus}'s raw {@code UPDATE} — see the class-level Javadoc for the two-path
     * rationale.
     */
    public void setStatus(AiHubChatArtifactStatus status) {
        this.status = status.ordinal();
    }

    /**
     * Returns the {@link Environment} this mutation-log row belongs to. Denormalised from the parent {@code AiHubChat}
     * for analytics-path performance — see the migration's rationale.
     */
    public Environment getEnvironment() {
        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            throw new IllegalStateException("Invalid environment value: " + environment);
        }

        return environments[environment];
    }

    public long getEnvironmentId() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        if (environment != null) {
            this.environment = environment.ordinal();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the wall-clock time of the last {@code status} transition, or {@code null} when the row's status was set
     * on insert and has never transitioned. Callers that want a single non-null timestamp can fall back to
     * {@link #getCreatedAt()} when this returns {@code null}.
     */
    public LocalDateTime getStatusChangedAt() {
        return statusChangedAt;
    }

    /**
     * Used only for initial construction (before first save) and JDBC row-mapper hydration. Lifecycle
     * <em>transitions</em> set this column via {@code AiHubChatArtifactService#updateStatus}'s raw {@code UPDATE} — see
     * the class-level Javadoc for the two-path rationale. Bypassing the service via this setter for a transition would
     * orphan the row's audit timestamp from the actual status change.
     */
    public void setStatusChangedAt(LocalDateTime statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        AiHubChatArtifact that = (AiHubChatArtifact) other;

        // Two transient (id == null) instances must not collide in a Set before persistence.
        if (id == null) {
            return this == that;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "AiHubChatArtifact{" +
            "id=" + id +
            ", chatId=" + chatId +
            ", kind=" + kind +
            ", artifactId='" + artifactId + '\'' +
            ", artifactName='" + artifactName + '\'' +
            ", status=" + status +
            ", environment=" + environment +
            ", createdAt=" + createdAt +
            ", statusChangedAt=" + statusChangedAt +
            '}';
    }
}
