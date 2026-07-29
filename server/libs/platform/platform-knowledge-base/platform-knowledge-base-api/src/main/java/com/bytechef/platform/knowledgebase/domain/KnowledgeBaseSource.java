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

package com.bytechef.platform.knowledgebase.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.bytechef.platform.connection.domain.Connection;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Domain class representing a Knowledge Base source — a configured binding of a Knowledge Base, a source component, an
 * optional connection, a sync cadence, and a lifecycle status. Periodically pulls document-shaped content from the
 * source into the target Knowledge Base via the auto-generated DataStream sync workflow. Workspace ownership is the
 * nullable {@code workspace_id} column — a source belongs to at most one workspace.
 *
 * @author Ivica Cardic
 */
@Table("knowledge_base_source")
public class KnowledgeBaseSource {

    @Id
    private Long id;

    private String name;

    @Column("source_component_name")
    private String sourceComponentName;

    @Column("source_component_version")
    private int sourceComponentVersion;

    @Column("source_cluster_element_name")
    @Nullable
    private String sourceClusterElementName;

    @Column("connection_id")
    @Nullable
    private AggregateReference<Connection, Long> connectionId;

    @Column("knowledge_base_id")
    private AggregateReference<KnowledgeBase, Long> knowledgeBaseId;

    private String cadence;

    private int status;

    private boolean enabled = true;

    @Column("last_sync_run_at")
    @Nullable
    private Instant lastSyncRunAt;

    @Column("last_sync_job_execution_id")
    @Nullable
    private Long lastSyncJobExecutionId;

    @Column("workflow_id")
    @Nullable
    private String workflowId;

    @Column
    @Nullable
    private MapWrapper parameters;

    @Column("metadata_fields")
    @Nullable
    private MapWrapper metadataFields;

    /**
     * Phase 17b paired-cadence support. When non-null, the workflow generator emits a second schedule trigger that runs
     * FULL_REPLACE on this rare cadence alongside the existing incremental {@link #cadence}. Null preserves MVP
     * single-trigger behavior.
     */
    @Column("full_replace_cadence")
    @Nullable
    private String fullReplaceCadence;

    /**
     * Phase 17b tombstone-derivation strategy. Persisted as the {@link TombstoneStrategy} ordinal — see that enum's
     * Javadoc for the stability contract. Defaults to {@link TombstoneStrategy#PERIODIC_FULL_REPLACE} (matches today's
     * behavior when paired with a daily full-replace cadence).
     */
    @Column("tombstone_strategy")
    private int tombstoneStrategy;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Version
    private int version;

    /**
     * Workspace the source belongs to. Nullable: a source belongs to at most one workspace, and null means none
     * applies. Workspace-scoped queries never match a null, so a workspace-less source is invisible to them.
     */
    @Column("workspace_id")
    private @Nullable Long workspaceId;

    public KnowledgeBaseSource() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceComponentName() {
        return sourceComponentName;
    }

    public void setSourceComponentName(String sourceComponentName) {
        this.sourceComponentName = sourceComponentName;
    }

    public int getSourceComponentVersion() {
        return sourceComponentVersion;
    }

    public void setSourceComponentVersion(int sourceComponentVersion) {
        this.sourceComponentVersion = sourceComponentVersion;
    }

    @Nullable
    public String getSourceClusterElementName() {
        return sourceClusterElementName;
    }

    public void setSourceClusterElementName(@Nullable String sourceClusterElementName) {
        this.sourceClusterElementName = sourceClusterElementName;
    }

    @Nullable
    public Long getConnectionId() {
        return connectionId == null ? null : connectionId.getId();
    }

    public void setConnectionId(@Nullable Long connectionId) {
        this.connectionId = connectionId == null ? null : AggregateReference.to(connectionId);
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBaseId == null ? null : knowledgeBaseId.getId();
    }

    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId == null ? null : AggregateReference.to(knowledgeBaseId);
    }

    public String getCadence() {
        return cadence;
    }

    public void setCadence(String cadence) {
        this.cadence = cadence;
    }

    public KnowledgeBaseSourceStatus getStatus() {
        return KnowledgeBaseSourceStatus.values()[status];
    }

    public void setStatus(KnowledgeBaseSourceStatus status) {
        this.status = status.ordinal();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Nullable
    public Instant getLastSyncRunAt() {
        return lastSyncRunAt;
    }

    public void setLastSyncRunAt(@Nullable Instant lastSyncRunAt) {
        this.lastSyncRunAt = lastSyncRunAt;
    }

    @Nullable
    public Long getLastSyncJobExecutionId() {
        return lastSyncJobExecutionId;
    }

    public void setLastSyncJobExecutionId(@Nullable Long lastSyncJobExecutionId) {
        this.lastSyncJobExecutionId = lastSyncJobExecutionId;
    }

    @Nullable
    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(@Nullable String workflowId) {
        this.workflowId = workflowId;
    }

    @Nullable
    public Map<String, ?> getParameters() {
        return parameters == null ? null : parameters.getMap();
    }

    public void setParameters(@Nullable Map<String, ?> parameters) {
        this.parameters = parameters == null ? null : new MapWrapper(parameters);
    }

    /**
     * Optional whitelist controlling which incoming metadata fields are flattened into KB document tags. {@code null}
     * (the default) preserves MVP behavior: every reader-emitted record field becomes a {@code key=value} tag. When set
     * to {@code {fields: [...]}}, only the listed field names are kept; everything else is dropped at write time.
     *
     * <p>
     * Mirrors {@code ContextStoreEntity.storedFields}, but applies to tag generation (not column projection) since KB
     * documents flatten metadata to a tag list rather than a structured row.
     * </p>
     */
    @Nullable
    public Map<String, ?> getMetadataFields() {
        return metadataFields == null ? null : metadataFields.getMap();
    }

    public void setMetadataFields(@Nullable Map<String, ?> metadataFields) {
        this.metadataFields = metadataFields == null ? null : new MapWrapper(metadataFields);
    }

    @Nullable
    public String getFullReplaceCadence() {
        return fullReplaceCadence;
    }

    public void setFullReplaceCadence(@Nullable String fullReplaceCadence) {
        this.fullReplaceCadence = fullReplaceCadence;
    }

    public TombstoneStrategy getTombstoneStrategy() {
        return TombstoneStrategy.values()[tombstoneStrategy];
    }

    public void setTombstoneStrategy(TombstoneStrategy tombstoneStrategy) {
        Objects.requireNonNull(tombstoneStrategy, "tombstoneStrategy");

        this.tombstoneStrategy = tombstoneStrategy.ordinal();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof KnowledgeBaseSource that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
