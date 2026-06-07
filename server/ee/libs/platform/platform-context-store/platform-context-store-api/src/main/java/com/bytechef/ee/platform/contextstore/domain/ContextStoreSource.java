/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.domain;

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
 * Domain class representing a Context Store source — a configured binding of a workspace, a source component, an
 * {@code ItemReader} cluster element on that component, an optional connection, a sync cadence, a lifecycle status, and
 * the record-shape metadata (id field, indexed fields, semantic-index fields, stored fields) that drives both record
 * persistence and retrieval. The configured source is periodically synced into the Context Store replica.
 *
 * <p>
 * Source absorbs what used to be {@code ContextStoreEntity} — the multi-entity-per-source design was over-promised (the
 * workflow generator only ever used {@code entities.get(0)}), so the shape is collapsed to 1:1. The {@code entityName}
 * field is the stable wire identifier used by the destination component handler and by ClickHouse table naming; the
 * {@code name} field is the display name (mutable independently of the wire identifier).
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@Table("context_store_source")
public class ContextStoreSource {

    @Id
    private Long id;

    /**
     * FK to the parent {@link ContextStore}. Environment is inherited via this column — there is no environment column
     * on the source row itself. Sources of the same {@code name} can exist across multiple stores within a workspace
     * because the uniqueness constraint is {@code (context_store_id, name)}, not {@code name} globally.
     */
    @Column("context_store_id")
    private AggregateReference<ContextStore, Long> contextStoreId;

    private String name;

    /**
     * Stable wire identifier for the source's records. Used by the destination component handler (carried in the sync
     * workflow JSON), by ClickHouse table naming, and by the record-routing path. Independent of {@link #name}, which
     * is a display label that can be renamed without invalidating already-synced records.
     */
    @Column("entity_name")
    private String entityName;

    @Nullable
    private String description;

    @Column("id_field")
    private String idField;

    @Column("stored_fields")
    @Nullable
    private MapWrapper storedFields;

    @Column("indexed_fields")
    private MapWrapper indexedFields;

    @Column("semantic_index_fields")
    @Nullable
    private MapWrapper semanticIndexFields;

    /**
     * Source-component parameters bound at create time (e.g. Airtable {@code baseId} + {@code tableId}). Passed into
     * the source-side {@code ItemReader} cluster element on every sync run. Stored as a JSONB map; the wire shape is
     * source-component specific and only validated against required-parameter metadata at create time.
     */
    @Column
    @Nullable
    private MapWrapper parameters;

    /**
     * Cached ClickHouse table name when the deployment is opted in to ClickHouse. Null for Postgres-only deployments.
     * Populated by the facade's create flow when the {@code ClickHouseTableProvisioner} bean is available.
     */
    @Column("clickhouse_table_name")
    @Nullable
    private String clickhouseTableName;

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

    public ContextStoreSource() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContextStoreId() {
        return contextStoreId == null ? null : contextStoreId.getId();
    }

    public void setContextStoreId(Long contextStoreId) {
        this.contextStoreId = contextStoreId == null ? null : AggregateReference.to(contextStoreId);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public String getIdField() {
        return idField;
    }

    public void setIdField(String idField) {
        this.idField = idField;
    }

    public Map<String, ?> getIndexedFields() {
        return indexedFields.getMap();
    }

    public void setIndexedFields(Map<String, ?> indexedFields) {
        this.indexedFields = new MapWrapper(Objects.requireNonNull(indexedFields, "indexedFields"));
    }

    public @Nullable Map<String, ?> getStoredFields() {
        return storedFields == null ? null : storedFields.getMap();
    }

    public void setStoredFields(@Nullable Map<String, ?> storedFields) {
        this.storedFields = storedFields == null ? null : new MapWrapper(storedFields);
    }

    @Nullable
    public Map<String, ?> getSemanticIndexFields() {
        return semanticIndexFields == null ? null : semanticIndexFields.getMap();
    }

    public void setSemanticIndexFields(@Nullable Map<String, ?> semanticIndexFields) {
        this.semanticIndexFields = semanticIndexFields == null ? null : new MapWrapper(semanticIndexFields);
    }

    @Nullable
    public Map<String, ?> getParameters() {
        return parameters == null ? null : parameters.getMap();
    }

    public void setParameters(@Nullable Map<String, ?> parameters) {
        this.parameters = parameters == null ? null : new MapWrapper(parameters);
    }

    @Nullable
    public String getClickhouseTableName() {
        return clickhouseTableName;
    }

    public void setClickhouseTableName(@Nullable String clickhouseTableName) {
        this.clickhouseTableName = clickhouseTableName;
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

    public String getCadence() {
        return cadence;
    }

    public void setCadence(String cadence) {
        this.cadence = cadence;
    }

    public ContextStoreSourceStatus getStatus() {
        return ContextStoreSourceStatus.values()[status];
    }

    public void setStatus(ContextStoreSourceStatus status) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ContextStoreSource that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
