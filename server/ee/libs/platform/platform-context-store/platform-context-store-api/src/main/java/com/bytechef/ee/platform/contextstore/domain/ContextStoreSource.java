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
 * @author Ivica Cardic
 * @version ee
 */
@Table("context_store_source")
public class ContextStoreSource {

    @Id
    private Long id;

    @Column("context_store_id")
    private AggregateReference<ContextStore, Long> contextStoreId;

    private String name;

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

    @Column
    @Nullable
    private MapWrapper parameters;

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

    @Column("full_replace_cadence")
    @Nullable
    private String fullReplaceCadence;

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

    @Column("workspace_id")
    private @Nullable Long workspaceId;

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
